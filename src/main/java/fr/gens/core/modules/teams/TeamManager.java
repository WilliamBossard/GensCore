package fr.gens.core.modules.teams;

import fr.gens.core.CorePlugin;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.UUID;


public class TeamManager {
    private final CorePlugin plugin;
    private final Map<Integer, TeamData> teamsById = new ConcurrentHashMap<>();
    private final Map<UUID, TeamData> teamsByPlayer = new ConcurrentHashMap<>();
    private final java.util.Set<String> teamNames = ConcurrentHashMap.newKeySet();

    public TeamManager(CorePlugin plugin) {
        this.plugin = plugin;
        loadTeams();
    }

    private void loadTeams() {
        fr.gens.core.modules.teams.TeamModule module = (fr.gens.core.modules.teams.TeamModule) plugin.getModuleManager().getModule("teams");
        if (module != null) {
            module.getTeamDAO().loadTeams(teamsById, teamsByPlayer);
            for (TeamData t : teamsById.values()) {
                teamNames.add(t.getName().toLowerCase());
            }
        }
        plugin.getLogger().info("Loaded " + teamsById.size() + " teams in memory.");
    }

    public TeamData getTeam(int id) {
        return teamsById.get(id);
    }

    public TeamData getPlayerTeam(UUID playerUuid) {
        return teamsByPlayer.get(playerUuid);
    }

    public TeamData createTeam(String name, UUID leader) {
        if (teamNames.contains(name.toLowerCase())) {
            return null; // Name taken
        }
        if (getPlayerTeam(leader) != null) return null; // Already in a team

        fr.gens.core.modules.teams.TeamModule module = (fr.gens.core.modules.teams.TeamModule) plugin.getModuleManager().getModule("teams");
        int id = module != null ? module.getTeamDAO().createTeam(name, leader) : -1;
        if (id != -1) {
            TeamData team = new TeamData(id, name, leader);
            teamsById.put(id, team);
            teamNames.add(name.toLowerCase());
            addMemberToDatabase(id, leader);
            team.addMember(leader);
            teamsByPlayer.put(leader, team);
            
            // Initialize stats row
            if (module != null) module.getTeamDAO().initTeamStats(id);
            return team;
        }
        return null;
    }

    public void createTeamAsync(String name, UUID leader, java.util.function.Consumer<TeamData> callback) {
        if (teamNames.contains(name.toLowerCase()) || getPlayerTeam(leader) != null) {
            callback.accept(null);
            return;
        }

        fr.gens.core.modules.teams.TeamModule module = (fr.gens.core.modules.teams.TeamModule) plugin.getModuleManager().getModule("teams");
        plugin.getFoliaLib().getImpl().runAsync((wrappedTask) -> {
            int id = module != null ? module.getTeamDAO().createTeam(name, leader) : -1;
            
            plugin.getFoliaLib().getImpl().runNextTick((t2) -> {
                if (id != -1) {
                    TeamData team = new TeamData(id, name, leader);
                    teamsById.put(id, team);
                    teamNames.add(name.toLowerCase());
                    addMemberToDatabase(id, leader);
                    team.addMember(leader);
                    teamsByPlayer.put(leader, team);
                    
                    if (module != null) {
                        plugin.getFoliaLib().getImpl().runAsync((t3) -> module.getTeamDAO().initTeamStats(id));
                    }
                    callback.accept(team);
                } else {
                    callback.accept(null);
                }
            });
        });
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
        teamNames.remove(team.getName().toLowerCase());
        teamsById.remove(team.getTeamId());
        fr.gens.core.modules.teams.TeamModule module = (fr.gens.core.modules.teams.TeamModule) plugin.getModuleManager().getModule("teams");
        if (module != null) {
            plugin.getFoliaLib().getImpl().runAsync((wrappedTask) -> {
                module.getTeamDAO().disbandTeam(team.getTeamId());
            });
        }
    }

    private void addMemberToDatabase(int teamId, UUID member) {
        fr.gens.core.modules.teams.TeamModule module = (fr.gens.core.modules.teams.TeamModule) plugin.getModuleManager().getModule("teams");
        if (module != null) {
            plugin.getFoliaLib().getImpl().runAsync((wrappedTask) -> {
                module.getTeamDAO().addMember(teamId, member);
            });
        }
    }

    private void removeMemberFromDatabase(UUID member) {
        fr.gens.core.modules.teams.TeamModule module = (fr.gens.core.modules.teams.TeamModule) plugin.getModuleManager().getModule("teams");
        if (module != null) {
            plugin.getFoliaLib().getImpl().runAsync((wrappedTask) -> {
                module.getTeamDAO().removeMember(member);
            });
        }
    }

    // --- WEB STATS EXTENSIONS ---

    public java.util.Map<String, Object> getBestTeamStats() {
        fr.gens.core.modules.teams.TeamModule module = (fr.gens.core.modules.teams.TeamModule) plugin.getModuleManager().getModule("teams");
        return module != null ? module.getTeamDAO().getBestTeamStats() : null;
    }

    public java.util.List<java.util.Map<String, Object>> getAllTeamStats() {
        fr.gens.core.modules.teams.TeamModule module = (fr.gens.core.modules.teams.TeamModule) plugin.getModuleManager().getModule("teams");
        return module != null ? module.getTeamDAO().getAllTeamStats(plugin.getTeamQuestManager()) : java.util.Collections.emptyList();
    }
}

