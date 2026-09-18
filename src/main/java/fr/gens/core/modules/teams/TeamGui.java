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

        boolean isLeader = team.getLeaderUuid().equals(player.getUniqueId());

        if (fr.gens.core.utils.FloodgateUtil.isBedrockPlayer(player.getUniqueId())) {
            fr.gens.core.modules.EconomyModule ecoMod = (fr.gens.core.modules.EconomyModule) plugin.getModuleManager().getModule("economy");
            boolean ecoEnabled = (ecoMod != null && ecoMod.isEnabled());
            TeamClaimManager claimMgr = plugin.getTeamManager().getClaimManager();
            int currentClaims = claimMgr != null ? claimMgr.getClaimsCount(team.getTeamId()) : 0;

            java.util.List<fr.gens.core.utils.BedrockFormManager.BedrockButton> buttons = new java.util.ArrayList<>();

            buttons.add(new fr.gens.core.utils.BedrockFormManager.BedrockButton("§6Améliorations de Guilde\n§r§8Bonus permanents", org.bukkit.Material.NETHER_STAR, p -> {
                openTeamUpgradesGui(p, team);
            }));

            String bankSub = ecoEnabled ? String.format("%.2f $", team.getBankBalance()) : team.getBankXp() + " Niveaux XP";
            buttons.add(new fr.gens.core.utils.BedrockFormManager.BedrockButton("§aBanque de Guilde\n§r§8Solde: " + bankSub, ecoEnabled ? org.bukkit.Material.GOLD_INGOT : org.bukkit.Material.EXPERIENCE_BOTTLE, p -> {
                p.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<green>[Guilde] Solde de la banque : <yellow>" + bankSub));
                if (ecoEnabled) {
                    p.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<gray>Déposer : <yellow>/team deposit <montant>"));
                    if (isLeader) {
                        p.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<gray>Retirer : <yellow>/team withdraw <montant>"));
                    }
                } else {
                    p.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<gray>Déposer : <yellow>/team depositxp <niveaux>"));
                    if (isLeader) {
                        p.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<gray>Retirer : <yellow>/team withdrawxp <niveaux>"));
                    }
                }
            }));

            String claimCost = ecoEnabled ? (TeamClaimManager.CLAIM_COST_MONEY + " $") : (TeamClaimManager.CLAIM_COST_XP + " XP");
            buttons.add(new fr.gens.core.utils.BedrockFormManager.BedrockButton("§9Revendiquer ce Chunk\n§r§8" + currentClaims + "/" + team.getMaxClaims() + " (" + claimCost + ")", org.bukkit.Material.GRASS_BLOCK, p -> {
                p.performCommand("team claim");
            }));

            buttons.add(new fr.gens.core.utils.BedrockFormManager.BedrockButton("§bQuêtes de Guilde\n§r§8Défis en coop", org.bukkit.Material.ENCHANTED_BOOK, p -> {
                p.performCommand("team quest");
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

        TeamGuiHolder holder = new TeamGuiHolder();
        Inventory inv = Bukkit.createInventory(holder, 45, fr.gens.core.utils.PlaceholderUtils.parseToComponent("<dark_gray>Guilde : " + team.getName()));
        holder.setInventory(inv);

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

        // Bouton Améliorations de Guilde
        ItemStack upgrades = new ItemStack(Material.NETHER_STAR);
        ItemMeta upgMeta = upgrades.getItemMeta();
        upgMeta.displayName(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<gold><bold>Améliorations de Guilde"));
        List<String> upgLore = new ArrayList<>();
        upgLore.add("<gray>Débloquez des bonus permanents pour votre guilde !");
        upgLore.add("<gray>Capacité, claims, bonus métiers, taxe HDV et quêtes.");
        upgLore.add("");
        upgLore.add("<yellow>Clic pour ouvrir le menu des améliorations");
        upgMeta.lore(java.util.Optional.ofNullable(upgLore).orElse(java.util.Collections.emptyList()).stream().map(s -> fr.gens.core.utils.PlaceholderUtils.parseToComponent((String)s)).collect(java.util.stream.Collectors.toList()));
        upgrades.setItemMeta(upgMeta);
        inv.setItem(37, upgrades);

        // Bouton Banque de Guilde
        fr.gens.core.modules.EconomyModule eco = (fr.gens.core.modules.EconomyModule) plugin.getModuleManager().getModule("economy");
        boolean isEco = (eco != null && eco.isEnabled());
        ItemStack bank = new ItemStack(isEco ? Material.GOLD_INGOT : Material.EXPERIENCE_BOTTLE);
        ItemMeta bankMeta = bank.getItemMeta();
        bankMeta.displayName(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<green><bold>Banque de Guilde"));
        List<String> bankLore = new ArrayList<>();
        if (isEco) {
            bankLore.add("<gray>Solde actuel : <gold>" + String.format("%.2f", team.getBankBalance()) + " $");
            bankLore.add("<gray>Utilisez <yellow>/team deposit <montant><gray> pour déposer.");
            if (isLeader) {
                bankLore.add("<gray>Utilisez <yellow>/team withdraw <montant><gray> pour retirer.");
            }
        } else {
            bankLore.add("<gray>Solde actuel : <green>" + team.getBankXp() + " Niveaux d'XP");
            bankLore.add("<gray>Utilisez <yellow>/team depositxp <niveaux><gray> pour déposer.");
            if (isLeader) {
                bankLore.add("<gray>Utilisez <yellow>/team withdrawxp <niveaux><gray> pour retirer.");
            }
        }
        bankLore.add("");
        bankLore.add("<dark_gray>Tous les achats de guilde sont prélevés ici.");
        bankMeta.lore(java.util.Optional.ofNullable(bankLore).orElse(java.util.Collections.emptyList()).stream().map(s -> fr.gens.core.utils.PlaceholderUtils.parseToComponent((String)s)).collect(java.util.stream.Collectors.toList()));
        bank.setItemMeta(bankMeta);
        inv.setItem(38, bank);

        // Bouton Territoire de Guilde (Claims)
        TeamClaimManager claimMgr = plugin.getTeamManager().getClaimManager();
        int currentClaims = claimMgr != null ? claimMgr.getClaimsCount(team.getTeamId()) : 0;
        int maxClaims = team.getMaxClaims();
        ItemStack claimItem = new ItemStack(Material.GRASS_BLOCK);
        ItemMeta claimItemMeta = claimItem.getItemMeta();
        claimItemMeta.displayName(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<blue><bold>Territoire de Guilde"));
        List<String> claimLore = new ArrayList<>();
        claimLore.add("<gray>Chunks revendiqués : <white>" + currentClaims + " / " + maxClaims);
        if (isEco) {
            claimLore.add("<gray>Coût par chunk : <gold>" + TeamClaimManager.CLAIM_COST_MONEY + " $ <gray>(banque)");
        } else {
            claimLore.add("<gray>Coût par chunk : <green>" + TeamClaimManager.CLAIM_COST_XP + " Niveaux XP <gray>(banque)");
        }
        claimLore.add("");
        if (isLeader) {
            claimLore.add("<yellow>Clic pour revendiquer le chunk actuel (/team claim)");
        } else {
            claimLore.add("<gray>Seul le chef de guilde peut revendiquer un chunk.");
        }
        claimItemMeta.lore(java.util.Optional.ofNullable(claimLore).orElse(java.util.Collections.emptyList()).stream().map(s -> fr.gens.core.utils.PlaceholderUtils.parseToComponent((String)s)).collect(java.util.stream.Collectors.toList()));
        claimItem.setItemMeta(claimItemMeta);
        inv.setItem(39, claimItem);

        // Afficher les membres
        int slot = 9;

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

        plugin.getFoliaLib().getScheduler().runAtEntity(player, task -> player.openInventory(inv));
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

        TeamQuestGuiHolder holder = new TeamQuestGuiHolder();
        org.bukkit.inventory.Inventory inv = org.bukkit.Bukkit.createInventory(holder, 27, fr.gens.core.utils.PlaceholderUtils.parseToComponent("<blue><bold>Quête de Guilde"));
        holder.setInventory(inv);
        
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
        
        plugin.getFoliaLib().getScheduler().runAtEntity(player, task -> player.openInventory(inv));
    }

    public void openTeamUpgradesGui(Player player, TeamData team) {
        if (team == null) return;
        boolean isLeader = team.getLeaderUuid().equals(player.getUniqueId());
        fr.gens.core.modules.EconomyModule ecoMod = (fr.gens.core.modules.EconomyModule) plugin.getModuleManager().getModule("economy");
        boolean isEco = (ecoMod != null && ecoMod.isEnabled());

        if (fr.gens.core.utils.FloodgateUtil.isBedrockPlayer(player.getUniqueId())) {
            java.util.List<fr.gens.core.utils.BedrockFormManager.BedrockButton> buttons = new java.util.ArrayList<>();

            String[] perkKeys = {"MEMBERS", "CLAIMS", "JOBS", "AH_TAX", "QUESTS"};
            String[] perkNames = {"Membres Max", "Territoire Étendu", "Bonus Métiers", "Réduction Taxe HDV", "Bonus Quêtes Coop"};
            int[] maxLvls = {3, 4, 3, 2, 2};
            Material[] icons = {Material.CHEST, Material.BEACON, Material.GOLDEN_PICKAXE, Material.GOLD_INGOT, Material.EXPERIENCE_BOTTLE};

            for (int i = 0; i < perkKeys.length; i++) {
                final String pKey = perkKeys[i];
                final String pName = perkNames[i];
                final int curLvl = team.getUpgradeLevel(pKey);
                final int maxLvl = maxLvls[i];
                final Material icon = icons[i];

                double costM = TeamManager.getPerkCostMoney(pKey, curLvl + 1);
                int costX = TeamManager.getPerkCostXp(pKey, curLvl + 1);
                boolean isMax = (curLvl >= maxLvl || (isEco ? costM < 0 : costX < 0));

                String costStr = isMax ? "MAX" : (isEco ? (costM + " $") : (costX + " XP"));
                String btnText = "§6" + pName + " [Niv. " + curLvl + "/" + maxLvl + "]\n§r§8Coût: " + costStr;

                buttons.add(new fr.gens.core.utils.BedrockFormManager.BedrockButton(btnText, icon, p -> {
                    if (isMax) {
                        p.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<yellow>Cette amélioration est déjà au niveau maximum."));
                        openTeamUpgradesGui(p, team);
                        return;
                    }
                    if (!isLeader) {
                        p.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<red>Seul le chef de guilde peut acheter des améliorations."));
                        openTeamUpgradesGui(p, team);
                        return;
                    }
                    boolean bought = plugin.getTeamManager().buyPerk(team, pKey);
                    if (bought) {
                        p.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<green>Amélioration " + pName + " achetée avec succès via la banque de guilde !"));
                    } else {
                        p.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<red>Fonds insuffisants dans la banque de guilde !"));
                    }
                    openTeamUpgradesGui(p, team);
                }));
            }

            buttons.add(new fr.gens.core.utils.BedrockFormManager.BedrockButton("§cRetour\n§r§8Menu de guilde", Material.BARRIER, this::openTeamGui));

            String bankStr = isEco ? String.format("%.2f $", team.getBankBalance()) : (team.getBankXp() + " Niveaux XP");
            fr.gens.core.utils.BedrockFormManager.openSimpleForm(player, "Améliorations de Guilde", "Banque de Guilde : " + bankStr + "\nSélectionnez une amélioration à débloquer :", buttons);
            return;
        }

        TeamUpgradesGuiHolder holder = new TeamUpgradesGuiHolder();
        Inventory inv = Bukkit.createInventory(holder, 27, fr.gens.core.utils.PlaceholderUtils.parseToComponent("<gold><bold>Améliorations de Guilde"));
        holder.setInventory(inv);

        ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.displayName(fr.gens.core.utils.PlaceholderUtils.parseToComponent(" "));
        glass.setItemMeta(glassMeta);
        for (int i = 0; i < 27; i++) inv.setItem(i, glass);

        inv.setItem(11, buildPerkItem("MEMBERS", "Membres Max", team.getUpgradeLevel("MEMBERS"), 3, Material.CHEST,
            "<gray>Membres actuels : <white>" + team.getMaxMembers(),
            "<gray>Prochain niveau : <white>" + (5 + 3 * (team.getUpgradeLevel("MEMBERS") + 1)) + " membres",
            team, isEco, isLeader));

        inv.setItem(12, buildPerkItem("CLAIMS", "Territoire Étendu", team.getUpgradeLevel("CLAIMS"), 4, Material.BEACON,
            "<gray>Claims actuels : <white>" + team.getMaxClaims() + " chunks",
            "<gray>Prochain niveau : <white>" + (4 + 4 * (team.getUpgradeLevel("CLAIMS") + 1)) + " chunks",
            team, isEco, isLeader));

        inv.setItem(13, buildPerkItem("JOBS", "Bonus Métiers", team.getUpgradeLevel("JOBS"), 3, Material.GOLDEN_PICKAXE,
            "<gray>Multiplicateur actuel : <white>" + String.format("%.2f", team.getJobsXpMultiplier()) + "x",
            "<gray>Prochain niveau : <white>" + String.format("%.2f", 1.0 + (0.05 * (team.getUpgradeLevel("JOBS") + 1))) + "x",
            team, isEco, isLeader));

        inv.setItem(14, buildPerkItem("AH_TAX", "Réduction Taxe HDV", team.getUpgradeLevel("AH_TAX"), 2, Material.GOLD_INGOT,
            "<gray>Réduction actuelle : <white>-" + String.format("%.0f", team.getAhTaxReduction() * 100) + "% taxe",
            "<gray>Prochain niveau : <white>-" + String.format("%.0f", (0.25 * (team.getUpgradeLevel("AH_TAX") + 1)) * 100) + "% taxe",
            team, isEco, isLeader));

        inv.setItem(15, buildPerkItem("QUESTS", "Bonus Quêtes Coop", team.getUpgradeLevel("QUESTS"), 2, Material.EXPERIENCE_BOTTLE,
            "<gray>Multiplicateur actuel : <white>" + String.format("%.2f", team.getQuestPointsMultiplier()) + "x",
            "<gray>Prochain niveau : <white>" + String.format("%.2f", (1.0 + 0.10 * (team.getUpgradeLevel("QUESTS") + 1))) + "x",
            team, isEco, isLeader));

        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta bMeta = back.getItemMeta();
        bMeta.displayName(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<red>Retour au menu de guilde"));
        back.setItemMeta(bMeta);
        inv.setItem(22, back);

        plugin.getFoliaLib().getScheduler().runAtEntity(player, task -> player.openInventory(inv));
    }

    private ItemStack buildPerkItem(String perkKey, String perkName, int curLvl, int maxLvl, Material mat,
                                   String currentEffect, String nextEffect, TeamData team, boolean isEco, boolean isLeader) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<gold><bold>" + perkName + " <yellow>[Niv. " + curLvl + "/" + maxLvl + "]"));
        List<String> lore = new ArrayList<>();
        lore.add(currentEffect);
        lore.add("");
        boolean isMax = (curLvl >= maxLvl);
        if (isMax) {
            lore.add("<green><bold>NIVEAU MAXIMUM ATTEINT");
        } else {
            lore.add(nextEffect);
            double costM = TeamManager.getPerkCostMoney(perkKey, curLvl + 1);
            int costX = TeamManager.getPerkCostXp(perkKey, curLvl + 1);
            lore.add("");
            if (isEco) {
                lore.add("<gray>Coût : <gold>" + costM + " $ <gray>(banque de guilde)");
                lore.add("<gray>Solde banque : <white>" + String.format("%.2f", team.getBankBalance()) + " $");
            } else {
                lore.add("<gray>Coût : <green>" + costX + " Niveaux XP <gray>(banque de guilde)");
                lore.add("<gray>Solde banque : <white>" + team.getBankXp() + " Niveaux XP");
            }
            lore.add("");
            if (isLeader) {
                lore.add("<yellow>Clic gauche pour acheter");
            } else {
                lore.add("<red>Seul le chef de guilde peut acheter");
            }
        }
        meta.lore(lore.stream().map(s -> fr.gens.core.utils.PlaceholderUtils.parseToComponent(s)).collect(java.util.stream.Collectors.toList()));
        item.setItemMeta(meta);
        return item;
    }

    public static class TeamGuiHolder implements org.bukkit.inventory.InventoryHolder {
        private org.bukkit.inventory.Inventory inventory;
        public void setInventory(org.bukkit.inventory.Inventory inv) { this.inventory = inv; }
        @Override public org.bukkit.inventory.Inventory getInventory() { return inventory; }
    }

    public static class TeamQuestGuiHolder implements org.bukkit.inventory.InventoryHolder {
        private org.bukkit.inventory.Inventory inventory;
        public void setInventory(org.bukkit.inventory.Inventory inv) { this.inventory = inv; }
        @Override public org.bukkit.inventory.Inventory getInventory() { return inventory; }
    }

    public static class TeamUpgradesGuiHolder implements org.bukkit.inventory.InventoryHolder {
        private org.bukkit.inventory.Inventory inventory;
        public void setInventory(org.bukkit.inventory.Inventory inv) { this.inventory = inv; }
        @Override public org.bukkit.inventory.Inventory getInventory() { return inventory; }
    }
}



