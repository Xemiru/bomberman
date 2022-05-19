package com.github.xemiru.mcbomberman.game;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class BombermanPlayer {

    public static final float MAX_WALKSPEED = 0.25f;
    public static final float MIN_WALKSPEED = 0.15f;
    public static final float WALKSPEED_INC = (MAX_WALKSPEED - MIN_WALKSPEED) / 7f;

    public int speed = 1;
    public int power = 1;
    public int bombs = 1;
    public int placedBombs = 0;
    public int bombCd = 0;

    public void applyToPlayer(Player player) {
        // slot 1: place bomb
        // slot 8: power
        // slot 9: speed

        var bombItem = placedBombs < bombs ? new ItemStack(Material.TNT, bombs - placedBombs) : null;
        player.getInventory().clear();
        player.getInventory().setItem(0, bombItem);
        player.getInventory().setItem(7, new ItemStack(Material.FLINT_AND_STEEL, power));
        player.getInventory().setItem(8, new ItemStack(Material.LEATHER_BOOTS, speed));
        Bukkit.getScheduler().scheduleSyncDelayedTask(Bukkit.getPluginManager().getPlugin("bomberman"), player::updateInventory);

        player.setWalkSpeed(this.getWalkSpeed());
    }

    public boolean addPowerup(Item item) {
        var type = item.getItemStack().getType();
        switch (type) {
            case FLINT_AND_STEEL -> power += 1;
            case LEATHER_BOOTS -> speed += 1;
            case TNT -> bombs += 1;
            default -> {
                return false;
            }
        }

        fixStats();
        return true;
    }

    public float getWalkSpeed() {
        if (this.speed == 1) return MIN_WALKSPEED;
        if (this.speed == 8) return MAX_WALKSPEED;
        return MIN_WALKSPEED + ((speed - 1) * WALKSPEED_INC);
    }

    private void fixStats() {
        this.speed = Math.min(this.speed, 8);
        this.power = Math.min(this.power, 8);
        this.bombs = Math.min(this.bombs, 8);
    }

}
