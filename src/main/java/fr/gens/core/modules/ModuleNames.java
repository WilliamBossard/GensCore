package fr.gens.core.modules;

/**
 * Constantes pour les noms de modules GensCore.
 * Utiliser ces constantes au lieu de strings littÃƒÂ©rales pour ÃƒÂ©viter les fautes de frappe.
 * Exemple : plugin.getModuleManager().getModule(ModuleNames.ECONOMY)
 */
public final class ModuleNames {

    // Ãƒâ€°conomie
    public static final String ECONOMY      = "economy";
    public static final String DYNAMIC_SHOP = "dynamicshop";
    public static final String AUCTION      = "auctionhouse";

    // Communication
    public static final String DISCORD      = "discord";
    public static final String CHAT         = "chat";
    public static final String MOTD         = "motd";

    // SÃƒÂ©curitÃƒÂ©
    public static final String AUTH         = "auth";
    public static final String MODERATION   = "moderation";

    // TÃƒÂ©lÃƒÂ©portation
    public static final String HOME         = "home";
    public static final String BACK         = "back";
    public static final String SPAWN        = "spawn";
    public static final String TPA          = "tpa";

    // Guildes / Teams
    public static final String TEAMS        = "teams";

    // Progression
    public static final String QUESTS       = "quests";
    public static final String JOBS         = "jobs";
    public static final String STATS        = "stats";

    // Gameplay
    public static final String SPAWNERS     = "spawners";
    public static final String LOCK         = "lock";
    public static final String LOOT         = "lootr";
    public static final String HEAD_DROP    = "headdrop";
    public static final String TOMB         = "tomb";
    public static final String FAST_LEAF    = "fastleafdecay";

    // Interface
    public static final String GUI          = "gui";
    public static final String TAB_BOARD    = "tabboard";

    // IntÃƒÂ©grations
    public static final String BLUE_MAP     = "bluemap";
    public static final String WEB          = "web";

    private ModuleNames() {
        throw new UnsupportedOperationException("Utility class");
    }
}
