package com.github.xemiru.mcbomberman.framework.event;

import com.github.xemiru.mcbomberman.framework.Game;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

public class PlayerJoinGameEvent extends GameEvent {

    private static HandlerList handlerList = new HandlerList();

    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }

    public static HandlerList getHandlerList() {
        return handlerList;
    }

    private Player player;

    public PlayerJoinGameEvent(Game game, Player player) {
        super(game);
        this.player = player;
    }

    public Player getPlayer() {
        return this.player;
    }

}
