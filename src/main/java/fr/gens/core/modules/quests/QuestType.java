package fr.gens.core.modules.quests;

public enum QuestType {
    BREAK,
    PLACE,
    KILL,
    CRAFT,
    FISH,
    SHEAR,
    COOK,
    CONSUME,
    BREED,
    PICKUP,
    TAME,
    VILLAGER_TRADE,
    GET,
    MILKING,
    EXP_POINTS,
    CARVE,
    PLAYER_DEATH,
    LOCATION,
    MYTHIC_MOBS,
    ELITE_MOBS,
    PLACEHOLDER,
    NU_VOTIFIER,
    PYRO_FISH,
    EMF_FISH,
    FARMING,
    LAUNCH,
    ENCHANT,
    EXP_LEVELS;

    public static QuestType fromString(String str) {
        for (QuestType type : values()) {
            if (type.name().equalsIgnoreCase(str)) {
                return type;
            }
        }
        return null;
    }
}

