package net.enelson.soptntrun.edit;

import org.bukkit.Location;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class EditorManager {

    private final Map<UUID, String> editingArenaNames = new HashMap<UUID, String>();
    private final Map<UUID, SetupSession> setupSessions = new HashMap<UUID, SetupSession>();
    private final Map<UUID, Location> areaFirstPoints = new HashMap<UUID, Location>();

    public void startEditing(UUID playerId, String arenaName) {
        editingArenaNames.put(playerId, arenaName);
    }

    public void stopEditing(UUID playerId) {
        editingArenaNames.remove(playerId);
        setupSessions.remove(playerId);
        areaFirstPoints.remove(playerId);
    }

    public String getEditingArenaName(UUID playerId) {
        return editingArenaNames.get(playerId);
    }

    public void startSpawnSetup(UUID playerId, String arenaName, int startSlot, int maxSlot) {
        setupSessions.put(playerId, new SetupSession(arenaName, SetupKind.SPAWN, Math.max(1, startSlot), Math.max(1, maxSlot)));
    }

    public void startAreaSetup(UUID playerId, String arenaName, int startStep) {
        setupSessions.put(playerId, new SetupSession(arenaName, SetupKind.AREA, Math.max(1, Math.min(2, startStep)), 2));
    }

    public void stopSetup(UUID playerId) {
        setupSessions.remove(playerId);
        areaFirstPoints.remove(playerId);
    }

    public SetupSession getSetupSession(UUID playerId) {
        return setupSessions.get(playerId);
    }

    public void setAreaFirstPoint(UUID playerId, Location location) {
        if (location == null) {
            areaFirstPoints.remove(playerId);
            return;
        }
        areaFirstPoints.put(playerId, location.clone());
    }

    public Location getAreaFirstPoint(UUID playerId) {
        Location location = areaFirstPoints.get(playerId);
        return location == null ? null : location.clone();
    }

    public enum SetupKind {
        AREA,
        SPAWN
    }

    public static final class SetupSession {

        private final String arenaName;
        private final SetupKind kind;
        private final int maxSlot;
        private int currentSlot;

        private SetupSession(String arenaName, SetupKind kind, int currentSlot, int maxSlot) {
            this.arenaName = arenaName;
            this.kind = kind;
            this.currentSlot = currentSlot;
            this.maxSlot = maxSlot;
        }

        public String getArenaName() {
            return arenaName;
        }

        public SetupKind getKind() {
            return kind;
        }

        public int getCurrentSlot() {
            return currentSlot;
        }

        public int getMaxSlot() {
            return maxSlot;
        }

        public boolean advance() {
            if (currentSlot >= maxSlot) {
                return false;
            }
            currentSlot++;
            return true;
        }

        public boolean goBack() {
            if (currentSlot <= 1) {
                return false;
            }
            currentSlot--;
            return true;
        }
    }
}
