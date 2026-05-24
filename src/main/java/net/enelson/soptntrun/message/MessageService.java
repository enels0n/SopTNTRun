package net.enelson.soptntrun.message;

import net.enelson.soptntrun.SopTNTRunPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.Map;

public final class MessageService {

    private final SopTNTRunPlugin plugin;

    public MessageService(SopTNTRunPlugin plugin) {
        this.plugin = plugin;
    }

    public void send(CommandSender sender, String path) {
        send(sender, path, null);
    }

    public void send(CommandSender sender, String path, Map<String, String> replacements) {
        if (sender == null) {
            return;
        }
        String message = plugin.getConfig().getString("messages." + path, "");
        if (message == null || message.trim().isEmpty()) {
            return;
        }
        String prefix = plugin.getConfig().getString("messages.prefix", "");
        String resolved = (prefix == null ? "" : prefix) + message;
        if (replacements != null) {
            for (Map.Entry<String, String> entry : replacements.entrySet()) {
                resolved = resolved.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', resolved));
    }
}
