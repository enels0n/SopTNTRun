package net.enelson.soptntrun.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired once for a player when they finish a TNT Run game, either by winning
 * (last one standing) or by losing (eliminated / falling out of the match).
 */
public final class TntRunFinishEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final boolean winner;

    public TntRunFinishEvent(Player player, boolean winner) {
        this.player = player;
        this.winner = winner;
    }

    public Player getPlayer() {
        return player;
    }

    public boolean isWinner() {
        return winner;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
