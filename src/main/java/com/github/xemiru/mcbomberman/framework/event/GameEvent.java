package com.github.xemiru.mcbomberman.framework.event;

import com.github.xemiru.mcbomberman.framework.Game;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class GameEvent extends Event {

    private static HandlerList handlerList = new HandlerList();

    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }

    public static HandlerList getHandlerList() {
        return handlerList;
    }

    private Game game;

    public GameEvent(Game game) {
        this.game = game;
    }

    public Game getGame() {
        return this.game;
    }

}
