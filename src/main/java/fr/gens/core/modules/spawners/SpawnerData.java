package fr.gens.core.modules.spawners;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bukkit.Location;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SpawnerData {

    private UUID id;
    private Location location;
    private String type;
    private int stackCount;
    private int storedExp;
    private Map<String, Integer> storedItems;
    private String lastInteractedPlayer;
    private int storageLevel;
    private int expLevel;
    private int speedLevel;
    private boolean isLootChest;
    private transient long lastGenerateMillis;
    
    private static final ObjectMapper mapper = new ObjectMapper();

    public SpawnerData(UUID id, Location location, String type, int stackCount, int storedExp, String itemsJson, String lastInteractedPlayer, int storageLevel, int expLevel, int speedLevel) {
        this.id = id;
        this.location = location;
        this.type = type;
        this.stackCount = stackCount;
        this.storedExp = storedExp;
        this.lastInteractedPlayer = lastInteractedPlayer;
        this.storageLevel = storageLevel;
        this.expLevel = expLevel;
        this.speedLevel = speedLevel;
        this.storedItems = new HashMap<>();
        this.lastGenerateMillis = System.currentTimeMillis();
        
        if (itemsJson != null && !itemsJson.isEmpty() && !itemsJson.equals("{}")) {
            try {
                this.storedItems = mapper.readValue(itemsJson, new TypeReference<Map<String, Integer>>() {});
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }
    }
    
    public SpawnerData(Location location, String type, int stackCount) {
        this.id = UUID.randomUUID();
        this.location = location;
        this.type = type;
        this.stackCount = stackCount;
        this.storedExp = 0;
        this.storedItems = new HashMap<>();
        this.lastInteractedPlayer = "None";
        this.storageLevel = 0;
        this.expLevel = 0;
        this.speedLevel = 0;
        this.isLootChest = false;
        this.lastGenerateMillis = System.currentTimeMillis();
    }

    public UUID getId() {
        return id;
    }

    public Location getLocation() {
        return location;
    }

    public String getType() {
        return type;
    }

    public int getStackCount() {
        return stackCount;
    }

    public void setStackCount(int stackCount) {
        this.stackCount = stackCount;
    }

    public int getStoredExp() {
        return storedExp;
    }

    public void addExp(int exp) {
        this.storedExp += exp;
    }
    
    public void setStoredExp(int exp) {
        this.storedExp = exp;
    }

    public Map<String, Integer> getStoredItems() {
        return storedItems;
    }

    public void addItem(String material, int amount) {
        this.storedItems.put(material, this.storedItems.getOrDefault(material, 0) + amount);
    }
    
    public void clearItems() {
        this.storedItems.clear();
    }
    
    public void removeItem(String material, int amount) {
        if (!this.storedItems.containsKey(material)) return;
        int current = this.storedItems.get(material);
        if (current <= amount) {
            this.storedItems.remove(material);
        } else {
            this.storedItems.put(material, current - amount);
        }
    }

    public String getLastInteractedPlayer() {
        return lastInteractedPlayer;
    }

    public void setLastInteractedPlayer(String lastInteractedPlayer) {
        this.lastInteractedPlayer = lastInteractedPlayer;
    }

    public String getItemsJson() {
        try {
            return mapper.writeValueAsString(this.storedItems);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return "{}";
        }
    }
    
    public int getStorageLevel() { return storageLevel; }
    public void setStorageLevel(int storageLevel) { this.storageLevel = storageLevel; }
    
    public int getExpLevel() { return expLevel; }
    public void setExpLevel(int expLevel) { this.expLevel = expLevel; }
    
    public int getSpeedLevel() { return speedLevel; }
    public void setSpeedLevel(int speedLevel) { this.speedLevel = speedLevel; }

    public boolean isLootChest() {
        return isLootChest;
    }

    public void setLootChest(boolean lootChest) {
        this.isLootChest = lootChest;
    }
    
    public long getLastGenerateMillis() {
        if (lastGenerateMillis == 0) lastGenerateMillis = System.currentTimeMillis();
        return lastGenerateMillis;
    }
    public void setLastGenerateMillis(long lastGenerateMillis) { this.lastGenerateMillis = lastGenerateMillis; }
}
