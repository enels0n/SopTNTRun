package net.enelson.soptntrun.stats;

import net.enelson.soptntrun.SopTNTRunPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public final class PlayerStatisticsManager {

    private final SopTNTRunPlugin plugin;
    private final File file;
    private YamlConfiguration config;

    public PlayerStatisticsManager(SopTNTRunPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "stats.yml");
        reload();
    }

    public void reload() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException exception) {
                plugin.getLogger().warning("Could not create stats.yml: " + exception.getMessage());
            }
        }
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public void save() {
        try {
            config.save(file);
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not save stats.yml: " + exception.getMessage());
        }
    }

    public void increment(String category, UUID playerId) {
        if (playerId == null) {
            return;
        }
        String path = category + "." + playerId.toString();
        config.set(path, getInt(category, playerId) + 1);
        save();
    }

    public int getInt(String category, UUID playerId) {
        return config.getInt(category + "." + playerId.toString(), 0);
    }
}
