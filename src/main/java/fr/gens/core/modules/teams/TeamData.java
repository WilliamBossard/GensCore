package fr.gens.core.modules.teams;

import fr.gens.core.utils.PlaceholderUtils;
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
    private double bankBalance;
    private int bankXp;
    private String color;
    private final java.util.Map<String, Integer> upgrades;
    private final java.util.Set<UUID> admins;

    public TeamData(int teamId, String name, UUID leaderUuid) {
        this.teamId = teamId;
        this.name = name;
        this.leaderUuid = leaderUuid;
        this.members = new ArrayList<>();
        this.members.add(leaderUuid);
        this.admins = java.util.concurrent.ConcurrentHashMap.newKeySet();
        this.autoLock = true;
        this.weeklyPoints = 0;
        this.totalPoints = 0;
        this.bankBalance = 0.0;
        this.bankXp = 0;
        this.color = "#2ecc71";
        this.upgrades = new java.util.concurrent.ConcurrentHashMap<>();
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
    public void removeMember(UUID uuid) {
        members.remove(uuid);
        admins.remove(uuid);
    }
    
    public boolean hasMember(UUID uuid) { return members.contains(uuid); }

    public boolean isLeader(UUID uuid) {
        return uuid != null && leaderUuid != null && leaderUuid.equals(uuid);
    }

    public boolean isAdmin(UUID uuid) {
        return uuid != null && admins.contains(uuid);
    }

    public boolean isAdminOrLeader(UUID uuid) {
        return isLeader(uuid) || isAdmin(uuid);
    }

    public boolean canManageMembers(UUID requester, UUID target) {
        if (requester == null || target == null) return false;
        if (requester.equals(target)) return false;
        if (isLeader(target)) return false;
        if (isLeader(requester)) return true;
        if (isAdmin(requester)) {
            return !isAdmin(target);
        }
        return false;
    }

    public void promoteAdmin(UUID uuid) {
        if (uuid != null && hasMember(uuid) && !isLeader(uuid)) {
            admins.add(uuid);
        }
    }

    public void demoteAdmin(UUID uuid) {
        if (uuid != null) {
            admins.remove(uuid);
        }
    }

    public java.util.Set<UUID> getAdmins() {
        return java.util.Collections.unmodifiableSet(admins);
    }

    public String getRoleName(UUID uuid) {
        if (isLeader(uuid)) return "LEADER";
        if (isAdmin(uuid)) return "ADMIN";
        return "MEMBER";
    }

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
    
    public double getBankBalance() {
        return bankBalance;
    }

    public void setBankBalance(double bankBalance) {
        if (Double.isFinite(bankBalance)) {
            this.bankBalance = Math.max(0.0, bankBalance);
        }
    }

    public synchronized void addBankBalance(double amount) {
        if (Double.isFinite(amount) && amount > 0) {
            this.bankBalance += amount;
        }
    }

    public synchronized boolean withdrawBankBalance(double amount) {
        if (Double.isFinite(amount) && amount > 0 && this.bankBalance >= amount) {
            this.bankBalance -= amount;
            return true;
        }
        return false;
    }

    public int getBankXp() {
        return bankXp;
    }

    public void setBankXp(int bankXp) {
        this.bankXp = Math.max(0, bankXp);
    }

    public synchronized void addBankXp(int xp) {
        if (xp > 0) {
            this.bankXp += xp;
        }
    }

    public synchronized boolean withdrawBankXp(int xp) {
        if (xp > 0 && this.bankXp >= xp) {
            this.bankXp -= xp;
            return true;
        }
        return false;
    }

    public String getColor() {
        return color != null ? color : "#2ecc71";
    }

    public void setColor(String color) {
        if (color != null && color.matches("^#([A-Fa-f0-9]{6})$")) {
            this.color = color;
        }
    }

    public java.util.Map<String, Integer> getUpgrades() {
        return upgrades;
    }

    public int getUpgradeLevel(String perkId) {
        return upgrades.getOrDefault(perkId.toUpperCase(), 0);
    }

    public void setUpgradeLevel(String perkId, int level) {
        upgrades.put(perkId.toUpperCase(), Math.max(0, level));
    }

    public int getMaxMembers() {
        // Base : 5 membres. +3 par niveau de perk MEMBERS (max lvl 3 -> 16 membres)
        return 5 + (getUpgradeLevel("MEMBERS") * 3);
    }

    public int getMaxClaims() {
        // Base : 4 chunks. +4 par niveau de perk CLAIMS (max lvl 4 -> 20 chunks)
        return 4 + (getUpgradeLevel("CLAIMS") * 4);
    }

    public double getJobsXpMultiplier() {
        // +5% par niveau de perk JOBS
        return 1.0 + (getUpgradeLevel("JOBS") * 0.05);
    }

    public double getAhTaxReduction() {
        // -25% par niveau de perk AH_TAX (max lvl 2 -> 50% de réduction)
        return Math.min(0.50, getUpgradeLevel("AH_TAX") * 0.25);
    }

    public double getQuestPointsMultiplier() {
        // +10% par niveau de perk QUESTS
        return 1.0 + (getUpgradeLevel("QUESTS") * 0.10);
    }

    public void broadcast(String message) {
        for (UUID uuid : members) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                p.sendMessage(PlaceholderUtils.parseToComponent("<dark_gray>[<aqua>Team " + name + "<dark_gray>] <gray>" + message));
            }
        }
    }
}



