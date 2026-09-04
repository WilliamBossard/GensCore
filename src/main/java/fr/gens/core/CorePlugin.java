package fr.gens.core;

import fr.gens.core.modules.ModuleManager;
import fr.gens.core.modules.teams.TeamManager;
import fr.gens.core.utils.DatabaseManager;
import fr.gens.core.utils.ActionBarManager;
import fr.gens.core.web.WebManager;
import com.tcoded.folialib.FoliaLib;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;


public class CorePlugin extends JavaPlugin {

    private ModuleManager moduleManager;
    private WebManager webManager;
    private StorageManager storageManager;
    private DatabaseManager databaseManager;
    private fr.gens.core.utils.ConfigManager configManager;
    private fr.gens.core.utils.LangManager langManager;
    private ActionBarManager actionBarManager;
    private TeamManager teamManager;
    private fr.gens.core.modules.teams.TeamQuestManager teamQuestManager;
    private fr.gens.core.utils.CommandManager commandManager;
    private FoliaLib foliaLib;
    private boolean isWiping = false;

    public boolean isWiping() {
        return isWiping;
    }

    public void setWiping(boolean wiping) {
        isWiping = wiping;
    }

    @Override
    public void onLoad() {
        // Le log SLF4J est géré par simplelogger.properties
    }

    @Override
    public void onEnable() {
        this.foliaLib = new FoliaLib(this);
        
        // 1. Initialiser le LangManager EN PREMIER car les autres en ont besoin pour logger
        this.langManager = new fr.gens.core.utils.LangManager(this);
        this.configManager = new fr.gens.core.utils.ConfigManager(this);
        
        getLangManager().sendConsoleMessage("core.startup_header");

        this.storageManager = new StorageManager(this);
        this.databaseManager = new DatabaseManager(this);
        this.actionBarManager = new ActionBarManager(this);
        this.actionBarManager.start();
        
        // 1.5 Initialiser le gestionnaire de commandes Cloud
        this.commandManager = new fr.gens.core.utils.CommandManager(this);
        
        // 1.6 Vérification des mises à jour
        new fr.gens.core.utils.UpdateChecker(this).checkForUpdates();
        
        // 2. Initialiser le gestionnaire de modules
        this.moduleManager = new ModuleManager(this);
        
        // 2. Enregistrer les modules
        this.moduleManager.registerModules();

        // 3. Initialiser les managers dependants des modules (comme TeamManager)
        this.teamManager = new TeamManager(this);
        this.teamQuestManager = new fr.gens.core.modules.teams.TeamQuestManager(this);
        
        // Demande à chaque module d'enregistrer ses commandes
        for (fr.gens.core.modules.Module module : this.moduleManager.getModules()) {
            module.registerCommands(this);
        }

        // Register non-module commands
        if (this.commandManager != null && this.commandManager.getAnnotationParser() != null) {
            this.commandManager.getAnnotationParser().parse(new fr.gens.core.commands.WebCommand(this));
            this.commandManager.getAnnotationParser().parse(new fr.gens.core.commands.ModuleCommand(this));
        }

        org.bukkit.configuration.file.FileConfiguration webConfig = getConfigManager().getConfig("modules/web.yml");
        if (webConfig.getBoolean("web.enabled", false)) {
            int webPort = webConfig.getInt("web.port", 8080);
            this.webManager = new WebManager(this, webPort);
            this.webManager.start();
        } else {
            getLangManager().sendConsoleMessage("core.web_disabled");
        }

        // 4. Lancer les rappels automatiques (Discord et Guilde)
        // 4. Lancer les rappels automatiques (Discord et Guilde)
        this.foliaLib.getScheduler().runTimerAsync((wrappedTask) -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p == null) continue;
                this.foliaLib.getScheduler().runAtEntity(p, (t) -> {
                    // Rappel Discord si pas de perm
                    if (!p.hasPermission("genscore.discord.linked")) {
                        getLangManager().sendMessage(p, "reminder.discord");
                    }
                    
                    // Rappel Guilde si pas de team
                    if (getTeamManager().getPlayerTeam(p.getUniqueId()) == null) {
                        getLangManager().sendMessage(p, "reminder.guild");
                    }
                });
            }
        }, 36000L, 36000L); // 36000 ticks = 30 minutes

        // L'arbre des commandes sera géré automatiquement par Bukkit / Player#updateCommands.
        
        getLangManager().sendConsoleMessage("core.startup_ready");
    }

    @Override
    public void onDisable() {
        if (webManager != null) {
            webManager.stop();
        }
        
        if (isWiping) {
            getLangManager().sendConsoleMessage("core.wiping_shutdown");
            if (databaseManager != null) {
                databaseManager.close();
            }
            return;
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
        if (this.langManager != null) {
            getLangManager().sendConsoleMessage("core.shutdown");
        }
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

    public fr.gens.core.utils.ConfigManager getConfigManager() {
        return configManager;
    }

    public fr.gens.core.utils.CommandManager getCommandManager() {
        return commandManager;
    }
    
    public FoliaLib getFoliaLib() {
        return foliaLib;
    }
}




