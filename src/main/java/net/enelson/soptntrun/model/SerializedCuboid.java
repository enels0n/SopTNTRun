package net.enelson.soptntrun.model;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

public final class SerializedCuboid {

    private final String world;
    private final int x1;
    private final int y1;
    private final int z1;
    private final int x2;
    private final int y2;
    private final int z2;

    public SerializedCuboid(String world, int x1, int y1, int z1, int x2, int y2, int z2) {
        this.world = world;
        this.x1 = x1;
        this.y1 = y1;
        this.z1 = z1;
        this.x2 = x2;
        this.y2 = y2;
        this.z2 = z2;
    }

    public static SerializedCuboid of(Location first, Location second) {
        return new SerializedCuboid(
                first.getWorld() == null ? "" : first.getWorld().getName(),
                first.getBlockX(),
                first.getBlockY(),
                first.getBlockZ(),
                second.getBlockX(),
                second.getBlockY(),
                second.getBlockZ()
        );
    }

    public static SerializedCuboid fromSection(ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        return new SerializedCuboid(
                section.getString("world", ""),
                section.getInt("x1"),
                section.getInt("y1"),
                section.getInt("z1"),
                section.getInt("x2"),
                section.getInt("y2"),
                section.getInt("z2")
        );
    }

    public void save(ConfigurationSection section) {
        section.set("world", world);
        section.set("x1", x1);
        section.set("y1", y1);
        section.set("z1", z1);
        section.set("x2", x2);
        section.set("y2", y2);
        section.set("z2", z2);
    }

    public boolean contains(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        if (!location.getWorld().getName().equalsIgnoreCase(world)) {
            return false;
        }
        int minX = Math.min(x1, x2);
        int maxX = Math.max(x1, x2);
        int minY = Math.min(y1, y2);
        int maxY = Math.max(y1, y2);
        int minZ = Math.min(z1, z2);
        int maxZ = Math.max(z1, z2);
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        return x >= minX && x <= maxX
                && y >= minY && y <= maxY
                && z >= minZ && z <= maxZ;
    }

    public String getWorld() {
        return world;
    }

    public int getMinX() {
        return Math.min(x1, x2);
    }

    public int getMaxX() {
        return Math.max(x1, x2);
    }

    public int getMinY() {
        return Math.min(y1, y2);
    }

    public int getMaxY() {
        return Math.max(y1, y2);
    }

    public int getMinZ() {
        return Math.min(z1, z2);
    }

    public int getMaxZ() {
        return Math.max(z1, z2);
    }
}
