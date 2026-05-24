package net.enelson.soptntrun.match;

import net.enelson.soptntrun.arena.TNTRunArena;

import java.util.Collections;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class WaitingMatch {

    private final TNTRunArena arena;
    private final Set<UUID> players = new LinkedHashSet<UUID>();
    private int countdownRemaining = -1;

    public WaitingMatch(TNTRunArena arena) {
        this.arena = arena;
    }

    public TNTRunArena getArena() {
        return arena;
    }

    public Set<UUID> getPlayers() {
        return Collections.unmodifiableSet(players);
    }

    public List<UUID> getOrderedPlayers() {
        return Collections.unmodifiableList(new ArrayList<UUID>(players));
    }

    public boolean add(UUID playerId) {
        return players.add(playerId);
    }

    public boolean remove(UUID playerId) {
        return players.remove(playerId);
    }

    public boolean has(UUID playerId) {
        return players.contains(playerId);
    }

    public int size() {
        return players.size();
    }

    public boolean isEmpty() {
        return players.isEmpty();
    }

    public int getCountdownRemaining() {
        return countdownRemaining;
    }

    public boolean hasCountdown() {
        return countdownRemaining >= 0;
    }

    public void startCountdown(int seconds) {
        this.countdownRemaining = Math.max(0, seconds);
    }

    public void resetCountdown() {
        this.countdownRemaining = -1;
    }

    public int tickCountdown() {
        return countdownRemaining--;
    }
}
