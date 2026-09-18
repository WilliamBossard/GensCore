package fr.gens.core.modules.teams;

import fr.gens.core.CorePlugin;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TeamClaimManager {

    private final CorePlugin plugin;
    // Cle: "world:x:z" -> teamId
    private final Map<String, Integer> claims = new ConcurrentHashMap<>();

    public static final double CLAIM_COST_MONEY = 1500.0;
    public static final int CLAIM_COST_XP = 10;

    public TeamClaimManager(CorePlugin plugin) {
        this.plugin = plugin;
    }

    public static String getChunkKey(String world, int x, int z) {
        return world + ":" + x + ":" + z;
    }

    public static String getChunkKey(Chunk chunk) {
        return getChunkKey(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
    }

    public Map<String, Integer> getClaimsMap() {
        return claims;
    }

    public boolean isChunkClaimed(String world, int x, int z) {
        return claims.containsKey(getChunkKey(world, x, z));
    }

    public boolean isChunkClaimed(Chunk chunk) {
        return claims.containsKey(getChunkKey(chunk));
    }

    public Integer getTeamIdAt(String world, int x, int z) {
        return claims.get(getChunkKey(world, x, z));
    }

    public Integer getTeamIdAt(Chunk chunk) {
        return claims.get(getChunkKey(chunk));
    }

    public TeamData getTeamAt(Chunk chunk) {
        Integer teamId = getTeamIdAt(chunk);
        if (teamId == null) return null;
        return plugin.getTeamManager().getTeam(teamId);
    }

    public int getClaimsCount(int teamId) {
        int count = 0;
        for (Integer id : claims.values()) {
            if (id != null && id == teamId) {
                count++;
            }
        }
        return count;
    }

    public List<ClaimPos> getTeamClaims(int teamId) {
        List<ClaimPos> list = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : claims.entrySet()) {
            if (entry.getValue() != null && entry.getValue() == teamId) {
                String[] split = entry.getKey().split(":");
                if (split.length == 3) {
                    list.add(new ClaimPos(split[0], Integer.parseInt(split[1]), Integer.parseInt(split[2])));
                }
            }
        }
        return list;
    }

    public enum ClaimResult {
        SUCCESS,
        ALREADY_CLAIMED_BY_SELF,
        ALREADY_CLAIMED_BY_OTHER,
        LIMIT_REACHED,
        NOT_ENOUGH_MONEY,
        NOT_ENOUGH_XP,
        NOT_LEADER,
        DATABASE_ERROR
    }

    public enum UnclaimResult {
        SUCCESS,
        NOT_CLAIMED,
        NOT_YOUR_CLAIM,
        NOT_LEADER,
        DATABASE_ERROR
    }

    public ClaimResult claimChunk(Player player, TeamData team, Chunk chunk) {
        if (!team.getLeaderUuid().equals(player.getUniqueId())) {
            return ClaimResult.NOT_LEADER;
        }

        String key = getChunkKey(chunk);
        Integer existingTeamId = claims.get(key);
        if (existingTeamId != null) {
            if (existingTeamId == team.getTeamId()) {
                return ClaimResult.ALREADY_CLAIMED_BY_SELF;
            } else {
                return ClaimResult.ALREADY_CLAIMED_BY_OTHER;
            }
        }

        int currentClaims = getClaimsCount(team.getTeamId());
        int maxAllowed = team.getMaxClaims();
        if (currentClaims >= maxAllowed) {
            return ClaimResult.LIMIT_REACHED;
        }

        fr.gens.core.modules.EconomyModule eco = (fr.gens.core.modules.EconomyModule) plugin.getModuleManager().getModule("economy");
        boolean isEcoEnabled = (eco != null && eco.isEnabled());

        // Verifier et prelever les fonds de la BANQUE DE GUILDE
        if (isEcoEnabled) {
            if (team.getBankBalance() < CLAIM_COST_MONEY) {
                return ClaimResult.NOT_ENOUGH_MONEY;
            }
            if (!team.withdrawBankBalance(CLAIM_COST_MONEY)) {
                return ClaimResult.NOT_ENOUGH_MONEY;
            }
        } else {
            if (team.getBankXp() < CLAIM_COST_XP) {
                return ClaimResult.NOT_ENOUGH_XP;
            }
            if (!team.withdrawBankXp(CLAIM_COST_XP)) {
                return ClaimResult.NOT_ENOUGH_XP;
            }
        }

        // Sauvegarde BDD
        fr.gens.core.modules.teams.TeamModule module = (fr.gens.core.modules.teams.TeamModule) plugin.getModuleManager().getModule("teams");
        if (module != null) {
            boolean dbSuccess = module.getTeamDAO().addClaim(team.getTeamId(), chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
            if (!dbSuccess) {
                // Rembourser la banque si erreur SQL
                if (isEcoEnabled) {
                    team.addBankBalance(CLAIM_COST_MONEY);
                } else {
                    team.addBankXp(CLAIM_COST_XP);
                }
                return ClaimResult.DATABASE_ERROR;
            }
            module.getTeamDAO().saveTeamBank(team);
        }

        claims.put(key, team.getTeamId());
        return ClaimResult.SUCCESS;
    }

    public UnclaimResult unclaimChunk(Player player, TeamData team, Chunk chunk) {
        if (!team.getLeaderUuid().equals(player.getUniqueId())) {
            return UnclaimResult.NOT_LEADER;
        }

        String key = getChunkKey(chunk);
        Integer existingTeamId = claims.get(key);
        if (existingTeamId == null) {
            return UnclaimResult.NOT_CLAIMED;
        }

        if (existingTeamId != team.getTeamId()) {
            return UnclaimResult.NOT_YOUR_CLAIM;
        }

        fr.gens.core.modules.teams.TeamModule module = (fr.gens.core.modules.teams.TeamModule) plugin.getModuleManager().getModule("teams");
        if (module != null) {
            boolean dbSuccess = module.getTeamDAO().removeClaim(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
            if (!dbSuccess) {
                return UnclaimResult.DATABASE_ERROR;
            }
        }

        claims.remove(key);
        return UnclaimResult.SUCCESS;
    }

    public void removeAllTeamClaims(int teamId) {
        claims.entrySet().removeIf(entry -> entry.getValue() != null && entry.getValue() == teamId);
    }

    public void loadClaims(Map<String, Integer> loadedClaims) {
        claims.clear();
        claims.putAll(loadedClaims);
    }

    public static class ClaimPos {
        public final String world;
        public final int x;
        public final int z;

        public ClaimPos(String world, int x, int z) {
            this.world = world;
            this.x = x;
            this.z = z;
        }
    }
}
