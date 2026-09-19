package fr.gens.core.modules.perks;

import fr.gens.core.CorePlugin;
import fr.gens.core.database.QuestDAO;
import fr.gens.core.database.SoloPerkDAO;
import fr.gens.core.modules.EconomyModule;
import fr.gens.core.modules.quests.QuestModule;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SoloPerkManager {

    public enum UnlockResult {
        SUCCESS,
        ALREADY_UNLOCKED,
        NOT_ENOUGH_QUESTS,
        NOT_ENOUGH_FUNDS,
        NOT_ENOUGH_XP,
        ERROR
    }

    private final CorePlugin plugin;
    private final SoloPerkDAO perkDAO;
    private final Map<UUID, Set<String>> unlockedPerksCache = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, Boolean>> perkSettingsCache = new ConcurrentHashMap<>();

    public SoloPerkManager(CorePlugin plugin, SoloPerkDAO perkDAO) {
        this.plugin = plugin;
        this.perkDAO = perkDAO;
    }

    public void loadPlayer(UUID uuid) {
        plugin.getFoliaLib().getScheduler().runAsync(task -> {
            Set<String> unlocked = perkDAO.getUnlockedPerks(uuid);
            Map<String, Boolean> settings = perkDAO.getPerkSettings(uuid);
            unlockedPerksCache.put(uuid, Collections.synchronizedSet(unlocked));
            perkSettingsCache.put(uuid, new ConcurrentHashMap<>(settings));
        });
    }

    public void unloadPlayer(UUID uuid) {
        unlockedPerksCache.remove(uuid);
        perkSettingsCache.remove(uuid);
    }

    public Set<String> getUnlockedPerks(UUID uuid) {
        return unlockedPerksCache.computeIfAbsent(uuid, k -> Collections.synchronizedSet(perkDAO.getUnlockedPerks(uuid)));
    }

    public boolean hasPerk(UUID uuid, SoloPerkType perk) {
        if (perk == null) return false;
        return getUnlockedPerks(uuid).contains(perk.getId());
    }

    public boolean isPerkActive(UUID uuid, SoloPerkType perk) {
        if (!hasPerk(uuid, perk)) return false;
        if (!perk.isToggleable()) return true;

        Map<String, Boolean> settings = perkSettingsCache.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>(perkDAO.getPerkSettings(uuid)));
        return settings.getOrDefault(perk.getId(), true);
    }

    public int getQuestsCompletedTotal(UUID uuid) {
        QuestModule questMod = (QuestModule) plugin.getModuleManager().getModule("quests");
        if (questMod != null && questMod.getQuestDAO() != null) {
            return questMod.getQuestDAO().getQuestsCompletedTotal(uuid);
        }
        return new QuestDAO(plugin).getQuestsCompletedTotal(uuid);
    }

    public boolean isEconomyEnabled() {
        EconomyModule ecoMod = (EconomyModule) plugin.getModuleManager().getModule("economy");
        return ecoMod != null && ecoMod.isEnabled();
    }

    public UnlockResult unlockPerk(Player player, SoloPerkType perk) {
        if (player == null) return UnlockResult.ERROR;
        return unlockPerk(player.getUniqueId(), perk);
    }

    public UnlockResult unlockPerk(UUID uuid, SoloPerkType perk) {
        if (perk == null || uuid == null) return UnlockResult.ERROR;

        if (hasPerk(uuid, perk)) {
            return UnlockResult.ALREADY_UNLOCKED;
        }

        int completed = getQuestsCompletedTotal(uuid);
        if (completed < perk.getRequiredQuests()) {
            return UnlockResult.NOT_ENOUGH_QUESTS;
        }

        boolean eco = isEconomyEnabled();
        EconomyModule ecoMod = (EconomyModule) plugin.getModuleManager().getModule("economy");
        Player onlinePlayer = plugin.getServer().getPlayer(uuid);

        if (perk.isMajor()) {
            if (eco && ecoMod != null) {
                double cost = perk.getCostMoney();
                if (!ecoMod.takeMoneyAtomic(uuid, cost)) {
                    return UnlockResult.NOT_ENOUGH_FUNDS;
                }
            } else {
                int costXp = perk.getCostXp();
                if (onlinePlayer == null) {
                    return UnlockResult.ERROR; // Les achats en XP requierent d'etre connecte
                }
                if (onlinePlayer.getLevel() < costXp) {
                    return UnlockResult.NOT_ENOUGH_XP;
                }
                onlinePlayer.setLevel(onlinePlayer.getLevel() - costXp);
            }
        }

        // Enregistrement immédiat dans le cache
        Set<String> unlocked = getUnlockedPerks(uuid);
        unlocked.add(perk.getId());

        if (perk.isToggleable()) {
            Map<String, Boolean> settings = perkSettingsCache.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());
            settings.put(perk.getId(), true);
        }

        // Sauvegarde asynchrone
        final long now = System.currentTimeMillis();
        plugin.getFoliaLib().getScheduler().runAsync(task -> {
            perkDAO.saveUnlockedPerk(uuid, perk.getId(), now);
            if (perk.isToggleable()) {
                perkDAO.savePerkSetting(uuid, perk.getId(), true);
            }
        });

        return UnlockResult.SUCCESS;
    }

    public boolean togglePerk(UUID uuid, SoloPerkType perk) {
        if (perk == null || !perk.isToggleable() || !hasPerk(uuid, perk)) {
            return false;
        }

        Map<String, Boolean> settings = perkSettingsCache.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>(perkDAO.getPerkSettings(uuid)));
        boolean newState = !settings.getOrDefault(perk.getId(), true);
        settings.put(perk.getId(), newState);

        plugin.getFoliaLib().getScheduler().runAsync(task -> {
            perkDAO.savePerkSetting(uuid, perk.getId(), newState);
        });

        return newState;
    }

    public Map<String, Object> getPlayerDataForWeb(UUID uuid) {
        Map<String, Object> data = new HashMap<>();
        int completed = getQuestsCompletedTotal(uuid);
        boolean eco = isEconomyEnabled();
        EconomyModule ecoMod = (EconomyModule) plugin.getModuleManager().getModule("economy");
        double balance = (eco && ecoMod != null) ? ecoMod.getBalance(uuid) : 0.0;

        Player online = plugin.getServer().getPlayer(uuid);
        int playerXpLevels = online != null ? online.getLevel() : 0;

        data.put("questsCompleted", completed);
        data.put("isEconomyEnabled", eco);
        data.put("balance", balance);
        data.put("xpLevels", playerXpLevels);

        Set<String> unlocked = getUnlockedPerks(uuid);
        Map<String, Boolean> settings = perkSettingsCache.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>(perkDAO.getPerkSettings(uuid)));

        Map<String, Object> perksMap = new HashMap<>();
        for (SoloPerkType type : SoloPerkType.values()) {
            Map<String, Object> pData = new HashMap<>();
            boolean isUnlocked = unlocked.contains(type.getId());
            boolean isEnabled = isUnlocked && (!type.isToggleable() || settings.getOrDefault(type.getId(), true));

            boolean canClaim = !isUnlocked && completed >= type.getRequiredQuests();
            if (canClaim && type.isMajor()) {
                if (eco) {
                    canClaim = balance >= type.getCostMoney();
                } else {
                    canClaim = playerXpLevels >= type.getCostXp();
                }
            }

            pData.put("id", type.getId());
            pData.put("nameFr", type.getNameFr());
            pData.put("nameEn", type.getNameEn());
            pData.put("descriptionFr", type.getDescriptionFr());
            pData.put("descriptionEn", type.getDescriptionEn());
            pData.put("icon", type.getIcon().name());
            pData.put("requiredQuests", type.getRequiredQuests());
            pData.put("costMoney", type.getCostMoney());
            pData.put("costXp", type.getCostXp());
            pData.put("isMajor", type.isMajor());
            pData.put("isToggleable", type.isToggleable());
            pData.put("isUnlocked", isUnlocked);
            pData.put("isEnabled", isEnabled);
            pData.put("canClaim", canClaim);

            perksMap.put(type.getId(), pData);
        }

        data.put("perks", perksMap);
        return data;
    }
}
