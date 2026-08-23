package fr.gens.core.modules.quests;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


public class PlayerQuestData {
    private final UUID uuid;
    private final String dateAssigned;
    
    // category -> Map<questId, progress>
    private final Map<String, Map<String, Integer>> activeQuests = new ConcurrentHashMap<>();
    // category -> Map<questId, completed>
    private final Map<String, Map<String, Boolean>> completedQuests = new ConcurrentHashMap<>();
    
    private int completedTotal = 0;
    private int rerollsDone = 0;
    public PlayerQuestData(UUID uuid, String dateAssigned) {
        this.uuid = uuid;
        this.dateAssigned = dateAssigned;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getDateAssigned() {
        return dateAssigned;
    }

    public void addQuest(String category, String questId, int progress, boolean completed) {
        activeQuests.computeIfAbsent(category, k -> new ConcurrentHashMap<>()).put(questId, progress);
        completedQuests.computeIfAbsent(category, k -> new ConcurrentHashMap<>()).put(questId, completed);
    }

    public int getProgress(String category, String questId) {
        return activeQuests.getOrDefault(category, new ConcurrentHashMap<>()).getOrDefault(questId, 0);
    }

    public boolean isCompleted(String category, String questId) {
        return completedQuests.getOrDefault(category, new ConcurrentHashMap<>()).getOrDefault(questId, false);
    }

    public void setProgress(String category, String questId, int progress) {
        activeQuests.computeIfAbsent(category, k -> new ConcurrentHashMap<>()).put(questId, progress);
    }

    public void setCompleted(String category, String questId, boolean completed) {
        completedQuests.computeIfAbsent(category, k -> new ConcurrentHashMap<>()).put(questId, completed);
    }

    public Map<String, Map<String, Integer>> getActiveQuests() {
        return activeQuests;
    }

    public Map<String, Map<String, Boolean>> getCompletedQuests() {
        return completedQuests;
    }
    
    public int getCompletedTotal() {
        return completedTotal;
    }

    public void setCompletedTotal(int completedTotal) {
        this.completedTotal = completedTotal;
    }

    public int getRerollsDone() {
        return rerollsDone;
    }

    public void setRerollsDone(int rerollsDone) {
        this.rerollsDone = rerollsDone;
    }
}

