package fr.gens.core.modules.tomb;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;


public class TombData {
    private final UUID id;
    private final UUID ownerId;
    private final Location location;
    private final ItemStack[] contents;
    private final int xp;
    private final long expirationTime; // timestamp in ms

    public TombData(UUID id, UUID ownerId, Location location, ItemStack[] contents, int xp, long expirationTime) {
        this.id = id;
        this.ownerId = ownerId;
        this.location = location;
        this.contents = contents;
        this.xp = xp;
        this.expirationTime = expirationTime;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public Location getLocation() {
        return location;
    }

    public ItemStack[] getContents() {
        return contents;
    }

    public int getXp() {
        return xp;
    }

    public long getExpirationTime() {
        return expirationTime;
    }

    public boolean isExpired() {
        if (expirationTime == -1) return false;
        return System.currentTimeMillis() > expirationTime;
    }
}

