package com.github.xemiru.mcbomberman;

import com.github.xemiru.mcbomberman.framework.Game;
import com.github.xemiru.mcbomberman.game.CountdownState;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
    }

    @EventHandler
    public void onChat(PlayerChatEvent e) {
        if(!e.getMessage().equalsIgnoreCase("start game")) return;

        var game = new Game(this);
        game.setState(new CountdownState());
        Bukkit.getOnlinePlayers().forEach(game::addPlayer);

        game.start();
    }

}
