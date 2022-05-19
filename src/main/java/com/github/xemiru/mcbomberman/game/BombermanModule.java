package com.github.xemiru.mcbomberman.game;

import com.github.xemiru.mcbomberman.framework.Game;
import com.github.xemiru.mcbomberman.framework.module.GameModule;
import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class BombermanModule implements GameModule {

    public static final int BOMB_FUSE_TICKS = 20 * 3;

    private Game game;
    private boolean allowActions = false;
    private Map<UUID, BombermanPlayer> players = new HashMap<>();
    private Map<UUID, Bomb> bombs = new HashMap<>();
    private BombermanGrid grid;

    private class Bomb {
        public UUID owner;
        public int power, x, y, fuse;
    }

    @Override
    public void register(Game game, boolean persistent) {
        this.game = game;
    }

    @Override
    public Game getGame() {
        return this.game;
    }

    @Override
    public void init() {
    }

    @Override
    public void disable() {
        this.reset();
    }

    @Override
    public void reset() {
        this.players.clear();
        this.clearBombs();

        this.allowActions = false;
        if (this.grid != null) grid.destroy();
        this.grid = null;
        System.out.println("bomb mod reset");
    }

    @Override
    public void tick() {
        // disable jumping for each living player
        this.game.forEachPlayer(p -> {
            getPlayer(p).ifPresent(bp -> {
                p.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 20 * 30, -10, true));
                if (bp.bombCd > 0)
                    bp.bombCd--;
            });
        });

        // tick down bombs
        bombs.keySet().removeIf(uid -> {
            var bomb = bombs.get(uid);
            bomb.fuse--;

            if (bomb.fuse <= 0) {
                var ent = Bukkit.getServer().getEntity(uid);
                if (ent != null) ent.remove();

                var owner = Bukkit.getPlayer(bomb.owner);
                var affected = grid.bomb(bomb.x, bomb.y, bomb.power);
                if (owner != null)
                    getPlayer(owner).ifPresent(bp -> {
                        bp.placedBombs--;
                        bp.applyToPlayer(owner);
                    });

                for (var auid : affected) {
                    var aent = Bukkit.getEntity(auid);
                    if (aent instanceof Item) aent.remove();
                    if (aent instanceof Player p) {
                        getPlayer(p).ifPresent(bp -> {
                            var oName = owner == null ? "<???>" : owner.getName();
                            printf("%s was eliminated by %s's bomb", p.getName(), oName);
                            players.remove(p.getUniqueId());
                            p.setGameMode(GameMode.SPECTATOR);
                        });
                    }
                }

                return true;
            }

            return false;
        });
    }

    public void clearBombs() {
        bombs.keySet().forEach(bombUid -> {
            var ent = Bukkit.getEntity(bombUid);
            if (ent != null) ent.remove();
        });
    }

    public void registerPlayer(Player player) {
        var bp = new BombermanPlayer();
        players.put(player.getUniqueId(), bp);
        player.getInventory().setHeldItemSlot(0);
        bp.applyToPlayer(player);
    }

    public boolean isAllowingActions() {
        return this.allowActions;
    }

    public void setAllowActions(boolean flag) {
        this.allowActions = flag;
    }

    public Optional<BombermanPlayer> getPlayer(Player player) {
        return Optional.ofNullable(this.players.get(player.getUniqueId()));
    }

    public int getLivingPlayers() {
        return this.players.size();
    }

    public BombermanGrid resetArena(
            int width, int height,
            Location nwCorner,
            Material floorTile,
            Material wallTile,
            Material softWallTile,
            String init
    ) {
        if (this.grid != null) this.grid.destroy();
        this.grid = new BombermanGrid(width, height, nwCorner, floorTile, wallTile, softWallTile, init);
        this.grid.render();
        return this.grid;
    }

    public BombermanGrid getArena() {
        return this.grid;
    }

    // shouldnt swap from first slot
    @EventHandler
    public void onHotbarSwap(PlayerItemHeldEvent e) {
        if (!game.containsPlayer(e.getPlayer())) return;
        if (e.getNewSlot() != 0) e.setCancelled(true);
    }

    // dropping items isnt allowed
    @EventHandler
    public void onDropItem(PlayerDropItemEvent e) {
        if (!game.containsPlayer(e.getPlayer())) return;
        e.setCancelled(true);
    }

    // powerups
    @EventHandler
    public void onPickupItem(EntityPickupItemEvent e) {
        if (e.getEntity() instanceof Player p) {
            if (!game.containsPlayer(p)) return;
            e.setCancelled(true);
            getPlayer(p).ifPresent(bp -> {
                bp.addPowerup(e.getItem());
                bp.applyToPlayer(p);

                e.getItem().remove();
            });
        }
    }

    @EventHandler
    public void onTryInteract(PlayerInteractEvent e) {
        // disable rclick interactions
        if (!game.containsPlayer(e.getPlayer())) return;
        e.setUseInteractedBlock(Event.Result.DENY);
        e.setUseItemInHand(Event.Result.DENY);
        e.setCancelled(true);

        // bomb placement
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (!this.isAllowingActions()) return;
            getPlayer(e.getPlayer()).ifPresent(bp -> {
                if (bp.placedBombs >= bp.bombs) return; // can't place any more bombs
                if (bp.bombCd > 0) return; // wait for cooldown

                var bloc = e.getClickedBlock().getRelative(e.getBlockFace()).getLocation();
                var loc = grid.getTilePositionOfBlock(bloc);
                var bomb = new Bomb();
                bomb.x = loc[0];
                bomb.y = loc[1];
                bomb.owner = e.getPlayer().getUniqueId();
                bomb.power = bp.power;
                bomb.fuse = BOMB_FUSE_TICKS;

                TNTPrimed ent = (TNTPrimed) bloc.getWorld().spawnEntity(grid.getTileCenter(bomb.x, bomb.y), EntityType.PRIMED_TNT, false);
                bloc.getWorld().playSound(ent, Sound.BLOCK_GRASS_PLACE, 1f, 1f);
                ent.setSource(e.getPlayer());
                ent.setFuseTicks(Integer.MAX_VALUE);
                ent.setVelocity(new Vector(0, 0.5, 0));
                bombs.put(ent.getUniqueId(), bomb);
                bp.placedBombs++;
                bp.applyToPlayer(e.getPlayer());
                bp.bombCd = 10;
            });
        }
    }

    // no placing blocks
    public void onTryPlace(BlockPlaceEvent e) {
        if (!game.containsPlayer(e.getPlayer())) return;
        e.setBuild(false);
        e.setCancelled(true);
    }

    public static void print(String message) {
        Bukkit.getServer().broadcastMessage(message);
    }

    public static void printf(String format, Object... args) {
        print(String.format(format, args));
    }

}
