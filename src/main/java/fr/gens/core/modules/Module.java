package fr.gens.core.modules;

public interface Module {
    String getName();
    String getDescription();
    boolean isEnabled();
    void enable();
    void disable();
    
    default void registerCommands(fr.gens.core.CorePlugin plugin) {
        // Optionnel : les modules peuvent surcharger cette méthode pour enregistrer leurs propres commandes
    }
}
