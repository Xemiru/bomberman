package com.github.xemiru.mcbomberman.framework.module;

import com.github.xemiru.mcbomberman.framework.Game;
import com.github.xemiru.mcbomberman.framework.GameState;
import com.github.xemiru.mcbomberman.framework.exception.ModuleCallbackException;
import org.bukkit.event.Listener;

/**
 * A module governing certain systems within a {@link Game}.
 * <p/>
 *
 * <h1>Exceptions</h1>
 * Uncaught exceptions for listener methods and abstract methods implemented by a {@link GameModule} will be wrapped in
 * a {@link ModuleCallbackException} and passed to the host Game's exception handler.
 * <p/>
 *
 * <h1>Lifecycle</h1>
 * Upon first being registered, the module's {@link #register(Game, boolean)} method is called. Implementing classes
 * must have a no-args public constructor unless they are attached to the game before the initialization of any states
 * through {@link Game#addModule(GameModule, boolean)}. Modules are otherwise registered and attached to a game when a
 * {@link GameState} who holds a field annotated with @{@link RegModule} is encountered.
 * <p>
 * A state boundary is crossed when transitioning between states. When encountering a state boundary, a module may
 * progress through its lifecycle.
 * <ul>
 *     <li>If the module is not currently active, it will go through initialization by receiving calls to
 *     {@link #init()} and {@link #postinit()}. Modules who were active before the state boundary was reached will only
 *     receive a call to {@link #postinit()}.</li>
 *     <li>Modules themselves are also {@link Listener}s, and will have their event handlers registered after the call
 *     to {@link #postinit()}.</li>
 *     <li>If the state-to-be marked the module with @{@link RegModule}, the module will also receive a call to
 *     {@link #reset()} after initialization.</li>
 *     <li>If the module is already active, it will go through disabling by receiving calls to {@link #predisable()} and
 *     {@link #disable()}.</li>
 * </ul>
 * <p>
 * To determine whether a module will be initialized or disabled when crossing a state boundary, the next game state is
 * checked.
 * <ul>
 *     <li>The game state must have fields annotated with @{@link GModule}, @{@link RegModule}, or @{@link OptModule}
 *     holding the module type to be used.</li>
 *     <li>If the next game state does not use the module (it lacks an annotated field with the module's type), the
 *     module will be disabled before the state's {@link GameState#onInit()} call.</li>
 * </ul>
 * <p>
 * An exception to these rules is a module registered with {@link RegModule#persistent()} set to true. Persistent
 * modules..
 * <ul>
 *     <li>.. are initialized only once.</li>
 *     <li>.. will never encounter calls to {@link #predisable()} or {@link #disable()} until the host Game gracefully
 *     shuts down.</li>
 *     <li>.. will still receive calls to {@link #reset()} when re-encountering their @{@link RegModule} tag.</li>
 *     <li>.. will know if they're persistent in the call to {@link #register(Game, boolean)}.</li>
 * </ul>
 */
public interface GameModule extends Listener {

    /**
     * Called when this module is first registered into a {@link Game}.
     * <p/>
     * Note that <b>the game might not be running</b> when this method is called.
     * <p/>
     * {@link #getGame()} must return the last Game instance passed to this method.
     *
     * @param game the host Game that this module is being bound to
     * @param persistent if the module was registered as persistent
     */
    void register(Game game, boolean persistent);

    /**
     * Returns the {@link Game} that this {@link GameModule} was registered with.
     * <p/>
     * This method must return the last Game instance that was provided to {@link #register(Game, boolean)}.
     */
    Game getGame();

    /**
     * Called when this module is initializing.
     * <p/>
     * See {@link GameModule} to learn about lifecycle events.
     */
    void init();

    /**
     * Called when this module makes it through a state boundary.
     * <p/>
     * See {@link GameModule} to learn about lifecycle events.
     */
    default void postinit() {}

    /**
     * Called on every tick while this {@link GameModule} is enabled.
     */
    default void tick() {}

    /**
     * Called when this module is scheduled to be disabled.
     * <p/>
     * This is the last point of communication between modules before being disabled. All modules scheduled to be
     * disabled will receive a call to this method before any call to {@link #disable()}.
     * <p/>
     * See {@link GameModule} to learn about lifecycle events.
     */
    default void predisable() {}

    /**
     * Called when this module is disabled.
     * <p/>
     * See {@link GameModule} to learn about lifecycle events.
     */
    void disable();

    /**
     * Called when this module is reset.
     * <p/>
     * See {@link GameModule} to learn about lifecycle events.
     */
    default void reset() {}

}
