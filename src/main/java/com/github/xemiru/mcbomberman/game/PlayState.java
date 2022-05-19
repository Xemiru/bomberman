package com.github.xemiru.mcbomberman.game;

import com.github.xemiru.mcbomberman.framework.GameState;
import com.github.xemiru.mcbomberman.framework.module.GModule;

public class PlayState extends GameState {

    @GModule
    BombermanModule bomberman;

    @Override
    protected void onInit() {
        game.forEachPlayer(p -> {
            bomberman.getPlayer(p).ifPresent(bp -> {
                bp.applyToPlayer(p);
            });
        });

        bomberman.setAllowActions(true);
    }

    @Override
    protected void tick() {
        /* if (bomberman.getLivingPlayers() <= 1) {
            game.setState(new WinState());
        } */
    }

    @Override
    protected void onExit(boolean formal) {

    }
}
