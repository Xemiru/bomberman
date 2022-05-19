package com.github.xemiru.mcbomberman.framework.event;

import com.github.xemiru.mcbomberman.framework.Game;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

import javax.annotation.Nonnull;
import java.util.Objects;

public class PlayerTryJoinGameEvent extends GameEvent {

    public enum Result {
        /**
         * The player is allowed to join the game.
         */
        ALLOW,
        /**
         * The player is not allowed to join the game.
         */
        DENY
    }

    private static HandlerList handlerList = new HandlerList();

    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }

    public static HandlerList getHandlerList() {
        return handlerList;
    }

    public Result result = Result.DENY;
    private Player player;

    public PlayerTryJoinGameEvent(Game game, Player player) {
        super(game);
        this.player = player;
    }

    public Player getPlayer() {
        return this.player;
    }

    public Result getResult() {
        return this.result;
    }

    public void setResult(@Nonnull Result result) {
        this.result = Objects.requireNonNull(result);
    }

}
