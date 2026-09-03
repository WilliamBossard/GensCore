package fr.gens.core.modules;

import fr.gens.core.CorePlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;


public class ShopModule implements Module, Listener {

    private final CorePlugin plugin;
    private boolean enabled = false;

    public ShopModule(CorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "Shop";
    }

    @Override
    public String getDescription() {
        return "Gère les boutiques des joueurs et l'économie interne.";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void initDatabase(fr.gens.core.utils.DatabaseManager dbManager) {
        dbManager.executeStatement("CREATE TABLE IF NOT EXISTS shop_categories (id VARCHAR(50) PRIMARY KEY, displayName VARCHAR(255) NOT NULL, icon VARCHAR(50) NOT NULL);");
        dbManager.executeStatement("CREATE TABLE IF NOT EXISTS shop_items (material VARCHAR(50) PRIMARY KEY, category_id VARCHAR(50) NOT NULL, buyPrice DOUBLE NOT NULL, sellPrice DOUBLE NOT NULL, stock INTEGER DEFAULT 0, targetStock INTEGER DEFAULT 1000, isCommand BOOLEAN DEFAULT 0, commandToExecute TEXT, isEnabled BOOLEAN DEFAULT 1, FOREIGN KEY(category_id) REFERENCES shop_categories(id) ON DELETE CASCADE);");
        dbManager.executeStatement("CREATE TABLE IF NOT EXISTS shop_history (id INTEGER PRIMARY KEY AUTOINCREMENT, material VARCHAR(50) NOT NULL, timestamp BIGINT NOT NULL, buyPrice DOUBLE NOT NULL, sellPrice DOUBLE NOT NULL, stock INTEGER NOT NULL, FOREIGN KEY(material) REFERENCES shop_items(material) ON DELETE CASCADE);");
    }

    @Override
    public void enable() {
        enabled = true;
        // Enregistrer les événements de ce module
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getLangManager().sendConsoleMessage("shopmodule.log_1");
    }

    @Override
    public void disable() {
        enabled = false;
        // Désenregistrer les événements de ce module pour qu'il s'arrête instantanément
        HandlerList.unregisterAll(this);
        plugin.getLangManager().sendConsoleMessage("shopmodule.log_2");
    }

    // Exemple d'événement qui ne fonctionne que si le module est actif
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!enabled) return; // Sécurité supplémentaire
        event.getPlayer().sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<green>Le Shop est actuellement ouvert !"));
    }
}



