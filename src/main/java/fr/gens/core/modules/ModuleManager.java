package fr.gens.core.modules;

import fr.gens.core.CorePlugin;
import fr.gens.core.modules.auth.AuthModule;
import fr.gens.core.modules.discord.DiscordModule;
import fr.gens.core.modules.gui.CustomGuiModule;
import fr.gens.core.modules.headdrop.HeadDropModule;
import fr.gens.core.modules.jobs.JobsModule;
import fr.gens.core.modules.lock.LockModule;
import fr.gens.core.modules.loot.LootModule;
import fr.gens.core.modules.moderation.ModerationModule;
import fr.gens.core.modules.motd.MotdModule;
import fr.gens.core.modules.quests.QuestModule;
import fr.gens.core.modules.shop.ShopModule;
import fr.gens.core.modules.spawners.SpawnerModule;
import fr.gens.core.modules.stats.StatsModule;
import fr.gens.core.modules.tabboard.TabBoardModule;
import fr.gens.core.modules.teams.TeamModule;
import fr.gens.core.modules.tomb.TombModule;
import fr.gens.core.modules.utils.UtilsModule;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;


public class ModuleManager {

    private final CorePlugin plugin;
    private final Map<String, Module> modules = new LinkedHashMap<>();

    public ModuleManager(CorePlugin plugin) {
        this.plugin = plugin;
    }

    public void registerModules() {
        // Enregistrement manuel des modules. 
        // NOTE AUX FUTURS DEVELOPPEURS : L'Auto-Discovery (org.reflections) a ete retire 
        // pour drastiquement ameliorer le temps de demarrage du plugin (Startup Time).
        // Vous devez ajouter manuellement chaque nouveau module ici !
        addModule(new UtilsModule(plugin));
        addModule(new TombModule(plugin));
        addModule(new TeleportTpaModule(plugin));
        addModule(new TeleportSpawnModule(plugin));
        addModule(new TeleportHomeModule(plugin));
        addModule(new TeleportBackModule(plugin));
        addModule(new TeamModule(plugin));
        addModule(new TabBoardModule(plugin));
        addModule(new StatsModule(plugin));
        addModule(new SpawnerModule(plugin));
        addModule(new ShopModule(plugin));
        addModule(new QuestModule(plugin));
        addModule(new MotdModule(plugin));
        addModule(new ModerationModule(plugin));
        addModule(new LootModule(plugin));
        addModule(new LockModule(plugin));
        addModule(new HeadDropModule(plugin));
        addModule(new CustomGuiModule(plugin));
        addModule(new GuiModule(plugin));
        addModule(new JobsModule(plugin));
        addModule(new FastLeafDecayModule(plugin));
        addModule(new EconomyModule(plugin));
        addModule(new ChatModule(plugin));
        addModule(new BlueMapModule(plugin));
        addModule(new DiscordModule(plugin));
        addModule(new AuctionHouseModule(plugin));
        addModule(new AuthModule(plugin));
        addModule(new BedrockSkinModule(plugin));
        addModule(new MinigamesModule(plugin));

        plugin.getLangManager().sendConsoleMessage("module.manager.loaded", net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("count", String.valueOf(modules.size())));

        // Charger les états depuis modules.yml et les activer si besoin
        org.bukkit.configuration.file.FileConfiguration modulesConfig = plugin.getConfigManager().getConfig("modules.yml");
        for (Module module : modules.values()) {
            String modKey = module.getName().toLowerCase();
            boolean shouldEnable = modulesConfig.getBoolean("modules." + modKey, 
                modulesConfig.getBoolean("modules." + modKey + ".enabled", 
                    "minigames".equals(modKey) ? modulesConfig.getBoolean("modules.minigame", true) : true));
            if (shouldEnable) {
                module.initDatabase(plugin.getDatabaseManager());
                module.enable();
            } else {
                module.disable();
            }
        }
    }

    private void addModule(Module module) {
        modules.put(module.getName().toLowerCase(), module);
    }

    public void disableAllModules() {
        java.util.List<Module> reversedModules = new java.util.ArrayList<>(modules.values());
        java.util.Collections.reverse(reversedModules);
        for (Module module : reversedModules) {
            if (module.isEnabled()) {
                try {
                    module.disable();
                } catch (Throwable t) {
                    plugin.getLogger().log(java.util.logging.Level.SEVERE, "Erreur lors de la désactivation du module " + module.getName(), t);
                }
            }
        }
    }

    public Module getModule(String name) {
        if (name == null) return null;
        String lower = name.toLowerCase();
        if ("minigame".equals(lower)) lower = "minigames";
        return modules.get(lower);
    }

    public Collection<Module> getModules() {
        return modules.values();
    }

    public boolean toggleModule(String name, boolean state) {
        Module module = getModule(name);
        if (module == null) return false;

        boolean changed = false;
        if (state && !module.isEnabled()) {
            module.enable();
            changed = true;
        } else if (!state && module.isEnabled()) {
            module.disable();
            changed = true;
        }

        if (changed) {
            org.bukkit.configuration.file.FileConfiguration modulesConfig = plugin.getConfigManager().getConfig("modules.yml");
            String modKey = module.getName().toLowerCase();
            modulesConfig.set("modules." + modKey, state);
            modulesConfig.set("modules." + modKey + ".enabled", state);
            if ("minigames".equals(modKey)) {
                modulesConfig.set("modules.minigame", state);
            }
            plugin.getConfigManager().saveConfig("modules.yml");
            return true;
        }
        return false;
    }
}




// IDE refresh
