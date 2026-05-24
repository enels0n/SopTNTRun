package net.enelson.soptntrun.arena;

import net.enelson.soptntrun.model.ArenaSettings;
import net.enelson.soptntrun.model.SerializedCuboid;
import net.enelson.soptntrun.model.SerializedLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class TNTRunArena {

    private final String name;
    private final String worldName;
    private ArenaState state;
    private ArenaSettings settings;
    private SerializedCuboid gameplayArea;
    private SerializedLocation spectatorSpawn;
    private final List<SerializedLocation> spawns = new ArrayList<SerializedLocation>();
    private final List<SerializedLocation> joinBlocks = new ArrayList<SerializedLocation>();

    public TNTRunArena(String name, String worldName, ArenaState state, ArenaSettings settings) {
        this.name = name;
        this.worldName = worldName;
        this.state = state;
        this.settings = settings;
    }

    public String getName() {
        return name;
    }

    public String getWorldName() {
        return worldName;
    }

    public ArenaState getState() {
        return state;
    }

    public void setState(ArenaState state) {
        this.state = state;
    }

    public ArenaSettings getSettings() {
        return settings;
    }

    public void setSettings(ArenaSettings settings) {
        this.settings = settings;
    }

    public SerializedCuboid getGameplayArea() {
        return gameplayArea;
    }

    public void setGameplayArea(SerializedCuboid gameplayArea) {
        this.gameplayArea = gameplayArea;
    }

    public SerializedLocation getSpectatorSpawn() {
        return spectatorSpawn;
    }

    public void setSpectatorSpawn(SerializedLocation spectatorSpawn) {
        this.spectatorSpawn = spectatorSpawn;
    }

    public void setSpawn(int slot, SerializedLocation location) {
        while (spawns.size() < slot) {
            spawns.add(null);
        }
        spawns.set(slot - 1, location);
    }

    public SerializedLocation getSpawn(int slot) {
        if (slot < 1 || slot > spawns.size()) {
            return null;
        }
        return spawns.get(slot - 1);
    }

    public List<SerializedLocation> getSpawns() {
        return Collections.unmodifiableList(spawns);
    }

    public void addJoinBlock(SerializedLocation location) {
        if (location == null) {
            return;
        }
        for (SerializedLocation existing : joinBlocks) {
            if (sameBlock(existing, location)) {
                return;
            }
        }
        joinBlocks.add(location);
    }

    public boolean removeJoinBlock(SerializedLocation location) {
        if (location == null) {
            return false;
        }
        for (int i = 0; i < joinBlocks.size(); i++) {
            if (sameBlock(joinBlocks.get(i), location)) {
                joinBlocks.remove(i);
                return true;
            }
        }
        return false;
    }

    public List<SerializedLocation> getJoinBlocks() {
        return Collections.unmodifiableList(joinBlocks);
    }

    private boolean sameBlock(SerializedLocation first, SerializedLocation second) {
        if (first == null || second == null) {
            return false;
        }
        return first.getWorld().equalsIgnoreCase(second.getWorld())
                && first.getBlockX() == second.getBlockX()
                && first.getBlockY() == second.getBlockY()
                && first.getBlockZ() == second.getBlockZ();
    }

    public int getConfiguredSpawnCount() {
        int count = 0;
        for (SerializedLocation spawn : spawns) {
            if (spawn != null) {
                count++;
            }
        }
        return count;
    }
}
