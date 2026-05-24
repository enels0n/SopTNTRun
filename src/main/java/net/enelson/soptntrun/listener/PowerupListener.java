package net.enelson.soptntrun.listener;

import net.enelson.soptntrun.SopTNTRunPlugin;
import net.enelson.soptntrun.match.PlayerGameState;
import net.enelson.soptntrun.match.RunningMatch;
import net.enelson.soptntrun.powerup.PowerupType;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

public final class PowerupListener implements Listener {

    private static final String DASH_ITEM_NAME = ChatColor.GOLD + "Dash Charge";
    private static final String SNOWBALL_ITEM_NAME = ChatColor.AQUA + "Knockback Snowball";

    private final SopTNTRunPlugin plugin;

    public PowerupListener(SopTNTRunPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Player)) {
            return;
        }
        Player player = (Player) entity;
        RunningMatch running = plugin.getMatchManager().getRunningMatch(player.getUniqueId());
        if (running == null || plugin.getMatchManager().getPlayerState(player.getUniqueId()) != PlayerGameState.RUNNING) {
            return;
        }
        Item item = event.getItem();
        PowerupType type = running.getPowerupType(item.getUniqueId());
        if (type == null) {
            return;
        }

        event.setCancelled(true);
        running.removeActivePowerup(item.getUniqueId());
        plugin.getStatistics().increment("powerups", player.getUniqueId());
        item.remove();

        if (type == PowerupType.FEATHER) {
            running.giveFeatherCharge(player.getUniqueId());
            player.setAllowFlight(true);
            plugin.getMessageService().send(player, "feather-picked-up");
            plugin.getMessageService().send(player, "feather-ready");
            return;
        }

        if (type == PowerupType.DASH) {
            running.giveDashCharge(player.getUniqueId());
            giveOrStack(player, createNamedItem(Material.SUGAR, DASH_ITEM_NAME));
            plugin.getMessageService().send(player, "dash-picked-up");
            plugin.getMessageService().send(player, "dash-ready");
            return;
        }

        giveOrStack(player, createNamedItem(Material.SNOWBALL, SNOWBALL_ITEM_NAME));
        plugin.getMessageService().send(player, "snowball-picked-up");
    }

    @EventHandler
    public void onToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        RunningMatch running = plugin.getMatchManager().getRunningMatch(player.getUniqueId());
        if (running == null || plugin.getMatchManager().getPlayerState(player.getUniqueId()) != PlayerGameState.RUNNING) {
            return;
        }
        if (!running.consumeFeatherCharge(player.getUniqueId())) {
            event.setCancelled(true);
            player.setAllowFlight(false);
            return;
        }

        event.setCancelled(true);
        player.setFlying(false);
        player.setAllowFlight(running.getFeatherCharges(player.getUniqueId()) > 0);

        Vector velocity = player.getVelocity();
        velocity.setY(running.getArena().getSettings().getFeatherJumpVelocity());
        player.setVelocity(velocity);
        plugin.getStatistics().increment("doublejumps", player.getUniqueId());
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        RunningMatch running = plugin.getMatchManager().getRunningMatch(player.getUniqueId());
        if (running == null || plugin.getMatchManager().getPlayerState(player.getUniqueId()) != PlayerGameState.RUNNING) {
            return;
        }

        ItemStack item = event.getItem();
        if (!isNamedItem(item, DASH_ITEM_NAME)) {
            return;
        }

        event.setCancelled(true);
        if (!running.consumeDashCharge(player.getUniqueId())) {
            consumeHeldItem(player);
            return;
        }

        Vector velocity = player.getLocation().getDirection().normalize().multiply(running.getArena().getSettings().getDashVelocity());
        velocity.setY(Math.max(velocity.getY(), 0.2D));
        player.setVelocity(velocity);
        consumeHeldItem(player);
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        Projectile projectile = event.getEntity();
        if (!(projectile instanceof Snowball)) {
            return;
        }
        ProjectileSource shooter = projectile.getShooter();
        if (!(shooter instanceof Player)) {
            return;
        }
        Player player = (Player) shooter;
        RunningMatch running = plugin.getMatchManager().getRunningMatch(player.getUniqueId());
        if (running == null || plugin.getMatchManager().getPlayerState(player.getUniqueId()) != PlayerGameState.RUNNING) {
            return;
        }
        if (!isNamedItem(player.getInventory().getItemInMainHand(), SNOWBALL_ITEM_NAME)) {
            return;
        }
        running.trackKnockbackSnowball(projectile.getUniqueId());
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        RunningMatch running = null;
        for (RunningMatch candidate : plugin.getMatchManager().getRunningMatches()) {
            if (candidate.isTrackedKnockbackSnowball(event.getEntity().getUniqueId())) {
                running = candidate;
                break;
            }
        }
        if (running == null) {
            return;
        }

        running.untrackKnockbackSnowball(event.getEntity().getUniqueId());
        Entity hit = event.getHitEntity();
        if (!(hit instanceof Player)) {
            return;
        }

        Player victim = (Player) hit;
        if (plugin.getMatchManager().getPlayerState(victim.getUniqueId()) != PlayerGameState.RUNNING) {
            return;
        }

        ProjectileSource shooter = event.getEntity().getShooter();
        if (!(shooter instanceof Player)) {
            return;
        }
        Player attacker = (Player) shooter;
        Vector push = victim.getLocation().toVector().subtract(attacker.getLocation().toVector()).normalize();
        push.multiply(running.getArena().getSettings().getSnowballKnockbackStrength());
        push.setY(0.35D);
        victim.setVelocity(push);
    }

    private void giveOrStack(Player player, ItemStack stack) {
        player.getInventory().addItem(stack);
    }

    private ItemStack createNamedItem(Material material, String displayName) {
        ItemStack stack = new ItemStack(material, 1);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(displayName);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private boolean isNamedItem(ItemStack item, String displayName) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta != null && displayName.equals(meta.getDisplayName());
    }

    private void consumeHeldItem(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) {
            return;
        }
        int amount = item.getAmount();
        if (amount <= 1) {
            player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
            return;
        }
        item.setAmount(amount - 1);
        player.getInventory().setItemInMainHand(item);
    }
}
