package fr.gens.core.modules;

public interface Module {
    String getName();
    String getDescription();
    boolean isEnabled();
    void enable();
    void disable();
    
    default void initDatabase(fr.gens.core.utils.DatabaseManager dbManager) {
        // Optionnel : les modules peuvent créer leurs tables SQL ici
    }
    
    default void registerCommands(fr.gens.core.CorePlugin plugin) {
        // Enregistre automatiquement les commandes annotées avec Cloud
        if (plugin.getCommandManager() != null && plugin.getCommandManager().getAnnotationParser() != null) {
            plugin.getCommandManager().getAnnotationParser().parse(this);
        }
    }
    
    default void unregisterListeners(org.bukkit.event.Listener listener) {
        org.bukkit.event.HandlerList.unregisterAll(listener);
    }
}

