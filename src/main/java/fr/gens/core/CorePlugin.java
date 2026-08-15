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
        this.teamManager = new TeamManager(this);
        this.teamQuestManager = new fr.gens.core.modules.teams.TeamQuestManager(this);
        this.actionBarManager = new ActionBarManager(this);
        this.actionBarManager.start();
        
        getLogger().info("============================");
        getLogger().info("   Démarrage de GensCore");
        getLogger().info("============================");

        // 1. Initialiser le gestionnaire de modules
        this.moduleManager = new ModuleManager(this);
        
        // 2. Enregistrer les modules (ex: le shop qu'on veut pouvoir désactiver)
        this.moduleManager.registerModules();

        // Attacher les commandes Bukkit aux modules correspondants
        org.bukkit.command.PluginCommand ecoCmd = getCommand("eco");
        ecoCmd.setExecutor((org.bukkit.command.CommandExecutor) moduleManager.getModule("economy"));
        ecoCmd.setTabCompleter((org.bukkit.command.TabCompleter) moduleManager.getModule("economy"));

        org.bukkit.command.PluginCommand payCmd = getCommand("pay");
        payCmd.setExecutor((org.bukkit.command.CommandExecutor) moduleManager.getModule("economy"));
        payCmd.setTabCompleter((org.bukkit.command.TabCompleter) moduleManager.getModule("economy"));

        getCommand("money").setExecutor((org.bukkit.command.CommandExecutor) moduleManager.getModule("economy"));
        getCommand("balance").setExecutor((org.bukkit.command.CommandExecutor) moduleManager.getModule("economy"));
        getCommand("baltop").setExecutor((org.bukkit.command.CommandExecutor) moduleManager.getModule("economy"));
        getCommand("sethome").setExecutor((org.bukkit.command.CommandExecutor) moduleManager.getModule("cmdhome"));
        
        org.bukkit.command.PluginCommand homeCmd = getCommand("home");
        homeCmd.setExecutor((org.bukkit.command.CommandExecutor) moduleManager.getModule("cmdhome"));
        homeCmd.setTabCompleter((org.bukkit.command.TabCompleter) moduleManager.getModule("cmdhome"));
        
        // Utils
        String[] utilsCmds = {"anvil", "craftingtable", "enchanttable", "ec", "feed"};
        for (String cmd : utilsCmds) {
            org.bukkit.command.PluginCommand c = getCommand(cmd);
            if (c != null) c.setExecutor((org.bukkit.command.CommandExecutor) moduleManager.getModule("utils"));
        }
        
        // Auth
        getCommand("register").setExecutor((org.bukkit.command.CommandExecutor) moduleManager.getModule("auth"));
        getCommand("login").setExecutor((org.bukkit.command.CommandExecutor) moduleManager.getModule("auth"));
        getCommand("changemdp").setExecutor((org.bukkit.command.CommandExecutor) moduleManager.getModule("auth"));
        
        // Team commands are registered in TeamModule.enable()

        org.bukkit.command.PluginCommand delhomeCmd = getCommand("delhome");
        delhomeCmd.setExecutor((org.bukkit.command.CommandExecutor) moduleManager.getModule("cmdhome"));
        delhomeCmd.setTabCompleter((org.bukkit.command.TabCompleter) moduleManager.getModule("cmdhome"));
        getCommand("back").setExecutor((org.bukkit.command.CommandExecutor) moduleManager.getModule("cmdback"));
        getCommand("setspawn").setExecutor((org.bukkit.command.CommandExecutor) moduleManager.getModule("cmdspawn"));
        getCommand("spawn").setExecutor((org.bukkit.command.CommandExecutor) moduleManager.getModule("cmdspawn"));

        getCommand("tpa").setExecutor((org.bukkit.command.CommandExecutor) moduleManager.getModule("cmdtpa"));
        getCommand("tpaccept").setExecutor((org.bukkit.command.CommandExecutor) moduleManager.getModule("cmdtpa"));
        if (getCommand("tpdeny") != null) getCommand("tpdeny").setExecutor((org.bukkit.command.CommandExecutor) moduleManager.getModule("cmdtpa"));
        if (getCommand("tpadeny") != null) getCommand("tpadeny").setExecutor((org.bukkit.command.CommandExecutor) moduleManager.getModule("cmdtpa"));
        getCommand("tpacancel").setExecutor((org.bukkit.command.CommandExecutor) moduleManager.getModule("cmdtpa"));

        getCommand("shop").setExecutor((org.bukkit.command.CommandExecutor) moduleManager.getModule("dynamicshop"));
        getCommand("ah").setExecutor((org.bukkit.command.CommandExecutor) moduleManager.getModule("auctionhouse"));

        WebCommand webCommand = new WebCommand(this);
        getCommand("web").setExecutor(webCommand);
        getCommand("web").setTabCompleter(webCommand);

        // 3. Démarrer le serveur Web pour le panel admin
        int webPort = getConfig().getInt("web.port", 8080);
        this.webManager = new WebManager(this, webPort);
        this.webManager.start();

        // 4. Lancer les rappels automatiques (Discord et Guilde)
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                // Rappel Discord si pas de perm
                if (!p.hasPermission("genscore.discord.linked")) {
                    p.sendMessage(MiniMessage.miniMessage().deserialize(
                        "<gray>N'oublie pas de lier ton compte Discord pour obtenir ton badge et tes récompenses ! <click:run_command:'/linktuto'><hover:show_text:'<green>Clique pour voir le tuto !'><gold><b>[Clique ici]</b></gold></hover></click></gray>"
                    ));
                }
                
                // Rappel Guilde si pas de team
                if (getTeamManager().getPlayerTeam(p.getUniqueId()) == null) {
                    p.sendMessage(MiniMessage.miniMessage().deserialize(
                        "<gray>Tu n'es pas encore dans une guilde ! Rejoins-en une ou crée la tienne pour profiter des avantages de groupe. <click:run_command:'/team'><hover:show_text:'<green>Ouvrir le menu de guilde'><gold><b>[Ouvrir le menu /team]</b></gold></hover></click></gray>"
                    ));
                }
            }
        }, 36000L, 36000L); // 36000 ticks = 30 minutes

        getLogger().info("GensCore est prêt ! Panel Web sur le port " + webPort + ".");
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
        getLogger().info("GensCore a été désactivé.");
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

    public TeamManager getTeamManager() {
        return teamManager;
    }

    public fr.gens.core.modules.teams.TeamQuestManager getTeamQuestManager() {
        return teamQuestManager;
    }
}
