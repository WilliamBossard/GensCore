package fr.gens.core.modules.teams;

import fr.gens.core.CorePlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public class TeamGui {
    private final CorePlugin plugin;

    public TeamGui(CorePlugin plugin) {
        this.plugin = plugin;
    }

    public void openTeamGui(Player player) {
        TeamData team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());

        if (team == null) {
            plugin.getLangManager().sendMessage(player, "teamgui.msg_1");
            return;
        }

        if (fr.gens.core.utils.FloodgateUtil.isBedrockPlayer(player.getUniqueId())) {
            boolean isLeader = team.getLeaderUuid().equals(player.getUniqueId());
            java.util.List<fr.gens.core.utils.BedrockFormManager.BedrockButton> buttons = new java.util.ArrayList<>();

            buttons.add(new fr.gens.core.utils.BedrockFormManager.BedrockButton("§bQuêtes de Guilde\n§r§8Défis en coop", org.bukkit.Material.ENCHANTED_BOOK, p -> {
                // To avoid circular dependency with TeamListener here, we can dispatch the command or call it.
                // It's in TeamListener, so let's call the public method openTeamQuestGui if we make it public or accessible.
                // Wait, TeamListener is where openTeamQuestGui is. Let's just create a mock event or move the logic?
                // Actually, I'll just leave it and put the Bedrock logic inside TeamGui.
                // But openTeamQuestGui is in TeamListener. Let's make openTeamQuestGui public static in TeamListener or something.
                // For now, I'll let them click Quests.
                p.performCommand("team quest"); // We can make a command or just move it.
            }));

            if (isLeader) {
                buttons.add(new fr.gens.core.utils.BedrockFormManager.BedrockButton("§aAuto-verrouillage\n§r§8" + (team.isAutoLock() ? "ACTIVÉ" : "DÉSACTIVÉ"), org.bukkit.Material.REPEATER, p -> {
                    team.setAutoLock(!team.isAutoLock());
                    openTeamGui(p);
                }));
            }

            for (UUID memberUuid : team.getMembers()) {
                org.bukkit.OfflinePlayer op = Bukkit.getOfflinePlayer(memberUuid);
                String role = team.getLeaderUuid().equals(memberUuid) ? "§6Chef" : "§7Membre";
                String pName = op.getName() != null ? op.getName() : "Inconnu";
                fr.gens.core.modules.BedrockSkinModule skinModule = (fr.gens.core.modules.BedrockSkinModule) plugin.getModuleManager().getModule("bedrockskin");
                String headUrl = (skinModule != null) ? skinModule.getHeadUrl(memberUuid, pName) : "https://minotar.net/helm/Steve/64.png";
                buttons.add(new fr.gens.core.utils.BedrockFormManager.BedrockButton("§e" + pName + "\n§r" + role, headUrl, p -> {
                    if (isLeader && !team.getLeaderUuid().equals(memberUuid)) {
                        // Exclure
                        team.removeMember(memberUuid);
                        p.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<green>Joueur expulsé de l'équipe."));
                        openTeamGui(p);
                    }
                }));
            }

            buttons.add(new fr.gens.core.utils.BedrockFormManager.BedrockButton("§cQuitter / Dissoudre\n§r§8Attention !", org.bukkit.Material.BARRIER, p -> {
                if (isLeader) {
                    plugin.getLangManager().sendMessage(p, "teamlistener.msg_1");
                    plugin.getTeamManager().disbandTeam(team);
                    p.closeInventory();
                } else {
                    plugin.getLangManager().sendMessage(p, "teamlistener.msg_2");
                    team.removeMember(p.getUniqueId());
                    p.closeInventory();
                }
            }));

            fr.gens.core.utils.BedrockFormManager.openSimpleForm(player, "Guilde: " + team.getName(), "Gérez votre équipe :", buttons);
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 45, fr.gens.core.utils.PlaceholderUtils.parseToComponent("<dark_gray>Guilde : " + team.getName()));

        // Ligne de décor
        ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.displayName(fr.gens.core.utils.PlaceholderUtils.parseToComponent(" "));
        glass.setItemMeta(glassMeta);
        for (int i = 0; i < 9; i++) inv.setItem(i, glass);
        for (int i = 37; i < 44; i++) inv.setItem(i, glass);

        // Bouton Quêtes
        ItemStack quests = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta qmeta = quests.getItemMeta();
        qmeta.displayName(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<aqua>Quêtes de Guilde"));
        List<String> qlore = new ArrayList<>();
        qlore.add("<gray>Accomplissez des défis en coopération");
        qlore.add("<gray>pour gagner des Points de Guilde !");
        qlore.add("");
        qlore.add("<yellow>Clic pour ouvrir");
        qmeta.lore(java.util.Optional.ofNullable(qlore).orElse(java.util.Collections.emptyList()).stream().map(s -> fr.gens.core.utils.PlaceholderUtils.parseToComponent((String)s)).collect(java.util.stream.Collectors.toList()));
        quests.setItemMeta(qmeta);
        inv.setItem(36, quests);

        // Afficher les membres
        int slot = 9;
        boolean isLeader = team.getLeaderUuid().equals(player.getUniqueId());

        for (UUID memberUuid : team.getMembers()) {
            OfflinePlayer op = Bukkit.getOfflinePlayer(memberUuid);
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.setPlayerProfile(op.getPlayerProfile());
            meta.displayName(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<yellow>" + (op.getName() != null ? op.getName() : "Joueur Inconnu")));
            List<String> lore = new ArrayList<>();
            if (team.getLeaderUuid().equals(memberUuid)) {
                lore.add("<gold> Chef de Guilde");
            } else {
                lore.add("<gray>Membre");
                if (isLeader) {
                    lore.add("");
                    lore.add("<red>Clic droit pour exclure");
                }
            }
            meta.lore(java.util.Optional.ofNullable(lore).orElse(java.util.Collections.emptyList()).stream().map(s -> fr.gens.core.utils.PlaceholderUtils.parseToComponent((String)s)).collect(java.util.stream.Collectors.toList()));
            head.setItemMeta(meta);

            inv.setItem(slot++, head);
            if (slot > 35) break; // Limite de 27 membres pour l'UI simple
        }

        // Bouton Paramètres si leader
        if (isLeader) {
            ItemStack settings = new ItemStack(Material.REPEATER);
            ItemMeta smeta = settings.getItemMeta();
            smeta.displayName(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<aqua>Paramètres de Verrouillage"));
            List<String> slore = new ArrayList<>();
            slore.add("<gray>Auto-verrouiller les coffres posés");
            slore.add("<gray>pour les membres de l'équipe :");
            slore.add(team.isAutoLock() ? "<green><bold>ACTIVÉ" : "<red><bold>DÉSACTIVÉ");
            slore.add("");
            slore.add("<yellow>Clic pour changer");
            smeta.lore(java.util.Optional.ofNullable(slore).orElse(java.util.Collections.emptyList()).stream().map(s -> fr.gens.core.utils.PlaceholderUtils.parseToComponent((String)s)).collect(java.util.stream.Collectors.toList()));
            settings.setItemMeta(smeta);
            inv.setItem(40, settings);
        }

        // Quitter la team
        ItemStack leave = new ItemStack(Material.BARRIER);
        ItemMeta lmeta = leave.getItemMeta();
        lmeta.displayName(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<red>Quitter la guilde"));
        leave.setItemMeta(lmeta);
        inv.setItem(44, leave);

        player.openInventory(inv);
    }

    public void openTeamQuestGui(Player player, TeamData team) {
        fr.gens.core.modules.teams.TeamQuestManager tqm = plugin.getTeamQuestManager();
        if (tqm == null) {
            player.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<red>Quêtes de guilde indisponibles."));
            return;
        }

        if (fr.gens.core.utils.FloodgateUtil.isBedrockPlayer(player.getUniqueId())) {
            java.util.List<fr.gens.core.utils.BedrockFormManager.BedrockButton> buttons = new java.util.ArrayList<>();
            int progress = tqm.getProgress(team.getTeamId());
            int goal = tqm.getGoal();
            String status = progress >= goal ? "§2[TERMINÉE]" : "Progression: " + progress + " / " + goal;
            
            String btnText = "§6Quête Hebdomadaire\n" + status;
            buttons.add(new fr.gens.core.utils.BedrockFormManager.BedrockButton(btnText, Material.NETHER_STAR, p -> openTeamQuestGui(p, team)));
            
            String ptsText = "§ePoints de Guilde\n§8Hebdo: " + team.getWeeklyPoints() + " | Total: " + team.getTotalPoints();
            buttons.add(new fr.gens.core.utils.BedrockFormManager.BedrockButton(ptsText, Material.SUNFLOWER, p -> openTeamQuestGui(p, team)));
            
            fr.gens.core.utils.BedrockFormManager.openSimpleForm(player, "Quête de Guilde", "Objectif actuel :\n" + tqm.getDesc(), buttons);
            return;
        }

        org.bukkit.inventory.Inventory inv = org.bukkit.Bukkit.createInventory(null, 27, fr.gens.core.utils.PlaceholderUtils.parseToComponent("<blue><bold>Quête de Guilde"));
        
        ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        org.bukkit.inventory.meta.ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.displayName(fr.gens.core.utils.PlaceholderUtils.parseToComponent(" "));
        glass.setItemMeta(glassMeta);
        for (int i = 0; i < 27; i++) inv.setItem(i, glass);
        
        if (tqm != null) {
            ItemStack questItem = new ItemStack(Material.NETHER_STAR);
            org.bukkit.inventory.meta.ItemMeta qmeta = questItem.getItemMeta();
            qmeta.displayName(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<gold><bold>Quête Hebdomadaire"));
            java.util.List<String> lore = new java.util.ArrayList<>();
            lore.add("<gray>" + tqm.getDesc());
            lore.add("");
            int progress = tqm.getProgress(team.getTeamId());
            int goal = tqm.getGoal();
            if (progress >= goal) {
                lore.add("<green><bold>TERMINÉE !");
            } else {
                lore.add("<yellow>Progression: <white>" + progress + " <yellow>/ <white>" + goal);
            }
            qmeta.lore(java.util.Optional.ofNullable(lore).orElse(java.util.Collections.emptyList()).stream().map(s -> fr.gens.core.utils.PlaceholderUtils.parseToComponent((String)s)).collect(java.util.stream.Collectors.toList()));
            questItem.setItemMeta(qmeta);
            inv.setItem(13, questItem);
            
            // Add points info
            ItemStack pointsItem = new ItemStack(Material.SUNFLOWER);
            org.bukkit.inventory.meta.ItemMeta pmeta = pointsItem.getItemMeta();
            pmeta.displayName(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<yellow>Points de Guilde"));
            java.util.List<String> plore = new java.util.ArrayList<>();
            plore.add("<gray>Hebdomadaire : <white>" + team.getWeeklyPoints());
            plore.add("<gray>Total : <white>" + team.getTotalPoints());
            pmeta.lore(java.util.Optional.ofNullable(plore).orElse(java.util.Collections.emptyList()).stream().map(s -> fr.gens.core.utils.PlaceholderUtils.parseToComponent((String)s)).collect(java.util.stream.Collectors.toList()));
            pointsItem.setItemMeta(pmeta);
            inv.setItem(22, pointsItem);
        }
        
        player.openInventory(inv);
    }
}



