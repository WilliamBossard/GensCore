package fr.gens.core.modules.quests;

import fr.gens.core.CorePlugin;
import fr.gens.core.modules.Module;
import fr.gens.core.modules.quests.listeners.QuestListener;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import cloud.commandframework.annotations.CommandMethod;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.meta.SkullMeta;



public class QuestModule implements Module, Listener {

    private final CorePlugin plugin;
    private boolean enabled = false;

    // Category -> List of Quests
    private final Map<String, List<Quest>> questsPool = new ConcurrentHashMap<>();
    // Player UUID -> PlayerQuestData
    private final Map<UUID, PlayerQuestData> playerData = new ConcurrentHashMap<>();
    
    private fr.gens.core.database.QuestDAO questDAO;

    // How many quests per category? Default to 3
    private final int QUESTS_PER_CATEGORY = 3;

    public QuestModule(CorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "Quests";
    }

    @Override
    public String getDescription() {
        return "Système de quêtes journalières (Remplace ODailyQuests).";
    }

    public fr.gens.core.database.QuestDAO getQuestDAO() {
        return questDAO;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void initDatabase(fr.gens.core.utils.DatabaseManager dbManager) {
        dbManager.executeStatement("CREATE TABLE IF NOT EXISTS player_quests_stats (uuid VARCHAR(36) PRIMARY KEY, player_name VARCHAR(16) NOT NULL, quests_completed INTEGER DEFAULT 0, rerolls_done INTEGER DEFAULT 0, last_reroll_date VARCHAR(255) DEFAULT '');");
        dbManager.executeStatement("CREATE TABLE IF NOT EXISTS player_quests_history (id INTEGER PRIMARY KEY AUTOINCREMENT, uuid VARCHAR(36) NOT NULL, player_name VARCHAR(16) NOT NULL, completion_date BIGINT NOT NULL);");
        dbManager.executeStatement("CREATE TABLE IF NOT EXISTS weekly_rewards (week_id VARCHAR(20) PRIMARY KEY, reward_description TEXT NOT NULL, winner_uuid VARCHAR(36), is_distributed INTEGER DEFAULT 0);");
        dbManager.executeStatement("CREATE TABLE IF NOT EXISTS player_active_quests (id INTEGER PRIMARY KEY AUTOINCREMENT, uuid VARCHAR(36) NOT NULL, date_assigned VARCHAR(10) NOT NULL, category VARCHAR(20) NOT NULL, quest_id VARCHAR(50) NOT NULL, progress INTEGER DEFAULT 0, completed BOOLEAN DEFAULT 0);");
        dbManager.executeStatement("CREATE INDEX IF NOT EXISTS idx_quests_history_uuid ON player_quests_history(uuid);");
        dbManager.executeStatement("CREATE INDEX IF NOT EXISTS idx_quests_history_date ON player_quests_history(completion_date);");
    }

    @Override
    public void enable() {
        enabled = true;
        
        this.questDAO = new fr.gens.core.database.QuestDAO(plugin);
        this.questDAO.initDatabase();

        migrateFromODailyQuests();
        loadQuests();


        plugin.getServer().getPluginManager().registerEvents(new QuestListener(this), plugin);
        plugin.getServer().getPluginManager().registerEvents(this, plugin); // For GUI clicks

        // Load data for currently online players (if reload)
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p == null) continue;
            loadPlayerData(p.getUniqueId(), p.getName());
        }

        // Check Weekly Rewards
        plugin.getFoliaLib().getScheduler().runLaterAsync((wrappedTask) -> this.checkWeeklyRewards(), 100L);

        plugin.getLogger().info("[Quests] Module activé, " + getTotalQuests() + " quests loaded.");
    }

    @Override
    public void disable() {
        org.bukkit.event.HandlerList.unregisterAll(this);
        enabled = false;
        saveAllData();
        questsPool.clear();
        playerData.clear();
        plugin.getLangManager().sendConsoleMessage("questmodule.log_1");
    }

    @Override
    public void registerCommands(CorePlugin plugin) {
        if (plugin.getCommandManager() != null && plugin.getCommandManager().getAnnotationParser() != null) {
            plugin.getCommandManager().getAnnotationParser().parse(this);
        }
    }

    private void checkWeeklyRewards() {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            String currentWeek = String.valueOf(cal.getTimeInMillis());
            
            // Generate reward if none exists for this week
            boolean hasReward = false;
            try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM weekly_rewards WHERE week_id = ?")) {
                ps.setString(1, currentWeek);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) hasReward = true;
                }
            }
            if (!hasReward) {
                List<String> possibleRewards = new ArrayList<>(Arrays.asList("5x Diamant", "1x Etoile du Nether", "2x Lingot de Netherite", "10x Emeraude"));
                if (plugin.getModuleManager().getModule("economy") != null && plugin.getModuleManager().getModule("economy").isEnabled()) {
                    possibleRewards.add("1000$");
                }
                String randomReward = possibleRewards.get(new Random().nextInt(possibleRewards.size()));
                try (PreparedStatement ps = conn.prepareStatement("INSERT INTO weekly_rewards (week_id, reward_description, is_distributed) VALUES (?, ?, 0)")) {
                    ps.setString(1, currentWeek);
                    ps.setString(2, randomReward);
                    ps.executeUpdate();
                }
                plugin.getLogger().info("[Quests] Nouvelle récompense de la semaine générée: " + randomReward);
            }
            
            // Distribute old rewards
            try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM weekly_rewards WHERE is_distributed = 0 AND week_id < ?")) {
                ps.setString(1, currentWeek);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String weekId = rs.getString("week_id");
                        String rewardDesc = rs.getString("reward_description");
                        long weekStart = Long.parseLong(weekId);
                        long weekEnd = weekStart + (7L * 24L * 60L * 60L * 1000L);
                        
                        // Find winner
                        String winnerUuid = null;
                        try (PreparedStatement getWinner = conn.prepareStatement(
                                "SELECT uuid FROM player_quests_history WHERE completion_date >= ? AND completion_date < ? GROUP BY uuid ORDER BY COUNT(*) DESC LIMIT 1")) {
                            getWinner.setLong(1, weekStart);
                            getWinner.setLong(2, weekEnd);
                            try (ResultSet winnerRs = getWinner.executeQuery()) {
                                if (winnerRs.next()) winnerUuid = winnerRs.getString("uuid");
                            }
                        }
                        
                        if (winnerUuid != null) {
                            try (PreparedStatement update = conn.prepareStatement("UPDATE weekly_rewards SET is_distributed = 1, winner_uuid = ? WHERE week_id = ?")) {
                                update.setString(1, winnerUuid);
                                update.setString(2, weekId);
                                update.executeUpdate();
                            }
                            
                            String giveCmd = "";
                            if (rewardDesc.contains("Diamant")) giveCmd = "give %player% diamond 5";
                            else if (rewardDesc.contains("Etoile")) giveCmd = "give %player% nether_star 1";
                            else if (rewardDesc.contains("Netherite")) giveCmd = "give %player% netherite_ingot 2";
                            else if (rewardDesc.contains("Emeraude")) giveCmd = "give %player% emerald 10";
                            else if (rewardDesc.contains("$")) giveCmd = "eco give %player% 1000";
                            
                            fr.gens.core.database.PendingCommandDAO pcd = new fr.gens.core.database.PendingCommandDAO(plugin);
                            pcd.initDatabase(); // just to ensure table exists
                            pcd.addPendingCommand(UUID.fromString(winnerUuid), giveCmd, "<green>Vous avez gagné le classement de quêtes de la semaine ! Voici votre lot : " + rewardDesc);
                            plugin.getLogger().info("[Quests] Récompense de la semaine distribuée au joueur " + winnerUuid);
                        } else {
                            try (PreparedStatement update = conn.prepareStatement("UPDATE weekly_rewards SET is_distributed = 1 WHERE week_id = ?")) {
                                update.setString(1, weekId);
                                update.executeUpdate();
                            }
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void checkPendingRewards(Player p) {
        fr.gens.core.database.PendingCommandDAO pcd = new fr.gens.core.database.PendingCommandDAO(plugin);
        pcd.initDatabase(); // just to ensure table exists
        pcd.processPendingCommands(p);
    }

    public void unloadPlayerData(UUID uuid) {
        playerData.remove(uuid);
    }

    private void migrateFromODailyQuests() {
        File oldQuestsFolder = new File(plugin.getServer().getWorldContainer(), "plugins/ODailyQuests/quests");
        File newQuestsFolder = new File(plugin.getDataFolder(), "quests");

        if (!newQuestsFolder.exists()) {
            newQuestsFolder.mkdirs();
        }

        if (oldQuestsFolder.exists() && newQuestsFolder.listFiles().length == 0) {
            plugin.getLangManager().sendConsoleMessage("questmodule.log_2");
            for (File file : oldQuestsFolder.listFiles()) {
                if (file.getName().endsWith(".yml")) {
                    File dest = new File(newQuestsFolder, file.getName());
                    try {
                        Files.copy(file.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            plugin.getLangManager().sendConsoleMessage("questmodule.log_3");
        }
    }

    private void loadQuests() {
        questsPool.clear();
        File newQuestsFolder = new File(plugin.getDataFolder(), "quests");
        if (!newQuestsFolder.exists()) return;

        for (File file : newQuestsFolder.listFiles()) {
            if (file.getName().endsWith(".yml")) {
                String category = file.getName().replace(".yml", "");
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                
                List<Quest> categoryQuests = new ArrayList<>();
                ConfigurationSection questsSection = config.getConfigurationSection("quests");
                
                if (questsSection != null) {
                    for (String key : questsSection.getKeys(false)) {
                        ConfigurationSection qSection = questsSection.getConfigurationSection(key);
                        String name = qSection.getString("name", "Unknown");
                        String menuItem = qSection.getString("menu_item", "PAPER");
                        List<String> description = qSection.getStringList("description");
                        String questTypeStr = qSection.getString("quest_type", "BREAK");
                        QuestType type = QuestType.fromString(questTypeStr);
                        
                        List<String> requiredTargets = new ArrayList<>();
                        if (qSection.isList("required")) {
                            requiredTargets = qSection.getStringList("required");
                        } else if (qSection.isString("required")) {
                            requiredTargets.add(qSection.getString("required"));
                        }
                        
                        int requiredAmount = qSection.getInt("required_amount", 1);
                        
                        List<String> commands = new ArrayList<>();
                        ConfigurationSection rewardSection = qSection.getConfigurationSection("reward");
                        if (rewardSection != null && rewardSection.contains("commands")) {
                            commands = rewardSection.getStringList("commands");
                        }

                        if (type != null) {
                            categoryQuests.add(new Quest(key, category, name, menuItem, description, type, requiredTargets, requiredAmount, commands));
                        } else {
                            plugin.getLogger().warning("Unknown quest type: " + questTypeStr + " in " + file.getName());
                        }
                    }
                }
                questsPool.put(category, categoryQuests);
            }
        }
    }

    private int getTotalQuests() {
        return questsPool.values().stream().mapToInt(List::size).sum();
    }

    private String getTodayString() {
        return new SimpleDateFormat("yyyy-MM-dd").format(new Date());
    }

    public void loadPlayerData(UUID uuid, String playerName) {
        plugin.getFoliaLib().getScheduler().runAsync((wrappedTask) -> {
            String today = getTodayString();
            PlayerQuestData data = new PlayerQuestData(uuid, today);

            try (Connection conn = plugin.getDatabaseManager().getConnection()) {
                // Ensure stat row exists
                try (PreparedStatement checkStat = conn.prepareStatement("INSERT OR IGNORE INTO player_quests_stats (uuid, player_name, quests_completed) VALUES (?, ?, ?)")) {
                    checkStat.setString(1, uuid.toString());
                    checkStat.setString(2, playerName);
                    checkStat.setInt(3, 0);
                    checkStat.executeUpdate();
                }

                // Load active quests
                boolean hasQuestsToday = false;
                try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM player_active_quests WHERE uuid = ?")) {
                    ps.setString(1, uuid.toString());
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            String assignedDate = rs.getString("date_assigned");
                            if (!assignedDate.equals(today)) {
                                continue; // Stale quests, will be deleted/ignored
                            }
                            hasQuestsToday = true;
                            data.addQuest(
                                rs.getString("category"),
                                rs.getString("quest_id"),
                                rs.getInt("progress"),
                                rs.getBoolean("completed")
                            );
                        }
                    }
                }
                
                if (!hasQuestsToday) {
                    // Assign new quests
                    assignNewQuests(uuid, data, conn);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            
            // Fetch stats (must be outside the connection block to avoid deadlock with connection pool = 1)
            data.setCompletedTotal(questDAO.getQuestsCompletedTotal(uuid));
            data.setRerollsDone(questDAO.getRerollsDone(uuid, today));

            plugin.getFoliaLib().getScheduler().runNextTick((t2) -> playerData.put(uuid, data));
        });
    }

    private void assignNewQuests(UUID uuid, PlayerQuestData data, Connection conn) throws SQLException {
        // Delete old quests
        try (PreparedStatement del = conn.prepareStatement("DELETE FROM player_active_quests WHERE uuid = ?")) {
            del.setString(1, uuid.toString());
            del.executeUpdate();
        }

        Random rand = new Random();
        try (PreparedStatement insert = conn.prepareStatement("INSERT INTO player_active_quests (uuid, date_assigned, category, quest_id, progress, completed) VALUES (?, ?, ?, ?, ?, ?)")) {
            for (Map.Entry<String, List<Quest>> entry : questsPool.entrySet()) {
                String category = entry.getKey();
                List<Quest> available = new ArrayList<>(entry.getValue());
                Collections.shuffle(available, rand);

                int limit = Math.min(QUESTS_PER_CATEGORY, available.size());
                for (int i = 0; i < limit; i++) {
                    Quest q = available.get(i);
                    data.addQuest(category, q.getId(), 0, false);
                    
                    insert.setString(1, uuid.toString());
                    insert.setString(2, data.getDateAssigned());
                    insert.setString(3, category);
                    insert.setString(4, q.getId());
                    insert.setInt(5, 0);
                    insert.setBoolean(6, false);
                    insert.addBatch();
                }
            }
            insert.executeBatch();
        }
    }

    public void handleQuestProgress(Player p, QuestType type, String target, int amount) {
        if (!enabled) return;
        
        // Progress for Team Quest
        fr.gens.core.modules.teams.TeamData team = plugin.getTeamManager().getPlayerTeam(p.getUniqueId());
        if (team != null && plugin.getTeamQuestManager() != null) {
            plugin.getTeamQuestManager().addProgress(team, type, target, amount);
        }

        PlayerQuestData data = playerData.get(p.getUniqueId());
        if (data == null) return;

        boolean updated = false;

        for (Map.Entry<String, Map<String, Integer>> catEntry : data.getActiveQuests().entrySet()) {
            String category = catEntry.getKey();
            for (Map.Entry<String, Integer> qEntry : catEntry.getValue().entrySet()) {
                String questId = qEntry.getKey();
                if (data.isCompleted(category, questId)) continue;

                Quest quest = getQuest(category, questId);
                if (quest == null) continue;

                if (quest.getType() == type && quest.isTarget(target)) {
                    int newProgress = qEntry.getValue() + amount;
                    if (newProgress >= quest.getRequiredAmount()) {
                        newProgress = quest.getRequiredAmount();
                        data.setCompleted(category, questId, true);
                        giveRewards(p, quest);
                        incrementStats(p);
                        p.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<green><bold>Quête Terminée ! <gray>Vous avez fini: ").append(fr.gens.core.utils.PlaceholderUtils.parseToComponent(quest.getName())));
                    }
                    data.setProgress(category, questId, newProgress);
                    updated = true;
                    
                    // Action Bar
                    boolean isDone = newProgress >= quest.getRequiredAmount();
                    String color = isDone ? "<green>" : "<yellow>";
                    Component msg = fr.gens.core.utils.PlaceholderUtils.parseToComponent("<dark_gray>[<aqua>Quêtes<dark_gray>] <white>")
                            .append(fr.gens.core.utils.PlaceholderUtils.parseToComponent(quest.getName()))
                            .append(fr.gens.core.utils.PlaceholderUtils.parseToComponent(" <dark_gray>» " + color + newProgress + " <gray>/ " + quest.getRequiredAmount()));
                    plugin.getActionBarManager().sendMessage(p, "quests", msg, 40);
                    
                    // Async save
                    final int finalProgress = newProgress;
                    final boolean finalCompleted = newProgress >= quest.getRequiredAmount();
                    plugin.getFoliaLib().getScheduler().runAsync((wrappedTask) -> {
                        try (Connection conn = plugin.getDatabaseManager().getConnection();
                             PreparedStatement ps = conn.prepareStatement("UPDATE player_active_quests SET progress = ?, completed = ? WHERE uuid = ? AND category = ? AND quest_id = ?")) {
                            ps.setInt(1, finalProgress);
                            ps.setBoolean(2, finalCompleted);
                            ps.setString(3, p.getUniqueId().toString());
                            ps.setString(4, category);
                            ps.setString(5, questId);
                            ps.executeUpdate();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    });
                }
            }
        }

        if (updated) {
            // Update GUI if open
            if (net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(p.getOpenInventory().title()).equals("Quêtes Journalières")) {
                openQuestsMenu(p);
            }
        }
    }

    private void incrementStats(Player p) {
        PlayerQuestData data = playerData.get(p.getUniqueId());
        if (data != null) {
            data.setCompletedTotal(data.getCompletedTotal() + 1);
        }
        plugin.getFoliaLib().getScheduler().runAsync((wrappedTask) -> {
            try (Connection conn = plugin.getDatabaseManager().getConnection()) {
                // Total stat update
                try (PreparedStatement ps = conn.prepareStatement("UPDATE player_quests_stats SET quests_completed = quests_completed + 1 WHERE uuid = ?")) {
                    ps.setString(1, p.getUniqueId().toString());
                    ps.executeUpdate();
                }
                // Add to history
                try (PreparedStatement ps = conn.prepareStatement("INSERT INTO player_quests_history (uuid, player_name, completion_date) VALUES (?, ?, ?)")) {
                    ps.setString(1, p.getUniqueId().toString());
                    ps.setString(2, p.getName());
                    ps.setLong(3, System.currentTimeMillis());
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    private void giveRewards(Player p, Quest quest) {
        for (String cmd : quest.getRewardCommands()) {
            cmd = cmd.replace("%player%", p.getName());
            if (cmd.startsWith("eco give")) {
                // Internal Economy
                String[] parts = cmd.split(" ");
                if (parts.length >= 4) {
                    try {
                        double amount = Double.parseDouble(parts[3]);
                        fr.gens.core.modules.EconomyModule eco = (fr.gens.core.modules.EconomyModule) plugin.getModuleManager().getModule("economy");
                        if (eco != null) {
                            eco.addMoney(p.getUniqueId(), amount);
                            p.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<green>+ " + amount + "$"));
                        }
                    } catch (NumberFormatException ignored) {}
                }
            } else {
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), cmd);
            }
        }
    }

    private Quest getQuest(String category, String id) {
        List<Quest> list = questsPool.get(category);
        if (list == null) return null;
        for (Quest q : list) {
            if (q.getId().equals(id)) return q;
        }
        return null;
    }

    public void saveAllData() {
        // Data is saved in real-time, no need for massive sync
    }

    @CommandMethod("quests")
    public void executeQuestsCommand(org.bukkit.command.CommandSender sender) {
        if (!(sender instanceof Player)) return;
        Player p = (Player) sender;
        if (!enabled) {
            plugin.getLangManager().sendMessage(p, "questmodule.msg_1");
            return;
        }
        openQuestsMenu(p);
    }

    @CommandMethod("quest")
    public void executeQuestCommand(org.bukkit.command.CommandSender sender) {
        executeQuestsCommand(sender);
    }

    @CommandMethod("quete")
    public void executeQueteCommand(org.bukkit.command.CommandSender sender) {
        executeQuestsCommand(sender);
    }

    @CommandMethod("quests reroll")
    public void executeQuestsReroll(org.bukkit.command.CommandSender sender) {
        if (!(sender instanceof org.bukkit.entity.Player)) return;
        org.bukkit.entity.Player p = (org.bukkit.entity.Player) sender;
        if (!enabled) {
            plugin.getLangManager().sendMessage(p, "questmodule.msg_1");
            return;
        }
        if (!p.hasPermission("genscore.quests.admin")) {
            return;
        }

        // Forcibly reroll all quests
        plugin.getFoliaLib().getScheduler().runAsync((wrappedTask) -> {
            try (Connection conn = plugin.getDatabaseManager().getConnection()) {
                PlayerQuestData data = new PlayerQuestData(p.getUniqueId(), getTodayString());
                assignNewQuests(p.getUniqueId(), data, conn);
                plugin.getFoliaLib().getScheduler().runAtEntity(p, (t2) -> {
                    playerData.put(p.getUniqueId(), data);
                    plugin.getLangManager().sendMessage(p, "questmodule.msg_2");
                    openQuestsMenu(p);
                });
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public void openQuestsMenu(Player p) {
        if (fr.gens.core.utils.FloodgateUtil.isBedrockPlayer(p.getUniqueId())) {
            PlayerQuestData data = playerData.get(p.getUniqueId());
            if (data == null) {
                plugin.getLangManager().sendMessage(p, "questmodule.msg_3");
                return;
            }

            int achieved = 0;
            int totalDay = 0;
            for (java.util.Map<String, Boolean> comp : data.getCompletedQuests().values()) {
                for (Boolean b : comp.values()) {
                    if (b) achieved++;
                }
            }
            for (java.util.Map<String, Integer> act : data.getActiveQuests().values()) {
                totalDay += act.size();
            }

            java.util.List<fr.gens.core.utils.BedrockFormManager.BedrockButton> buttons = new java.util.ArrayList<>();
            buttons.add(new fr.gens.core.utils.BedrockFormManager.BedrockButton("§b§lStatistiques\n§r§8" + achieved + "/" + totalDay + " quêtes accomplies", org.bukkit.Material.PLAYER_HEAD, player -> openQuestsMenu(player)));

            for (java.util.Map.Entry<String, java.util.Map<String, Integer>> catEntry : data.getActiveQuests().entrySet()) {
                String category = catEntry.getKey();
                for (java.util.Map.Entry<String, Integer> qEntry : catEntry.getValue().entrySet()) {
                    String questId = qEntry.getKey();
                    boolean completed = data.isCompleted(category, questId);
                    Quest q = getQuest(category, questId);
                    if (q == null) continue;

                    org.bukkit.Material mat = org.bukkit.Material.matchMaterial(q.getMenuItem());
                    if (mat == null) mat = org.bukkit.Material.PAPER;
                    
                    String btnText = q.getName() + "\n";
                    for (String l : q.getDescription()) {
                        l = l.replace("%required%", String.valueOf(q.getRequiredAmount()));
                        if (l.contains("%status%")) {
                            if (completed) {
                                l = l.replace("%status%", "TERMINÉ");
                            } else {
                                l = l.replace("%status%", qEntry.getValue() + "/" + q.getRequiredAmount());
                            }
                        }
                        btnText += "§7" + net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(fr.gens.core.utils.PlaceholderUtils.parseToComponent(l)) + "\n";
                    }
                    if (completed) {
                        btnText += "§2[TERMINÉ]";
                    } else {
                        btnText += "§eProgression: " + qEntry.getValue() + " / " + q.getRequiredAmount() + " (Clic Reroll)";
                    }

                    buttons.add(new fr.gens.core.utils.BedrockFormManager.BedrockButton(btnText, mat, player -> {
                        int limit = plugin.getConfigManager().getConfig("modules/quests.yml").getInt("quests.max_rerolls_per_day", 3);
                        if (!completed && data.getRerollsDone() < limit) {
                            rerollQuest(player, category, questId, data);
                            player.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<green>La quête a été remplacée !"));
                            openQuestsMenu(player);
                        } else if (!completed) {
                            player.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<red>Vous n'avez plus de rerolls disponibles !"));
                        }
                    }));
                }
            }

            fr.gens.core.utils.BedrockFormManager.openSimpleForm(p, "Quêtes Journalières", "Voici vos quêtes du jour :", buttons);
            return;
        }

        QuestGuiHolder holder = new QuestGuiHolder();
        Inventory inv = Bukkit.createInventory(holder, 45, fr.gens.core.utils.PlaceholderUtils.parseToComponent("<blue><bold>Quêtes Journalières"));
        holder.setInventory(inv);
        PlayerQuestData data = playerData.get(p.getUniqueId());
        
        if (data == null) {
            plugin.getLangManager().sendMessage(p, "questmodule.msg_3");
            return;
        }

        // Fill background
        ItemStack bluePane = new ItemStack(Material.BLUE_STAINED_GLASS_PANE);
        ItemMeta bm = bluePane.getItemMeta(); bm.displayName(fr.gens.core.utils.PlaceholderUtils.parseToComponent(" ")); bluePane.setItemMeta(bm);
        int[] blueSlots = {2,3,4, 6,7,8, 10,11, 18,19, 20, 27,28, 29, 37,38,39,40, 42,43,44};
        for(int s : blueSlots) if(s < 45) inv.setItem(s, bluePane);

        ItemStack cyanPane = new ItemStack(Material.CYAN_STAINED_GLASS_PANE);
        ItemMeta cm = cyanPane.getItemMeta(); cm.displayName(fr.gens.core.utils.PlaceholderUtils.parseToComponent(" ")); cyanPane.setItemMeta(cm);
        int[] cyanSlots = {13, 15, 17, 22, 24, 26, 31, 33, 35};
        for(int s : cyanSlots) if(s < 45) inv.setItem(s, cyanPane);

        // Player Head Stats
        int completedTotal = data.getCompletedTotal();
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta sm = (SkullMeta) head.getItemMeta();
        sm.setPlayerProfile(p.getPlayerProfile());
        sm.displayName(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<aqua>" + p.getName()));
        List<String> hl = new ArrayList<>();
        hl.add("<dark_aqua>Statut:");
        
        // Count today's achieved
        int achieved = 0;
        int totalDay = 0;
        for (Map<String, Boolean> comp : data.getCompletedQuests().values()) {
            for (Boolean b : comp.values()) {
                if (b) achieved++;
            }
        }
        for (Map<String, Integer> act : data.getActiveQuests().values()) {
            totalDay += act.size();
        }
        
        hl.add(" <dark_aqua><bold>| <gray>Quêtes accomplies : <aqua>" + achieved + "<dark_aqua>/<aqua>" + totalDay);
        hl.add(" <dark_aqua><bold>| <gray>Total complété : <aqua>" + completedTotal);
        sm.lore(java.util.Optional.ofNullable(hl).orElse(java.util.Collections.emptyList()).stream().map(s -> fr.gens.core.utils.PlaceholderUtils.parseToComponent((String)s)).collect(java.util.stream.Collectors.toList()));
        head.setItemMeta(sm);
        inv.setItem(4, head);

        // Map categories to specific columns
        Map<String, Integer[]> catSlots = new HashMap<>();
        catSlots.put("easy", new Integer[]{11, 20, 29});
        catSlots.put("medium", new Integer[]{13, 22, 31});
        catSlots.put("hard", new Integer[]{15, 24, 33});

        int defaultSlot = 0;

        for (Map.Entry<String, Map<String, Integer>> catEntry : data.getActiveQuests().entrySet()) {
            String category = catEntry.getKey();
            Integer[] slots = catSlots.get(category.toLowerCase());
            int index = 0;
            
            for (Map.Entry<String, Integer> qEntry : catEntry.getValue().entrySet()) {
                String questId = qEntry.getKey();
                boolean completed = data.isCompleted(category, questId);
                Quest q = getQuest(category, questId);
                if (q == null) continue;

                Material mat = Material.matchMaterial(q.getMenuItem());
                if (mat == null) mat = Material.PAPER;
                
                ItemStack item = new ItemStack(mat);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.displayName(fr.gens.core.utils.PlaceholderUtils.parseToComponent(q.getName()));
                    List<String> lore = new ArrayList<>();
                    for (String l : q.getDescription()) {
                        l = l.replace("%required%", String.valueOf(q.getRequiredAmount()));
                        if (l.contains("%status%")) {
                            if (completed) {
                                l = l.replace("%status%", "<aqua>TERMINÉ");
                            } else {
                                l = l.replace("%status%", "<aqua>" + qEntry.getValue() + "<dark_aqua>/<aqua>" + q.getRequiredAmount());
                            }
                        }
                        lore.add(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(fr.gens.core.utils.PlaceholderUtils.parseToComponent(l)));
                    }
                    if (p.hasPermission("genscore.quests.reroll") && !completed) {
                        lore.add(" ");
                        int limit = plugin.getConfigManager().getConfig("modules/quests.yml").getInt("quests.max_rerolls_per_day", 3);
                        int used = data.getRerollsDone();
                        lore.add("<light_purple>» Clic Droit pour Reroll (" + used + "/" + limit + ")");
                    } else if (completed) {
                        lore.add(" ");
                        lore.add("<green> Récompenses récupérées !");
                        meta.addEnchant(org.bukkit.enchantments.Enchantment.LURE, 1, true);
                        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
                    }
                    meta.lore(java.util.Optional.ofNullable(lore).orElse(java.util.Collections.emptyList()).stream().map(s -> fr.gens.core.utils.PlaceholderUtils.parseToComponent((String)s)).collect(java.util.stream.Collectors.toList()));
                    item.setItemMeta(meta);
                }

                int slotToUse = defaultSlot;
                if (slots != null && index < slots.length) {
                    slotToUse = slots[index];
                }
                if (slotToUse < 45) {
                    inv.setItem(slotToUse, item);
                }
                index++;
                defaultSlot++;
            }
        }
        
        p.openInventory(inv);
    }

    public static class QuestGuiHolder implements org.bukkit.inventory.InventoryHolder {
        private Inventory inventory;
        public void setInventory(Inventory inv) { this.inventory = inv; }
        @Override public Inventory getInventory() { return inventory; }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!enabled) return;
        if (event.getInventory().getHolder() instanceof QuestGuiHolder) {
            if (event.getClickedInventory() == null) return;
            if (!event.getClickedInventory().equals(event.getView().getTopInventory())) {
                if (event.getAction() == org.bukkit.event.inventory.InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                    event.setCancelled(true);
                }
                return;
            }
            
            event.setCancelled(true);
            
            if (event.getClick() == ClickType.RIGHT) {
                Player p = (Player) event.getWhoClicked();
                if (!p.hasPermission("genscore.quests.reroll")) return;
                
                ItemStack item = event.getCurrentItem();
                if (item == null || !item.hasItemMeta()) return;
                net.kyori.adventure.text.Component displayName = item.getItemMeta().displayName();
                
                PlayerQuestData data = playerData.get(p.getUniqueId());
                if (data == null) return;
                
                // Find quest by name
                for (Map.Entry<String, Map<String, Integer>> catEntry : data.getActiveQuests().entrySet()) {
                    String category = catEntry.getKey();
                    for (String questId : catEntry.getValue().keySet()) {
                        Quest q = getQuest(category, questId);
                        if (q != null && fr.gens.core.utils.PlaceholderUtils.parseToComponent(q.getName()).equals(displayName)) {
                            // Check completed
                            if (data.isCompleted(category, questId)) return;
                            
                            // Check limit
                            int limit = plugin.getConfigManager().getConfig("modules/quests.yml").getInt("quests.max_rerolls_per_day", 3);
                            int used = data.getRerollsDone();
                            if (used >= limit) {
                                p.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<red>Vous avez atteint la limite de " + limit + " rerolls pour aujourd'hui !"));
                                return;
                            }
                            
                            // Reroll this specific quest
                            data.setRerollsDone(used + 1);
                            plugin.getFoliaLib().getScheduler().runAsync((wrappedTask) -> {
                                questDAO.setRerollsDone(p.getUniqueId(), used + 1, getTodayString());
                            });
                            rerollQuest(p, category, questId, data);
                            return;
                        }
                    }
                }
            }
        }
    }

    private void rerollQuest(Player p, String category, String oldQuestId, PlayerQuestData data) {
        List<Quest> available = questsPool.get(category);
        if (available == null || available.size() <= 1) {
            plugin.getLangManager().sendMessage(p, "questmodule.msg_4");
            return;
        }
        
        Quest newQuest = null;
        Random rand = new Random();
        for (int i = 0; i < 20; i++) {
            Quest candidate = available.get(rand.nextInt(available.size()));
            if (!candidate.getId().equals(oldQuestId) && !data.getActiveQuests().getOrDefault(category, new HashMap<>()).containsKey(candidate.getId())) {
                newQuest = candidate;
                break;
            }
        }
        
        if (newQuest == null) {
            plugin.getLangManager().sendMessage(p, "questmodule.msg_5");
            return;
        }
        
        final String nqId = newQuest.getId();
        
        // Database update
        plugin.getFoliaLib().getScheduler().runAsync((wrappedTask) -> {
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement ps = conn.prepareStatement("UPDATE player_active_quests SET quest_id = ?, progress = 0, completed = 0 WHERE uuid = ? AND category = ? AND quest_id = ?")) {
                ps.setString(1, nqId);
                ps.setString(2, p.getUniqueId().toString());
                ps.setString(3, category);
                ps.setString(4, oldQuestId);
                ps.executeUpdate();
                
                plugin.getFoliaLib().getScheduler().runAtEntity(p, (t2) -> {
                    data.getActiveQuests().get(category).remove(oldQuestId);
                    data.getCompletedQuests().get(category).remove(oldQuestId);
                    data.addQuest(category, nqId, 0, false);
                    plugin.getLangManager().sendMessage(p, "questmodule.msg_6");
                    openQuestsMenu(p);
                });
                
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }
}





