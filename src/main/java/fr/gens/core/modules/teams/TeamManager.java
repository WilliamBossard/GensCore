package fr.gens.core.modules.teams;

import fr.gens.core.CorePlugin;

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
        plugin.getDatabaseManager().getTeamDAO().loadTeams(teamsById, teamsByPlayer);
        plugin.getLogger().info("Loaded " + teamsById.size() + " teams in memory.");
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

        int id = plugin.getDatabaseManager().getTeamDAO().createTeam(name, leader);
        if (id != -1) {
            TeamData team = new TeamData(id, name, leader);
            teamsById.put(id, team);
            addMemberToDatabase(id, leader);
            team.addMember(leader);
            teamsByPlayer.put(leader, team);
            
            // Initialize stats row
            plugin.getDatabaseManager().getTeamDAO().initTeamStats(id);
            return team;
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
        plugin.getDatabaseManager().getTeamDAO().disbandTeam(team.getTeamId());
    }

    private void addMemberToDatabase(int teamId, UUID member) {
        plugin.getDatabaseManager().getTeamDAO().addMember(teamId, member);
    }

    private void removeMemberFromDatabase(UUID member) {
        plugin.getDatabaseManager().getTeamDAO().removeMember(member);
    }

    // --- WEB STATS EXTENSIONS ---

    public java.util.Map<String, Object> getBestTeamStats() {
        return plugin.getDatabaseManager().getTeamDAO().getBestTeamStats();
    }

    public java.util.List<java.util.Map<String, Object>> getAllTeamStats() {
        return plugin.getDatabaseManager().getTeamDAO().getAllTeamStats(plugin.getTeamQuestManager());
    }
}
