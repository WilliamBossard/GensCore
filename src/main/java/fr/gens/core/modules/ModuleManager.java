package fr.gens.core.modules;

import fr.gens.core.CorePlugin;
// Les anciens imports manuels des modules ont été supprimés car l'Auto-Discovery (org.reflections) s'en charge.
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
        addModule(new fr.gens.core.modules.utils.UtilsModule(plugin));
        addModule(new fr.gens.core.modules.tomb.TombModule(plugin));
        addModule(new fr.gens.core.modules.TeleportTpaModule(plugin));
        addModule(new fr.gens.core.modules.TeleportSpawnModule(plugin));
        addModule(new fr.gens.core.modules.TeleportHomeModule(plugin));
        addModule(new fr.gens.core.modules.TeleportBackModule(plugin));
        addModule(new fr.gens.core.modules.teams.TeamModule(plugin));
        addModule(new fr.gens.core.modules.tabboard.TabBoardModule(plugin));
        addModule(new fr.gens.core.modules.stats.StatsModule(plugin));
        addModule(new fr.gens.core.modules.spawners.SpawnerModule(plugin));
        addModule(new fr.gens.core.modules.shop.ShopModule(plugin));
        addModule(new fr.gens.core.modules.quests.QuestModule(plugin));
        addModule(new fr.gens.core.modules.motd.MotdModule(plugin));
        addModule(new fr.gens.core.modules.moderation.ModerationModule(plugin));
        addModule(new fr.gens.core.modules.loot.LootModule(plugin));
        addModule(new fr.gens.core.modules.lock.LockModule(plugin));
        addModule(new fr.gens.core.modules.headdrop.HeadDropModule(plugin));
        addModule(new fr.gens.core.modules.gui.CustomGuiModule(plugin));
        addModule(new fr.gens.core.modules.jobs.JobsModule(plugin));
        addModule(new fr.gens.core.modules.FastLeafDecayModule(plugin));
        addModule(new fr.gens.core.modules.EconomyModule(plugin));
        addModule(new fr.gens.core.modules.ChatModule(plugin));
        addModule(new fr.gens.core.modules.BlueMapModule(plugin));
        addModule(new fr.gens.core.modules.discord.DiscordModule(plugin));
        addModule(new fr.gens.core.modules.AuctionHouseModule(plugin));
        addModule(new fr.gens.core.modules.auth.AuthModule(plugin));

        plugin.getLangManager().sendConsoleMessage("module.manager.loaded", net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("count", String.valueOf(modules.size())));

        // Charger les états depuis modules.yml et les activer si besoin
        org.bukkit.configuration.file.FileConfiguration modulesConfig = plugin.getConfigManager().getConfig("modules.yml");
        for (Module module : modules.values()) {
            boolean shouldEnable = modulesConfig.getBoolean("modules." + module.getName().toLowerCase(), true);
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
                module.disable();
            }
        }
    }

    public Module getModule(String name) {
        return modules.get(name.toLowerCase());
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
            modulesConfig.set("modules." + module.getName().toLowerCase() + ".enabled", state);
            plugin.getConfigManager().saveConfig("modules.yml");
            return true;
        }
        return false;
    }
}

