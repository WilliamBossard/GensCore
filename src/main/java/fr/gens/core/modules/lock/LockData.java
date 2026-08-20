package fr.gens.core.modules.lock;

import java.util.UUID;


public class LockData {
    private String location;
    private UUID ownerUuid;
    private int teamId;

    public LockData(String location, UUID ownerUuid, int teamId) {
        this.location = location;
        this.ownerUuid = ownerUuid;
        this.teamId = teamId;
    }

    public String getLocation() { return location; }
    public UUID getOwnerUuid() { return ownerUuid; }
    public int getTeamId() { return teamId; }
}

