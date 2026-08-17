package fr.gens.core;

import fr.gens.core.modules.ModuleManager;
import fr.gens.core.modules.auth.AuthModule;
import fr.gens.core.modules.utils.UtilsModule;
import fr.gens.core.modules.gui.CustomGuiModule;
import fr.gens.core.modules.teams.TeamManager;
import fr.gens.core.utils.DatabaseManager;
import fr.gens.core.utils.ActionBarManager;
import fr.gens.core.StorageManager;
import fr.gens.core.web.WebManager;
import fr.gens.core.commands.WebCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class CorePlugin extends JavaPlugin {

    private static CorePlugin instance;
    private ModuleManager moduleManager;
    private WebManager webManager;
    private StorageManager storageManager;
    private DatabaseManager databaseManager;
    private fr.gens.core.utils.LangManager langManager;
    private ActionBarManager actionBarManager;
    private TeamManager teamManager;
    private fr.gens.core.modules.teams.TeamQuestManager teamQuestManager;

    @Override
    public void onLoad() {
        // Le log SLF4J est géré par simplelogger.properties
    }

    @Override
    public void onEnable() {
        instance = this;
        this.storageManager = new StorageManager(this);
        this.databaseManager = new DatabaseManager(this);
        this.langManager = new fr.gens.core.utils.LangManager(this);
        this.teamManager = new TeamManager(this);
        this.teamQuestManager = new fr.gens.core.modules.teams.TeamQuestManager(this);
        this.actionBarManager = new ActionBarManager(this);
        this.actionBarManager.start();
        
        getLangManager().sendConsoleMessage("core.startup_header");

        // 1. Initialiser le gestionnaire de modules
        this.moduleManager = new ModuleManager(this);
        
        // 2. Enregistrer les modules
        this.moduleManager.registerModules();
        
        // Demande à chaque module d'enregistrer ses commandes
        for (fr.gens.core.modules.Module module : this.moduleManager.getModules()) {
            module.registerCommands(this);
        }

        WebCommand webCommand = new WebCommand(this);
        getCommand("web").setExecutor(webCommand);
        getCommand("web").setTabCompleter(webCommand);

        fr.gens.core.commands.ModuleCommand moduleCommand = new fr.gens.core.commands.ModuleCommand(this);
        getCommand("module").setExecutor(moduleCommand);
        getCommand("module").setTabCompleter(moduleCommand);

        if (getConfig().getBoolean("web.enabled", false)) {
            int webPort = getConfig().getInt("web.port", 8080);
            this.webManager = new WebManager(this, webPort);
            this.webManager.start();
        } else {
            getLangManager().sendConsoleMessage("core.web_disabled");
        }

        // 4. Lancer les rappels automatiques (Discord et Guilde)
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                // Rappel Discord si pas de perm
                if (!p.hasPermission("genscore.discord.linked")) {
                    getLangManager().sendMessage(p, "reminder.discord");
                }
                
                // Rappel Guilde si pas de team
                if (getTeamManager().getPlayerTeam(p.getUniqueId()) == null) {
                    getLangManager().sendMessage(p, "reminder.guild");
                }
            }
        }, 36000L, 36000L); // 36000 ticks = 30 minutes

        getLangManager().sendConsoleMessage("core.startup_ready");
    }

    @Override
    public void onDisable() {
        if (this.webManager != null) {
            this.webManager.stop();
        }
        if (this.actionBarManager != null) {
            this.actionBarManager.stop();
        }
        if (this.moduleManager != null) {
            this.moduleManager.disableAllModules();
        }
        if (this.databaseManager != null) {
            this.databaseManager.close();
        }
        getLangManager().sendConsoleMessage("core.shutdown");
    }

    public static CorePlugin getInstance() {
        return instance;
    }

    public ModuleManager getModuleManager() {
        return moduleManager;
    }

    public StorageManager getStorageManager() {
        return storageManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public ActionBarManager getActionBarManager() {
        return actionBarManager;
    }

    public fr.gens.core.utils.LangManager getLangManager() {
        return langManager;
    }

    public TeamManager getTeamManager() {
        return teamManager;
    }

    public fr.gens.core.modules.teams.TeamQuestManager getTeamQuestManager() {
        return teamQuestManager;
    }
}
