package com.github.xemiru.mcbomberman.framework;

import com.github.xemiru.mcbomberman.framework.exception.StateCallbackException;
import com.github.xemiru.mcbomberman.util.Utility;
import org.bukkit.event.Listener;

/**
 * A state of a {@link Game}.
 * <p/>
 * Game states determine what happens in response to several events over the course of a game. They themselves are
 * {@link Listener}s overseen by a Game; their events will always be registered when they become the current state and
 * unregistered when they are no longer the current state.
 * <p/>
 *
 * <h1>Exceptions</h1>
 * Uncaught exceptions for listener methods and abstract methods implemented by a {@link GameState} will be wrapped in
 * a {@link StateCallbackException} and passed to the host Game's exception handler.
 * <p/>
 *
 * <h1>Lifecycle</h1>
 * The entry point for any game state is the {@link #onInit()} method. It is called <b>right before it becomes the
 * current state, but after the previous state has been released</b>.
 * <p/>
 * It is after this point that the state becomes properly active as the game's current state. Listeners are ready to
 * intercept events, but one should take care to note that <b>events are unfiltered; they are in all forms standard
 * Bukkit event handlers.</b>
 * <p/>
 * When the game is ready to remove the state, {@link #onExit(boolean)} is called to notify it. It is called before
 * the state is unregistered, that is, <b>it is still the current state of the associated Game during the runtime of the
 * method.</b>
 */
public abstract class GameState implements Listener {

    /**
     * The {@link Game} that currently owns this state.
     */
    protected Game game;

    /**
     * Called when this state is about to become a {@link Game}'s current state.
     * <p/>
     * When this method is called, {@link #game} to the game trying to claim this state.
     */
    protected abstract void onInit();

    /**
     * Called when this state is about to be removed as a {@link Game}'s current state.
     *
     * @param formal whether the exit was intended
     */
    protected abstract void onExit(boolean formal);

    /**
     * Called on every tick where this state is active.
     */
    protected void tick() {}

}
