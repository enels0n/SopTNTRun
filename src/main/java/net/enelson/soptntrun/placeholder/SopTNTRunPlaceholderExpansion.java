package net.enelson.soptntrun.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.enelson.soptntrun.SopTNTRunPlugin;
import net.enelson.soptntrun.match.PlayerGameState;
import net.enelson.soptntrun.match.RunningMatch;
import net.enelson.soptntrun.match.WaitingMatch;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.UUID;

public final class SopTNTRunPlaceholderExpansion extends PlaceholderExpansion {

    private final SopTNTRunPlugin plugin;

    public SopTNTRunPlaceholderExpansion(SopTNTRunPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "soptntrun";
    }

    @Override
    public String getAuthor() {
        return "E_NeLsOn";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String params) {
        if (player == null || params == null) {
            return "";
        }
        String key = params.trim().toLowerCase(Locale.ROOT);
        UUID playerId = player.getUniqueId();
        WaitingMatch waiting = plugin.getMatchManager().getWaitingMatch(playerId);
        RunningMatch running = plugin.getMatchManager().getRunningMatch(playerId);
        PlayerGameState state = plugin.getMatchManager().getPlayerState(playerId);

        switch (key) {
            case "in_game":
                return state == PlayerGameState.NONE ? "no" : "yes";
            case "game_status":
                return state.name().toLowerCase(Locale.ROOT);
            case "arena":
                return plugin.getMatchManager().getTrackedArenaName(playerId);
            case "mode":
                return "solo";
            case "players_total":
                if (waiting != null) {
                    return Integer.toString(waiting.size());
                }
                return running == null ? "0" : Integer.toString(running.getPlayers().size());
            case "alive_players":
                return running == null ? "0" : Integer.toString(running.getAliveCount());
            case "countdown":
                return waiting == null ? "0" : Integer.toString(Math.max(0, waiting.getCountdownRemaining()));
            case "min_players":
                if (waiting != null) {
                    return Integer.toString(waiting.getArena().getSettings().getMinPlayers());
                }
                return running == null ? Integer.toString(plugin.getConfig().getInt("settings.default-min-players", 2))
                        : Integer.toString(running.getArena().getSettings().getMinPlayers());
            case "max_players":
                if (waiting != null) {
                    return Integer.toString(waiting.getArena().getSettings().getMaxPlayers());
                }
                return running == null ? Integer.toString(plugin.getConfig().getInt("settings.default-max-players", 12))
                        : Integer.toString(running.getArena().getSettings().getMaxPlayers());
            case "stats_games":
                return Integer.toString(plugin.getStatistics().getInt("games", playerId));
            case "stats_wins":
                return Integer.toString(plugin.getStatistics().getInt("wins", playerId));
            case "stats_losses":
                return Integer.toString(plugin.getStatistics().getInt("losses", playerId));
            case "stats_falls":
                return Integer.toString(plugin.getStatistics().getInt("falls", playerId));
            case "stats_powerups":
                return Integer.toString(plugin.getStatistics().getInt("powerups", playerId));
            case "stats_doublejumps":
                return Integer.toString(plugin.getStatistics().getInt("doublejumps", playerId));
            default:
                return null;
        }
    }
}
