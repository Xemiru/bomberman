package com.github.xemiru.mcbomberman.game;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class BombermanGrid {

    public enum Space {
        /**
         * Regular bombs can't pass through this space type.
         * <p/>
         * Represented as # in initialization strings.
         */
        SOLID('#'),
        /**
         * Fire will break this space type and occupy it.
         * <p/>
         * Represented as # in initialization strings.
         */
        SOFT('X'),
        /**
         * Nothing is in this space.
         * <p/>
         * Represented as the space character (' ') in initialization strings.
         */
        EMPTY(' ');

        public final char rep;

        Space(char rep) {
            this.rep = rep;
        }

        public static Space getSpace(char rep) {
            for (var space : Space.values()) {
                if (space.rep == rep) return space;
            }

            throw new IllegalArgumentException("Unknown space representation \"" + rep + "\"");
        }
    }

    public class Bomb {
        int power, timer;
    }

    private Space[][] grid;
    private Location nwCorner;
    private Location nwSpaceCorner;

    private int width, height;
    private Material floorTile, wallTile, softWallTile;

    public BombermanGrid(int width, int height, Location nwCorner, Material floorTile, Material wallTile, Material softWallTile, String init) {
        if (init.length() < width * height)
            throw new IllegalArgumentException("Initialization string does not have enough characters for the grid size.");

        this.width = width;
        this.height = height;
        this.softWallTile = softWallTile;
        this.floorTile = floorTile;
        this.wallTile = wallTile;

        this.nwCorner = nwCorner;
        this.nwSpaceCorner = nwCorner.clone();
        nwSpaceCorner.add(1, 1, 1);

        this.grid = new Space[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                var index = (y * height) + x;
                this.grid[y][x] = Space.getSpace(init.charAt(index));
            }
        }
    }

    public void render() {
        // clear space
        this.destroy();

        // nwcorner is the nw floor tile
        var world = nwCorner.getWorld();
        var nwx = nwCorner.getBlockX();
        var nwy = nwCorner.getBlockY();
        var nwz = nwCorner.getBlockZ();

        // draw the walls
        for (int y = 0; y < 3; y++) {
            // corners
            world.setType(nwx, nwy + y, nwz, wallTile);
            world.setType(nwx + 1 + (width * 2), nwy + y, nwz, wallTile);
            world.setType(nwx, nwy + y, nwz + 1 + (height * 2), wallTile);
            world.setType(nwx + 1 + (width * 2), nwy + y, nwz + 1 + (height * 2), wallTile);

            for (int i = 0; i < width * 2; i++) {
                // east wall
                world.setType(
                        nwx + 1 + i,
                        nwy + y,
                        nwz,
                        wallTile);

                // west wall
                world.setType(
                        nwx + 1 + i,
                        nwy + y,
                        nwz + 1 + (height * 2),
                        wallTile);
            }

            for (int i = 0; i < height * 2; i++) {
                // south wall
                world.setType(
                        nwx,
                        nwy + y,
                        nwz + 1 + i,
                        wallTile);

                // north wall
                world.setType(
                        nwx + 1 + (width * 2),
                        nwy + y,
                        nwz + 1 + i,
                        wallTile);
            }

        }

        // draw the floor
        for (int z = 0; z < height * 2; z++)
            for (int x = 0; x < width * 2; x++)
                world.setType(nwx + 1 + x, nwy, nwz + 1 + z, floorTile);

        // draw the grid objects
        for (int z = 0; z < height; z++)
            for (int x = 0; x < width; x++)
                setSpace(x, z, grid[z][x]);

        // ensure spawn locations are free
        setSpace(1, 0, Space.EMPTY);
        setSpace(0, 0, Space.EMPTY);
        setSpace(0, 1, Space.EMPTY);

        setSpace(width - 2, 0, Space.EMPTY);
        setSpace(width - 1, 0, Space.EMPTY);
        setSpace(width - 1, 1, Space.EMPTY);

        setSpace(0, height - 2, Space.EMPTY);
        setSpace(0, height - 1, Space.EMPTY);
        setSpace(1, height - 1, Space.EMPTY);

        setSpace(width - 2, height - 1, Space.EMPTY);
        setSpace(width - 1, height - 1, Space.EMPTY);
        setSpace(width - 1, height - 2, Space.EMPTY);
    }

    private Space getSpace(int x, int y) {
        if (x < 0 || x >= this.width) return Space.SOLID;
        if (y < 0 || y >= this.height) return Space.SOLID;

        return this.grid[y][x];
    }

    private void setSpace(int x, int y, Space space) {
        if (x < 0 || x >= this.width) throw new IndexOutOfBoundsException();
        if (y < 0 || y >= this.height) throw new IndexOutOfBoundsException();

        var tileNw = getTileNwCorner(x, y);
        var world = tileNw.getWorld();
        var nwx = tileNw.getBlockX();
        var nwy = tileNw.getBlockY();
        var nwz = tileNw.getBlockZ();

        var material = switch (space) {
            case SOFT -> softWallTile;
            case SOLID -> wallTile;
            case EMPTY -> Material.AIR;
        };

        world.setType(nwx, nwy, nwz, material);
        world.setType(nwx + 1, nwy, nwz, material);
        world.setType(nwx, nwy, nwz + 1, material);
        world.setType(nwx + 1, nwy, nwz + 1, material);
        world.setType(nwx, nwy + 1, nwz, material);
        world.setType(nwx + 1, nwy + 1, nwz, material);
        world.setType(nwx, nwy + 1, nwz + 1, material);
        world.setType(nwx + 1, nwy + 1, nwz + 1, material);

        if (space == Space.SOLID) {
            world.setType(nwx, nwy + 1, nwz, Material.BARRIER);
            world.setType(nwx + 1, nwy + 1, nwz, Material.BARRIER);
            world.setType(nwx, nwy + 1, nwz + 1, Material.BARRIER);
            world.setType(nwx + 1, nwy + 1, nwz + 1, Material.BARRIER);
        }

        this.grid[y][x] = space;
    }

    public Location getTileNwCorner(int x, int y) {
        return this.nwSpaceCorner.clone().add(x * 2, 0, y * 2);
    }

    public Location getTileCenter(int x, int y) {
        return this.getTileNwCorner(x, y).add(1, 0, 1);
    }

    public Location getNorthwestCornerSpawn() {
        return getTileCenter(0, 0);
    }

    public Location getNortheastCornerSpawn() {
        return getTileCenter(width - 1, 0);
    }

    public Location getSouthwestCornerSpawn() {
        return getTileCenter(0, height - 1);
    }

    public Location getSoutheastCornerSpawn() {
        return getTileCenter(width - 1, height - 1);
    }

    public int[] getTilePositionOfBlock(Location loc) {
        return this.getTilePositionOfBlock(loc.getBlockX(), loc.getBlockZ());
    }

    public int[] getTilePositionOfBlock(int x, int z) {
        var bx = x - this.nwSpaceCorner.getBlockX();
        var bz = z - this.nwSpaceCorner.getBlockZ();
        return new int[]{bx / 2, bz / 2};
    }

    public Set<UUID> bomb(int x, int y, int power) {
        bombTile(x, y);
        int minX = x;
        int maxX = x;
        int minY = y;
        int maxY = y;

        maxX += bombSpread(x, y, power, 0);
        minX -= bombSpread(x, y, power, 1);
        maxY += bombSpread(x, y, power, 2);
        minY -= bombSpread(x, y, power, 3);

        // find entities in the blast zone
        Set<UUID> ents = new HashSet<>();
        ents.addAll(getEntitiesInTiles(minX, y, maxX, y));
        ents.addAll(getEntitiesInTiles(x, minY, x, maxY));

        return ents;
    }

    private Set<UUID> getEntitiesInTiles(int xmin, int ymin, int xmax, int ymax) {
        var nw = getTileNwCorner(xmin, ymin);
        var se = getTileNwCorner(xmax, ymax).add(2, 0, 2);

        Set<UUID> entities = new HashSet<>();
        nw.getWorld().getEntities().forEach(e -> {
            var loc = e.getLocation();
            var x = loc.getX();
            var z = loc.getZ();

            if (x >= nw.getX() && x <= se.getX() && z >= nw.getZ() && z <= se.getZ()) entities.add(e.getUniqueId());
        });

        return entities;
    }

    private int bombSpread(int x, int y, int power, int direction) {
        for (int i = 1; i <= power; i++) {
            var cnt = switch (direction) {
                case 0 -> bombTile(x + i, y);
                case 1 -> bombTile(x - i, y);
                case 2 -> bombTile(x, y + i);
                default -> bombTile(x, y - i);
            };

            if (!cnt) return i;
        }

        return power;
    }

    private boolean bombTile(int x, int y) {
        var loc = getTileCenter(x, y);
        var expLoc = loc.clone().add(0, 1, 0);
        loc.getWorld().createExplosion(expLoc, 0f, false, false);

        switch (getSpace(x, y)) {
            case SOLID -> {
                return false;
            }
            case SOFT -> {
                setSpace(x, y, Space.EMPTY);
                float rand = ThreadLocalRandom.current().nextFloat();
                Material dropped = null;
                if (rand >= 0.0 && rand < 0.25) {
                    dropped = Material.LEATHER_BOOTS;
                } else if (rand >= 0.25 && rand < 0.50) {
                    dropped = Material.FLINT_AND_STEEL;
                } else if (rand >= 0.50 && rand < 0.75) {
                    dropped = Material.TNT;
                }

                if (dropped != null) {
                    var plg = Bukkit.getPluginManager().getPlugin("bomberman");
                    final Material finalDropped = dropped;
                    Bukkit.getScheduler().scheduleSyncDelayedTask(plg, () -> {
                        loc.getWorld().dropItemNaturally(loc, new ItemStack(finalDropped, 1));
                    }, 10);
                }

                return false;
            }
            default -> {
                return true;
            }
        }
    }

    public void destroy() {
        var world = nwCorner.getWorld();
        var nwx = nwCorner.getBlockX();
        var nwy = nwCorner.getBlockY();
        var nwz = nwCorner.getBlockZ();

        for (int y = 0; y < 3; y++) {
            for (int z = 0; z < height * 2 + 2; z++) {
                for (int x = 0; x < width * 2 + 2; x++) {
                    world.setType(nwx + x, nwy + y, nwz + z, Material.AIR);
                }
            }
        }
    }

}
