package fr.gens.core.database;

import fr.gens.core.CorePlugin;
import fr.gens.core.modules.teams.TeamData;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


public class TeamDAO {

    private final CorePlugin plugin;

    public TeamDAO(CorePlugin plugin) {
        this.plugin = plugin;
    }

    public void initDatabase() {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             Statement stmt = conn.createStatement()) {
            
            stmt.execute("CREATE TABLE IF NOT EXISTS genscore_teams (" +
                    "team_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "name VARCHAR(32) UNIQUE NOT NULL, " +
                    "leader_uuid VARCHAR(36) NOT NULL" +
                    ");");

            stmt.execute("CREATE TABLE IF NOT EXISTS genscore_team_members (" +
                    "team_id INTEGER, " +
                    "player_uuid VARCHAR(36) PRIMARY KEY, " +
                    "FOREIGN KEY(team_id) REFERENCES genscore_teams(team_id) ON DELETE CASCADE" +
                    ");");

            stmt.execute("CREATE TABLE IF NOT EXISTS genscore_team_stats (" +
                    "team_id INTEGER PRIMARY KEY, " +
                    "weekly_points INTEGER DEFAULT 0, " +
                    "total_points INTEGER DEFAULT 0, " +
                    "FOREIGN KEY(team_id) REFERENCES genscore_teams(team_id) ON DELETE CASCADE" +
                    ");");
                    
            stmt.execute("CREATE TABLE IF NOT EXISTS genscore_team_quests (" +
                    "team_id INTEGER PRIMARY KEY, " +
                    "quest_id VARCHAR(50), " +
                    "progress INTEGER DEFAULT 0, " +
                    "FOREIGN KEY(team_id) REFERENCES genscore_teams(team_id) ON DELETE CASCADE" +
                    ");");
                    
            stmt.execute("CREATE TABLE IF NOT EXISTS genscore_locks (" +
                    "lock_id VARCHAR(50) PRIMARY KEY, " +
                    "locked_by VARCHAR(36), " +
                    "timestamp BIGINT" +
                    ");");
                    
            stmt.execute("CREATE TABLE IF NOT EXISTS genscore_pending_rewards (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "uuid VARCHAR(36), " +
                    "amount DOUBLE, " +
                    "item_data TEXT" +
                    ");");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_genscore_pending_rewards_uuid ON genscore_pending_rewards(uuid);");
                    
        } catch (SQLException e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Erreur lors de la crÃƒÆ’Ã‚Â©ation des tables des teams", e);
        }
    }

    public void addPendingReward(UUID uuid, double amount, String itemData) {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement("INSERT INTO genscore_pending_rewards (uuid, amount, item_data) VALUES (?, ?, ?)")) {
            stmt.setString(1, uuid.toString());
            stmt.setDouble(2, amount);
            stmt.setString(3, itemData);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void processPendingRewards(org.bukkit.entity.Player player) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            boolean hasRewards = false;
            try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM genscore_pending_rewards WHERE uuid = ?")) {
                stmt.setString(1, player.getUniqueId().toString());
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    hasRewards = true;
                    double amount = rs.getDouble("amount");
                    String itemData = rs.getString("item_data");
                    
                    if (amount > 0) {
                        fr.gens.core.modules.EconomyModule eco = (fr.gens.core.modules.EconomyModule) plugin.getModuleManager().getModule("economy");
                        if (eco != null && eco.isEnabled()) {
                            eco.addMoney(player.getUniqueId(), amount);
                            plugin.getLangManager().sendMessage(player, "economy.pending_reward", net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("amount", String.valueOf(amount)));
                        }
                    }
                    if (itemData != null && !itemData.isEmpty()) {
                        String[] parts = itemData.split(":");
                        if (parts.length == 2) {
                            try {
                                org.bukkit.Material mat = org.bukkit.Material.valueOf(parts[0]);
                                int count = Integer.parseInt(parts[1]);
                                org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(mat, count);
                                
                                plugin.getFoliaLib().getImpl().runAtEntity(player, (t2) -> {
                                    java.util.HashMap<Integer, org.bukkit.inventory.ItemStack> excess = player.getInventory().addItem(item);
                                    for (org.bukkit.inventory.ItemStack drop : excess.values()) {
                                        player.getWorld().dropItemNaturally(player.getLocation(), drop);
                                    }
                                    plugin.getLangManager().sendMessage(player, "guild.reward_received");
                                });
                            } catch (Exception e) {
                                plugin.getLangManager().sendMessage(player, "error.invalid_reward");
                            }
                        }
                    }
                }
            }
            if (hasRewards) {
                try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM genscore_pending_rewards WHERE uuid = ?")) {
                    stmt.setString(1, player.getUniqueId().toString());
                    stmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void loadTeams(Map<Integer, TeamData> teamsById, Map<UUID, TeamData> teamsByPlayer) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            // Load all teams
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM genscore_teams")) {
                while (rs.next()) {
                    int id = rs.getInt("team_id");
                    String name = rs.getString("name");
                    String leaderStr = rs.getString("leader_uuid");
                    if (leaderStr != null) {
                        TeamData team = new TeamData(id, name, UUID.fromString(leaderStr));
                        teamsById.put(id, team);
                    }
                }
            }

            // Load members
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM genscore_team_members")) {
                while (rs.next()) {
                    int teamId = rs.getInt("team_id");
                    String uuidStr = rs.getString("player_uuid");
                    if (uuidStr != null) {
                        TeamData team = teamsById.get(teamId);
                        if (team != null) {
                            UUID memberUuid = UUID.fromString(uuidStr);
                            team.addMember(memberUuid);
                            teamsByPlayer.put(memberUuid, team);
                        }
                    }
                }
            }

            // Load stats
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM genscore_team_stats")) {
                while (rs.next()) {
                    int teamId = rs.getInt("team_id");
                    TeamData team = teamsById.get(teamId);
                    if (team != null) {
                        team.setWeeklyPoints(rs.getInt("weekly_points"));
                        team.setTotalPoints(rs.getInt("total_points"));
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLangManager().sendConsoleError("teammanager.log_1");
            e.printStackTrace();
        }
    }

    public int createTeam(String name, UUID leader) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            String sql = "INSERT INTO genscore_teams (name, leader_uuid) VALUES (?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, name);
                stmt.setString(2, leader.toString());
                stmt.executeUpdate();

                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        return generatedKeys.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public void initTeamStats(int teamId) {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement statStmt = conn.prepareStatement("INSERT INTO genscore_team_stats (team_id, weekly_points, total_points) VALUES (?, 0, 0)")) {
            statStmt.setInt(1, teamId);
            statStmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void disbandTeam(int teamId) {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM genscore_teams WHERE team_id = ?")) {
            stmt.setInt(1, teamId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addMember(int teamId, UUID member) {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement("INSERT INTO genscore_team_members (team_id, player_uuid) VALUES (?, ?)")) {
            stmt.setInt(1, teamId);
            stmt.setString(2, member.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void removeMember(UUID member) {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM genscore_team_members WHERE player_uuid = ?")) {
            stmt.setString(1, member.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLangManager().sendConsoleError("db.query_error");
            e.printStackTrace();
        }
    }

    // --- Web Stats ---
    public Map<String, Object> getBestTeamStats() {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT t.team_id, t.name, t.leader_uuid, s.weekly_points, s.total_points " +
                 "FROM genscore_teams t " +
                 "LEFT JOIN genscore_team_stats s ON t.team_id = s.team_id " +
                 "ORDER BY s.total_points DESC LIMIT 1")) {
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int teamId = rs.getInt("team_id");
                    Map<String, Object> teamObj = new HashMap<>();
                    teamObj.put("name", rs.getString("name"));
                    teamObj.put("weekly_points", rs.getInt("weekly_points"));
                    teamObj.put("total_points", rs.getInt("total_points"));
                    
                    List<Map<String, String>> members = new ArrayList<>();
                    try (PreparedStatement mStmt = conn.prepareStatement(
                        "SELECT m.player_uuid, COALESCE(p.username, 'Unknown') as name " +
                        "FROM genscore_team_members m LEFT JOIN player_profiles p ON m.player_uuid = p.uuid " +
                        "WHERE m.team_id = ?")) {
                        mStmt.setInt(1, teamId);
                        try (ResultSet mrs = mStmt.executeQuery()) {
                            while (mrs.next()) {
                                Map<String, String> m = new HashMap<>();
                                m.put("uuid", mrs.getString("player_uuid"));
                                m.put("name", mrs.getString("name"));
                                members.add(m);
                            }
                        }
                    }
                    teamObj.put("members", members);
                    return teamObj;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Map<String, Object>> getAllTeamStats(fr.gens.core.modules.teams.TeamQuestManager questManager) {
        List<Map<String, Object>> teamsList = new ArrayList<>();
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT t.team_id, t.name, t.leader_uuid, s.weekly_points, s.total_points " +
                 "FROM genscore_teams t " +
                 "LEFT JOIN genscore_team_stats s ON t.team_id = s.team_id " +
                 "ORDER BY s.weekly_points DESC")) {
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int teamId = rs.getInt("team_id");
                    Map<String, Object> teamObj = new HashMap<>();
                    teamObj.put("name", rs.getString("name"));
                    teamObj.put("weekly_points", rs.getInt("weekly_points"));
                    teamObj.put("total_points", rs.getInt("total_points"));
                    
                    int progress = questManager != null ? questManager.getProgress(teamId) : 0;
                    int goal = questManager != null ? questManager.getGoal() : 1;
                    String desc = questManager != null ? questManager.getDesc() : "QuÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Âªte non dÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©finie";
                    double percentage = Math.min(100.0, ((double) progress / goal) * 100.0);
                    
                    teamObj.put("quest_progress_percent", Math.round(percentage));
                    teamObj.put("quest_progress", progress);
                    teamObj.put("quest_goal", goal);
                    teamObj.put("quest_desc", desc);
                    
                    List<Map<String, String>> members = new ArrayList<>();
                    try (PreparedStatement mStmt = conn.prepareStatement(
                        "SELECT m.player_uuid, COALESCE(p.username, 'Unknown') as name " +
                        "FROM genscore_team_members m LEFT JOIN player_profiles p ON m.player_uuid = p.uuid " +
                        "WHERE m.team_id = ?")) {
                        mStmt.setInt(1, teamId);
                        try (ResultSet mrs = mStmt.executeQuery()) {
                            while (mrs.next()) {
                                Map<String, String> m = new HashMap<>();
                                m.put("uuid", mrs.getString("player_uuid"));
                                m.put("name", mrs.getString("name"));
                                members.add(m);
                            }
                        }
                    }
                    teamObj.put("members", members);
                    teamsList.add(teamObj);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return teamsList;
    }

    // --- Team Quests ---
    
    public void clearTeamQuests() {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM genscore_team_quests")) {
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public Map<Integer, Integer> loadTeamQuestProgress(String activeQuestId) {
        Map<Integer, Integer> progressMap = new HashMap<>();
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT team_id, progress FROM genscore_team_quests WHERE quest_id = ?")) {
            stmt.setString(1, activeQuestId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    progressMap.put(rs.getInt("team_id"), rs.getInt("progress"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return progressMap;
    }
    
    public void saveTeamQuestProgress(int teamId, String activeQuestId, int progress) {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO genscore_team_quests (team_id, quest_id, progress) VALUES (?, ?, ?) " +
                     "ON CONFLICT(team_id) DO UPDATE SET progress = ?, quest_id = ?")) {
            stmt.setInt(1, teamId);
            stmt.setString(2, activeQuestId);
            stmt.setInt(3, progress);
            stmt.setInt(4, progress);
            stmt.setString(5, activeQuestId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public void saveTeamStats(int teamId, int weeklyPoints, int totalPoints) {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement("UPDATE genscore_team_stats SET weekly_points = ?, total_points = ? WHERE team_id = ?")) {
            stmt.setInt(1, weeklyPoints);
            stmt.setInt(2, totalPoints);
            stmt.setInt(3, teamId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

