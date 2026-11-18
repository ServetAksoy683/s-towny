package com.stowny.managers;

import com.stowny.model.Invite;
import com.stowny.model.PendingCreation;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Bellekte tutulan gecici veriler: davetler ve onay bekleyen kuruluslar. */
public class InviteManager {

    private final Map<UUID, Invite> playerInvites = new HashMap<>();
    private final Map<String, Invite> townInvites = new HashMap<>();
    private final Map<String, Invite> kingdomInvites = new HashMap<>();
    private final Map<UUID, PendingCreation> pendingCreations = new HashMap<>();

    public void invitePlayer(UUID player, Invite invite) { playerInvites.put(player, invite); }
    public Invite getPlayerInvite(UUID player) { return playerInvites.get(player); }
    public void clearPlayerInvite(UUID player) { playerInvites.remove(player); }

    public void inviteTown(String townName, Invite invite) { townInvites.put(townName.toLowerCase(), invite); }
    public Invite getTownInvite(String townName) { return townInvites.get(townName.toLowerCase()); }
    public void clearTownInvite(String townName) { townInvites.remove(townName.toLowerCase()); }

    public void inviteKingdom(String kingdomName, Invite invite) { kingdomInvites.put(kingdomName.toLowerCase(), invite); }
    public Invite getKingdomInvite(String kingdomName) { return kingdomInvites.get(kingdomName.toLowerCase()); }
    public void clearKingdomInvite(String kingdomName) { kingdomInvites.remove(kingdomName.toLowerCase()); }

    public void setPendingCreation(UUID player, PendingCreation creation) { pendingCreations.put(player, creation); }
    public PendingCreation getPendingCreation(UUID player) { return pendingCreations.get(player); }
    public void clearPendingCreation(UUID player) { pendingCreations.remove(player); }
}
