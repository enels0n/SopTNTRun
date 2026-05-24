package net.enelson.soptntrun.model;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

public final class FloorDefinition {

    private final String id;
    private int topY;
    private int destroyDelayTicks;
    private final Set<Material> materials = new LinkedHashSet<Material>();

    public FloorDefinition(String id, int topY, int destroyDelayTicks) {
        this.id = id;
        this.topY = topY;
        this.destroyDelayTicks = destroyDelayTicks;
    }

    public static FloorDefinition fromSection(String id, ConfigurationSection section, int defaultDelay) {
        if (section == null) {
            return null;
        }
        FloorDefinition definition = new FloorDefinition(
                id,
                section.getInt("top-y"),
                section.getInt("destroy-delay-ticks", defaultDelay)
        );
        for (String raw : section.getStringList("materials")) {
            Material material = Material.matchMaterial(raw);
            if (material != null) {
                definition.materials.add(material);
            }
        }
        return definition;
    }

    public void save(ConfigurationSection section) {
        section.set("top-y", topY);
        section.set("destroy-delay-ticks", destroyDelayTicks);
        java.util.List<String> names = new java.util.ArrayList<String>();
        for (Material material : materials) {
            names.add(material.name().toLowerCase(Locale.ROOT));
        }
        section.set("materials", names);
    }

    public String getId() {
        return id;
    }

    public int getTopY() {
        return topY;
    }

    public void setTopY(int topY) {
        this.topY = topY;
    }

    public int getDestroyDelayTicks() {
        return destroyDelayTicks;
    }

    public void setDestroyDelayTicks(int destroyDelayTicks) {
        this.destroyDelayTicks = destroyDelayTicks;
    }

    public Set<Material> getMaterials() {
        return Collections.unmodifiableSet(materials);
    }

    public boolean addMaterial(Material material) {
        return materials.add(material);
    }

    public boolean removeMaterial(Material material) {
        return materials.remove(material);
    }
}
