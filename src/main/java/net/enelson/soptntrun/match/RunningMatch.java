package net.enelson.soptntrun.match;

import net.enelson.soptntrun.arena.TNTRunArena;
import net.enelson.soptntrun.powerup.PowerupType;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class RunningMatch {

    private final TNTRunArena arena;
    private final Set<UUID> players = new LinkedHashSet<UUID>();
    private final Set<UUID> alivePlayers = new LinkedHashSet<UUID>();
    private final Map<String, BlockState> savedBlocks = new LinkedHashMap<String, BlockState>();
    private final Set<String> scheduledBlocks = new LinkedHashSet<String>();
    private final Map<UUID, PowerupType> activePowerups = new LinkedHashMap<UUID, PowerupType>();
    private final List<SavedGroundItem> savedGroundItems = new ArrayList<SavedGroundItem>();
    private final Map<UUID, Integer> featherCharges = new HashMap<UUID, Integer>();
    private final Map<UUID, Integer> dashCharges = new HashMap<UUID, Integer>();
    private final Set<UUID> knockbackSnowballs = new LinkedHashSet<UUID>();
    private int powerupSpawnCooldownSeconds;
    private int endingSecondsRemaining = -1;
    private int winnerFireworkTaskId = -1;
    private UUID winner;
    private Location winnerCelebrationLocation;

    public RunningMatch(TNTRunArena arena, Set<UUID> initialPlayers) {
        this.arena = arena;
        this.players.addAll(initialPlayers);
        this.alivePlayers.addAll(initialPlayers);
        this.powerupSpawnCooldownSeconds = arena.getSettings().getPowerupSpawnIntervalSeconds();
    }

    public TNTRunArena getArena() {
        return arena;
    }

    public Set<UUID> getPlayers() {
        return Collections.unmodifiableSet(players);
    }

    public Set<UUID> getAlivePlayers() {
        return Collections.unmodifiableSet(alivePlayers);
    }

    public boolean isAlive(UUID playerId) {
        return alivePlayers.contains(playerId);
    }

    public int getAliveCount() {
        return alivePlayers.size();
    }

    public boolean eliminate(UUID playerId) {
        if (!players.contains(playerId) || !alivePlayers.remove(playerId)) {
            return false;
        }
        return true;
    }

    public void removeFromMatch(UUID playerId) {
        players.remove(playerId);
        alivePlayers.remove(playerId);
    }

    public void beginEnding(UUID winner, int endingSeconds) {
        this.winner = winner;
        this.endingSecondsRemaining = endingSeconds;
    }

    public boolean isEnding() {
        return endingSecondsRemaining >= 0;
    }

    public int tickEnding() {
        if (endingSecondsRemaining < 0) {
            return -1;
        }
        return endingSecondsRemaining--;
    }

    public UUID getWinner() {
        return winner;
    }

    public int getWinnerFireworkTaskId() {
        return winnerFireworkTaskId;
    }

    public void setWinnerFireworkTaskId(int winnerFireworkTaskId) {
        this.winnerFireworkTaskId = winnerFireworkTaskId;
    }

    public Location getWinnerCelebrationLocation() {
        return winnerCelebrationLocation == null ? null : winnerCelebrationLocation.clone();
    }

    public void setWinnerCelebrationLocation(Location winnerCelebrationLocation) {
        this.winnerCelebrationLocation = winnerCelebrationLocation == null ? null : winnerCelebrationLocation.clone();
    }

    public void saveBlock(Block block) {
        String key = key(block);
        if (!savedBlocks.containsKey(key)) {
            savedBlocks.put(key, block.getState());
        }
    }

    public boolean markScheduled(Block block) {
        return scheduledBlocks.add(key(block));
    }

    public void clearScheduled(Block block) {
        scheduledBlocks.remove(key(block));
    }

    public Iterable<BlockState> getSavedBlocks() {
        return savedBlocks.values();
    }

    public void clearSavedBlocks() {
        savedBlocks.clear();
        scheduledBlocks.clear();
    }

    public void addSavedGroundItem(SavedGroundItem item) {
        if (item != null) {
            savedGroundItems.add(item);
        }
    }

    public List<SavedGroundItem> getSavedGroundItems() {
        return savedGroundItems;
    }

    public void clearSavedGroundItems() {
        savedGroundItems.clear();
    }

    public boolean addActivePowerup(UUID itemId, PowerupType type) {
        return activePowerups.put(itemId, type) == null;
    }

    public PowerupType removeActivePowerup(UUID itemId) {
        return activePowerups.remove(itemId);
    }

    public boolean isTrackedPowerup(UUID itemId) {
        return activePowerups.containsKey(itemId);
    }

    public PowerupType getPowerupType(UUID itemId) {
        return activePowerups.get(itemId);
    }

    public int getActivePowerupCount() {
        return activePowerups.size();
    }

    public void giveFeatherCharge(UUID playerId) {
        Integer current = featherCharges.get(playerId);
        featherCharges.put(playerId, current == null ? 1 : current + 1);
    }

    public int getFeatherCharges(UUID playerId) {
        Integer current = featherCharges.get(playerId);
        return current == null ? 0 : current.intValue();
    }

    public boolean consumeFeatherCharge(UUID playerId) {
        int current = getFeatherCharges(playerId);
        if (current <= 0) {
            return false;
        }
        if (current == 1) {
            featherCharges.remove(playerId);
        } else {
            featherCharges.put(playerId, current - 1);
        }
        return true;
    }

    public void giveDashCharge(UUID playerId) {
        Integer current = dashCharges.get(playerId);
        dashCharges.put(playerId, current == null ? 1 : current + 1);
    }

    public int getDashCharges(UUID playerId) {
        Integer current = dashCharges.get(playerId);
        return current == null ? 0 : current.intValue();
    }

    public boolean consumeDashCharge(UUID playerId) {
        int current = getDashCharges(playerId);
        if (current <= 0) {
            return false;
        }
        if (current == 1) {
            dashCharges.remove(playerId);
        } else {
            dashCharges.put(playerId, current - 1);
        }
        return true;
    }

    public void trackKnockbackSnowball(UUID projectileId) {
        knockbackSnowballs.add(projectileId);
    }

    public boolean isTrackedKnockbackSnowball(UUID projectileId) {
        return knockbackSnowballs.contains(projectileId);
    }

    public boolean untrackKnockbackSnowball(UUID projectileId) {
        return knockbackSnowballs.remove(projectileId);
    }

    public int tickPowerupCooldown() {
        return powerupSpawnCooldownSeconds--;
    }

    public void resetPowerupCooldown() {
        powerupSpawnCooldownSeconds = arena.getSettings().getPowerupSpawnIntervalSeconds();
    }

    public int getPowerupSpawnCooldownSeconds() {
        return powerupSpawnCooldownSeconds;
    }

    public void clearPowerups() {
        activePowerups.clear();
        featherCharges.clear();
        dashCharges.clear();
        knockbackSnowballs.clear();
    }

    private String key(Block block) {
        return block.getWorld().getName() + ":" + block.getX() + ":" + block.getY() + ":" + block.getZ();
    }
}
