package fr.gens.core.modules.tomb;

import fr.gens.core.CorePlugin;
import fr.gens.core.modules.Module;
import org.bukkit.Bukkit;


public class TombModule implements Module {

    private final CorePlugin plugin;
    private boolean enabled;
    private TombManager tombManager;
    private int checkTask = -1;
    
    private fr.gens.core.database.TombDAO tombDAO;

    public TombModule(CorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "tomb";
    }

    @Override
    public String getDescription() {
        return "GÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¨re les tombes des joueurs ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â  leur mort";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void initDatabase(fr.gens.core.utils.DatabaseManager dbManager) {
        dbManager.executeStatement("CREATE TABLE IF NOT EXISTS tombs (id VARCHAR(36) PRIMARY KEY, owner_id VARCHAR(36) NOT NULL, world VARCHAR(50) NOT NULL, x DOUBLE NOT NULL, y DOUBLE NOT NULL, z DOUBLE NOT NULL, contents TEXT NOT NULL, xp INTEGER NOT NULL, expiration_time BIGINT NOT NULL);");
    }

    @Override
    public void enable() {
        this.enabled = true;
        
        this.tombDAO = new fr.gens.core.database.TombDAO(plugin);
        this.tombDAO.initDatabase();
        
        this.tombManager = new TombManager(plugin, this);
        this.tombManager.loadTombs();

        Bukkit.getPluginManager().registerEvents(new TombListener(plugin, this), plugin);

        // TÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢che de vÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©rification des expirations et mise ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â  jour de l'hologramme toutes les secondes (20 ticks)
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

    public fr.gens.core.database.TombDAO getTombDAO() {
        return tombDAO;
    }
}

