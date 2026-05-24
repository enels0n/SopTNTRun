package net.enelson.soptntrun.match;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;

public final class SavedGroundItem {

    private final Location location;
    private final ItemStack itemStack;
    private final int pickupDelay;

    private SavedGroundItem(Location location, ItemStack itemStack, int pickupDelay) {
        this.location = location.clone();
        this.itemStack = itemStack.clone();
        this.pickupDelay = pickupDelay;
    }

    public static SavedGroundItem capture(Item item) {
        return new SavedGroundItem(item.getLocation(), item.getItemStack(), item.getPickupDelay());
    }

    public void restore() {
        World world = location.getWorld();
        if (world == null) {
            return;
        }
        Item item = world.dropItem(location, itemStack.clone());
        item.setPickupDelay(pickupDelay);
    }
}
