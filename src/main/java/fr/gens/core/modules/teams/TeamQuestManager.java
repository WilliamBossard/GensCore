package fr.gens.core.modules.teams;

import fr.gens.core.CorePlugin;
import fr.gens.core.modules.quests.QuestType;
import org.bukkit.Bukkit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class TeamQuestManager {
    private final CorePlugin plugin;
    
    private static class QuestDef {
        String id;
        QuestType type;
        String target;
        int amount;
        int points;
        String desc;

        QuestDef(String id, QuestType type, String target, int amount, int points, String desc) {
            this.id = id;
            this.type = type;
            this.target = target;
            this.amount = amount;
            this.points = points;
            this.desc = desc;
        }
    }

    private final java.util.List<QuestDef> questPool = java.util.Arrays.asList(
        new QuestDef("weekly_1", QuestType.BREAK, "STONE", 25000, 1000, "Mine 25 000 blocs de roche en équipe !"),
        new QuestDef("weekly_2", QuestType.KILL, "ZOMBIE", 2500, 1000, "Tuez 2 500 zombies en équipe !"),
        new QuestDef("weekly_3", QuestType.BREAK, "OAK_LOG", 5000, 1000, "Coupez 5 000 bûches de chêne en équipe !"),
        new QuestDef("weekly_4", QuestType.BREAK, "WHEAT", 10000, 1000, "Récoltez 10 000 blés en équipe !"),
        new QuestDef("weekly_5", QuestType.KILL, "SKELETON", 2500, 1000, "Éliminez 2 500 squelettes en équipe !"),
        new QuestDef("weekly_6", QuestType.BREAK, "NETHERRACK", 50000, 1000, "Mine 50 000 blocs de netherrack en équipe !")
    );

    private QuestDef activeQuest;

    // teamId -> progress
    private final Map<Integer, Integer> teamProgress = new HashMap<>();

    public TeamQuestManager(CorePlugin plugin) {
        this.plugin = plugin;
        checkRotation();
        loadProgress();
    }

    private void checkRotation() {
        org.bukkit.configuration.file.FileConfiguration config = plugin.getConfigManager().getConfig("modules/teams.yml");
        long lastRotation = config.getLong("teams.last_rotation", 0);
        long currentWeek = System.currentTimeMillis() / (1000 * 60 * 60 * 24 * 7); // Identifiant unique pour la semaine
        
        String savedId = config.getString("teams.active_quest", "");
        
        if (lastRotation != currentWeek || savedId.isEmpty()) {
            // Rotation!
            java.util.Collections.shuffle(questPool);
            activeQuest = questPool.get(0);
            
            config.set("teams.last_rotation", currentWeek);
            config.set("teams.active_quest", activeQuest.id);
            plugin.getConfigManager().saveConfig("modules/teams.yml");
            
            // Clear progress
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement stmt = conn.prepareStatement("DELETE FROM genscore_team_quests")) {
                stmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            teamProgress.clear();
        } else {
            activeQuest = questPool.stream().filter(q -> q.id.equals(savedId)).findFirst().orElse(questPool.get(0));
        }
    }

    private void loadProgress() {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT team_id, progress FROM genscore_team_quests WHERE quest_id = ?")) {
            stmt.setString(1, activeQuest.id);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    teamProgress.put(rs.getInt("team_id"), rs.getInt("progress"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void saveProgress(int teamId, int progress) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                         "INSERT INTO genscore_team_quests (team_id, quest_id, progress) VALUES (?, ?, ?) " +
                         "ON CONFLICT(team_id) DO UPDATE SET progress = ?, quest_id = ?")) {
                stmt.setInt(1, teamId);
                stmt.setString(2, activeQuest.id);
                stmt.setInt(3, progress);
                stmt.setInt(4, progress);
                stmt.setString(5, activeQuest.id);
                stmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public void addProgress(TeamData team, QuestType type, String target, int amount) {
        if (team == null) return;
        if (type != activeQuest.type) return;
        if (!target.equalsIgnoreCase(activeQuest.target)) return;

        int current = teamProgress.getOrDefault(team.getTeamId(), 0);
        if (current >= activeQuest.amount) return; // Déjà fini

        current += amount;
        if (current >= activeQuest.amount) {
            current = activeQuest.amount;
            team.broadcast("<green><bold> Votre guilde a terminé la Quête Hebdomadaire !");
            team.broadcast("<yellow>+" + activeQuest.points + " Points de Guilde !");
            
            // Ajouter les points
            team.addPoints(activeQuest.points);
            
            // Sauvegarder les points dans la DB
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try (Connection conn = plugin.getDatabaseManager().getConnection();
                     PreparedStatement stmt = conn.prepareStatement("UPDATE genscore_team_stats SET weekly_points = ?, total_points = ? WHERE team_id = ?")) {
                    stmt.setInt(1, team.getWeeklyPoints());
                    stmt.setInt(2, team.getTotalPoints());
                    stmt.setInt(3, team.getTeamId());
                    stmt.executeUpdate();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                
                // Distribuer les récompenses
                fr.gens.core.modules.EconomyModule eco = (fr.gens.core.modules.EconomyModule) plugin.getModuleManager().getModule("economy");
                boolean ecoEnabled = (eco != null && eco.isEnabled());
                double rewardAmount = ecoEnabled ? 50000.0 : 0.0;
                String rewardItem = !ecoEnabled ? "DIAMOND_BLOCK:4" : "";
                
                for (java.util.UUID memberId : team.getMembers()) {
                    plugin.getDatabaseManager().addPendingReward(memberId, rewardAmount, rewardItem);
                    org.bukkit.entity.Player p = Bukkit.getPlayer(memberId);
                    if (p != null && p.isOnline()) {
                        Bukkit.getScheduler().runTask(plugin, () -> plugin.getDatabaseManager().processPendingRewards(p));
                    }
                }
            });
        }
        
        teamProgress.put(team.getTeamId(), current);
        saveProgress(team.getTeamId(), current);
    }

    public int getProgress(int teamId) {
        return teamProgress.getOrDefault(teamId, 0);
    }
    
    public int getGoal() {
        return activeQuest.amount;
    }
    
    public String getDesc() {
        fr.gens.core.modules.EconomyModule eco = (fr.gens.core.modules.EconomyModule) plugin.getModuleManager().getModule("economy");
        boolean ecoEnabled = (eco != null && eco.isEnabled());
        String rewardStr = ecoEnabled ? "50 000 $ / membre" : "4 Blocs de Diamant / membre";
        return activeQuest.desc + " (Récompense: " + rewardStr + ")";
    }
}
