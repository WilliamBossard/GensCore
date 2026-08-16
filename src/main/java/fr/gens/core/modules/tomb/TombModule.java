package fr.gens.core.modules.tomb;

import fr.gens.core.CorePlugin;
import fr.gens.core.modules.Module;
import org.bukkit.Bukkit;

public class TombModule implements Module {

    private final CorePlugin plugin;
    private boolean enabled;
    private TombManager tombManager;
    private int checkTask = -1;

    public TombModule(CorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "tomb";
    }

    @Override
    public String getDescription() {
        return "Gère les tombes des joueurs à leur mort";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void enable() {
        this.enabled = true;
        this.tombManager = new TombManager(plugin, this);
        this.tombManager.loadTombs();

        Bukkit.getPluginManager().registerEvents(new TombListener(plugin, this), plugin);

        // Tâche de vérification des expirations et mise à jour de l'hologramme toutes les secondes (20 ticks)
        this.checkTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            tombManager.checkExpirations();
        }, 20L, 20L);
    }

    @Override
    public void disable() {
        this.enabled = false;
        if (checkTask != -1) {
            Bukkit.getScheduler().cancelTask(checkTask);
            checkTask = -1;
        }
    }

    public TombManager getTombManager() {
        return tombManager;
    }
}
