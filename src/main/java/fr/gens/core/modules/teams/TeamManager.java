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
    private final TeamClaimManager claimManager;

    public TeamManager(CorePlugin plugin) {
        this.plugin = plugin;
        this.claimManager = new TeamClaimManager(plugin);
        loadTeams();
    }

    private void loadTeams() {
        fr.gens.core.modules.teams.TeamModule module = (fr.gens.core.modules.teams.TeamModule) plugin.getModuleManager().getModule("teams");
        if (module != null) {
            module.getTeamDAO().loadTeams(teamsById, teamsByPlayer);
            for (TeamData t : teamsById.values()) {
                teamNames.add(t.getName().toLowerCase());
            }
            module.getTeamDAO().loadClaims(this.claimManager.getClaimsMap());
        }
        plugin.getLogger().info("Loaded " + teamsById.size() + " teams and " + claimManager.getClaimsMap().size() + " claims in memory.");
    }

    public TeamClaimManager getClaimManager() {
        return claimManager;
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
        plugin.getFoliaLib().getScheduler().runAsync((wrappedTask) -> {
            int id = module != null ? module.getTeamDAO().createTeam(name, leader) : -1;
            
            plugin.getFoliaLib().getScheduler().runNextTick((t2) -> {
                if (id != -1) {
                    TeamData team = new TeamData(id, name, leader);
                    teamsById.put(id, team);
                    teamNames.add(name.toLowerCase());
                    addMemberToDatabase(id, leader);
                    team.addMember(leader);
                    teamsByPlayer.put(leader, team);
                    
                    if (module != null) {
                        plugin.getFoliaLib().getScheduler().runAsync((t3) -> module.getTeamDAO().initTeamStats(id));
                    }
                    callback.accept(team);
                } else {
                    callback.accept(null);
                }
            });
        });
    }

    public boolean addMember(TeamData team, UUID newMember) {
        if (team == null || newMember == null) return false;
        if (getPlayerTeam(newMember) != null) return false;
        if (team.getMembers().size() >= team.getMaxMembers()) return false;
        addMemberToDatabase(team.getTeamId(), newMember);
        team.addMember(newMember);
        teamsByPlayer.put(newMember, team);
        return true;
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
        claimManager.removeAllTeamClaims(team.getTeamId());
        for (UUID uuid : team.getMembers()) {
            teamsByPlayer.remove(uuid);
            removeMemberFromDatabase(uuid);
        }
        teamNames.remove(team.getName().toLowerCase());
        teamsById.remove(team.getTeamId());
        fr.gens.core.modules.teams.TeamModule module = (fr.gens.core.modules.teams.TeamModule) plugin.getModuleManager().getModule("teams");
        if (module != null) {
            plugin.getFoliaLib().getScheduler().runAsync((wrappedTask) -> {
                module.getTeamDAO().removeAllTeamClaims(team.getTeamId());
                module.getTeamDAO().disbandTeam(team.getTeamId());
            });
        }
    }

    public void promoteAdmin(TeamData team, UUID member) {
        if (team == null || member == null) return;
        team.promoteAdmin(member);
        fr.gens.core.modules.teams.TeamModule module = (fr.gens.core.modules.teams.TeamModule) plugin.getModuleManager().getModule("teams");
        if (module != null) {
            plugin.getFoliaLib().getScheduler().runAsync((wrappedTask) -> {
                module.getTeamDAO().updateMemberRole(team.getTeamId(), member, "ADMIN");
            });
        }
    }

    public void demoteAdmin(TeamData team, UUID member) {
        if (team == null || member == null) return;
        team.demoteAdmin(member);
        fr.gens.core.modules.teams.TeamModule module = (fr.gens.core.modules.teams.TeamModule) plugin.getModuleManager().getModule("teams");
        if (module != null) {
            plugin.getFoliaLib().getScheduler().runAsync((wrappedTask) -> {
                module.getTeamDAO().updateMemberRole(team.getTeamId(), member, "MEMBER");
            });
        }
    }

    private void addMemberToDatabase(int teamId, UUID member) {
        fr.gens.core.modules.teams.TeamModule module = (fr.gens.core.modules.teams.TeamModule) plugin.getModuleManager().getModule("teams");
        if (module != null) {
            plugin.getFoliaLib().getScheduler().runAsync((wrappedTask) -> {
                module.getTeamDAO().addMember(teamId, member);
            });
        }
    }

    private void removeMemberFromDatabase(UUID member) {
        fr.gens.core.modules.teams.TeamModule module = (fr.gens.core.modules.teams.TeamModule) plugin.getModuleManager().getModule("teams");
        if (module != null) {
            plugin.getFoliaLib().getScheduler().runAsync((wrappedTask) -> {
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

    public boolean buyPerk(TeamData team, String perkId) {
        if (team == null || perkId == null) return false;
        perkId = perkId.toUpperCase();
        int currentLvl = team.getUpgradeLevel(perkId);

        fr.gens.core.modules.EconomyModule eco = (fr.gens.core.modules.EconomyModule) plugin.getModuleManager().getModule("economy");
        boolean isEcoEnabled = (eco != null && eco.isEnabled());

        double costMoney = getPerkCostMoney(perkId, currentLvl + 1);
        int costXp = getPerkCostXp(perkId, currentLvl + 1);

        if (costMoney < 0 || costXp < 0) return false; // Niveau max atteint

        if (isEcoEnabled) {
            if (team.getBankBalance() < costMoney) return false;
            if (!team.withdrawBankBalance(costMoney)) return false;
        } else {
            if (team.getBankXp() < costXp) return false;
            if (!team.withdrawBankXp(costXp)) return false;
        }

        team.setUpgradeLevel(perkId, currentLvl + 1);

        fr.gens.core.modules.teams.TeamModule module = (fr.gens.core.modules.teams.TeamModule) plugin.getModuleManager().getModule("teams");
        if (module != null) {
            final String pId = perkId;
            final int nextLvl = currentLvl + 1;
            plugin.getFoliaLib().getScheduler().runAsync(t -> {
                module.getTeamDAO().saveUpgrade(team.getTeamId(), pId, nextLvl);
                module.getTeamDAO().saveTeamBank(team);
            });
        }
        return true;
    }

    public static double getPerkCostMoney(String perkId, int targetLevel) {
        switch (perkId.toUpperCase()) {
            case "MEMBERS":
                if (targetLevel == 1) return 5000.0;
                if (targetLevel == 2) return 15000.0;
                if (targetLevel == 3) return 35000.0;
                return -1;
            case "CLAIMS":
                if (targetLevel == 1) return 4000.0;
                if (targetLevel == 2) return 10000.0;
                if (targetLevel == 3) return 20000.0;
                if (targetLevel == 4) return 40000.0;
                return -1;
            case "JOBS":
                if (targetLevel == 1) return 10000.0;
                if (targetLevel == 2) return 25000.0;
                if (targetLevel == 3) return 50000.0;
                return -1;
            case "AH_TAX":
                if (targetLevel == 1) return 8000.0;
                if (targetLevel == 2) return 20000.0;
                return -1;
            case "QUESTS":
                if (targetLevel == 1) return 12000.0;
                if (targetLevel == 2) return 30000.0;
                return -1;
            default:
                return -1;
        }
    }

    public static int getPerkCostXp(String perkId, int targetLevel) {
        switch (perkId.toUpperCase()) {
            case "MEMBERS":
                if (targetLevel == 1) return 25;
                if (targetLevel == 2) return 45;
                if (targetLevel == 3) return 70;
                return -1;
            case "CLAIMS":
                if (targetLevel == 1) return 20;
                if (targetLevel == 2) return 35;
                if (targetLevel == 3) return 55;
                if (targetLevel == 4) return 80;
                return -1;
            case "JOBS":
                if (targetLevel == 1) return 30;
                if (targetLevel == 2) return 50;
                if (targetLevel == 3) return 80;
                return -1;
            case "AH_TAX":
                if (targetLevel == 1) return 30;
                if (targetLevel == 2) return 55;
                return -1;
            case "QUESTS":
                if (targetLevel == 1) return 35;
                if (targetLevel == 2) return 60;
                return -1;
            default:
                return -1;
        }
    }

    public void saveBankAsync(TeamData team) {
        fr.gens.core.modules.teams.TeamModule module = (fr.gens.core.modules.teams.TeamModule) plugin.getModuleManager().getModule("teams");
        if (module != null) {
            plugin.getFoliaLib().getScheduler().runAsync(t -> module.getTeamDAO().saveTeamBank(team));
        }
    }

    public void saveColorAsync(TeamData team) {
        fr.gens.core.modules.teams.TeamModule module = (fr.gens.core.modules.teams.TeamModule) plugin.getModuleManager().getModule("teams");
        if (module != null) {
            plugin.getFoliaLib().getScheduler().runAsync(t -> module.getTeamDAO().saveTeamColor(team));
        }
    }
}




