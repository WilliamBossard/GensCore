package fr.gens.core.utils;

import fr.gens.core.CorePlugin;
import org.bukkit.Bukkit;

import java.util.LinkedHashMap;
import java.util.Map;

public class MetricsService {

    private final CorePlugin plugin;
    private final long startTime;

    public MetricsService(CorePlugin plugin) {
        this.plugin = plugin;
        this.startTime = System.currentTimeMillis();
    }

    public Map<String, Object> getMetricsMap() {
        Map<String, Object> metrics = new LinkedHashMap<>();

        // Statut general
        metrics.put("status", "ONLINE");
        metrics.put("uptime_seconds", (System.currentTimeMillis() - startTime) / 1000);
        metrics.put("timestamp", System.currentTimeMillis());

        // JVM
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();

        Map<String, Object> jvm = new LinkedHashMap<>();
        jvm.put("used_memory_mb", usedMemory / (1024 * 1024));
        jvm.put("total_memory_mb", totalMemory / (1024 * 1024));
        jvm.put("max_memory_mb", maxMemory / (1024 * 1024));
        jvm.put("free_memory_mb", freeMemory / (1024 * 1024));
        jvm.put("active_threads", Thread.activeCount());
        jvm.put("java_version", System.getProperty("java.version"));
        metrics.put("jvm", jvm);

        // Serveur & Folia
        Map<String, Object> server = new LinkedHashMap<>();
        server.put("online_players", Bukkit.getOnlinePlayers().size());
        server.put("max_players", Bukkit.getMaxPlayers());
        server.put("is_folia", plugin.getFoliaLib() != null && plugin.getFoliaLib().isFolia());
        server.put("server_version", Bukkit.getVersion());
        metrics.put("server", server);

        // Base de donnees
        Map<String, Object> database = new LinkedHashMap<>();
        if (plugin.getDatabaseManager() != null) {
            database.put("type", plugin.getDatabaseManager().getDatabaseType());
            database.put("is_mysql", plugin.getDatabaseManager().isMySQL());
            database.put("active_connections", plugin.getDatabaseManager().getActiveConnections());
            database.put("idle_connections", plugin.getDatabaseManager().getIdleConnections());
            database.put("total_connections", plugin.getDatabaseManager().getTotalConnections());
            database.put("threads_awaiting", plugin.getDatabaseManager().getThreadsAwaitingConnection());
        } else {
            database.put("type", "UNKNOWN");
            database.put("is_mysql", false);
        }
        metrics.put("database", database);

        // Modules
        Map<String, Object> modules = new LinkedHashMap<>();
        if (plugin.getModuleManager() != null) {
            long enabledCount = plugin.getModuleManager().getModules().stream()
                    .filter(fr.gens.core.modules.Module::isEnabled)
                    .count();
            modules.put("active_count", enabledCount);
            modules.put("total_count", plugin.getModuleManager().getModules().size());
        }
        metrics.put("modules", modules);

        return metrics;
    }
}
