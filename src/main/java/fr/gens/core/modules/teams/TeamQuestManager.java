package fr.gens.core.modules.teams;

import fr.gens.core.CorePlugin;
import fr.gens.core.modules.quests.QuestType;
import org.bukkit.Bukkit;

import java.util.HashMap;
import java.util.Map;


public class TeamQuestManager {
    private final CorePlugin plugin;
    
    private static class QuestDef {
        String id;
        QuestType type;
        String target;
        int amount;
        int points;
        String desc;

        QuestDef(String id, QuestType type, String target, int amount, int points, String desc) {
            this.id = id;
            this.type = type;
            this.target = target;
            this.amount = amount;
            this.points = points;
            this.desc = desc;
        }
    }

    private final java.util.List<QuestDef> questPool = java.util.Arrays.asList(
        new QuestDef("weekly_1", QuestType.BREAK, "STONE", 25000, 1000, "Mine 25 000 blocs de roche en ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©quipe !"),
        new QuestDef("weekly_2", QuestType.KILL, "ZOMBIE", 2500, 1000, "Tuez 2 500 zombies en ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©quipe !"),
        new QuestDef("weekly_3", QuestType.BREAK, "OAK_LOG", 5000, 1000, "Coupez 5 000 bÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â»ches de chÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Âªne en ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©quipe !"),
        new QuestDef("weekly_4", QuestType.BREAK, "WHEAT", 10000, 1000, "RÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©coltez 10 000 blÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©s en ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©quipe !"),
        new QuestDef("weekly_5", QuestType.KILL, "SKELETON", 2500, 1000, "ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â°liminez 2 500 squelettes en ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©quipe !"),
        new QuestDef("weekly_6", QuestType.BREAK, "NETHERRACK", 50000, 1000, "Mine 50 000 blocs de netherrack en ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©quipe !")
    );

    private QuestDef activeQuest;

    // teamId -> progress
    private final Map<Integer, Integer> teamProgress = new HashMap<>();

    public TeamQuestManager(CorePlugin plugin) {
        this.plugin = plugin;
        checkRotation();
        loadProgress();
    }

    private void checkRotation() {
        org.bukkit.configuration.file.FileConfiguration config = plugin.getConfigManager().getConfig("modules/teams.yml");
        long lastRotation = config.getLong("teams.last_rotation", 0);
        long currentWeek = System.currentTimeMillis() / (1000 * 60 * 60 * 24 * 7); // Identifiant unique pour la semaine
        
        String savedId = config.getString("teams.active_quest", "");
        
        if (lastRotation != currentWeek || savedId.isEmpty()) {
            // Rotation!
            java.util.Collections.shuffle(questPool);
            activeQuest = questPool.get(0);
            
            config.set("teams.last_rotation", currentWeek);
            config.set("teams.active_quest", activeQuest.id);
            plugin.getConfigManager().saveConfig("modules/teams.yml");
            
            fr.gens.core.modules.teams.TeamModule module = (fr.gens.core.modules.teams.TeamModule) plugin.getModuleManager().getModule("teams");
            if (module != null) module.getTeamDAO().clearTeamQuests();
            teamProgress.clear();
        } else {
            activeQuest = questPool.stream().filter(q -> q.id.equals(savedId)).findFirst().orElse(questPool.get(0));
        }
    }

    private void loadProgress() {
        fr.gens.core.modules.teams.TeamModule module = (fr.gens.core.modules.teams.TeamModule) plugin.getModuleManager().getModule("teams");
        if (module != null) teamProgress.putAll(module.getTeamDAO().loadTeamQuestProgress(activeQuest.id));
    }

    public void saveProgress(int teamId, int progress) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            fr.gens.core.modules.teams.TeamModule module = (fr.gens.core.modules.teams.TeamModule) plugin.getModuleManager().getModule("teams");
            if (module != null) module.getTeamDAO().saveTeamQuestProgress(teamId, activeQuest.id, progress);
        });
    }

    public void addProgress(TeamData team, QuestType type, String target, int amount) {
        if (team == null) return;
        if (type != activeQuest.type) return;
        if (!target.equalsIgnoreCase(activeQuest.target)) return;

        int current = teamProgress.getOrDefault(team.getTeamId(), 0);
        if (current >= activeQuest.amount) return; // DÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©jÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â  fini

        current += amount;
        if (current >= activeQuest.amount) {
            current = activeQuest.amount;
            team.broadcast("<green><bold> Votre guilde a terminÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â© la QuÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Âªte Hebdomadaire !");
            team.broadcast("<yellow>+" + activeQuest.points + " Points de Guilde !");
            
            // Ajouter les points
            team.addPoints(activeQuest.points);
            
            // Sauvegarder les points dans la DB
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                fr.gens.core.modules.teams.TeamModule module = (fr.gens.core.modules.teams.TeamModule) plugin.getModuleManager().getModule("teams");
                if (module != null) module.getTeamDAO().saveTeamStats(team.getTeamId(), team.getWeeklyPoints(), team.getTotalPoints());
                
                // Distribuer les rÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©compenses
                fr.gens.core.modules.EconomyModule eco = (fr.gens.core.modules.EconomyModule) plugin.getModuleManager().getModule("economy");
                boolean ecoEnabled = (eco != null && eco.isEnabled());
                double rewardAmount = ecoEnabled ? 50000.0 : 0.0;
                String rewardItem = !ecoEnabled ? "DIAMOND_BLOCK:4" : "";
                
                for (java.util.UUID memberId : team.getMembers()) {

                    if (module != null) {
                        module.getTeamDAO().addPendingReward(memberId, rewardAmount, rewardItem);
                    }
                    org.bukkit.entity.Player p = Bukkit.getPlayer(memberId);
                    if (p != null && p.isOnline()) {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            if (module != null) module.getTeamDAO().processPendingRewards(p);
                        });
                    }
                }
            });
        }
        
        teamProgress.put(team.getTeamId(), current);
        saveProgress(team.getTeamId(), current);
    }

    public int getProgress(int teamId) {
        return teamProgress.getOrDefault(teamId, 0);
    }
    
    public int getGoal() {
        return activeQuest.amount;
    }
    
    public String getDesc() {
        fr.gens.core.modules.EconomyModule eco = (fr.gens.core.modules.EconomyModule) plugin.getModuleManager().getModule("economy");
        boolean ecoEnabled = (eco != null && eco.isEnabled());
        String rewardStr = ecoEnabled ? "50 000 $ / membre" : "4 Blocs de Diamant / membre";
        return activeQuest.desc + " (RÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©compense: " + rewardStr + ")";
    }
}

