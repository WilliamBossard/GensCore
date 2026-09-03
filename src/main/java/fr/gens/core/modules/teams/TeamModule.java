package fr.gens.core.modules.teams;

import fr.gens.core.CorePlugin;
import fr.gens.core.modules.Module;
import org.bukkit.Bukkit;


public class TeamModule implements Module {
    private final CorePlugin plugin;
    private TeamListener teamListener;
    private TeamGui teamGui;
    private TeamCommand teamCommand;
    private boolean enabled;
    
    private fr.gens.core.database.TeamDAO teamDAO;

    public TeamModule(CorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() { return "Teams"; }

    @Override
    public String getDescription() { return "Système de guildes et d'équipes."; }

    @Override
    public boolean isEnabled() { return enabled; }

    public fr.gens.core.database.TeamDAO getTeamDAO() {
        return teamDAO;
    }

    public TeamCommand getTeamCommand() {
        return teamCommand;
    }

    @Override
    public void initDatabase(fr.gens.core.utils.DatabaseManager dbManager) {
        dbManager.executeStatement("CREATE TABLE IF NOT EXISTS genscore_teams (team_id INTEGER PRIMARY KEY AUTOINCREMENT, name VARCHAR(32) UNIQUE, leader_uuid VARCHAR(36));");
        dbManager.executeStatement("CREATE TABLE IF NOT EXISTS genscore_team_stats (team_id INTEGER PRIMARY KEY, weekly_points INTEGER DEFAULT 0, total_points INTEGER DEFAULT 0, FOREIGN KEY(team_id) REFERENCES genscore_teams(team_id) ON DELETE CASCADE);");
        dbManager.executeStatement("CREATE TABLE IF NOT EXISTS genscore_team_quests (team_id INTEGER PRIMARY KEY, quest_id TEXT, progress INTEGER DEFAULT 0, FOREIGN KEY(team_id) REFERENCES genscore_teams(team_id) ON DELETE CASCADE);");
        dbManager.executeStatement("CREATE TABLE IF NOT EXISTS genscore_team_members (team_id INTEGER, player_uuid VARCHAR(36) PRIMARY KEY, FOREIGN KEY(team_id) REFERENCES genscore_teams(team_id));");
    }

    @Override
    public void enable() {
        enabled = true;
        
        this.teamDAO = new fr.gens.core.database.TeamDAO(plugin);
        this.teamDAO.initDatabase();
        
        teamGui = new TeamGui(plugin);
        teamListener = new TeamListener(plugin, teamGui);

        teamCommand = new TeamCommand(plugin, teamGui, this);
        Bukkit.getPluginManager().registerEvents(teamListener, plugin);
        plugin.getLangManager().sendConsoleMessage("teammodule.log_1");
    }

    @Override
    public void registerCommands(fr.gens.core.CorePlugin plugin) {
        if (plugin.getCommandManager() != null && plugin.getCommandManager().getAnnotationParser() != null && teamCommand != null) {
            plugin.getCommandManager().getAnnotationParser().parse(teamCommand);
        }
    }

    @Override
    public void disable() {
        enabled = false;
        if (teamListener != null) {
            org.bukkit.event.HandlerList.unregisterAll(teamListener);
        }
        plugin.getLangManager().sendConsoleMessage("teammodule.log_2");
    }
}



