package fr.gens.core.modules;

import fr.gens.core.CorePlugin;

public class MinigamesModule implements Module {

    private final CorePlugin plugin;
    private boolean enabled = false;

    public MinigamesModule(CorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "minigames";
    }

    @Override
    public String getDescription() {
        return "Mini-jeux interactifs sur le panel web (Roue de la fortune, Casino)";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void enable() {
        this.enabled = true;
        try {
            org.bukkit.configuration.file.FileConfiguration config = plugin.getConfigManager().getConfig("modules/minigames.yml");
            config.set("minigames.enabled", true);
            plugin.getConfigManager().saveConfigAsync("modules/minigames.yml");
        } catch (Throwable ignored) {}
    }

    @Override
    public void disable() {
        this.enabled = false;
        try {
            org.bukkit.configuration.file.FileConfiguration config = plugin.getConfigManager().getConfig("modules/minigames.yml");
            config.set("minigames.enabled", false);
            plugin.getConfigManager().saveConfigAsync("modules/minigames.yml");
        } catch (Throwable ignored) {}
    }
}
