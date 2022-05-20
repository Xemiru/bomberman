package com.github.xemiru.mcbomberman.framework;

import com.github.xemiru.mcbomberman.framework.event.GameEvent;
import com.github.xemiru.mcbomberman.framework.event.PlayerJoinGameEvent;
import com.github.xemiru.mcbomberman.framework.event.PlayerQuitGameEvent;
import com.github.xemiru.mcbomberman.framework.event.PlayerTryJoinGameEvent;
import com.github.xemiru.mcbomberman.framework.exception.ModuleCallbackException;
import com.github.xemiru.mcbomberman.framework.exception.StateCallbackException;
import com.github.xemiru.mcbomberman.framework.exception.StateTransitionException;
import com.github.xemiru.mcbomberman.framework.module.GModule;
import com.github.xemiru.mcbomberman.framework.module.GameModule;
import com.github.xemiru.mcbomberman.framework.module.OptModule;
import com.github.xemiru.mcbomberman.framework.module.RegModule;
import com.github.xemiru.mcbomberman.util.Utility;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * A minigame.
 * <p/>
 * <h1>Modules</h1>
 * Modules can be considered the "backend" of minigames; they handle properties of a game that are required by several
 * states. They are enabled and disabled as necessary; a given module will not be enabled unless the current state
 * requires it. Persistent modules are an exception to the latter, and stay enabled for the lifetime of the Game.
 * <p/>
 * Refer to {@link GameModule} for more detail.
 * <p/>
 * <h1>GameStates</h1>
 * GameStates are the phases of a game that decide the immediate events and rules. A game may be comprised of multiple
 * states, each one establishing their own rules and event handlers.
 * <p/>
 * Refer to {@link GameState} for more detail.
 * <p/>
 * <h1>Setup</h1>
 * Games can be directly instantiated; they only need an instance of the plugin that owns them. The setup of a game
 * primarily involves setting the game's initial state and registering the participants of the game if dynamic joining
 * isn't allowed (see {@link #addPlayer(Player)}). The initial state can be set directly with
 * {@link #setState(GameState)}.
 * <p/>
 * Modules can be requested by {@link GameState}s themselves, but some modules may require instantiation prior to the
 * start of the game. Said modules can be registered with the game using {@link #addModule(GameModule, boolean)}.
 * <b>GameModules should not be registered this way during the runtime of the game;</b> see the linked method for
 * details.
 */
public class Game {

    private class RegisteredEvent {

        private final Class<? extends Event> eventType;
        private final EventHandler annot;
        private final Method getHandlerList, eventMethod;
        private Listener listener;

        public RegisteredEvent(Class<? extends Event> eventType, EventHandler annot, Method eventMethod) {
            try {
                this.eventType = eventType;
                this.annot = annot;

                this.getHandlerList = eventType.getDeclaredMethod("getHandlerList");
                this.eventMethod = eventMethod;

                this.getHandlerList.setAccessible(true);
                this.eventMethod.setAccessible(true);
            } catch (Exception e) {
                throw new IllegalArgumentException("Poorly-implemented event (missing getHandlerList static method).", e);
            }
        }

        public void register(Plugin plugin, GameState state) {
            register(plugin, (Object) state);
        }

        public void register(Plugin plugin, GameModule module) {
            register(plugin, (Object) module);
        }

        private void register(Plugin plugin, Object item) {
            this.listener = (Listener) item;
            Bukkit.getPluginManager().registerEvent(
                    this.eventType,
                    this.listener,
                    this.annot.priority(),
                    (listener, event) -> {
                        if (eventType.isInstance(event)) {
                            if (event instanceof GameEvent e) {
                                if (item instanceof GameState s)
                                    if (s.game != e.getGame()) return;

                                if (item instanceof GameModule s)
                                    if (s.getGame() != e.getGame()) return;
                            }

                            Game.this.withGameExceptionHandler(() -> {
                                try {
                                    eventMethod.invoke(item, event);
                                } catch (Exception e) {
                                    if (item instanceof GameState)
                                        throw new StateCallbackException("A state event handler raised an uncaught exception.", e);

                                    if (item instanceof GameModule)
                                        throw new ModuleCallbackException("A module event handler raised an uncaught exception.", e);
                                }
                            });
                        }
                    },
                    plugin,
                    this.annot.ignoreCancelled()
            );
        }

        public void unregister() {
            if (this.listener == null) return;

            try {
                ((HandlerList) this.getHandlerList.invoke(null)).unregister(this.listener);
            } catch (Exception e) {
                throw new IllegalStateException("Couldn't unregister event.", e);
            }
        }

    }

    private class RegisteredModule {
        public GameModule module;
        public boolean persistent;
        public boolean enabled = false;
        public List<RegisteredEvent> events = null;
    }

    private static final Consumer<Exception> defaultHandler = Utility::sneakyThrow;
    private static Set<Game> activeGames = new HashSet<>();

    private GameState state = null;
    private final List<RegisteredEvent> events = new LinkedList<>();
    private final Map<Class<? extends GameModule>, RegisteredModule> modules = new HashMap<>();

    private Consumer<Exception> exHandler = defaultHandler;
    private final Set<UUID> players = new HashSet<>();
    private int runnerTask = -1;
    private JavaPlugin plugin;

    private GameState toState = null;
    private boolean formalStateChange = false;
    private boolean changingState = false;

    public Game(@Nonnull JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Returns the number of players currently associated with this {@link Game}.
     */
    public int getPlayerCount() {
        this.checkPlayers();
        return this.players.size();
    }

    /**
     * Perform a function for each {@link Player} currently associated with this {@link Game}.
     *
     * @param block the function to perform
     */
    public void forEachPlayer(Consumer<Player> block) {
        this.checkPlayers();
        this.players.forEach(uid -> {
            var player = Bukkit.getPlayer(uid);
            if (player != null) block.accept(player);
        });
    }

    /**
     * Checks whether a {@link Player} is involved in this {@link Game}.
     *
     * @param player the UUID of the player to query
     * @return if the player is in this Game
     */
    public boolean containsPlayer(@Nonnull UUID player) {
        Objects.requireNonNull(player);
        return this.players.contains(player);
    }

    /**
     * Checks whether a {@link Player} is involved in this {@link Game}.
     *
     * @param player the player to query
     * @return if the player is in this Game
     */
    public boolean containsPlayer(Player player) {
        return this.containsPlayer(player.getUniqueId());
    }

    /**
     * Adds a {@link Player} to this {@link Game}.
     * <p/>
     * When the game is not running, this method always accepts players who haven't already been registered. Otherwise,
     * a {@link PlayerTryJoinGameEvent} is emitted to ask the current state and/or its modules whether it will accept the
     * new player.
     * <p/>
     * On successful registration, a running game will be notified of a player's arrival with a
     * {@link PlayerJoinGameEvent}.
     *
     * @param player the Player to add
     * @return if the Player was accepted into the game
     */
    public boolean addPlayer(@Nonnull Player player) {
        Objects.requireNonNull(player);
        var uid = player.getUniqueId();
        if (this.isRunning()) {
            if (players.contains(uid)) return false;
            var event = new PlayerTryJoinGameEvent(this, player);

            Bukkit.getPluginManager().callEvent(event);
            if (event.result == PlayerTryJoinGameEvent.Result.DENY) return false;
        }

        if (!players.add(uid)) return false;
        if (this.isRunning()) Bukkit.getPluginManager().callEvent(new PlayerJoinGameEvent(this, player));
        return true;
    }

    /**
     * Removes a {@link Player} from this {@link Game}.
     * <p/>
     * When the game is running, a {@link PlayerQuitGameEvent} notifies the current state and/or its modules of the
     * player's departure.
     *
     * @param player the Player to remove
     */
    public void removePlayer(@Nonnull Player player) {
        Objects.requireNonNull(player);
        var uid = player.getUniqueId();
        if (!players.contains(uid)) return;

        if (this.isRunning()) {
            var event = new PlayerQuitGameEvent(this, player);
            Bukkit.getPluginManager().callEvent(event);
        }

        players.remove(player.getUniqueId());
    }

    /**
     * @return the current active GameState
     */
    public GameState getState() {
        return this.state;
    }

    /**
     * Sets this {@link Game}'s current state.
     * <p/>
     * State changes will <b>not occur immediately;</b> the transition begins once the current state and all loaded
     * modules have finished their current tick.
     *
     * @param state  the state to go to
     * @param formal if the state change was intended
     */
    public void setState(GameState state, boolean formal) {
        if (this.isRunning()) {
            this.changingState = true;
            this.toState = state;
            this.formalStateChange = formal;
        } else {
            this.state = state;
        }
    }

    /**
     * Formally sets this {@link Game}'s current state.
     * <p/>
     * State changes will <b>not occur immediately;</b> the transition begins once the current state and all loaded
     * modules have finished their current tick.
     *
     * @param state the state to go to
     * @see #setState(GameState, boolean)
     */
    public void setState(GameState state) {
        this.setState(state, true);
    }

    /**
     * Set the default exception handler for exceptions that occur inside {@link GameState}s' listener and callback
     * methods.
     * <p/>
     * By default, the exception is rethrown and acts similarly to an exception that encounters neither this handler nor
     * the GameState's.
     *
     * @param e the exception handler
     */
    public void setUncaughtExceptionHandler(@Nonnull Consumer<Exception> e) {
        Objects.requireNonNull(e);
        this.exHandler = e;
    }

    /**
     * Starts the game, if it isn't already running.
     * <p/>
     * The current game state, if one is active, will be initialized and begin properly receiving events.
     * <p/>
     * Exceptions encountered during startup are passed to the game's exception handler.
     */
    public void start() {
        if (!this.isRunning()) {
            try {
                this.checkPlayers();
                this.enableState(this.state);
                activeGames.add(this);
                this.runnerTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(this.plugin, () -> {
                    this.modules.values().forEach(mod -> {
                        if (!mod.enabled) return;
                        withGameExceptionHandler(() -> withModuleCallback(mod.module::tick));
                    });

                    if (this.state != null) {
                        withGameExceptionHandler(() -> withStateCallback(state::tick));
                    }

                    if (this.changingState) this.changeRunningState();
                }, 0, 1);
            } catch (Exception e) {
                exHandler.accept(e);
            }
        }
    }

    /**
     * Stops the game, if it's running.
     * <p/>
     * The current game state, if one is active, will formally be disabled and removed from the game.
     * <p/>
     * Exceptions encountered during shutdown are directly propagated by this method.
     */
    public void stop() {
        if (this.isRunning()) {
            Bukkit.getScheduler().cancelTask(this.runnerTask);

            try {
                // TODO more graceful exit?
                this.disableState(true, true, true);
            } finally {
                this.state = null;
                activeGames.remove(this);
            }
        }
    }

    /**
     * @return if this Game is running
     */
    public boolean isRunning() {
        return this.runnerTask >= 0;
    }

    /**
     * Adds a {@link GameModule} to this {@link Game}.
     * <p/>
     * Calling this method manually during the runtime of a game is not recommended; the module will properly go through
     * registration, but <b>activation doesn't begin until the transition to the next {@link GameState}.</b> This also
     * applies to persistent modules.
     *
     * @param module the GameModule to register
     * @param persistent if this module will be persistent
     * @return if the module was successfully registered
     */
    public boolean addModule(GameModule module, boolean persistent) {
        if (modules.containsKey(module.getClass())) return false;
        var regmod = new RegisteredModule();
        regmod.module = module;
        regmod.persistent = persistent;
        modules.put(module.getClass(), regmod);
        module.register(this, persistent);

        return true;
    }

    /**
     * Retrieve a potentially-registered {@link GameModule}.
     *
     * @param type the type of the module to retrieve
     * @return an Optional with the GameModule, or empty if not present
     */
    public Optional<GameModule> getModule(Class<? extends GameModule> type) {
        return Optional.ofNullable(this.modules.get(type)).map(it -> it.module);
    }

    // ---
    //
    // internal
    //
    // ---

    private void changeRunningState() {
        withGameExceptionHandler(() -> {
            this.disableState(formalStateChange, toState == null, false);
            this.state = null;
            if (toState == null) return;
            this.enableState(toState);
        });

        this.changingState = false;
    }

    private Class<? extends GameModule> checkModuleField(Field field) {
        try {
            var type = field.getType();
            if (type == Optional.class) {
                var str = field.getGenericType().getTypeName();
                str = str.substring(str.indexOf('<') + 1, str.indexOf('>'));
                type = Class.forName(str);
            }

            if (GameModule.class.isAssignableFrom(type)) return (Class<? extends GameModule>) type;
            throw new IllegalArgumentException("Field does not hold a module type");
        } catch (Exception e) {
            Utility.sneakyThrow(e);
            return null; // impossible
        }
    }

    private Set<RegisteredModule> prepareModules(GameState state) {
        var stateType = state.getClass();
        var modules = new HashSet<RegisteredModule>();
        for (Field field : stateType.getDeclaredFields()) {
            Object obj = null;
            if (field.isAnnotationPresent(RegModule.class)) {
                var annot = field.getAnnotation(RegModule.class);
                var type = checkModuleField(field);
                var mod = this.modules.computeIfAbsent(type, t -> {
                    try {
                        var module = type.getConstructor().newInstance();
                        module.register(this, annot.persistent());
                        var regModule = new RegisteredModule();
                        regModule.module = module;
                        regModule.persistent = annot.persistent();
                        return regModule;
                    } catch (Exception e) {
                        throw new IllegalStateException(String.format("Couldn't instantiate module %s (failed to call public no-args constructor).", type), e);
                    }
                });

                mod.module.reset();
                obj = mod.module;
                modules.add(mod);
            } else if (field.isAnnotationPresent(GModule.class)) {
                var type = checkModuleField(field);

                if (!this.modules.containsKey(type))
                    throw new IllegalStateException(String.format("Couldn't provide module %s (not initialized, try @RegModule?).", type));

                var rmod = this.modules.get(type);
                obj = rmod.module;
                modules.add(rmod);
            } else if (field.isAnnotationPresent(OptModule.class)) {
                var type = checkModuleField(field);
                var rmod = this.modules.get(type);
                obj = Optional.ofNullable(rmod).map(it -> it.module);
            }

            if (obj != null) {
                field.setAccessible(true);
                try {
                    field.set(state, obj);
                } catch (Exception e) {
                    Utility.sneakyThrow(e);
                }
            }
        }

        return modules;
    }

    private <T extends Listener> List<RegisteredEvent> prepareEvents(Class<T> listener) {
        List<RegisteredEvent> events = new LinkedList<>();
        for (Method method : listener.getDeclaredMethods()) {
            EventHandler annot = method.getDeclaredAnnotation(EventHandler.class);
            if (annot == null) continue;

            if (method.getParameterCount() != 1)
                throw new IllegalArgumentException(String.format("Listener implements an invalid event handler (%s takes more or less than 1 argument).", method.getName()));

            Class<?> eventType = method.getParameters()[0].getType();
            if (!Event.class.isAssignableFrom(eventType))
                throw new IllegalArgumentException(String.format("Listener implements an invalid event handler (%s's event argument is not Bukkit event).", method.getName()));

            events.add(new RegisteredEvent((Class<? extends Event>) eventType, annot, method));
        }

        return events;
    }

    private void enableState(GameState state) {
        if (state != null) {
            var type = state.getClass();
            Set<RegisteredModule> modules;
            try {
                modules = prepareModules(state);
            } catch (Exception e) {
                throw new StateTransitionException("Failed to inject modules.", e);
            }

            // find all the modules to be disabled
            Set<RegisteredModule> disabled = this.modules.values().stream()
                    .filter(mod -> mod.enabled && !mod.persistent && !modules.contains(mod))
                    .collect(Collectors.toSet());

            withModuleCallback(() -> disabled.forEach(mod -> mod.module.predisable()));
            disabled.forEach(mod -> {
                try {
                    mod.events.forEach(RegisteredEvent::unregister);
                } catch (Exception e) {
                    throw new StateTransitionException("Failed to unregister module events.", e);
                } finally {
                    withModuleCallback(() -> {
                        mod.module.disable();
                        mod.enabled = false;
                    });
                }
            });

            // prepare required modules' events
            modules.forEach(mod -> {
                // do this only once; module events won't change
                if (mod.events == null) {
                    try {
                        mod.events = prepareEvents(mod.module.getClass());
                    } catch (Exception e) {
                        throw new StateTransitionException("Failed to collect module events.", e);
                    }
                }
            });

            // enable required modules
            modules.forEach(mod -> {
                if (!mod.enabled) {
                    try {
                        mod.events.forEach(it -> it.register(this.plugin, mod.module));
                    } catch (Exception e) {
                        throw new StateTransitionException("Failed to register module events.", e);
                    }

                    withModuleCallback(mod.module::init);
                    mod.enabled = true;
                }
            });

            withModuleCallback(() -> modules.forEach(mod -> mod.module.postinit()));

            // register state events

            try {
                prepareEvents(type).forEach(e -> {
                    e.register(this.plugin, state);
                    this.events.add(e);
                });
            } catch (Exception e) {
                throw new StateTransitionException("Failed to register state events.", e);
            }

            state.game = this;
            withStateCallback(state::onInit);

            this.state = state;
        }
    }

    private void disableState(boolean formal, boolean disableModules, boolean disablePersistentModules) {
        if (this.state != null) {
            this.events.forEach(RegisteredEvent::unregister);
            this.events.clear();
            withStateCallback(() -> this.state.onExit(formal));

            this.state.game = null;
        }

        if (disableModules) {
            this.modules.values().forEach(m -> {
                if (!m.enabled) return;
                if (m.persistent && !disablePersistentModules) return;
                withModuleCallback(() -> m.module.predisable());
            });

            this.modules.values().forEach(m -> {
                if (!m.enabled) return;
                if (m.persistent && !disablePersistentModules) return;
                try {
                    m.events.forEach(RegisteredEvent::unregister);
                } catch (Exception e) {
                    throw new StateTransitionException("Failed to unregister module events.", e);
                } finally {
                    withModuleCallback(() -> {
                        m.module.disable();
                        m.enabled = false;
                    });
                }
            });
        }
    }

    private void checkPlayers() {
        this.players.removeIf(uid -> Bukkit.getPlayer(uid) == null);
    }

    private void withGameExceptionHandler(Runnable r) {
        try {
            r.run();
        } catch (Exception e) {
            exHandler.accept(e);
        }
    }

    private void withStateCallback(Runnable r) {
        try {
            r.run();
        } catch (Exception e) {
            throw new StateCallbackException("State callback raised exception.", e);
        }
    }

    private void withModuleCallback(Runnable r) {
        try {
            r.run();
        } catch (Exception e) {
            throw new ModuleCallbackException("Module callback raised exception.", e);
        }
    }

}
