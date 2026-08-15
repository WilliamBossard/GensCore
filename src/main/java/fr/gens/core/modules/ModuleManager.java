package fr.gens.core.modules;

import fr.gens.core.CorePlugin;
import fr.gens.core.modules.shop.ShopModule;
import fr.gens.core.modules.headdrop.HeadDropModule;
import fr.gens.core.modules.tabboard.TabBoardModule;
import fr.gens.core.modules.discord.DiscordModule;
import fr.gens.core.modules.AuctionHouseModule;
import fr.gens.core.modules.loot.LootModule;
import fr.gens.core.modules.auth.AuthModule;
import fr.gens.core.modules.utils.UtilsModule;
import fr.gens.core.modules.shop.ShopModule;
import fr.gens.core.modules.gui.CustomGuiModule;
import fr.gens.core.modules.quests.QuestModule;
import fr.gens.core.modules.EconomyModule;
import fr.gens.core.modules.headdrop.HeadDropModule;
import fr.gens.core.modules.spawners.SpawnerModule;
import fr.gens.core.modules.jobs.JobsModule;
import fr.gens.core.modules.moderation.ModerationModule;
import fr.gens.core.modules.stats.StatsModule;
import fr.gens.core.modules.teams.TeamModule;
import fr.gens.core.modules.lock.LockModule;
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
        // Enregistrement de tous les modules
        addModule(new ShopModule(plugin));
        addModule(new EconomyModule(plugin));
        addModule(new ChatModule(plugin));
        addModule(new fr.gens.core.modules.motd.MotdModule(plugin));
        addModule(new GuiModule(plugin));
        addModule(new FastLeafDecayModule(plugin));
        addModule(new TeleportHomeModule(plugin));
        addModule(new TeleportBackModule(plugin));
        addModule(new TeleportSpawnModule(plugin));
        addModule(new TeleportTpaModule(plugin));
        addModule(new AuctionHouseModule(plugin));
        addModule(new HeadDropModule(plugin));
        addModule(new TabBoardModule(plugin));
        addModule(new fr.gens.core.modules.tomb.TombModule(plugin));
        addModule(new CustomGuiModule(plugin));
        addModule(new DiscordModule(plugin));
        addModule(new QuestModule(plugin));
        addModule(new AuthModule(plugin));
        addModule(new UtilsModule(plugin));
        addModule(new fr.gens.core.modules.shop.ShopModule(plugin));
        addModule(new fr.gens.core.modules.loot.LootModule(plugin));
        addModule(new SpawnerModule(plugin));
        addModule(new BlueMapModule(plugin));
        addModule(new ModerationModule(plugin));
        addModule(new StatsModule(plugin));
        addModule(new JobsModule(plugin));
        addModule(new TeamModule(plugin));
        addModule(new LockModule(plugin));
        
        plugin.getLogger().info(modules.size() + " modules enregistrés au total.");

        // Charger les états depuis la config et les activer si besoin
        for (Module module : modules.values()) {
            boolean shouldEnable = plugin.getStorageManager().getConfig().getBoolean("modules." + module.getName() + ".enabled", true);
            if (shouldEnable) {
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
        for (Module module : modules.values()) {
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
            plugin.getStorageManager().getConfig().set("modules." + module.getName() + ".enabled", state);
            plugin.getStorageManager().saveConfig();
            return true;
        }
        return false;
    }
}
