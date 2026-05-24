package net.enelson.soptntrun.powerup;

import org.bukkit.Material;

public enum PowerupType {

    FEATHER(Material.FEATHER, "Feather"),
    DASH(Material.SUGAR, "Dash"),
    KNOCKBACK_SNOWBALL(Material.SNOWBALL, "Knockback Snowball");

    private final Material material;
    private final String displayName;

    PowerupType(Material material, String displayName) {
        this.material = material;
        this.displayName = displayName;
    }

    public Material getMaterial() {
        return material;
    }

    public String getDisplayName() {
        return displayName;
    }
}
