package net.enelson.soptntrun.party;

import net.enelson.sopparty.api.SopPartyApi;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public final class SopPartyPartyBridgeAdapter implements PartyBridge {

    private final SopPartyApi api;

    public SopPartyPartyBridgeAdapter(SopPartyApi api) {
        this.api = api;
    }

    @Override
    public List<UUID> getMemberUuids(Player player) {
        return api.getMemberUuids(player);
    }

    @Override
    public boolean isLeader(Player player) {
        return api.isLeader(player);
    }
}
