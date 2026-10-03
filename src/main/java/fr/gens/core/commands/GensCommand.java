package fr.gens.core.commands;

import fr.gens.core.CorePlugin;
import fr.gens.core.utils.MetricsService;
import fr.gens.core.utils.PlaceholderUtils;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.Permission;

import java.util.Map;

public class GensCommand {

    private final CorePlugin plugin;

    public GensCommand(CorePlugin plugin) {
        this.plugin = plugin;
    }

    @Command("gens")
    @Permission("genscore.admin")
    public void executeRoot(CommandSender sender) {
        sender.sendMessage(PlaceholderUtils.parseToComponent("<dark_gray>----------------------------------------"));
        sender.sendMessage(PlaceholderUtils.parseToComponent("<gold><bold>[GensCore]</bold> <gray>Panneau d'administration systeme (v" + plugin.getPluginMeta().getVersion() + ")"));
        sender.sendMessage(PlaceholderUtils.parseToComponent("<yellow>/gens status <dark_gray>- <gray>Affiche l'etat general et les performances (JVM, Folia)"));
        sender.sendMessage(PlaceholderUtils.parseToComponent("<yellow>/gens db <dark_gray>- <gray>Affiche l'etat du moteur de base de donnees et du pool"));
        sender.sendMessage(PlaceholderUtils.parseToComponent("<yellow>/module <nom> <on|off> <dark_gray>- <gray>Active ou desactive un module"));
        sender.sendMessage(PlaceholderUtils.parseToComponent("<dark_gray>----------------------------------------"));
    }

    @Command("gens db")
    @Permission("genscore.admin")
    public void executeDb(CommandSender sender) {
        if (plugin.getDatabaseManager() == null) {
            sender.sendMessage(PlaceholderUtils.parseToComponent("<red>[GensCore] Base de donnees non initialisee."));
            return;
        }

        String dbType = plugin.getDatabaseManager().getDatabaseType().toUpperCase();
        boolean isMysql = plugin.getDatabaseManager().isMySQL();
        String engineDisplay = isMysql ? "<aqua>MySQL / MariaDB (Distant)</aqua>" : "<green>SQLite (Local WAL)</green>";

        int active = plugin.getDatabaseManager().getActiveConnections();
        int idle = plugin.getDatabaseManager().getIdleConnections();
        int total = plugin.getDatabaseManager().getTotalConnections();
        int waiting = plugin.getDatabaseManager().getThreadsAwaitingConnection();

        sender.sendMessage(PlaceholderUtils.parseToComponent("<dark_gray>----------------------------------------"));
        sender.sendMessage(PlaceholderUtils.parseToComponent("<gold><bold>[GensCore Base de Donnees]</bold>"));
        sender.sendMessage(PlaceholderUtils.parseToComponent("<gray>Moteur actif : " + engineDisplay));
        sender.sendMessage(PlaceholderUtils.parseToComponent("<gray>Type detecte : <yellow>" + dbType));
        sender.sendMessage(PlaceholderUtils.parseToComponent("<gray>Pool HikariCP : <yellow>" + active + "</yellow> actives <dark_gray>| <yellow>" + idle + "</yellow> inactives <dark_gray>| <yellow>" + total + "</yellow> total"));
        if (waiting > 0) {
            sender.sendMessage(PlaceholderUtils.parseToComponent("<red>Threads en attente : " + waiting));
        }
        sender.sendMessage(PlaceholderUtils.parseToComponent("<dark_gray>----------------------------------------"));
    }

    @Command("gens status")
    @Permission("genscore.admin")
    public void executeStatus(CommandSender sender) {
        MetricsService metrics = plugin.getMetricsService();
        if (metrics == null) {
            sender.sendMessage(PlaceholderUtils.parseToComponent("<red>[GensCore] Service de metriques indisponible."));
            return;
        }

        Map<String, Object> data = metrics.getMetricsMap();
        @SuppressWarnings("unchecked")
        Map<String, Object> jvm = (Map<String, Object>) data.get("jvm");
        @SuppressWarnings("unchecked")
        Map<String, Object> server = (Map<String, Object>) data.get("server");
        @SuppressWarnings("unchecked")
        Map<String, Object> db = (Map<String, Object>) data.get("database");
        @SuppressWarnings("unchecked")
        Map<String, Object> modules = (Map<String, Object>) data.get("modules");

        long uptimeSec = ((Number) data.getOrDefault("uptime_seconds", 0L)).longValue();
        long hours = uptimeSec / 3600;
        long minutes = (uptimeSec % 3600) / 60;
        long seconds = uptimeSec % 60;
        String uptimeStr = String.format("%02dh %02dm %02ds", hours, minutes, seconds);

        sender.sendMessage(PlaceholderUtils.parseToComponent("<dark_gray>----------------------------------------"));
        sender.sendMessage(PlaceholderUtils.parseToComponent("<gold><bold>[GensCore Sante du Systeme]</bold>"));
        sender.sendMessage(PlaceholderUtils.parseToComponent("<gray>Uptime : <yellow>" + uptimeStr + "</yellow> <dark_gray>| <gray>Java : <yellow>" + (jvm != null ? jvm.get("java_version") : "?") + "</yellow>"));
        
        if (jvm != null) {
            long used = ((Number) jvm.getOrDefault("used_memory_mb", 0)).longValue();
            long total = ((Number) jvm.getOrDefault("total_memory_mb", 0)).longValue();
            long max = ((Number) jvm.getOrDefault("max_memory_mb", 0)).longValue();
            sender.sendMessage(PlaceholderUtils.parseToComponent("<gray>Memoire JVM : <yellow>" + used + " MB</yellow> utilisee <dark_gray>/ <gray>" + total + " MB allouee <dark_gray>(Max: " + max + " MB)"));
        }

        if (server != null) {
            boolean folia = Boolean.TRUE.equals(server.get("is_folia"));
            String modeStr = folia ? "<aqua>Folia (Multi-thread regional)</aqua>" : "<green>Paper Standard</green>";
            sender.sendMessage(PlaceholderUtils.parseToComponent("<gray>Mode d'execution : " + modeStr));
            sender.sendMessage(PlaceholderUtils.parseToComponent("<gray>Joueurs connectes : <yellow>" + server.get("online_players") + "</yellow> / <yellow>" + server.get("max_players") + "</yellow>"));
        }

        if (db != null) {
            String dbType = String.valueOf(db.getOrDefault("type", "sqlite")).toUpperCase();
            sender.sendMessage(PlaceholderUtils.parseToComponent("<gray>Base de donnees : <yellow>" + dbType + "</yellow> <dark_gray>(Connexions: " + db.get("active_connections") + "/" + db.get("total_connections") + ")"));
        }

        if (modules != null) {
            sender.sendMessage(PlaceholderUtils.parseToComponent("<gray>Modules actifs : <green>" + modules.get("active_count") + "</green> / <yellow>" + modules.get("total_count") + "</yellow>"));
        }

        sender.sendMessage(PlaceholderUtils.parseToComponent("<dark_gray>----------------------------------------"));
    }
}
