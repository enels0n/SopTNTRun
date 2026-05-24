package net.enelson.soptntrun.config;

import net.enelson.soptntrun.SopTNTRunPlugin;
import net.enelson.soptntrun.model.SerializedLocation;

import java.io.File;

public final class TNTRunConfig {

    private final SopTNTRunPlugin plugin;
    private File arenasFolder;

    public TNTRunConfig(SopTNTRunPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        this.arenasFolder = new File(plugin.getDataFolder(), "arenas");
        if (!arenasFolder.exists()) {
            arenasFolder.mkdirs();
        }
    }

    public File getArenasFolder() {
        return arenasFolder;
    }

    public SerializedLocation getGlobalSpawn() {
        return SerializedLocation.fromSection(plugin.getConfig().getConfigurationSection("global-spawn"));
    }

    public void setGlobalSpawn(SerializedLocation location) {
        plugin.getConfig().set("global-spawn", null);
        if (location != null) {
            location.save(plugin.getConfig().createSection("global-spawn"));
        }
        plugin.saveConfig();
    }
}
