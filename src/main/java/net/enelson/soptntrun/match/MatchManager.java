package net.enelson.soptntrun.match;

import net.enelson.soptntrun.SopTNTRunPlugin;
import net.enelson.soptntrun.arena.ArenaState;
import net.enelson.soptntrun.event.TntRunFinishEvent;
import net.enelson.soptntrun.arena.TNTRunArena;
import net.enelson.soptntrun.listener.ControlItemListener;
import net.enelson.soptntrun.model.PowerupSpawnShape;
import net.enelson.soptntrun.model.SerializedCuboid;
import net.enelson.soptntrun.model.SerializedLocation;
import net.enelson.soptntrun.powerup.PowerupType;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Firework;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class MatchManager {

    private final SopTNTRunPlugin plugin;
    private final Map<String, WaitingMatch> waitingMatches = new LinkedHashMap<String, WaitingMatch>();
    private final Map<String, RunningMatch> runningMatches = new LinkedHashMap<String, RunningMatch>();
    private final Map<UUID, String> trackedArenaNames = new LinkedHashMap<UUID, String>();
    private final Map<UUID, PlayerGameState> playerStates = new LinkedHashMap<UUID, PlayerGameState>();
    private final Map<UUID, SavedPlayerState> savedPlayerStates = new LinkedHashMap<UUID, SavedPlayerState>();
    private int tickerTaskId = -1;
    private int floorTaskId = -1;

    public MatchManager(SopTNTRunPlugin plugin) {
        this.plugin = plugin;
    }

    public void reset() {
        if (tickerTaskId != -1) {
            Bukkit.getScheduler().cancelTask(tickerTaskId);
            tickerTaskId = -1;
        }
        if (floorTaskId != -1) {
            Bukkit.getScheduler().cancelTask(floorTaskId);
            floorTaskId = -1;
        }
        waitingMatches.clear();
        for (RunningMatch match : runningMatches.values()) {
            cancelWinnerFireworks(match);
        }
        runningMatches.clear();
        trackedArenaNames.clear();
        playerStates.clear();
        savedPlayerStates.clear();
    }

    public void shutdownAndEvacuate() {
        if (tickerTaskId != -1) {
            Bukkit.getScheduler().cancelTask(tickerTaskId);
            tickerTaskId = -1;
        }
        if (floorTaskId != -1) {
            Bukkit.getScheduler().cancelTask(floorTaskId);
            floorTaskId = -1;
        }

        for (RunningMatch match : new ArrayList<RunningMatch>(runningMatches.values())) {
            cancelWinnerFireworks(match);
            restoreArenaGroundItems(match);
            removeTrackedPowerups(match);
            match.clearPowerups();
            match.clearSavedBlocks();
        }

        Set<UUID> trackedPlayers = new LinkedHashSet<UUID>();
        trackedPlayers.addAll(trackedArenaNames.keySet());
        trackedPlayers.addAll(savedPlayerStates.keySet());
        for (UUID playerId : trackedPlayers) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                restoreAndSendToGlobalSpawn(player);
            }
            trackedArenaNames.remove(playerId);
            playerStates.remove(playerId);
            savedPlayerStates.remove(playerId);
        }

        waitingMatches.clear();
        runningMatches.clear();
    }

    public void startTicker() {
        if (tickerTaskId != -1) {
            Bukkit.getScheduler().cancelTask(tickerTaskId);
        }
        if (floorTaskId != -1) {
            Bukkit.getScheduler().cancelTask(floorTaskId);
        }
        tickerTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
            @Override
            public void run() {
                tickWaitingMatches();
                tickRunningMatches();
            }
        }, 20L, 20L);
        floorTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
            @Override
            public void run() {
                tickRunningPlayersFloor();
            }
        }, 1L, 1L);
    }

    public WaitingMatch getWaitingMatch(UUID playerId) {
        String arenaName = trackedArenaNames.get(playerId);
        return arenaName == null ? null : waitingMatches.get(normalize(arenaName));
    }

    public RunningMatch getRunningMatch(UUID playerId) {
        String arenaName = trackedArenaNames.get(playerId);
        return arenaName == null ? null : runningMatches.get(normalize(arenaName));
    }

    public Iterable<RunningMatch> getRunningMatches() {
        return Collections.unmodifiableCollection(runningMatches.values());
    }

    public String getTrackedArenaName(UUID playerId) {
        String name = trackedArenaNames.get(playerId);
        return name == null ? "" : name;
    }

    public PlayerGameState getPlayerState(UUID playerId) {
        PlayerGameState state = playerStates.get(playerId);
        return state == null ? PlayerGameState.NONE : state;
    }

    public String joinSpecific(Player actor, String arenaName) {
        TNTRunArena arena = plugin.getArenaManager().getArena(arenaName);
        if (arena == null) {
            return "NO_ARENA";
        }
        if (!isArenaMatchmakingCandidate(arena)) {
            return "UNAVAILABLE";
        }
        List<Player> members = resolveOnlineMembers(actor);
        if (members.isEmpty()) {
            return "EMPTY";
        }
        int free = arena.getSettings().getMaxPlayers() - getWaitingSize(arena.getName());
        if (free < members.size()) {
            return "NO_ROOM";
        }
        return addPlayersToWaitingArena(arena, members) ? "OK" : "ALREADY";
    }

    public String joinRandom(Player actor) {
        List<Player> members = resolveOnlineMembers(actor);
        if (members.isEmpty()) {
            return "EMPTY";
        }

        List<TNTRunArena> waiting = new ArrayList<TNTRunArena>();
        List<TNTRunArena> empty = new ArrayList<TNTRunArena>();
        for (TNTRunArena arena : plugin.getArenaManager().getArenas()) {
            if (!isArenaMatchmakingCandidate(arena)) {
                continue;
            }
            int current = getWaitingSize(arena.getName());
            int free = arena.getSettings().getMaxPlayers() - current;
            if (free < members.size()) {
                continue;
            }
            if (current > 0) {
                waiting.add(arena);
            } else {
                empty.add(arena);
            }
        }

        TNTRunArena selected = !waiting.isEmpty()
                ? waiting.get((int) (Math.random() * waiting.size()))
                : (!empty.isEmpty() ? empty.get((int) (Math.random() * empty.size())) : null);
        if (selected == null) {
            return "NO_ROOM";
        }
        return addPlayersToWaitingArena(selected, members) ? "OK" : "ALREADY";
    }

    public boolean leave(Player player) {
        UUID playerId = player.getUniqueId();
        WaitingMatch match = getWaitingMatch(playerId);
        if (match == null) {
            return false;
        }
        if (plugin.getPartyBridge().isLeader(player)) {
            List<UUID> memberIds = new ArrayList<UUID>(plugin.getPartyBridge().getMemberUuids(player));
            for (UUID memberId : memberIds) {
                if (!match.has(memberId)) {
                    continue;
                }
                Player member = Bukkit.getPlayer(memberId);
                match.remove(memberId);
                trackedArenaNames.remove(memberId);
                playerStates.remove(memberId);
                if (member != null && member.isOnline()) {
                    restoreAndSendToGlobalSpawn(member);
                    if (!memberId.equals(playerId)) {
                        plugin.getMessageService().send(member, "left-arena");
                    }
                } else {
                    savedPlayerStates.remove(memberId);
                }
            }
        } else {
            match.remove(playerId);
            trackedArenaNames.remove(playerId);
            playerStates.remove(playerId);
            restoreAndSendToGlobalSpawn(player);
        }
        if (match.isEmpty()) {
            waitingMatches.remove(normalize(match.getArena().getName()));
        }
        return true;
    }

    public void handleDisconnect(Player player) {
        eliminate(player, false);
    }

    public void eliminate(Player player, boolean teleportToSpectator) {
        if (player == null) {
            return;
        }
        RunningMatch match = getRunningMatch(player.getUniqueId());
        if (match == null || !match.eliminate(player.getUniqueId())) {
            return;
        }
        trackedArenaNames.remove(player.getUniqueId());
        playerStates.remove(player.getUniqueId());
        match.removeFromMatch(player.getUniqueId());
        plugin.getStatistics().increment("losses", player.getUniqueId());
        plugin.getStatistics().increment("falls", player.getUniqueId());
        plugin.getMessageService().send(player, "eliminated");
        Bukkit.getPluginManager().callEvent(new TntRunFinishEvent(player, false));
        restoreAndSendToGlobalSpawn(player);
        if (!match.isEnding() && match.getAliveCount() <= 1) {
            UUID winner = match.getAlivePlayers().isEmpty() ? null : match.getAlivePlayers().iterator().next();
            beginEnding(match, winner);
        }
    }

    public void handleFloorTrigger(Player player, RunningMatch match) {
        Block topBlock = findBreakableBlock(player.getLocation().clone().add(0.0D, -1.0D, 0.0D), match.getArena());
        if (topBlock == null) {
            return;
        }
        final Block bottomBlock = topBlock.getRelative(BlockFace.DOWN);
        if (!match.markScheduled(topBlock)) {
            return;
        }
        final long delay = match.getArena().getSettings().getDefaultDestroyDelayTicks();
        match.saveBlock(topBlock);
        match.saveBlock(bottomBlock);
        new BukkitRunnable() {
            @Override
            public void run() {
                RunningMatch current = getRunningMatch(player.getUniqueId());
                if (current == null || current != match) {
                    return;
                }
                match.clearScheduled(topBlock);
                topBlock.setType(Material.AIR);
                bottomBlock.setType(Material.AIR);
            }
        }.runTaskLater(plugin, delay);
    }

    private boolean addPlayersToWaitingArena(TNTRunArena arena, List<Player> players) {
        WaitingMatch match = waitingMatches.get(normalize(arena.getName()));
        if (match == null) {
            match = new WaitingMatch(arena);
            waitingMatches.put(normalize(arena.getName()), match);
        }
        for (Player player : players) {
            if (playerStates.containsKey(player.getUniqueId()) && playerStates.get(player.getUniqueId()) != PlayerGameState.NONE) {
                return false;
            }
        }
        for (Player player : players) {
            savePlayerStateIfNeeded(player);
            match.add(player.getUniqueId());
            trackedArenaNames.put(player.getUniqueId(), arena.getName());
            playerStates.put(player.getUniqueId(), PlayerGameState.WAITING);
            preparePlayerForArena(player);
            teleportWaitingPlayerToArenaSpawn(player, match);
            ControlItemListener.giveLeaveQueueItem(player);
        }
        return true;
    }

    private void tickWaitingMatches() {
        List<WaitingMatch> snapshot = new ArrayList<WaitingMatch>(waitingMatches.values());
        for (WaitingMatch match : snapshot) {
            if (match.size() < match.getArena().getSettings().getMinPlayers()) {
                if (match.hasCountdown()) {
                    match.resetCountdown();
                    match.getArena().setState(ArenaState.WAITING);
                    for (UUID playerId : match.getPlayers()) {
                        playerStates.put(playerId, PlayerGameState.WAITING);
                    }
                    broadcast(match.getPlayers(), "match-cancelled");
                }
                continue;
            }
            if (!match.hasCountdown()) {
                match.startCountdown(match.getArena().getSettings().getCountdownSeconds());
                match.getArena().setState(ArenaState.STARTING);
                for (UUID playerId : match.getPlayers()) {
                    playerStates.put(playerId, PlayerGameState.STARTING);
                }
            }
            int seconds = match.tickCountdown();
            broadcast(match.getPlayers(), "countdown", replacements("seconds", Integer.toString(Math.max(0, seconds))));
            if (seconds <= 0) {
                startMatch(match);
            }
        }
    }

    private void tickRunningMatches() {
        List<RunningMatch> snapshot = new ArrayList<RunningMatch>(runningMatches.values());
        for (RunningMatch match : snapshot) {
            if (!match.isEnding()) {
                int cooldown = match.tickPowerupCooldown();
                if (cooldown <= 0) {
                    spawnPowerupIfPossible(match);
                    match.resetPowerupCooldown();
                }
            }
            if (!match.isEnding()) {
                continue;
            }
            int current = match.tickEnding();
            if (current <= 0) {
                finishMatch(match);
            }
        }
    }

    private void tickRunningPlayersFloor() {
        for (RunningMatch match : new ArrayList<RunningMatch>(runningMatches.values())) {
            for (UUID playerId : match.getAlivePlayers()) {
                Player player = Bukkit.getPlayer(playerId);
                if (player == null || !player.isOnline() || playerStates.get(playerId) != PlayerGameState.RUNNING) {
                    continue;
                }
                handleFloorTrigger(player, match);
            }
        }
    }

    private void startMatch(WaitingMatch waiting) {
        TNTRunArena arena = waiting.getArena();
        List<UUID> orderedPlayers = new ArrayList<UUID>(waiting.getOrderedPlayers());
        if (orderedPlayers.isEmpty()) {
            waitingMatches.remove(normalize(arena.getName()));
            arena.setState(ArenaState.WAITING);
            return;
        }
        if (arena.getConfiguredSpawnCount() < orderedPlayers.size()) {
            waiting.resetCountdown();
            arena.setState(ArenaState.WAITING);
            for (UUID playerId : waiting.getPlayers()) {
                playerStates.put(playerId, PlayerGameState.WAITING);
            }
            return;
        }

        waitingMatches.remove(normalize(arena.getName()));
        RunningMatch running = new RunningMatch(arena, new LinkedHashSet<UUID>(orderedPlayers));
        runningMatches.put(normalize(arena.getName()), running);
        arena.setState(ArenaState.RUNNING);

        captureArenaGroundItems(running);
        for (int i = 0; i < orderedPlayers.size(); i++) {
            UUID playerId = orderedPlayers.get(i);
            Player player = Bukkit.getPlayer(playerId);
            playerStates.put(playerId, PlayerGameState.RUNNING);
            if (player == null || !player.isOnline()) {
                continue;
            }
            player.getInventory().clear();
            player.setGameMode(GameMode.SURVIVAL);
            player.setAllowFlight(false);
            plugin.getStatistics().increment("games", playerId);
            plugin.getMessageService().send(player, "match-started", replacements("name", arena.getName()));
        }
    }

    private void beginEnding(RunningMatch match, UUID winnerId) {
        match.beginEnding(winnerId, 10);
        match.getArena().setState(ArenaState.ENDING);
        if (winnerId != null) {
            plugin.getStatistics().increment("wins", winnerId);
            Player winner = Bukkit.getPlayer(winnerId);
            String winnerName = winner == null ? winnerId.toString() : winner.getName();
            broadcast(match.getPlayers(), "winner", replacements("player", winnerName, "name", match.getArena().getName()));
            if (winner != null && winner.isOnline()) {
                Bukkit.getPluginManager().callEvent(new TntRunFinishEvent(winner, true));
                Location celebration = getWinnerCelebrationLocation(match.getArena());
                match.setWinnerCelebrationLocation(celebration);
                winner.setAllowFlight(true);
                winner.setFlying(true);
                if (celebration != null) {
                    winner.teleport(celebration);
                }
                startWinnerFireworks(match, winner);
            }
        }
        for (UUID playerId : match.getPlayers()) {
            playerStates.put(playerId, PlayerGameState.ENDING);
        }
    }

    private void finishMatch(RunningMatch match) {
        TNTRunArena arena = match.getArena();
        cancelWinnerFireworks(match);
        for (BlockState state : match.getSavedBlocks()) {
            state.update(true, false);
        }
        match.clearSavedBlocks();
        restoreArenaGroundItems(match);
        removeTrackedPowerups(match);
        match.clearPowerups();

        for (UUID playerId : match.getPlayers()) {
            Player player = Bukkit.getPlayer(playerId);
            trackedArenaNames.remove(playerId);
            playerStates.remove(playerId);
            if (player != null && player.isOnline()) {
                restoreAndSendToGlobalSpawn(player);
            } else {
                savedPlayerStates.remove(playerId);
            }
        }
        runningMatches.remove(normalize(arena.getName()));
        arena.setState(ArenaState.WAITING);
    }

    private Block findBreakableBlock(Location location, TNTRunArena arena) {
        if (location == null || location.getWorld() == null || arena.getGameplayArea() == null) {
            return null;
        }
        int startY = location.getBlockY() + 1;
        for (int depth = 0; depth <= 1; depth++) {
            Block block = getBlockUnderPlayer(location.getWorld(), location.getX(), startY - depth, location.getZ(), arena);
            if (block != null) {
                return block;
            }
        }
        return null;
    }

    private Block getBlockUnderPlayer(org.bukkit.World world, double x, int y, double z, TNTRunArena arena) {
        final double playerBoundingBoxAdd = 0.3D;
        List<Block> candidates = Arrays.asList(
                world.getBlockAt((int) Math.floor(x + playerBoundingBoxAdd), y, (int) Math.floor(z - playerBoundingBoxAdd)),
                world.getBlockAt((int) Math.floor(x - playerBoundingBoxAdd), y, (int) Math.floor(z + playerBoundingBoxAdd)),
                world.getBlockAt((int) Math.floor(x + playerBoundingBoxAdd), y, (int) Math.floor(z + playerBoundingBoxAdd)),
                world.getBlockAt((int) Math.floor(x - playerBoundingBoxAdd), y, (int) Math.floor(z - playerBoundingBoxAdd))
        );
        for (Block block : candidates) {
            if (!isInsideGameplay(arena, block.getLocation())) {
                continue;
            }
            if (block.getType() != Material.AIR) {
                return block;
            }
        }
        return null;
    }

    private int getWaitingSize(String arenaName) {
        WaitingMatch match = waitingMatches.get(normalize(arenaName));
        return match == null ? 0 : match.size();
    }

    private boolean isArenaMatchmakingCandidate(TNTRunArena arena) {
        return arena.getState() != ArenaState.DISABLED
                && arena.getState() != ArenaState.EDITING
                && arena.getState() != ArenaState.RUNNING
                && arena.getState() != ArenaState.ENDING
                && arena.getState() != ArenaState.RESETTING;
    }

    private List<Player> resolveOnlineMembers(Player actor) {
        List<UUID> memberIds = plugin.getPartyBridge().getMemberUuids(actor);
        List<Player> players = new ArrayList<Player>();
        for (UUID memberId : memberIds) {
            Player member = Bukkit.getPlayer(memberId);
            if (member == null || !member.isOnline()) {
                return new ArrayList<Player>();
            }
            players.add(member);
        }
        return players;
    }

    private void broadcast(Set<UUID> players, String path) {
        broadcast(players, path, Collections.<String, String>emptyMap());
    }

    private void broadcast(Set<UUID> players, String path, Map<String, String> replacements) {
        for (UUID playerId : players) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                plugin.getMessageService().send(player, path, replacements);
            }
        }
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

    private void spawnPowerupIfPossible(RunningMatch match) {
        if (match.getActivePowerupCount() >= match.getArena().getSettings().getMaxActivePowerups()) {
            return;
        }
        Location spawn = findRandomPowerupSpawn(match.getArena());
        if (spawn == null) {
            return;
        }

        PowerupType type = randomPowerupType();
        ItemStack itemStack = new ItemStack(type.getMaterial(), 1);
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(type.getDisplayName());
            itemStack.setItemMeta(meta);
        }

        final Item item = spawn.getWorld().dropItem(spawn, itemStack);
        item.setPickupDelay(0);
        item.setVelocity(item.getVelocity().zero());
        match.addActivePowerup(item.getUniqueId(), type);

        new BukkitRunnable() {
            @Override
            public void run() {
                RunningMatch current = runningMatches.get(normalize(match.getArena().getName()));
                if (current == null || current != match) {
                    return;
                }
                if (current.removeActivePowerup(item.getUniqueId()) != null && item.isValid()) {
                    item.remove();
                }
            }
        }.runTaskLater(plugin, match.getArena().getSettings().getPowerupDespawnSeconds() * 20L);
    }

    private Location findRandomPowerupSpawn(TNTRunArena arena) {
        SerializedCuboid area = arena.getGameplayArea();
        if (area == null) {
            return null;
        }
        org.bukkit.World world = Bukkit.getWorld(arena.getWorldName());
        if (world == null) {
            return null;
        }
        double centerX = (area.getMinX() + area.getMaxX() + 1.0D) / 2.0D;
        double centerZ = (area.getMinZ() + area.getMaxZ() + 1.0D) / 2.0D;
        double radius = resolvePowerupSpawnRadius(arena);
        PowerupSpawnShape shape = arena.getSettings().getPowerupSpawnShape();
        for (int attempt = 0; attempt < 64; attempt++) {
            Location probe = shape == PowerupSpawnShape.SQUARE
                    ? randomizeSquareLocation(world, centerX, centerZ, radius)
                    : randomizeCircleLocation(world, centerX, centerZ, radius);
            int x = probe.getBlockX();
            int z = probe.getBlockZ();
            if (x < area.getMinX() || x > area.getMaxX() || z < area.getMinZ() || z > area.getMaxZ()) {
                continue;
            }
            for (int y = area.getMaxY(); y >= area.getMinY(); y--) {
                Block top = world.getBlockAt(x, y, z);
                if (top.getType() == Material.AIR) {
                    continue;
                }
                return top.getLocation().add(0.5D, 1.15D, 0.5D);
            }
        }
        return null;
    }

    private void removeTrackedPowerups(RunningMatch match) {
        for (org.bukkit.World world : Bukkit.getWorlds()) {
            for (Item item : world.getEntitiesByClass(Item.class)) {
                if (match.isTrackedPowerup(item.getUniqueId())) {
                    item.remove();
                }
            }
        }
    }

    private void captureArenaGroundItems(RunningMatch match) {
        SerializedCuboid area = match.getArena().getGameplayArea();
        org.bukkit.World world = Bukkit.getWorld(match.getArena().getWorldName());
        if (area == null || world == null) {
            return;
        }
        for (Item item : world.getEntitiesByClass(Item.class)) {
            if (item == null || !item.isValid() || item.isDead()) {
                continue;
            }
            if (!isInsideGameplay(match.getArena(), item.getLocation())) {
                continue;
            }
            match.addSavedGroundItem(SavedGroundItem.capture(item));
            item.remove();
        }
    }

    private void restoreArenaGroundItems(RunningMatch match) {
        for (SavedGroundItem savedGroundItem : match.getSavedGroundItems()) {
            savedGroundItem.restore();
        }
        match.clearSavedGroundItems();
    }

    private void savePlayerStateIfNeeded(Player player) {
        if (!savedPlayerStates.containsKey(player.getUniqueId())) {
            savedPlayerStates.put(player.getUniqueId(), SavedPlayerState.capture(player));
        }
    }

    private void preparePlayerForArena(Player player) {
        player.setGameMode(GameMode.ADVENTURE);
        player.setAllowFlight(false);
        player.setFlying(false);
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setSaturation(20.0F);
        player.setFireTicks(0);
        player.setFallDistance(0.0F);
        player.setExp(0.0F);
        player.setLevel(0);
    }

    private void teleportWaitingPlayerToArenaSpawn(Player player, WaitingMatch match) {
        List<UUID> orderedPlayers = match.getOrderedPlayers();
        int index = orderedPlayers.indexOf(player.getUniqueId());
        if (index < 0) {
            return;
        }
        SerializedLocation spawn = match.getArena().getSpawn(index + 1);
        if (spawn == null) {
            return;
        }
        Location location = spawn.toBukkit();
        if (location != null) {
            player.teleport(location);
        }
    }

    private void restoreAndSendToGlobalSpawn(Player player) {
        SavedPlayerState saved = savedPlayerStates.remove(player.getUniqueId());
        if (saved != null) {
            saved.restore(player);
        }
        SerializedLocation globalSpawn = plugin.getTNTRunConfig().getGlobalSpawn();
        if (globalSpawn != null) {
            Location destination = globalSpawn.toBukkit();
            if (destination != null) {
                player.teleport(destination);
            }
        }
    }

    public void teleportToAssignedSpawn(Player player) {
        WaitingMatch waitingMatch = getWaitingMatch(player.getUniqueId());
        if (waitingMatch != null) {
            teleportWaitingPlayerToArenaSpawn(player, waitingMatch);
            return;
        }
        RunningMatch runningMatch = getRunningMatch(player.getUniqueId());
        if (runningMatch == null) {
            return;
        }
        List<UUID> orderedPlayers = new ArrayList<UUID>(runningMatch.getPlayers());
        int index = orderedPlayers.indexOf(player.getUniqueId());
        if (index < 0) {
            return;
        }
        SerializedLocation spawn = runningMatch.getArena().getSpawn(index + 1);
        if (spawn == null) {
            return;
        }
        Location location = spawn.toBukkit();
        if (location != null) {
            player.teleport(location);
        }
    }

    public void enforceWinnerCelebrationBounds(Player player, RunningMatch match) {
        if (player == null || match == null || match.getWinner() == null) {
            return;
        }
        if (!player.getUniqueId().equals(match.getWinner())) {
            return;
        }
        Location celebration = match.getWinnerCelebrationLocation();
        if (celebration == null) {
            celebration = getWinnerCelebrationLocation(match.getArena());
            match.setWinnerCelebrationLocation(celebration);
        }
        if (celebration == null) {
            return;
        }
        if (match.getArena().getGameplayArea() != null && !match.getArena().getGameplayArea().contains(player.getLocation())) {
            player.teleport(celebration);
        }
        if (!player.getAllowFlight()) {
            player.setAllowFlight(true);
        }
        if (!player.isFlying()) {
            player.setFlying(true);
        }
    }

    private boolean isInsideGameplay(TNTRunArena arena, Location location) {
        return arena.getGameplayArea() != null && arena.getGameplayArea().contains(location);
    }

    private Location getWinnerCelebrationLocation(TNTRunArena arena) {
        SerializedCuboid area = arena.getGameplayArea();
        if (area == null) {
            return null;
        }
        org.bukkit.World world = Bukkit.getWorld(arena.getWorldName());
        if (world == null) {
            return null;
        }
        double x = (area.getMinX() + area.getMaxX() + 1.0D) / 2.0D;
        double z = (area.getMinZ() + area.getMaxZ() + 1.0D) / 2.0D;
        double y = area.getMaxY() + 0.1D;
        return new Location(world, x, y, z);
    }

    private void startWinnerFireworks(final RunningMatch match, final Player winner) {
        if (!plugin.getConfig().getBoolean("settings.winner-fireworks-enabled", true)) {
            return;
        }
        final double radius = resolveWinnerFireworksRadius(match.getArena());
        int interval = Math.max(2, match.getArena().getSettings().getWinnerFireworksIntervalTicks());
        int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
            @Override
            public void run() {
                RunningMatch current = getRunningMatch(winner.getUniqueId());
                if (current == null || current != match || !match.isEnding() || !winner.isOnline()) {
                    cancelWinnerFireworks(match);
                    return;
                }
                Location spawnLocation = match.getWinnerCelebrationLocation();
                if (spawnLocation == null) {
                    spawnLocation = winner.getLocation();
                }
                spawnLocation = randomizeFireworkLocation(spawnLocation, radius);
                Firework firework = winner.getWorld().spawn(spawnLocation, Firework.class);
                FireworkMeta meta = firework.getFireworkMeta();
                meta.addEffect(FireworkEffect.builder()
                        .with(FireworkEffect.Type.BALL_LARGE)
                        .withColor(Color.GREEN, Color.RED, Color.ORANGE)
                        .flicker(true)
                        .trail(true)
                        .build());
                meta.setPower(1);
                firework.setFireworkMeta(meta);
            }
        }, 0L, interval);
        match.setWinnerFireworkTaskId(taskId);
    }

    private Location randomizeFireworkLocation(Location origin, double radius) {
        if (origin == null || radius <= 0.0D) {
            return origin;
        }
        double angle = Math.random() * Math.PI * 2.0D;
        double distance = Math.random() * radius;
        return origin.clone().add(Math.cos(angle) * distance, 0.0D, Math.sin(angle) * distance);
    }

    private Location randomizeCircleLocation(org.bukkit.World world, double centerX, double centerZ, double radius) {
        double safeRadius = Math.max(0.5D, radius);
        double angle = Math.random() * Math.PI * 2.0D;
        double distance = Math.sqrt(Math.random()) * safeRadius;
        double x = centerX + Math.cos(angle) * distance;
        double z = centerZ + Math.sin(angle) * distance;
        return new Location(world, x, 0.0D, z);
    }

    private Location randomizeSquareLocation(org.bukkit.World world, double centerX, double centerZ, double radius) {
        double safeRadius = Math.max(0.5D, radius);
        double x = centerX + ((Math.random() * 2.0D) - 1.0D) * safeRadius;
        double z = centerZ + ((Math.random() * 2.0D) - 1.0D) * safeRadius;
        return new Location(world, x, 0.0D, z);
    }

    private double resolvePowerupSpawnRadius(TNTRunArena arena) {
        double configured = arena.getSettings().getPowerupSpawnRadius();
        if (configured > 0.0D) {
            return configured;
        }
        SerializedCuboid area = arena.getGameplayArea();
        if (area == null) {
            return 2.0D;
        }
        double widthX = Math.max(1.0D, area.getMaxX() - area.getMinX() + 1.0D);
        double widthZ = Math.max(1.0D, area.getMaxZ() - area.getMinZ() + 1.0D);
        return Math.min(widthX, widthZ) / 2.0D;
    }

    private double resolveWinnerFireworksRadius(TNTRunArena arena) {
        double configured = arena.getSettings().getWinnerFireworksRadius();
        if (configured > 0.0D) {
            return configured;
        }
        SerializedCuboid area = arena.getGameplayArea();
        if (area == null) {
            return 2.0D;
        }
        double widthX = Math.max(1.0D, area.getMaxX() - area.getMinX() + 1.0D);
        double widthZ = Math.max(1.0D, area.getMaxZ() - area.getMinZ() + 1.0D);
        return Math.max(widthX, widthZ) / 2.0D;
    }

    private void cancelWinnerFireworks(RunningMatch match) {
        if (match == null) {
            return;
        }
        int taskId = match.getWinnerFireworkTaskId();
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            match.setWinnerFireworkTaskId(-1);
        }
    }

    private PowerupType randomPowerupType() {
        PowerupType[] values = PowerupType.values();
        return values[(int) Math.floor(Math.random() * values.length)];
    }

    private int randomBetween(int min, int max) {
        if (max <= min) {
            return min;
        }
        return min + (int) Math.floor(Math.random() * ((max - min) + 1));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
