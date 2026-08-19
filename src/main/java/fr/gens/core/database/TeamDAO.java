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
}
