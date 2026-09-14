package fr.gens.core.database;

import fr.gens.core.CorePlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;


public class PendingCommandDAO {

    private final CorePlugin plugin;

    public PendingCommandDAO(CorePlugin plugin) {
        this.plugin = plugin;
    }

    public void initDatabase() {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             Statement stmt = conn.createStatement()) {
            
            stmt.execute("CREATE TABLE IF NOT EXISTS pending_rewards (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "uuid VARCHAR(36) NOT NULL, " +
                    "command TEXT, " +
                    "message TEXT, " +
                    "item_data TEXT" +
                    ");");

            // Migration transparente pour les bases de données existantes
            try {
                stmt.execute("ALTER TABLE pending_rewards ADD COLUMN item_data TEXT;");
            } catch (SQLException ignored) {
                // La colonne existe déjà
            }
                    
        } catch (SQLException e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Erreur lors de la création de la table pending_rewards", e);
        }
    }

    public void addPendingCommand(UUID uuid, String command, String message) {
        addPendingReward(uuid, command, message, null);
    }

    public void addPendingReward(UUID uuid, String command, String message, String itemDataBase64) {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement("INSERT INTO pending_rewards (uuid, command, message, item_data) VALUES (?, ?, ?, ?)")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, command);
            ps.setString(3, message);
            ps.setString(4, itemDataBase64);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Représente une récompense, un item ou une commande différée en attente d'attribution pour un joueur.
     * Déclaré sous forme de record (Java 16+) pour l'immuabilité et la concision.
     */
    public record PendingReward(int id, String command, String message, String itemData) {}

    public void processPendingCommands(Player p) {
        plugin.getFoliaLib().getScheduler().runAsync((wrappedTask) -> {
            java.util.List<PendingReward> rewardsToProcess = new java.util.ArrayList<>();

            // NOTE ARCHITECTURALE (SQLite Concurrence & Folia) :
            // 1. Sur SQLite, exécuter un DELETE pendant qu'un ResultSet de SELECT est actif sur la même
            //    connexion provoque l'erreur critique 'SQLITE_LOCKED: database table is locked'.
            //    On extrait donc TOUTES les récompenses en mémoire avant de fermer la requête de sélection.
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT id, command, message, item_data FROM pending_rewards WHERE uuid = ?")) {
                ps.setString(1, p.getUniqueId().toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        rewardsToProcess.add(new PendingReward(
                                rs.getInt("id"),
                                rs.getString("command"),
                                rs.getString("message"),
                                rs.getString("item_data")
                        ));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(java.util.logging.Level.SEVERE, "Erreur lors de la récupération des récompenses en attente", e);
                return;
            }

            if (rewardsToProcess.isEmpty()) return;

            // 2. Traitement des commandes, messages et items en attente :
            for (PendingReward reward : rewardsToProcess) {
                // Sur Folia, dispatchCommand avec la console peut impacter l'état global du serveur (permissions,
                // broadcasts, gestion des mondes). Il doit impérativement s'exécuter sur le GlobalRegionScheduler (runNextTick).
                if (reward.command() != null && !reward.command().isEmpty()) {
                    String finalCmd = reward.command().replace("%player%", p.getName());
                    plugin.getFoliaLib().getScheduler().runNextTick((t) -> {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCmd);
                    });
                }

                // L'envoi du message privé et la remise d'item sont planifiés sur le thread régional du joueur (runAtEntity)
                plugin.getFoliaLib().getScheduler().runAtEntity(p, (t) -> {
                    if (!p.isOnline()) return;

                    if (reward.message() != null && !reward.message().isEmpty()) {
                        p.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent(reward.message()));
                    }

                    // Distribution sécurisée de l'objet différé (ex: achat Hôtel des Ventes hors-ligne)
                    if (reward.itemData() != null && !reward.itemData().isEmpty()) {
                        try {
                            org.bukkit.inventory.ItemStack item = fr.gens.core.utils.ItemSerializer.fromBase64(reward.itemData());
                            if (item != null && item.getType() != org.bukkit.Material.AIR) {
                                java.util.Map<Integer, org.bukkit.inventory.ItemStack> leftover = p.getInventory().addItem(item);
                                if (!leftover.isEmpty()) {
                                    for (org.bukkit.inventory.ItemStack drop : leftover.values()) {
                                        p.getWorld().dropItemNaturally(p.getLocation(), drop);
                                    }
                                }
                            }
                        } catch (Exception ex) {
                            plugin.getLogger().warning("[PendingCommandDAO] Erreur lors de la distribution d'item en attente: " + ex.getMessage());
                        }
                    }
                });
            }

            // 3. Suppression des récompenses traitées (maintenant que le ResultSet est libéré)
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement del = conn.prepareStatement("DELETE FROM pending_rewards WHERE id = ?")) {
                for (PendingReward reward : rewardsToProcess) {
                    del.setInt(1, reward.id());
                    del.addBatch();
                }
                del.executeBatch();
            } catch (SQLException e) {
                plugin.getLogger().log(java.util.logging.Level.SEVERE, "Erreur lors de la suppression des récompenses traitées", e);
            }
        });
    }
}



