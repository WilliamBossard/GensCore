package fr.gens.core.modules;

import fr.gens.core.CorePlugin;
// Les anciens imports manuels des modules ont été supprimés car l'Auto-Discovery (org.reflections) s'en charge.
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;


public class ModuleManager {

    private final CorePlugin plugin;
    private final Map<String, Module> modules = new LinkedHashMap<>();

    public ModuleManager(CorePlugin plugin) {
        this.plugin = plugin;
    }

    public void registerModules() {
        // Enregistrement dynamique de tous les modules avec org.reflections
        org.reflections.Reflections reflections = new org.reflections.Reflections("fr.gens.core.modules");
        java.util.Set<Class<? extends Module>> moduleClasses = reflections.getSubTypesOf(Module.class);

        for (Class<? extends Module> clazz : moduleClasses) {
            if (java.lang.reflect.Modifier.isAbstract(clazz.getModifiers()) || clazz.isInterface()) {
                continue;
            }
            try {
                Module module = clazz.getConstructor(CorePlugin.class).newInstance(plugin);
                addModule(module);
            } catch (Exception e) {
                plugin.getLogger().severe("Impossible de charger le module dynamiquement: " + clazz.getName());
                e.printStackTrace();
            }
        }
        
        plugin.getLangManager().sendConsoleMessage("module.manager.loaded", net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("count", String.valueOf(modules.size())));

        // Charger les états depuis modules.yml et les activer si besoin
        org.bukkit.configuration.file.FileConfiguration modulesConfig = plugin.getConfigManager().getConfig("modules.yml");
        for (Module module : modules.values()) {
            boolean shouldEnable = modulesConfig.getBoolean("modules." + module.getName().toLowerCase(), true);
            if (shouldEnable) {
                module.initDatabase(plugin.getDatabaseManager());
                module.enable();
            } else {
                module.disable();
            }
        }
    }

    private void addModule(Module module) {
        modules.put(module.getName().toLowerCase(), module);
    }

    public void disableAllModules() {
        java.util.List<Module> reversedModules = new java.util.ArrayList<>(modules.values());
        java.util.Collections.reverse(reversedModules);
        for (Module module : reversedModules) {
            if (module.isEnabled()) {
                module.disable();
            }
        }
    }

    public Module getModule(String name) {
        return modules.get(name.toLowerCase());
    }

    public Collection<Module> getModules() {
        return modules.values();
    }

    public boolean toggleModule(String name, boolean state) {
        Module module = getModule(name);
        if (module == null) return false;

        boolean changed = false;
        if (state && !module.isEnabled()) {
            module.enable();
            changed = true;
        } else if (!state && module.isEnabled()) {
            module.disable();
            changed = true;
        }

        if (changed) {
            org.bukkit.configuration.file.FileConfiguration modulesConfig = plugin.getConfigManager().getConfig("modules.yml");
            modulesConfig.set("modules." + module.getName().toLowerCase() + ".enabled", state);
            plugin.getConfigManager().saveConfig("modules.yml");
            return true;
        }
        return false;
    }
}

