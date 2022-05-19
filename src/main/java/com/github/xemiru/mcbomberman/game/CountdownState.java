package com.github.xemiru.mcbomberman.game;

import com.github.xemiru.mcbomberman.framework.GameState;
import com.github.xemiru.mcbomberman.framework.module.RegModule;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffectType;

import java.util.Stack;
import java.util.concurrent.ThreadLocalRandom;

public class CountdownState extends GameState {

    @RegModule
    BombermanModule bomberman;

    private int countdown = 20 * 5;

    @Override
    protected void onInit() {
        var world = Bukkit.getWorlds().get(0);

        // generate arena string
        var arenaString = "" +
                "                   " +
                " # # # # # # # # # " +
                "                   " +
                " # # # # # # # # # " +
                "                   " +
                " # # # # # # # # # " +
                "                   " +
                " # # # # # # # # # " +
                "                   " +
                " # # # # # # # # # " +
                "                   " +
                " # # # # # # # # # " +
                "                   " +
                " # # # # # # # # # " +
                "                   " +
                " # # # # # # # # # " +
                "                   " +
                " # # # # # # # # # " +
                "                   ";

        var arenaStringCh = arenaString.toCharArray();
        for (int i = 0; i < arenaString.length(); i++) {
            char ch = arenaStringCh[i];
            if (ch == ' ')
                if (ThreadLocalRandom.current().nextBoolean()) arenaStringCh[i] = 'X';
        }

        // make the arena
        var arena = bomberman.resetArena(
                19,
                19,
                new Location(world, 0, 192, 0),
                Material.BEDROCK,
                Material.POLISHED_ANDESITE,
                Material.OAK_WOOD,
                String.valueOf(arenaStringCh));

        var spawnPoints = new Stack<Location>();
        var mapCenter = arena.getTileCenter(10, 10).toVector();
        spawnPoints.add(arena.getNorthwestCornerSpawn());
        spawnPoints.add(arena.getNortheastCornerSpawn());
        spawnPoints.add(arena.getSouthwestCornerSpawn());
        spawnPoints.add(arena.getSoutheastCornerSpawn());
        bomberman.setAllowActions(false);
        game.forEachPlayer(p -> {
            if (spawnPoints.empty()) return;
            var spawn = spawnPoints.pop();
            var lookDir = mapCenter.clone().subtract(p.getLocation().toVector()).normalize();
            spawn.setDirection(lookDir);
            p.teleport(spawn);
            bomberman.registerPlayer(p);
            p.removePotionEffect(PotionEffectType.JUMP);
            p.setWalkSpeed(0);
        });

        System.out.println("cd init");
    }

    @Override
    protected void tick() {
        if (countdown <= 0) {
            this.game.setState(new PlayState());
            this.game.forEachPlayer(p -> {
                p.sendTitle("GO!", "", 0, 30, 10);
            });
        } else if (countdown % 20 == 0) {
            this.game.forEachPlayer(p -> {
                int sec = countdown / 20;
                p.sendTitle("" + sec, "", 0, 20, 20);
            });
        }

        countdown--;
    }

    @Override
    protected void onExit(boolean formal) {

    }

}
