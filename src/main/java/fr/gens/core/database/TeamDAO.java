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

            // Migration des colonnes Banque et Couleur de Guilde
            plugin.getDatabaseManager().addColumnIfNotExists("genscore_teams", "bank_balance", "DOUBLE DEFAULT 0.0");
            plugin.getDatabaseManager().addColumnIfNotExists("genscore_teams", "bank_xp", "INTEGER DEFAULT 0");
            plugin.getDatabaseManager().addColumnIfNotExists("genscore_teams", "color", "VARCHAR(7) DEFAULT '#2ecc71'");
            plugin.getDatabaseManager().addColumnIfNotExists("genscore_team_members", "role", "VARCHAR(16) DEFAULT 'MEMBER'");

            // Table des claims territoriaux de guilde
            stmt.execute("CREATE TABLE IF NOT EXISTS genscore_team_claims (" +
                    "team_id INTEGER, " +
                    "world VARCHAR(64), " +
                    "chunk_x INT, " +
                    "chunk_z INT, " +
                    "PRIMARY KEY(world, chunk_x, chunk_z), " +
                    "FOREIGN KEY(team_id) REFERENCES genscore_teams(team_id) ON DELETE CASCADE" +
                    ");");

            // Table des ameliorations de guilde
            stmt.execute("CREATE TABLE IF NOT EXISTS genscore_team_upgrades (" +
                    "team_id INTEGER, " +
                    "perk_id VARCHAR(32), " +
                    "level INTEGER DEFAULT 0, " +
                    "PRIMARY KEY(team_id, perk_id), " +
                    "FOREIGN KEY(team_id) REFERENCES genscore_teams(team_id) ON DELETE CASCADE" +
                    ");");

            // Table du home de guilde
            stmt.execute("CREATE TABLE IF NOT EXISTS genscore_team_home (" +
                    "team_id INTEGER PRIMARY KEY, " +
                    "world VARCHAR(64), " +
                    "x DOUBLE, y DOUBLE, z DOUBLE, " +
                    "yaw FLOAT DEFAULT 0, pitch FLOAT DEFAULT 0, " +
                    "FOREIGN KEY(team_id) REFERENCES genscore_teams(team_id) ON DELETE CASCADE" +
                    ");");

            // Table du coffre de guilde
            stmt.execute("CREATE TABLE IF NOT EXISTS genscore_team_vault (" +
                    "team_id INTEGER, " +
                    "slot INTEGER, " +
                    "item_data TEXT, " +
                    "PRIMARY KEY(team_id, slot), " +
                    "FOREIGN KEY(team_id) REFERENCES genscore_teams(team_id) ON DELETE CASCADE" +
                    ");");

            // Colonne de migration: timestamp du dernier interet bancaire
            plugin.getDatabaseManager().addColumnIfNotExists("genscore_teams", "last_interest_at", "BIGINT DEFAULT 0");

        } catch (SQLException e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Erreur lors de la création des tables des teams", e);
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
                                
                                plugin.getFoliaLib().getScheduler().runAtEntity(player, (t2) -> {
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
                        try {
                            team.setBankBalance(rs.getDouble("bank_balance"));
                            team.setBankXp(rs.getInt("bank_xp"));
                            String color = rs.getString("color");
                            if (color != null && !color.isEmpty()) team.setColor(color);
                            team.setLastInterestAt(rs.getLong("last_interest_at"));
                        } catch (Exception ignored) {}
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
                            try {
                                String role = rs.getString("role");
                                if ("ADMIN".equalsIgnoreCase(role)) {
                                    team.promoteAdmin(memberUuid);
                                }
                            } catch (Exception ignored) {}
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

            // Load upgrades
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM genscore_team_upgrades")) {
                while (rs.next()) {
                    int teamId = rs.getInt("team_id");
                    String perkId = rs.getString("perk_id");
                    int level = rs.getInt("level");
                    TeamData team = teamsById.get(teamId);
                    if (team != null && perkId != null) {
                        team.setUpgradeLevel(perkId, level);
                    }
                }
            }

            // Load home and vault for each team
            for (TeamData team : teamsById.values()) {
                loadHome(team);
                loadVault(team);
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

    public void updateMemberRole(int teamId, UUID member, String role) {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement("UPDATE genscore_team_members SET role = ? WHERE team_id = ? AND player_uuid = ?")) {
            stmt.setString(1, role);
            stmt.setInt(2, teamId);
            stmt.setString(3, member.toString());
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
                    String desc = questManager != null ? questManager.getDesc() : "Quête non définie";
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

    public void loadClaims(Map<String, Integer> claimsMap) {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM genscore_team_claims")) {
            while (rs.next()) {
                int teamId = rs.getInt("team_id");
                String world = rs.getString("world");
                int x = rs.getInt("chunk_x");
                int z = rs.getInt("chunk_z");
                claimsMap.put(fr.gens.core.modules.teams.TeamClaimManager.getChunkKey(world, x, z), teamId);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean addClaim(int teamId, String world, int chunkX, int chunkZ) {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO genscore_team_claims (team_id, world, chunk_x, chunk_z) VALUES (?, ?, ?, ?) " +
                     "ON CONFLICT(world, chunk_x, chunk_z) DO UPDATE SET team_id = ?")) {
            stmt.setInt(1, teamId);
            stmt.setString(2, world);
            stmt.setInt(3, chunkX);
            stmt.setInt(4, chunkZ);
            stmt.setInt(5, teamId);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean removeClaim(String world, int chunkX, int chunkZ) {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM genscore_team_claims WHERE world = ? AND chunk_x = ? AND chunk_z = ?")) {
            stmt.setString(1, world);
            stmt.setInt(2, chunkX);
            stmt.setInt(3, chunkZ);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void removeAllTeamClaims(int teamId) {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM genscore_team_claims WHERE team_id = ?")) {
            stmt.setInt(1, teamId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void saveTeamBank(TeamData team) {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement("UPDATE genscore_teams SET bank_balance = ?, bank_xp = ? WHERE team_id = ?")) {
            stmt.setDouble(1, team.getBankBalance());
            stmt.setInt(2, team.getBankXp());
            stmt.setInt(3, team.getTeamId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void saveTeamColor(TeamData team) {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement("UPDATE genscore_teams SET color = ? WHERE team_id = ?")) {
            stmt.setString(1, team.getColor());
            stmt.setInt(2, team.getTeamId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void saveUpgrade(int teamId, String perkId, int level) {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO genscore_team_upgrades (team_id, perk_id, level) VALUES (?, ?, ?) " +
                     "ON CONFLICT(team_id, perk_id) DO UPDATE SET level = ?")) {
            stmt.setInt(1, teamId);
            stmt.setString(2, perkId.toUpperCase());
            stmt.setInt(3, level);
            stmt.setInt(4, level);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    // ---- GUILD HOME ----
    public void saveHome(TeamData team) {
        org.bukkit.Location loc = team.getHomeLocation();
        if (loc == null || loc.getWorld() == null) {
            // Delete home if null
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                         "DELETE FROM genscore_team_home WHERE team_id = ?")) {
                stmt.setInt(1, team.getTeamId());
                stmt.executeUpdate();
            } catch (SQLException e) { e.printStackTrace(); }
            return;
        }
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO genscore_team_home (team_id, world, x, y, z, yaw, pitch) VALUES (?,?,?,?,?,?,?) " +
                     "ON CONFLICT(team_id) DO UPDATE SET world=excluded.world, x=excluded.x, y=excluded.y, z=excluded.z, yaw=excluded.yaw, pitch=excluded.pitch")) {
            stmt.setInt(1, team.getTeamId());
            stmt.setString(2, loc.getWorld().getName());
            stmt.setDouble(3, loc.getX());
            stmt.setDouble(4, loc.getY());
            stmt.setDouble(5, loc.getZ());
            stmt.setFloat(6, loc.getYaw());
            stmt.setFloat(7, loc.getPitch());
            stmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void loadHome(TeamData team) {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT world, x, y, z, yaw, pitch FROM genscore_team_home WHERE team_id = ?")) {
            stmt.setInt(1, team.getTeamId());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String worldName = rs.getString("world");
                org.bukkit.World world = org.bukkit.Bukkit.getWorld(worldName);
                if (world != null) {
                    org.bukkit.Location loc = new org.bukkit.Location(
                            world,
                            rs.getDouble("x"),
                            rs.getDouble("y"),
                            rs.getDouble("z"),
                            rs.getFloat("yaw"),
                            rs.getFloat("pitch"));
                    team.setHomeLocation(loc);
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // ---- GUILD VAULT ----
    public void saveVault(TeamData team) {
        int vaultSize = team.getVaultSize();
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            // Clear all slots first
            try (PreparedStatement del = conn.prepareStatement(
                    "DELETE FROM genscore_team_vault WHERE team_id = ?")) {
                del.setInt(1, team.getTeamId());
                del.executeUpdate();
            }
            // Insert all occupied slots
            try (PreparedStatement ins = conn.prepareStatement(
                    "INSERT INTO genscore_team_vault (team_id, slot, item_data) VALUES (?, ?, ?)")) {
                for (java.util.Map.Entry<Integer, org.bukkit.inventory.ItemStack> entry : team.getVaultContents().entrySet()) {
                    int slot = entry.getKey();
                    org.bukkit.inventory.ItemStack item = entry.getValue();
                    if (slot < 0 || slot >= vaultSize || item == null || item.getType() == org.bukkit.Material.AIR) continue;
                    try {
                        byte[] data = item.serializeAsBytes();
                        String base64 = java.util.Base64.getEncoder().encodeToString(data);
                        ins.setInt(1, team.getTeamId());
                        ins.setInt(2, slot);
                        ins.setString(3, base64);
                        ins.addBatch();
                    } catch (Exception ex) { ex.printStackTrace(); }
                }
                ins.executeBatch();
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void loadVault(TeamData team) {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT slot, item_data FROM genscore_team_vault WHERE team_id = ?")) {
            stmt.setInt(1, team.getTeamId());
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                int slot = rs.getInt("slot");
                String base64 = rs.getString("item_data");
                if (base64 == null || base64.isEmpty()) continue;
                try {
                    byte[] data = java.util.Base64.getDecoder().decode(base64);
                    org.bukkit.inventory.ItemStack item = org.bukkit.inventory.ItemStack.deserializeBytes(data);
                    team.setVaultItem(slot, item);
                } catch (Exception ex) { ex.printStackTrace(); }
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // ---- WEB FALLBACK: charge la guilde d'un joueur depuis la BDD (si absent de la RAM) ----
    /**
     * Charge la TeamData complète depuis la BDD pour un UUID de joueur donné.
     * Utilisé par l'API web quand le joueur n'est pas chargé en mémoire RAM.
     * @return TeamData fully loaded, or null if the player has no team.
     */
    public TeamData getTeamByPlayerUuid(UUID playerUuid) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            // 1. Find team_id via members table
            int teamId = -1;
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT team_id FROM genscore_team_members WHERE player_uuid = ?")) {
                stmt.setString(1, playerUuid.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        teamId = rs.getInt("team_id");
                    }
                }
            }
            if (teamId == -1) {
                try (PreparedStatement stmt = conn.prepareStatement(
                        "SELECT team_id FROM genscore_teams WHERE leader_uuid = ?")) {
                    stmt.setString(1, playerUuid.toString());
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            teamId = rs.getInt("team_id");
                        }
                    }
                }
            }
            if (teamId == -1) return null;

            // 2. Load team base data
            TeamData team = null;
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT * FROM genscore_teams WHERE team_id = ?")) {
                stmt.setInt(1, teamId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        String leaderStr = rs.getString("leader_uuid");
                        if (leaderStr == null) return null;
                        team = new TeamData(teamId, rs.getString("name"), UUID.fromString(leaderStr));
                        try {
                            team.setBankBalance(rs.getDouble("bank_balance"));
                            team.setBankXp(rs.getInt("bank_xp"));
                            String color = rs.getString("color");
                            if (color != null && !color.isEmpty()) team.setColor(color);
                            team.setLastInterestAt(rs.getLong("last_interest_at"));
                        } catch (Exception ignored) {}
                    }
                }
            }
            if (team == null) return null;

            // 3. Load all members
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT player_uuid, role FROM genscore_team_members WHERE team_id = ?")) {
                stmt.setInt(1, teamId);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String uuidStr = rs.getString("player_uuid");
                        if (uuidStr == null) continue;
                        UUID mUuid = UUID.fromString(uuidStr);
                        team.addMember(mUuid);
                        try {
                            String role = rs.getString("role");
                            if ("ADMIN".equalsIgnoreCase(role)) team.promoteAdmin(mUuid);
                        } catch (Exception ignored) {}
                    }
                }
            }

            // 4. Load upgrades
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT perk_id, level FROM genscore_team_upgrades WHERE team_id = ?")) {
                stmt.setInt(1, teamId);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String perkId = rs.getString("perk_id");
                        int level = rs.getInt("level");
                        if (perkId != null) team.setUpgradeLevel(perkId, level);
                    }
                }
            }

            // 5. Load stats
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT weekly_points, total_points FROM genscore_team_stats WHERE team_id = ?")) {
                stmt.setInt(1, teamId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        team.setWeeklyPoints(rs.getInt("weekly_points"));
                        team.setTotalPoints(rs.getInt("total_points"));
                    }
                }
            }

            return team;
        } catch (Exception e) {
            plugin.getLogger().warning("[TeamDAO] getTeamByPlayerUuid error for " + playerUuid + ": " + e.getMessage());
            return null;
        }
    }

    // ---- BANK INTEREST TIMESTAMP ----
    public void saveInterestTimestamp(TeamData team) {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE genscore_teams SET last_interest_at = ? WHERE team_id = ?")) {
            stmt.setLong(1, team.getLastInterestAt());
            stmt.setInt(2, team.getTeamId());
            stmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }
}




