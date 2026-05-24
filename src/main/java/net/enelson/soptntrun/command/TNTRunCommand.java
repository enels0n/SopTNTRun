package net.enelson.soptntrun.command;

import net.enelson.soptntrun.SopTNTRunPlugin;
import net.enelson.soptntrun.arena.ArenaState;
import net.enelson.soptntrun.arena.TNTRunArena;
import net.enelson.soptntrun.edit.EditorManager;
import net.enelson.soptntrun.listener.ControlItemListener;
import net.enelson.soptntrun.model.SerializedLocation;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class TNTRunCommand implements CommandExecutor, TabCompleter {

    private final SopTNTRunPlugin plugin;

    public TNTRunCommand(SopTNTRunPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("/tntrun create <name> <minPlayers> <maxPlayers>");
            sender.sendMessage("/tntrun edit <arena>");
            sender.sendMessage("/tntrun save");
            sender.sendMessage("/tntrun list");
            sender.sendMessage("/tntrun reload");
            sender.sendMessage("/tntrun join <arena>");
            sender.sendMessage("/tntrun random");
            sender.sendMessage("/tntrun leave");
            sender.sendMessage("/tntrun setglobalspawn");
            sender.sendMessage("/tntrun setjoinblock");
            sender.sendMessage("/tntrun removejoinblock");
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        if ("reload".equals(sub)) {
            if (!sender.hasPermission("soptntrun.admin")) {
                return true;
            }
            plugin.reloadPlugin();
            sender.sendMessage("SopTNTRun reloaded.");
            return true;
        }
        if ("list".equals(sub)) {
            sender.sendMessage("Arenas: " + plugin.getArenaManager().getArenaNames());
            return true;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage("Player only.");
            return true;
        }
        Player player = (Player) sender;

        if ("create".equals(sub)) {
            if (!player.hasPermission("soptntrun.admin")) {
                return true;
            }
            if (args.length < 4) {
                player.sendMessage("/tntrun create <name> <minPlayers> <maxPlayers>");
                return true;
            }
            int minPlayers = parseInt(args[2]);
            int maxPlayers = parseInt(args[3]);
            if (minPlayers < 1 || maxPlayers < minPlayers) {
                plugin.getMessageService().send(player, "invalid-number");
                return true;
            }
            TNTRunArena arena = plugin.getArenaManager().createArena(args[1], minPlayers, maxPlayers, player.getWorld().getName());
            plugin.getEditorManager().startEditing(player.getUniqueId(), arena.getName());
            plugin.getMessageService().send(player, "arena-created", replacements("name", arena.getName()));
            plugin.getMessageService().send(player, "entered-edit", replacements("name", arena.getName()));
            return true;
        }

        if ("edit".equals(sub)) {
            if (!player.hasPermission("soptntrun.admin")) {
                return true;
            }
            if (args.length < 2) {
                return true;
            }
            TNTRunArena arena = plugin.getArenaManager().getArena(args[1]);
            if (arena == null) {
                plugin.getMessageService().send(player, "no-arena");
                return true;
            }
            arena.setState(ArenaState.EDITING);
            plugin.getEditorManager().startEditing(player.getUniqueId(), arena.getName());
            plugin.getMessageService().send(player, "entered-edit", replacements("name", arena.getName()));
            return true;
        }

        if ("save".equals(sub)) {
            if (!player.hasPermission("soptntrun.admin")) {
                return true;
            }
            TNTRunArena arena = getEditingArena(player);
            if (arena == null) {
                plugin.getMessageService().send(player, "no-edit");
                return true;
            }
            arena.setState(ArenaState.WAITING);
            plugin.getArenaManager().saveArena(arena);
            plugin.getEditorManager().stopEditing(player.getUniqueId());
            ControlItemListener.clearSetupItems(player);
            plugin.getMessageService().send(player, "arena-saved", replacements("name", arena.getName()));
            plugin.getMessageService().send(player, "left-edit");
            return true;
        }

        if ("pos1".equals(sub)) {
            handlePos1(player);
            return true;
        }
        if ("pos2".equals(sub)) {
            handlePos2(player);
            return true;
        }
        if ("setglobalspawn".equals(sub)) {
            plugin.getTNTRunConfig().setGlobalSpawn(SerializedLocation.of(player.getLocation()));
            plugin.getMessageService().send(player, "set-point", replacements("point", "global-spawn", "name", "global"));
            return true;
        }
        if ("setspawn".equals(sub)) {
            TNTRunArena arena = getEditingArena(player);
            if (arena == null) {
                plugin.getMessageService().send(player, "no-edit");
                return true;
            }
            int slot = args.length < 2 ? 1 : parseInt(args[1]);
            if (slot < 1) {
                plugin.getMessageService().send(player, "invalid-number");
                return true;
            }
            plugin.getEditorManager().startSpawnSetup(player.getUniqueId(), arena.getName(), slot, arena.getSettings().getMaxPlayers());
            ControlItemListener.giveSetupItems(player, plugin.getEditorManager().getSetupSession(player.getUniqueId()));
            plugin.getMessageService().send(player, "spawn-setup-started", replacements("slot", Integer.toString(slot), "max", Integer.toString(arena.getSettings().getMaxPlayers())));
            return true;
        }
        if ("setjoinblock".equals(sub)) {
            TNTRunArena arena = getEditingArena(player);
            if (arena == null) {
                plugin.getMessageService().send(player, "no-edit");
                return true;
            }
            arena.addJoinBlock(SerializedLocation.of(player.getLocation()));
            plugin.getArenaManager().saveArena(arena);
            plugin.getMessageService().send(player, "set-point", replacements("point", "join-block", "name", arena.getName()));
            return true;
        }
        if ("removejoinblock".equals(sub)) {
            TNTRunArena arena = getEditingArena(player);
            if (arena == null) {
                plugin.getMessageService().send(player, "no-edit");
                return true;
            }
            arena.removeJoinBlock(SerializedLocation.of(player.getLocation()));
            plugin.getArenaManager().saveArena(arena);
            plugin.getMessageService().send(player, "set-point", replacements("point", "join-block-removed", "name", arena.getName()));
            return true;
        }
        if ("join".equals(sub)) {
            if (args.length < 2) {
                return true;
            }
            String result = plugin.getMatchManager().joinSpecific(player, args[1]);
            handleJoinResult(player, result, args[1]);
            return true;
        }
        if ("random".equals(sub)) {
            String result = plugin.getMatchManager().joinRandom(player);
            handleJoinResult(player, result, plugin.getMatchManager().getTrackedArenaName(player.getUniqueId()));
            return true;
        }
        if ("leave".equals(sub)) {
            if (plugin.getMatchManager().leave(player)) {
                plugin.getMessageService().send(player, "left-arena");
            }
            return true;
        }
        return true;
    }

    private void handleJoinResult(Player player, String result, String arenaName) {
        if ("OK".equals(result)) {
            plugin.getMessageService().send(player, "joined-arena", replacements("name", arenaName));
            return;
        }
        if ("NO_ROOM".equals(result)) {
            plugin.getMessageService().send(player, "party-no-room");
            return;
        }
        if ("NO_ARENA".equals(result)) {
            plugin.getMessageService().send(player, "no-arena");
            return;
        }
        plugin.getMessageService().send(player, "random-no-arena");
    }

    private void handlePos1(Player player) {
        TNTRunArena arena = getEditingArena(player);
        if (arena == null) {
            plugin.getMessageService().send(player, "no-edit");
            return;
        }
        plugin.getEditorManager().startAreaSetup(player.getUniqueId(), arena.getName(), 1);
        ControlItemListener.giveSetupItems(player, plugin.getEditorManager().getSetupSession(player.getUniqueId()));
        plugin.getMessageService().send(player, "area-setup-started");
    }

    private void handlePos2(Player player) {
        TNTRunArena arena = getEditingArena(player);
        if (arena == null) {
            plugin.getMessageService().send(player, "no-edit");
            return;
        }
        plugin.getEditorManager().startAreaSetup(player.getUniqueId(), arena.getName(), 2);
        ControlItemListener.giveSetupItems(player, plugin.getEditorManager().getSetupSession(player.getUniqueId()));
        plugin.getMessageService().send(player, "area-setup-step-two");
    }

    private TNTRunArena getEditingArena(Player player) {
        EditorManager editorManager = plugin.getEditorManager();
        String arenaName = editorManager.getEditingArenaName(player.getUniqueId());
        return arenaName == null ? null : plugin.getArenaManager().getArena(arenaName);
    }

    private int parseInt(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException exception) {
            return Integer.MIN_VALUE;
        }
    }

    private Map<String, String> replacements(String key, String value) {
        Map<String, String> replacements = new HashMap<String, String>();
        replacements.put(key, value);
        return replacements;
    }

    private Map<String, String> replacements(String key1, String value1, String key2, String value2) {
        Map<String, String> replacements = new HashMap<String, String>();
        replacements.put(key1, value1);
        replacements.put(key2, value2);
        return replacements;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(Arrays.asList(
                    "create", "edit", "save", "list", "reload",
                    "pos1", "pos2", "setglobalspawn",
                    "setspawn", "setjoinblock", "removejoinblock",
                    "join", "random", "leave"
            ), args[0]);
        }
        if (args.length == 2 && ("edit".equalsIgnoreCase(args[0]) || "join".equalsIgnoreCase(args[0]))) {
            return filter(plugin.getArenaManager().getArenaNames(), args[1]);
        }
        if (args.length == 2 && "setspawn".equalsIgnoreCase(args[0]) && sender instanceof Player) {
            TNTRunArena arena = getEditingArena((Player) sender);
            if (arena == null) {
                return Collections.emptyList();
            }
            List<String> slots = new ArrayList<String>();
            int max = Math.max(arena.getSettings().getMaxPlayers(), arena.getConfiguredSpawnCount() + 1);
            for (int i = 1; i <= max; i++) {
                slots.add(Integer.toString(i));
            }
            return filter(slots, args[1]);
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> values, String input) {
        String lowered = input == null ? "" : input.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<String>();
        for (String value : values) {
            if (value.toLowerCase(Locale.ROOT).startsWith(lowered)) {
                result.add(value);
            }
        }
        return result;
    }
}
