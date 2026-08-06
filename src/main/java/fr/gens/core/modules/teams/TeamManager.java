package fr.gens.core.modules.teams;

import fr.gens.core.CorePlugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TeamManager {
    private final CorePlugin plugin;
    private final Map<Integer, TeamData> teamsById = new HashMap<>();
    private final Map<UUID, TeamData> teamsByPlayer = new HashMap<>();

    public TeamManager(CorePlugin plugin) {
        this.plugin = plugin;
        loadTeams();
    }

    private void loadTeams() {
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
            plugin.getLogger().info("Chargé " + teamsById.size() + " équipes en mémoire.");
        } catch (SQLException e) {
            plugin.getLogger().severe("Erreur lors du chargement des équipes !");
            e.printStackTrace();
        }
    }

    public TeamData getTeam(int id) {
        return teamsById.get(id);
    }

    public TeamData getPlayerTeam(UUID playerUuid) {
        return teamsByPlayer.get(playerUuid);
    }

    public TeamData createTeam(String name, UUID leader) {
        for (TeamData t : teamsById.values()) {
            if (t.getName().equalsIgnoreCase(name)) {
                return null; // Name taken
            }
        }
        if (getPlayerTeam(leader) != null) return null; // Already in a team

        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            String sql = "INSERT INTO genscore_teams (name, leader_uuid) VALUES (?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, name);
                stmt.setString(2, leader.toString());
                stmt.executeUpdate();

                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int id = generatedKeys.getInt(1);
                        TeamData team = new TeamData(id, name, leader);
                        teamsById.put(id, team);
                        addMemberToDatabase(id, leader);
                        team.addMember(leader);
                        teamsByPlayer.put(leader, team);
                        
                        // Initialize stats row
                        try (PreparedStatement statStmt = conn.prepareStatement("INSERT INTO genscore_team_stats (team_id, weekly_points, total_points) VALUES (?, 0, 0)")) {
                            statStmt.setInt(1, id);
                            statStmt.executeUpdate();
                        }
                        return team;
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void addMember(TeamData team, UUID newMember) {
        if (getPlayerTeam(newMember) != null) return;
        addMemberToDatabase(team.getTeamId(), newMember);
        team.addMember(newMember);
        teamsByPlayer.put(newMember, team);
    }

    public void removeMember(TeamData team, UUID member) {
        removeMemberFromDatabase(member);
        team.removeMember(member);
        teamsByPlayer.remove(member);
        
        if (team.getMembers().isEmpty() || member.equals(team.getLeaderUuid())) {
            disbandTeam(team);
        }
    }

    public void disbandTeam(TeamData team) {
        for (UUID uuid : team.getMembers()) {
            teamsByPlayer.remove(uuid);
            removeMemberFromDatabase(uuid);
        }
        teamsById.remove(team.getTeamId());
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM genscore_teams WHERE team_id = ?")) {
            stmt.setInt(1, team.getTeamId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void addMemberToDatabase(int teamId, UUID member) {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement("INSERT INTO genscore_team_members (team_id, player_uuid) VALUES (?, ?)")) {
            stmt.setInt(1, teamId);
            stmt.setString(2, member.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void removeMemberFromDatabase(UUID member) {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM genscore_team_members WHERE player_uuid = ?")) {
            stmt.setString(1, member.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
