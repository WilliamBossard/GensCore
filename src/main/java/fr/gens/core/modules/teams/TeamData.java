package fr.gens.core.modules.teams;

import fr.gens.core.utils.PlaceholderUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

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

    // Guild Home
    private Location homeLocation;
    // Guild Vault: slot -> ItemStack (transient, loaded from DB)
    private final java.util.Map<Integer, ItemStack> vaultContents = new java.util.concurrent.ConcurrentHashMap<>();
    // Bank Interest: timestamp of last interest payment
    private long lastInterestAt = 0L;

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

    // ---- GUILD_HOME ----
    public Location getHomeLocation() { return homeLocation; }
    public void setHomeLocation(Location loc) { this.homeLocation = loc; }

    /** Warmup in seconds before /team home teleports. Level 0 = not unlocked. */
    public int getGuildHomeWarmup() {
        int lvl = getUpgradeLevel("GUILD_HOME");
        if (lvl <= 0) return -1; // Not unlocked
        if (lvl == 1) return 5;
        if (lvl == 2) return 3;
        return 0; // Level 3: instant
    }

    /** Cooldown in seconds between /team home uses. */
    public int getGuildHomeCooldown() {
        int lvl = getUpgradeLevel("GUILD_HOME");
        if (lvl <= 0) return -1;
        if (lvl == 1) return 900; // 15 min
        if (lvl == 2) return 300; // 5 min
        return 60;               // Level 3: 1 min
    }

    // ---- TERRITORY_BUFF ----
    /** Returns the list of potion effects to apply inside claimed chunks, based on TERRITORY_BUFF level. */
    public List<PotionEffect> getTerritoryBuffEffects() {
        int lvl = getUpgradeLevel("TERRITORY_BUFF");
        List<PotionEffect> effects = new ArrayList<>();
        if (lvl <= 0) return effects;
        // Level 1: Regeneration I (infinite, ambient)
        effects.add(new PotionEffect(PotionEffectType.REGENERATION, Integer.MAX_VALUE, 0, true, false));
        effects.add(new PotionEffect(PotionEffectType.SATURATION, Integer.MAX_VALUE, 0, true, false));
        if (lvl >= 2) {
            // Level 2: + Speed I
            effects.add(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 0, true, false));
        }
        if (lvl >= 3) {
            // Level 3: + Haste I
            effects.add(new PotionEffect(PotionEffectType.HASTE, Integer.MAX_VALUE, 0, true, false));
        }
        return effects;
    }

    // ---- BANK_INTEREST ----
    /** Daily interest rate (e.g. 0.01 = 1%). Returns 0 if not unlocked. */
    public double getBankInterestRate() {
        int lvl = getUpgradeLevel("BANK_INTEREST");
        if (lvl == 1) return 0.01;
        if (lvl == 2) return 0.025;
        return 0.0;
    }

    /** Max interest earned per day in dollars. */
    public double getBankInterestCap() {
        int lvl = getUpgradeLevel("BANK_INTEREST");
        if (lvl == 1) return 10000.0;
        if (lvl == 2) return 25000.0;
        return 0.0;
    }

    public long getLastInterestAt() { return lastInterestAt; }
    public void setLastInterestAt(long ts) { this.lastInterestAt = ts; }

    // ---- SPAWNER_EFFICIENCY ----
    /** Returns multiplier to apply to spawner delay (< 1.0 = faster). 1.0 if not unlocked. */
    public double getSpawnerEfficiencyMultiplier() {
        int lvl = getUpgradeLevel("SPAWNER_EFFICIENCY");
        if (lvl == 1) return 0.85; // 15% faster
        if (lvl == 2) return 0.70; // 30% faster
        return 1.0;
    }

    // ---- GUILD_VAULT ----
    /** Returns vault size in slots (0 = not unlocked). */
    public int getVaultSize() {
        int lvl = getUpgradeLevel("GUILD_VAULT");
        if (lvl == 1) return 9;
        if (lvl == 2) return 27;
        if (lvl >= 3) return 54;
        return 0;
    }

    public java.util.Map<Integer, ItemStack> getVaultContents() { return vaultContents; }

    public void setVaultItem(int slot, ItemStack item) {
        if (item == null || item.getType() == org.bukkit.Material.AIR) {
            vaultContents.remove(slot);
        } else {
            vaultContents.put(slot, item);
        }
    }
}



