package com.github.xemiru.mcbomberman.game;

import com.github.xemiru.mcbomberman.framework.GameState;
import com.github.xemiru.mcbomberman.framework.module.GModule;

public class WinState extends GameState {

    @GModule
    BombermanModule bomberman;

    private int ticks;

    @Override
    protected void onInit() {
        this.ticks = 0;
        StringBuilder winners = new StringBuilder();
        this.game.forEachPlayer(p -> {
            bomberman.getPlayer(p).ifPresent(bp -> {
                if (winners.length() > 0) winners.append(", ");
                winners.append(p.getName());
            });
        });

        winners.append(" wins!");
        this.game.forEachPlayer(p -> {
            p.sendTitle("Game over!", winners.toString(), 0, 90, 10);
        });

        bomberman.setAllowActions(false);
        bomberman.clearBombs();
    }

    @Override
    protected void tick() {
        ticks++;
        if (ticks > 20 * 10) {
            game.stop();
        }
    }

    @Override
    protected void onExit(boolean formal) {

    }

}
