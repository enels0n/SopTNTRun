package net.enelson.soptntrun.arena;

import net.enelson.soptntrun.SopTNTRunPlugin;
import net.enelson.soptntrun.model.ArenaSettings;
import net.enelson.soptntrun.model.PowerupSpawnShape;
import net.enelson.soptntrun.model.SerializedCuboid;
import net.enelson.soptntrun.model.SerializedLocation;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ArenaManager {

    private final SopTNTRunPlugin plugin;
    private final Map<String, TNTRunArena> arenas = new LinkedHashMap<String, TNTRunArena>();

    public ArenaManager(SopTNTRunPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        arenas.clear();
        File[] files = plugin.getTNTRunConfig().getArenasFolder().listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (!file.isFile() || !file.getName().toLowerCase(Locale.ROOT).endsWith(".yml")) {
                continue;
            }
            TNTRunArena arena = loadArena(file);
            if (arena != null) {
                arenas.put(normalize(arena.getName()), arena);
            }
        }
    }

    public TNTRunArena createArena(String name, int minPlayers, int maxPlayers, String worldName) {
        ArenaSettings settings = new ArenaSettings(
                minPlayers,
                maxPlayers,
                plugin.getConfig().getInt("settings.default-countdown-seconds", 15),
                plugin.getConfig().getInt("settings.default-destroy-delay-ticks", 6),
                plugin.getConfig().getInt("settings.default-powerup-spawn-interval-seconds", 12),
                plugin.getConfig().getInt("settings.default-powerup-despawn-seconds", 8),
                plugin.getConfig().getDouble("settings.default-feather-jump-velocity", 1.15D),
                plugin.getConfig().getDouble("settings.default-dash-velocity", 1.1D),
                plugin.getConfig().getDouble("settings.default-snowball-knockback-strength", 1.15D),
                plugin.getConfig().getInt("settings.default-max-active-powerups", 1),
                plugin.getConfig().getDouble("settings.default-powerup-spawn-radius", -1.0D),
                PowerupSpawnShape.parse(plugin.getConfig().getString("settings.default-powerup-spawn-shape", "circle")),
                plugin.getConfig().getInt("settings.default-winner-fireworks-interval-ticks", 20),
                plugin.getConfig().getDouble("settings.default-winner-fireworks-radius", -1.0D)
        );
        TNTRunArena arena = new TNTRunArena(name, worldName, ArenaState.EDITING, settings);
        arenas.put(normalize(name), arena);
        saveArena(arena);
        return arena;
    }

    public boolean saveArena(TNTRunArena arena) {
        File targetFile = new File(plugin.getTNTRunConfig().getArenasFolder(), arena.getName() + ".yml");
        YamlConfiguration config = new YamlConfiguration();
        config.set("arena.name", arena.getName());
        config.set("arena.world", arena.getWorldName());
        config.set("arena.state", arena.getState().name());

        if (arena.getGameplayArea() != null) {
            arena.getGameplayArea().save(config.createSection("arena.gameplay-area"));
        }
        if (arena.getSpectatorSpawn() != null) {
            arena.getSpectatorSpawn().save(config.createSection("arena.spectator-spawn"));
        }

        ConfigurationSection spawns = config.createSection("arena.spawns");
        int index = 1;
        for (SerializedLocation spawn : arena.getSpawns()) {
            if (spawn != null) {
                spawn.save(spawns.createSection("slot-" + index));
            }
            index++;
        }

        ConfigurationSection joinBlocks = config.createSection("arena.join-blocks");
        int joinIndex = 1;
        for (SerializedLocation joinBlock : arena.getJoinBlocks()) {
            if (joinBlock != null) {
                joinBlock.save(joinBlocks.createSection("block-" + joinIndex));
            }
            joinIndex++;
        }

        saveSettings(config.createSection("settings"), arena.getSettings());

        try {
            config.save(targetFile);
            return true;
        } catch (IOException exception) {
            plugin.getLogger().severe("Failed to save arena " + arena.getName() + ": " + exception.getMessage());
            return false;
        }
    }

    public TNTRunArena getArena(String name) {
        return arenas.get(normalize(name));
    }

    public Collection<TNTRunArena> getArenas() {
        return Collections.unmodifiableCollection(arenas.values());
    }

    public List<String> getArenaNames() {
        List<String> names = new ArrayList<String>();
        for (TNTRunArena arena : arenas.values()) {
            names.add(arena.getName());
        }
        return names;
    }

    public boolean hasArena(String name) {
        return arenas.containsKey(normalize(name));
    }

    public TNTRunArena findArenaByJoinBlock(org.bukkit.Location location) {
        if (location == null || location.getWorld() == null) {
            return null;
        }
        String world = location.getWorld().getName();
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        for (TNTRunArena arena : arenas.values()) {
            for (SerializedLocation joinBlock : arena.getJoinBlocks()) {
                if (joinBlock == null) {
                    continue;
                }
                if (joinBlock.getWorld().equalsIgnoreCase(world)
                        && joinBlock.getBlockX() == x
                        && joinBlock.getBlockY() == y
                        && joinBlock.getBlockZ() == z) {
                    return arena;
                }
            }
        }
        return null;
    }

    private TNTRunArena loadArena(File file) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        String name = config.getString("arena.name", file.getName().replace(".yml", ""));
        String world = config.getString("arena.world", "");
        TNTRunArena arena = new TNTRunArena(
                name,
                world,
                parseState(config.getString("arena.state", ArenaState.DISABLED.name())),
                loadSettings(config.getConfigurationSection("settings"))
        );
        arena.setGameplayArea(SerializedCuboid.fromSection(config.getConfigurationSection("arena.gameplay-area")));
        arena.setSpectatorSpawn(SerializedLocation.fromSection(config.getConfigurationSection("arena.spectator-spawn")));

        ConfigurationSection spawns = config.getConfigurationSection("arena.spawns");
        if (spawns != null) {
            for (String key : spawns.getKeys(false)) {
                if (!key.startsWith("slot-")) {
                    continue;
                }
                int slot;
                try {
                    slot = Integer.parseInt(key.substring("slot-".length()));
                } catch (NumberFormatException ignored) {
                    continue;
                }
                arena.setSpawn(slot, SerializedLocation.fromSection(spawns.getConfigurationSection(key)));
            }
        }

        ConfigurationSection joinBlocks = config.getConfigurationSection("arena.join-blocks");
        if (joinBlocks != null) {
            for (String key : joinBlocks.getKeys(false)) {
                SerializedLocation joinBlock = SerializedLocation.fromSection(joinBlocks.getConfigurationSection(key));
                if (joinBlock != null) {
                    arena.addJoinBlock(joinBlock);
                }
            }
        }

        return arena;
    }

    private void saveSettings(ConfigurationSection section, ArenaSettings settings) {
        section.set("min-players", settings.getMinPlayers());
        section.set("max-players", settings.getMaxPlayers());
        section.set("countdown-seconds", settings.getCountdownSeconds());
        section.set("default-destroy-delay-ticks", settings.getDefaultDestroyDelayTicks());
        section.set("powerup-spawn-interval-seconds", settings.getPowerupSpawnIntervalSeconds());
        section.set("powerup-despawn-seconds", settings.getPowerupDespawnSeconds());
        section.set("feather-jump-velocity", settings.getFeatherJumpVelocity());
        section.set("dash-velocity", settings.getDashVelocity());
        section.set("snowball-knockback-strength", settings.getSnowballKnockbackStrength());
        section.set("max-active-powerups", settings.getMaxActivePowerups());
        section.set("powerup-spawn-radius", settings.getPowerupSpawnRadius());
        section.set("powerup-spawn-shape", settings.getPowerupSpawnShape().name().toLowerCase(Locale.ROOT));
        section.set("winner-fireworks-interval-ticks", settings.getWinnerFireworksIntervalTicks());
        section.set("winner-fireworks-radius", settings.getWinnerFireworksRadius());
    }

    private ArenaSettings loadSettings(ConfigurationSection section) {
        return new ArenaSettings(
                readInt(section, "min-players", plugin.getConfig().getInt("settings.default-min-players", 2)),
                readInt(section, "max-players", plugin.getConfig().getInt("settings.default-max-players", 12)),
                readInt(section, "countdown-seconds", plugin.getConfig().getInt("settings.default-countdown-seconds", 15)),
                readInt(section, "default-destroy-delay-ticks", plugin.getConfig().getInt("settings.default-destroy-delay-ticks", 6)),
                readInt(section, "powerup-spawn-interval-seconds", plugin.getConfig().getInt("settings.default-powerup-spawn-interval-seconds", 12)),
                readInt(section, "powerup-despawn-seconds", plugin.getConfig().getInt("settings.default-powerup-despawn-seconds", 8)),
                readDouble(section, "feather-jump-velocity", plugin.getConfig().getDouble("settings.default-feather-jump-velocity", 1.15D)),
                readDouble(section, "dash-velocity", plugin.getConfig().getDouble("settings.default-dash-velocity", 1.1D)),
                readDouble(section, "snowball-knockback-strength", plugin.getConfig().getDouble("settings.default-snowball-knockback-strength", 1.15D)),
                readInt(section, "max-active-powerups", plugin.getConfig().getInt("settings.default-max-active-powerups", 1)),
                readDouble(section, "powerup-spawn-radius", plugin.getConfig().getDouble("settings.default-powerup-spawn-radius", -1.0D)),
                PowerupSpawnShape.parse(readString(section, "powerup-spawn-shape", plugin.getConfig().getString("settings.default-powerup-spawn-shape", "circle"))),
                readInt(section, "winner-fireworks-interval-ticks", plugin.getConfig().getInt("settings.default-winner-fireworks-interval-ticks", 20)),
                readDouble(section, "winner-fireworks-radius", plugin.getConfig().getDouble("settings.default-winner-fireworks-radius", -1.0D))
        );
    }

    private int readInt(ConfigurationSection section, String path, int fallback) {
        return section == null ? fallback : section.getInt(path, fallback);
    }

    private double readDouble(ConfigurationSection section, String path, double fallback) {
        return section == null ? fallback : section.getDouble(path, fallback);
    }

    private String readString(ConfigurationSection section, String path, String fallback) {
        return section == null ? fallback : section.getString(path, fallback);
    }

    private ArenaState parseState(String raw) {
        try {
            return ArenaState.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return ArenaState.DISABLED;
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
