package fr.gens.core.modules.teams;

import fr.gens.core.CorePlugin;
import fr.gens.core.modules.Module;
import org.bukkit.Bukkit;


public class TeamModule implements Module {
    private final CorePlugin plugin;
    private TeamListener teamListener;
    private TeamGui teamGui;
    private boolean enabled;
    
    private fr.gens.core.database.TeamDAO teamDAO;

    public TeamModule(CorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() { return "Teams"; }

    @Override
    public String getDescription() { return "SystÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¨me de guildes et d'ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©quipes."; }

    @Override
    public boolean isEnabled() { return enabled; }

    public fr.gens.core.database.TeamDAO getTeamDAO() {
        return teamDAO;
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

        TeamCommand teamCmd = new TeamCommand(plugin, teamGui);
        org.bukkit.command.PluginCommand cmd_team = plugin.getCommand("team");
        if (cmd_team != null) cmd_team.setExecutor(teamCmd);
        org.bukkit.command.PluginCommand cmd_team_tc = plugin.getCommand("team");
        if (cmd_team_tc != null) cmd_team_tc.setTabCompleter(teamCmd);
        Bukkit.getPluginManager().registerEvents(teamListener, plugin);
        plugin.getLangManager().sendConsoleMessage("teammodule.log_1");
    }

    @Override
    public void disable() {
        enabled = false;
        plugin.getLangManager().sendConsoleMessage("teammodule.log_2");
    }
}

