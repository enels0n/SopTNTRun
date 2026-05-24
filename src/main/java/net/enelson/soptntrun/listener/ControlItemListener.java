package net.enelson.soptntrun.listener;

import net.enelson.soptntrun.SopTNTRunPlugin;
import net.enelson.soptntrun.arena.TNTRunArena;
import net.enelson.soptntrun.edit.EditorManager;
import net.enelson.soptntrun.model.SerializedCuboid;
import net.enelson.soptntrun.model.SerializedLocation;
import net.enelson.soptntrun.match.PlayerGameState;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ControlItemListener implements Listener {

    private static final String SET_POINT_NAME = ChatColor.GOLD + "Set Point";
    private static final String PREVIOUS_NAME = ChatColor.RED + "Back";
    private static final String NEXT_NAME = ChatColor.GREEN + "Next";
    private static final String LEAVE_QUEUE_NAME = ChatColor.RED + "Leave TNTRun";

    private final SopTNTRunPlugin plugin;

    public ControlItemListener(SopTNTRunPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) {
            return;
        }

        if (isNamedItem(item, LEAVE_QUEUE_NAME)) {
            PlayerGameState state = plugin.getMatchManager().getPlayerState(player.getUniqueId());
            if (state == PlayerGameState.WAITING || state == PlayerGameState.STARTING) {
                event.setCancelled(true);
                if (plugin.getMatchManager().leave(player)) {
                    plugin.getMessageService().send(player, "left-arena");
                }
            }
            return;
        }

        EditorManager.SetupSession session = plugin.getEditorManager().getSetupSession(player.getUniqueId());
        if (session == null) {
            return;
        }

        event.setCancelled(true);
        TNTRunArena arena = plugin.getArenaManager().getArena(session.getArenaName());
        if (arena == null) {
            plugin.getEditorManager().stopSetup(player.getUniqueId());
            clearSetupItems(player);
            return;
        }

        if (isNamedItem(item, SET_POINT_NAME)) {
            handleSetPoint(player, arena, session);
            return;
        }
        if (isNamedItem(item, NEXT_NAME)) {
            if (session.advance()) {
                giveSetupItems(player, session);
                sendStepMessage(player, session);
            }
            return;
        }
        if (isNamedItem(item, PREVIOUS_NAME)) {
            if (session.goBack()) {
                giveSetupItems(player, session);
                sendStepMessage(player, session);
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();
        if (isControlItem(current) || isControlItem(cursor)) {
            event.setCancelled(true);
            return;
        }
        if (plugin.getEditorManager().getSetupSession(player.getUniqueId()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        Item dropped = event.getItemDrop();
        if (isControlItem(dropped.getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onSwap(PlayerSwapHandItemsEvent event) {
        if (isControlItem(event.getMainHandItem()) || isControlItem(event.getOffHandItem())) {
            event.setCancelled(true);
        }
    }

    public static void giveSetupItems(Player player, EditorManager.SetupSession session) {
        if (session == null) {
            return;
        }
        player.getInventory().clear();
        player.getInventory().setItem(0, namedItem(Material.NETHER_STAR, SET_POINT_NAME, describeSetLore(session)));
        player.getInventory().setItem(1, namedItem(Material.ARROW, PREVIOUS_NAME, ChatColor.GRAY + "Go back to previous step"));
        player.getInventory().setItem(2, namedItem(Material.SPECTRAL_ARROW, NEXT_NAME, ChatColor.GRAY + "Skip to next step"));
        player.updateInventory();
    }

    public static void clearSetupItems(Player player) {
        clearIfNamed(player, SET_POINT_NAME);
        clearIfNamed(player, NEXT_NAME);
        clearIfNamed(player, PREVIOUS_NAME);
        player.updateInventory();
    }

    public static void giveLeaveQueueItem(Player player) {
        player.getInventory().setItem(8, namedItem(Material.BARRIER, LEAVE_QUEUE_NAME, ChatColor.GRAY + "Right click to leave the queue"));
        player.updateInventory();
    }

    private void handleSetPoint(Player player, TNTRunArena arena, EditorManager.SetupSession session) {
        if (session.getKind() == EditorManager.SetupKind.AREA) {
            handleAreaSetPoint(player, arena, session);
            return;
        }
        handleSpawnSetPoint(player, arena, session);
    }

    private void handleAreaSetPoint(Player player, TNTRunArena arena, EditorManager.SetupSession session) {
        if (session.getCurrentSlot() == 1) {
            plugin.getEditorManager().setAreaFirstPoint(player.getUniqueId(), player.getLocation());
            plugin.getMessageService().send(player, "set-point", replacements("point", "pos1", "name", arena.getName()));
            if (session.advance()) {
                giveSetupItems(player, session);
                plugin.getMessageService().send(player, "area-setup-step-two");
            }
            return;
        }

        Location first = plugin.getEditorManager().getAreaFirstPoint(player.getUniqueId());
        if (first == null) {
            plugin.getMessageService().send(player, "area-setup-missing-first");
            return;
        }
        arena.setGameplayArea(SerializedCuboid.of(first, player.getLocation()));
        plugin.getArenaManager().saveArena(arena);
        plugin.getMessageService().send(player, "set-point", replacements("point", "pos2", "name", arena.getName()));
        plugin.getEditorManager().stopSetup(player.getUniqueId());
        clearSetupItems(player);
        plugin.getMessageService().send(player, "area-setup-finished");
    }

    private void handleSpawnSetPoint(Player player, TNTRunArena arena, EditorManager.SetupSession session) {
        int slot = session.getCurrentSlot();
        arena.setSpawn(slot, SerializedLocation.of(player.getLocation()));
        plugin.getArenaManager().saveArena(arena);
        plugin.getMessageService().send(player, "set-point", replacements("point", "spawn-" + slot, "name", arena.getName()));
        if (session.advance()) {
            giveSetupItems(player, session);
            plugin.getMessageService().send(player, "spawn-setup-next", replacements("slot", Integer.toString(session.getCurrentSlot()), "max", Integer.toString(session.getMaxSlot())));
            return;
        }
        giveSetupItems(player, session);
        plugin.getMessageService().send(player, "spawn-setup-finished", replacements("max", Integer.toString(session.getMaxSlot())));
    }

    private void sendStepMessage(Player player, EditorManager.SetupSession session) {
        if (session.getKind() == EditorManager.SetupKind.AREA) {
            if (session.getCurrentSlot() == 1) {
                plugin.getMessageService().send(player, "area-setup-started");
            } else {
                plugin.getMessageService().send(player, "area-setup-step-two");
            }
            return;
        }
        plugin.getMessageService().send(player, "spawn-setup-next", replacements("slot", Integer.toString(session.getCurrentSlot()), "max", Integer.toString(session.getMaxSlot())));
    }

    private static void clearIfNamed(Player player, String name) {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (isNamedItem(stack, name)) {
                player.getInventory().setItem(i, null);
            }
        }
    }

    private static ItemStack namedItem(Material material, String displayName, String loreLine) {
        ItemStack stack = new ItemStack(material, 1);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(displayName);
            meta.setLore(Collections.singletonList(loreLine));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private static String describeSetLore(EditorManager.SetupSession session) {
        if (session.getKind() == EditorManager.SetupKind.AREA) {
            return ChatColor.YELLOW + "Set gameplay area point " + session.getCurrentSlot() + "/2";
        }
        return ChatColor.YELLOW + "Set spawn slot " + session.getCurrentSlot() + "/" + session.getMaxSlot();
    }

    private static boolean isControlItem(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR || !stack.hasItemMeta()) {
            return false;
        }
        String displayName = stack.getItemMeta() == null ? null : stack.getItemMeta().getDisplayName();
        return displayName != null && Arrays.asList(SET_POINT_NAME, NEXT_NAME, PREVIOUS_NAME, LEAVE_QUEUE_NAME).contains(displayName);
    }

    private static boolean isNamedItem(ItemStack stack, String name) {
        if (stack == null || stack.getType() == Material.AIR || !stack.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = stack.getItemMeta();
        return meta != null && name.equals(meta.getDisplayName());
    }

    private Map<String, String> replacements(String key, String value) {
        Map<String, String> replacements = new LinkedHashMap<String, String>();
        replacements.put(key, value);
        return replacements;
    }

    private Map<String, String> replacements(String key1, String value1, String key2, String value2) {
        Map<String, String> replacements = new LinkedHashMap<String, String>();
        replacements.put(key1, value1);
        replacements.put(key2, value2);
        return replacements;
    }
}
