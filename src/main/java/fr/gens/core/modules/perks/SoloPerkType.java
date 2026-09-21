package fr.gens.core.modules.perks;

import org.bukkit.Material;

public enum SoloPerkType {

    // --- Paliers Gratuits (Quetes uniquement) ---
    FREE_REROLL("second_souffle", "Second Souffle", "Second Wind",
            "Offre +1 relance quotidienne gratuite de quete.",
            "Grants +1 daily free quest reroll.",
            Material.ENDER_PEARL, 5, 0.0, 0, false, false),

    EXTRA_HOME("nomade", "Nomade", "Nomad",
            "Debloque +1 point de teleportation personnel (/sethome).",
            "Unlocks +1 personal home waypoint (/sethome).",
            Material.RED_BED, 15, 0.0, 0, false, false),

    SPEED_BOOST("fouille_legere", "Foulee Legere", "Light Stride",
            "Octroie Vitesse I permanent tant que vous etes hors combat.",
            "Grants permanent Speed I while out of combat.",
            Material.FEATHER, 30, 0.0, 0, false, false),

    JOBS_XP("savoir_artisan", "Savoir de l'Artisan", "Artisan Knowledge",
            "Bonus passif de +5% d'experience sur tous les metiers.",
            "Permanent +5% job experience bonus on all professions.",
            Material.EXPERIENCE_BOTTLE, 50, 0.0, 0, false, false),

    WARMUP_REDUCTION("poche_dimensionnelle", "Poche Dimensionnelle", "Dimensional Pocket",
            "Reduit de 50% le temps d'attente de toutes les teleportations.",
            "Reduces all teleportation warmup delays by 50%.",
            Material.COMPASS, 75, 0.0, 0, false, false),

    FEED_ACCESS("festin_infini", "Festin Infini", "Infinite Feast",
            "Debloque l'acces a la commande /feed (cooldown de 15 minutes).",
            "Unlocks access to /feed command (15-minute cooldown).",
            Material.GOLDEN_CARROT, 100, 0.0, 0, false, false),

    // --- Maitrises Majeures Payantes (Quetes + $ ou XP) ---
    MAGNET("aimant_recolte", "Aimant de Recolte", "Harvest Magnet",
            "Attire automatiquement les items au sol dans un rayon de 5 blocs (activable On/Off).",
            "Attracts nearby dropped items within 5 blocks (toggleable On/Off).",
            Material.IRON_INGOT, 25, 20000.0, 40, true, true),

    DOUBLE_DROP("benediction_minerale", "Benediction Minerale", "Mineral Blessing",
            "5% de chance de doubler les drops de minerais bruts et de bois recoltes.",
            "5% chance to double raw ore and timber harvesting drops.",
            Material.RAW_IRON, 40, 35000.0, 60, true, false),

    PORTABLE_WORKBENCH("atelier_portatif", "Atelier Portatif", "Portable Workbench",
            "Acces illimite et permanent aux commandes /craft et /ec.",
            "Unlimited permanent access to /craft and /ec commands.",
            Material.CRAFTING_TABLE, 60, 50000.0, 80, true, false),

    AUTO_SMELT("fonte_instantanee", "Fonte Instantanee", "Instant Smelt",
            "Convertit automatiquement les minerais bruts mines en lingots (activable On/Off).",
            "Automatically smelts mined raw ores into ingots (toggleable On/Off).",
            Material.FURNACE, 80, 75000.0, 100, true, true),

    KEEP_EXP("ame_preservee", "Ame Preservee", "Preserved Soul",
            "Conserve 50% de vos niveaux d'experience lors d'un deces.",
            "Retains 50% of your experience levels upon death.",
            Material.TOTEM_OF_UNDYING, 100, 100000.0, 120, true, false);

    private final String id;
    private final String nameFr;
    private final String nameEn;
    private final String descriptionFr;
    private final String descriptionEn;
    private final Material icon;
    private final int requiredQuests;
    private final double costMoney;
    private final int costXp;
    private final boolean major;
    private final boolean toggleable;

    SoloPerkType(String id, String nameFr, String nameEn, String descriptionFr, String descriptionEn,
                 Material icon, int requiredQuests, double costMoney, int costXp, boolean major, boolean toggleable) {
        this.id = id;
        this.nameFr = nameFr;
        this.nameEn = nameEn;
        this.descriptionFr = descriptionFr;
        this.descriptionEn = descriptionEn;
        this.icon = icon;
        this.requiredQuests = requiredQuests;
        this.costMoney = costMoney;
        this.costXp = costXp;
        this.major = major;
        this.toggleable = toggleable;
    }

    public String getId() { return id; }
    public String getNameFr() { return nameFr; }
    public String getNameEn() { return nameEn; }
    public String getDescriptionFr() { return descriptionFr; }
    public String getDescriptionEn() { return descriptionEn; }
    public Material getIcon() { return icon; }
    public int getRequiredQuests() { return requiredQuests; }
    public double getCostMoney() { return costMoney; }
    public int getCostXp() { return costXp; }
    public boolean isMajor() { return major; }
    public boolean isToggleable() { return toggleable; }

    public static SoloPerkType fromId(String id) {
        if (id == null) return null;
        for (SoloPerkType type : values()) {
            if (type.id.equalsIgnoreCase(id) || type.name().equalsIgnoreCase(id)) {
                return type;
            }
        }
        return null;
    }
}
