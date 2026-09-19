package fr.gens.core.modules.perks;

import fr.gens.core.CorePlugin;
import fr.gens.core.database.SoloPerkDAO;
import fr.gens.core.modules.Module;
import fr.gens.core.utils.DatabaseManager;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

public class SoloPerkModule implements Module {

    private final CorePlugin plugin;
    private boolean enabled = false;

    private SoloPerkDAO perkDAO;
    private SoloPerkManager manager;
    private SoloPerkListener listener;
    private SoloPerkGui gui;
    private SoloPerkCommand command;

    public SoloPerkModule(CorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "solo_perks";
    }

    @Override
    public String getDescription() {
        return "Bonus et maitrises individuels lies a la progression des quetes";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void initDatabase(DatabaseManager dbManager) {
        this.perkDAO = new SoloPerkDAO(plugin);
        this.perkDAO.initDatabase();
    }

    private boolean commandsRegistered = false;

    @Override
    public void enable() {
        if (perkDAO == null) {
            this.perkDAO = new SoloPerkDAO(plugin);
            this.perkDAO.initDatabase();
        }

        if (this.manager == null) {
            this.manager = new SoloPerkManager(plugin, perkDAO);
        }
        if (this.listener == null) {
            this.listener = new SoloPerkListener(plugin, manager);
        }
        if (this.gui == null) {
            this.gui = new SoloPerkGui(plugin, manager);
        }
        if (this.command == null) {
            this.command = new SoloPerkCommand(this, manager, gui);
        }

        plugin.getServer().getPluginManager().registerEvents(listener, plugin);
        listener.startTasks();

        registerCommands(plugin);

        // Charger les joueurs deja connectes (ex: en cas de reload)
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            manager.loadPlayer(p.getUniqueId());
        }

        this.enabled = true;
    }

    @Override
    public void registerCommands(CorePlugin plugin) {
        if (commandsRegistered) return;
        if (this.command == null) {
            if (this.perkDAO == null) {
                this.perkDAO = new SoloPerkDAO(plugin);
                this.perkDAO.initDatabase();
            }
            if (this.manager == null) {
                this.manager = new SoloPerkManager(plugin, perkDAO);
            }
            if (this.gui == null) {
                this.gui = new SoloPerkGui(plugin, manager);
            }
            this.command = new SoloPerkCommand(this, manager, gui);
        }

        if (plugin.getCommandManager() != null && plugin.getCommandManager().getAnnotationParser() != null && command != null) {
            plugin.getCommandManager().getAnnotationParser().parse(command);
            commandsRegistered = true;
        }
    }

    @Override
    public void disable() {
        if (listener != null) {
            listener.stopTasks();
            HandlerList.unregisterAll(listener);
        }

        this.enabled = false;
    }

    public SoloPerkManager getManager() {
        return manager;
    }

    public SoloPerkGui getGui() {
        return gui;
    }

    public SoloPerkDAO getPerkDAO() {
        return perkDAO;
    }
}
