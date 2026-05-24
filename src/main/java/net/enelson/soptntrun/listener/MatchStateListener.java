package net.enelson.soptntrun.listener;

import net.enelson.soptntrun.SopTNTRunPlugin;
import net.enelson.soptntrun.arena.TNTRunArena;
import net.enelson.soptntrun.match.PlayerGameState;
import net.enelson.soptntrun.match.RunningMatch;
import net.enelson.soptntrun.match.WaitingMatch;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public final class MatchStateListener implements Listener {

    private final SopTNTRunPlugin plugin;

    public MatchStateListener(SopTNTRunPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        PlayerGameState state = plugin.getMatchManager().getPlayerState(playerId);
        if (event.getTo() == null) {
            return;
        }

        if (state == PlayerGameState.NONE) {
            if (!movedBlock(event.getFrom(), event.getTo())) {
                return;
            }
            TNTRunArena arena = plugin.getArenaManager().findArenaByJoinBlock(event.getTo().getBlock().getLocation());
            if (arena == null) {
                return;
            }
            String result = plugin.getMatchManager().joinSpecific(player, arena.getName());
            if ("OK".equals(result)) {
                plugin.getMessageService().send(player, "joined-arena", java.util.Collections.singletonMap("name", arena.getName()));
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
            return;
        }

        if (state == PlayerGameState.WAITING || state == PlayerGameState.STARTING) {
            WaitingMatch waiting = plugin.getMatchManager().getWaitingMatch(playerId);
            if (waiting == null || !movedBlock(event.getFrom(), event.getTo())) {
                return;
            }
            if (waiting.getArena().getGameplayArea() != null
                    && event.getTo().getY() < waiting.getArena().getGameplayArea().getMinY()) {
                plugin.getMatchManager().teleportToAssignedSpawn(player);
            }
            return;
        }

        RunningMatch running = plugin.getMatchManager().getRunningMatch(playerId);
        if (running == null) {
            return;
        }

        if (state != PlayerGameState.RUNNING) {
            if (state == PlayerGameState.ENDING) {
                plugin.getMatchManager().enforceWinnerCelebrationBounds(player, running);
            }
            return;
        }
        if (running.getArena().getGameplayArea() != null
                && event.getTo().getY() < running.getArena().getGameplayArea().getMinY()) {
            plugin.getMatchManager().eliminate(player, true);
            return;
        }
        if (movedBlock(event.getFrom(), event.getTo())) {
            plugin.getMatchManager().handleFloorTrigger(player, running);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Player)) {
            return;
        }
        if (plugin.getMatchManager().getPlayerState(((Player) entity).getUniqueId()) != PlayerGameState.NONE) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        PlayerGameState state = plugin.getMatchManager().getPlayerState(player.getUniqueId());
        if (state == PlayerGameState.WAITING || state == PlayerGameState.STARTING) {
            plugin.getMatchManager().leave(player);
            return;
        }
        if (state == PlayerGameState.RUNNING) {
            plugin.getMatchManager().handleDisconnect(player);
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        if (plugin.getMatchManager().getPlayerState(event.getPlayer().getUniqueId()) != PlayerGameState.NONE) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        if (plugin.getMatchManager().getPlayerState(event.getPlayer().getUniqueId()) != PlayerGameState.NONE) {
            event.setCancelled(true);
        }
    }

    private boolean movedBlock(Location from, Location to) {
        return from.getBlockX() != to.getBlockX()
                || from.getBlockY() != to.getBlockY()
                || from.getBlockZ() != to.getBlockZ();
    }
}
