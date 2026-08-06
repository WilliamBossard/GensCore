package fr.gens.core.modules.quests;

import java.util.List;

public class Quest {
    private final String id;
    private final String category; // e.g. "easy", "medium", "hard"
    private final String name;
    private final String menuItem;
    private final List<String> description;
    private final QuestType type;
    private final List<String> requiredTargets;
    private final int requiredAmount;
    private final List<String> rewardCommands;

    public Quest(String id, String category, String name, String menuItem, List<String> description, QuestType type, List<String> requiredTargets, int requiredAmount, List<String> rewardCommands) {
        this.id = id;
        this.category = category;
        this.name = name;
        this.menuItem = menuItem;
        this.description = description;
        this.type = type;
        this.requiredTargets = requiredTargets;
        this.requiredAmount = requiredAmount;
        this.rewardCommands = rewardCommands;
    }

    public String getId() {
        return id;
    }

    public String getCategory() {
        return category;
    }

    public String getName() {
        return name;
    }

    public String getMenuItem() {
        return menuItem;
    }

    public List<String> getDescription() {
        return description;
    }

    public QuestType getType() {
        return type;
    }

    public List<String> getRequiredTargets() {
        return requiredTargets;
    }

    public int getRequiredAmount() {
        return requiredAmount;
    }

    public List<String> getRewardCommands() {
        return rewardCommands;
    }

    public boolean isTarget(String target) {
        for (String req : requiredTargets) {
            if (req.equalsIgnoreCase(target)) {
                return true;
            }
        }
        return false;
    }
}
