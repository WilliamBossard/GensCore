package fr.gens.core.modules.teams;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TeamData {
    private int teamId;
    private String name;
    private UUID leaderUuid;
    private List<UUID> members;
    private boolean autoLock; // Setting for team chests
    private int weeklyPoints;
    private int totalPoints;

    public TeamData(int teamId, String name, UUID leaderUuid) {
        this.teamId = teamId;
        this.name = name;
        this.leaderUuid = leaderUuid;
        this.members = new ArrayList<>();
        this.members.add(leaderUuid);
        this.autoLock = true;
        this.weeklyPoints = 0;
        this.totalPoints = 0;
    }

    public int getTeamId() { return teamId; }
    public String getName() { return name; }
    public UUID getLeaderUuid() { return leaderUuid; }
    
    public void setLeaderUuid(UUID leader) { this.leaderUuid = leader; }

    public List<UUID> getMembers() { return members; }
    
    public void addMember(UUID uuid) {
        if (!members.contains(uuid)) {
            members.add(uuid);
        }
    }
    public void removeMember(UUID uuid) { members.remove(uuid); }
    
    public boolean hasMember(UUID uuid) { return members.contains(uuid); }

    public boolean isAutoLock() { return autoLock; }
    public void setAutoLock(boolean autoLock) { this.autoLock = autoLock; }
    
    public int getWeeklyPoints() {
        return weeklyPoints;
    }

    public void setWeeklyPoints(int weeklyPoints) {
        this.weeklyPoints = weeklyPoints;
    }

    public int getTotalPoints() {
        return totalPoints;
    }

    public void setTotalPoints(int totalPoints) {
        this.totalPoints = totalPoints;
    }

    public void addPoints(int points) {
        this.weeklyPoints += points;
        this.totalPoints += points;
    }
    
    public void broadcast(String message) {
        for (UUID uuid : members) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                p.sendMessage("<dark_gray>[<aqua>Team " + name + "<dark_gray>] <gray>" + message);
            }
        }
    }
}
