package net.enelson.soptntrun.party;

import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public interface PartyBridge {

    List<UUID> getMemberUuids(Player player);

    boolean isLeader(Player player);
}
