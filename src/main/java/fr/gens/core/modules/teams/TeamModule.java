package fr.gens.core.modules.teams;

import fr.gens.core.CorePlugin;
import fr.gens.core.modules.Module;
import org.bukkit.Bukkit;

public class TeamModule implements Module {
    private final CorePlugin plugin;
    private TeamListener teamListener;
    private TeamGui teamGui;
    private boolean enabled;

    public TeamModule(CorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() { return "Teams"; }

    @Override
    public String getDescription() { return "Système de guildes et d'équipes."; }

    @Override
    public boolean isEnabled() { return enabled; }

    @Override
    public void enable() {
        enabled = true;
        teamGui = new TeamGui(plugin);
        teamListener = new TeamListener(plugin, teamGui);

        TeamCommand teamCmd = new TeamCommand(plugin, teamGui);
        plugin.getCommand("team").setExecutor(teamCmd);
        plugin.getCommand("team").setTabCompleter(teamCmd);
        Bukkit.getPluginManager().registerEvents(teamListener, plugin);
        plugin.getLangManager().sendConsoleMessage("teammodule.log_1");
    }

    @Override
    public void disable() {
        enabled = false;
        plugin.getLangManager().sendConsoleMessage("teammodule.log_2");
    }
}
