package net.enelson.soptntrun.match;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class SavedPlayerState {

    private final ItemStack[] contents;
    private final ItemStack[] armor;
    private final Location location;
    private final GameMode gameMode;
    private final double health;
    private final int foodLevel;
    private final float saturation;
    private final float exhaustion;
    private final int totalExperience;
    private final int level;
    private final float exp;
    private final boolean allowFlight;
    private final boolean flying;
    private final List<PotionEffect> potionEffects;

    private SavedPlayerState(Player player) {
        this.contents = cloneArray(player.getInventory().getContents());
        this.armor = cloneArray(player.getInventory().getArmorContents());
        this.location = player.getLocation().clone();
        this.gameMode = player.getGameMode();
        this.health = player.getHealth();
        this.foodLevel = player.getFoodLevel();
        this.saturation = player.getSaturation();
        this.exhaustion = player.getExhaustion();
        this.totalExperience = player.getTotalExperience();
        this.level = player.getLevel();
        this.exp = player.getExp();
        this.allowFlight = player.getAllowFlight();
        this.flying = player.isFlying();
        this.potionEffects = new ArrayList<PotionEffect>(player.getActivePotionEffects());
    }

    public static SavedPlayerState capture(Player player) {
        return new SavedPlayerState(player);
    }

    public void restore(Player player) {
        player.getInventory().setContents(cloneArray(contents));
        player.getInventory().setArmorContents(cloneArray(armor));
        player.setGameMode(gameMode);
        player.setAllowFlight(allowFlight);
        try {
            player.setFlying(allowFlight && flying);
        } catch (IllegalArgumentException ignored) {
        }
        player.setHealth(Math.max(1.0D, Math.min(player.getMaxHealth(), health)));
        player.setFoodLevel(foodLevel);
        player.setSaturation(saturation);
        player.setExhaustion(exhaustion);
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
        for (PotionEffect effect : potionEffects) {
            player.addPotionEffect(effect, true);
        }
        player.setTotalExperience(0);
        player.setLevel(0);
        player.setExp(0.0F);
        player.giveExp(totalExperience);
        player.setLevel(level);
        player.setExp(exp);
    }

    public Location getLocation() {
        return location.clone();
    }

    private static ItemStack[] cloneArray(ItemStack[] source) {
        if (source == null) {
            return new ItemStack[0];
        }
        ItemStack[] clone = new ItemStack[source.length];
        for (int i = 0; i < source.length; i++) {
            clone[i] = source[i] == null ? null : source[i].clone();
        }
        return clone;
    }
}
