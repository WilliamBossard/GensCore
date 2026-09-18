package fr.gens.core.modules.teams;

import fr.gens.core.CorePlugin;
import fr.gens.core.modules.BedrockSkinModule;
import fr.gens.core.modules.BlueMapModule;
import fr.gens.core.modules.EconomyModule;
import fr.gens.core.utils.BedrockFormManager;
import fr.gens.core.utils.FloodgateUtil;
import fr.gens.core.utils.PlaceholderUtils;
import net.kyori.adventure.text.Component;
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
        EconomyModule ecoMod = (EconomyModule) plugin.getModuleManager().getModule("economy");
        boolean ecoEnabled = (ecoMod != null && ecoMod.isEnabled());
        TeamClaimManager claimMgr = plugin.getTeamManager().getClaimManager();
        int currentClaims = claimMgr != null ? claimMgr.getClaimsCount(team.getTeamId()) : 0;
        int maxClaims = team.getMaxClaims();
        OfflinePlayer leaderOp = Bukkit.getOfflinePlayer(team.getLeaderUuid());
        String leaderName = leaderOp.getName() != null ? leaderOp.getName() : "Inconnu";

        if (FloodgateUtil.isBedrockPlayer(player.getUniqueId())) {
            List<BedrockFormManager.BedrockButton> buttons = new ArrayList<>();

            buttons.add(new BedrockFormManager.BedrockButton("§6Améliorations de Guilde\n§r§8Bonus permanents", Material.NETHER_STAR, p -> {
                openTeamUpgradesGui(p, team);
            }));

            String bankSub = ecoEnabled ? String.format("%.2f $", team.getBankBalance()) : team.getBankXp() + " Niveaux XP";
            buttons.add(new BedrockFormManager.BedrockButton("§aBanque de Guilde\n§r§8Solde: " + bankSub, ecoEnabled ? Material.GOLD_INGOT : Material.EXPERIENCE_BOTTLE, p -> {
                openTeamBankBedrockForm(p, team);
            }));

            buttons.add(new BedrockFormManager.BedrockButton("§bQuêtes de Guilde\n§r§8Défis en coop", Material.ENCHANTED_BOOK, p -> {
                openTeamQuestGui(p, team);
            }));

            String claimCost = ecoEnabled ? (TeamClaimManager.CLAIM_COST_MONEY + " $") : (TeamClaimManager.CLAIM_COST_XP + " XP");
            buttons.add(new BedrockFormManager.BedrockButton("§9Revendiquer ce Chunk\n§r§8" + currentClaims + "/" + maxClaims + " (" + claimCost + ")", Material.GRASS_BLOCK, p -> {
                p.performCommand("team claim");
            }));

            if (isLeader) {
                buttons.add(new BedrockFormManager.BedrockButton("§dCouleur BlueMap\n§r§8Couleur: " + team.getColor(), Material.PAINTING, p -> {
                    openTeamColorBedrockForm(p, team);
                }));

                buttons.add(new BedrockFormManager.BedrockButton("§eAuto-verrouillage\n§r§8" + (team.isAutoLock() ? "ACTIVÉ" : "DÉSACTIVÉ"), Material.REPEATER, p -> {
                    team.setAutoLock(!team.isAutoLock());
                    openTeamGui(p);
                }));
            }

            for (UUID memberUuid : team.getMembers()) {
                OfflinePlayer op = Bukkit.getOfflinePlayer(memberUuid);
                String role = team.getLeaderUuid().equals(memberUuid) ? "§6Chef" : "§7Membre";
                String pName = op.getName() != null ? op.getName() : "Inconnu";
                BedrockSkinModule skinModule = (BedrockSkinModule) plugin.getModuleManager().getModule("bedrockskin");
                String headUrl = (skinModule != null) ? skinModule.getHeadUrl(memberUuid, pName) : "https://minotar.net/helm/Steve/64.png";
                buttons.add(new BedrockFormManager.BedrockButton("§e" + pName + "\n§r" + role, headUrl, p -> {
                    if (isLeader && !team.getLeaderUuid().equals(memberUuid)) {
                        team.removeMember(memberUuid);
                        p.sendMessage(PlaceholderUtils.parseToComponent("<green>Joueur expulsé de l'équipe."));
                        openTeamGui(p);
                    }
                }));
            }

            buttons.add(new BedrockFormManager.BedrockButton("§cQuitter / Dissoudre\n§r§8Attention !", Material.BARRIER, p -> {
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

            String content = "Chef : " + leaderName +
                    "\nMembres : " + team.getMembers().size() + " / " + team.getMaxMembers() +
                    "\nTerritoire : " + currentClaims + " / " + maxClaims + " chunks" +
                    "\nBanque : " + bankSub +
                    "\nCouleur 3D (BlueMap) : " + team.getColor();

            BedrockFormManager.openSimpleForm(player, "Guilde : " + team.getName(), content, buttons);
            return;
        }

        TeamGuiHolder holder = new TeamGuiHolder();
        Inventory inv = Bukkit.createInventory(holder, 45, PlaceholderUtils.parseToComponent("<dark_gray>Guilde : " + team.getName()));
        holder.setInventory(inv);

        // Décor bordures
        ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.displayName(PlaceholderUtils.parseToComponent(" "));
        glass.setItemMeta(glassMeta);

        // Ligne 0 : Vitres + Ecu / Bannière récapitulative au centre (slot 4)
        for (int i = 0; i < 9; i++) inv.setItem(i, glass);

        ItemStack overview = new ItemStack(Material.SHIELD);
        ItemMeta oMeta = overview.getItemMeta();
        oMeta.displayName(PlaceholderUtils.parseToComponent("<gold><bold>Guilde : " + team.getName()));
        List<String> oLore = new ArrayList<>();
        oLore.add("<gray>Chef de Guilde : <yellow>" + leaderName);
        oLore.add("<gray>Membres : <white>" + team.getMembers().size() + " / " + team.getMaxMembers());
        oLore.add("<gray>Territoire : <white>" + currentClaims + " / " + maxClaims + " chunks");
        oLore.add("<gray>Banque commune : <gold>" + (ecoEnabled ? String.format("%.2f $", team.getBankBalance()) : team.getBankXp() + " Niveaux XP"));
        oLore.add("<gray>Points Hebdomadaires : <aqua>" + team.getWeeklyPoints() + " pts <dark_gray>(Total: " + team.getTotalPoints() + ")");
        oLore.add("<gray>Couleur BlueMap : <white>" + team.getColor());
        oLore.add("<gray>Auto-verrouillage : " + (team.isAutoLock() ? "<green>Activé" : "<red>Désactivé"));
        oMeta.lore(parseLore(oLore));
        overview.setItemMeta(oMeta);
        inv.setItem(4, overview);

        // Ligne 4 : Vitres de base
        for (int i = 36; i < 45; i++) inv.setItem(i, glass);

        // Membres (slots 9 à 35)
        int slot = 9;
        for (UUID memberUuid : team.getMembers()) {
            OfflinePlayer op = Bukkit.getOfflinePlayer(memberUuid);
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.setPlayerProfile(op.getPlayerProfile());
            meta.displayName(PlaceholderUtils.parseToComponent("<yellow>" + (op.getName() != null ? op.getName() : "Joueur Inconnu")));
            List<String> lore = new ArrayList<>();
            if (team.getLeaderUuid().equals(memberUuid)) {
                lore.add("<gold>★ Chef de Guilde");
            } else {
                lore.add("<gray>Membre");
                if (isLeader) {
                    lore.add("");
                    lore.add("<red>Clic droit pour exclure");
                }
            }
            meta.lore(parseLore(lore));
            head.setItemMeta(meta);

            inv.setItem(slot++, head);
            if (slot > 35) break;
        }

        // Bouton Quêtes
        ItemStack quests = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta qmeta = quests.getItemMeta();
        qmeta.displayName(PlaceholderUtils.parseToComponent("<aqua><bold>Quêtes de Guilde"));
        List<String> qlore = new ArrayList<>();
        qlore.add("<gray>Accomplissez des défis en coopération");
        qlore.add("<gray>pour gagner des Points de Guilde !");
        qlore.add("");
        qlore.add("<yellow>Clic pour ouvrir les quêtes");
        qmeta.lore(parseLore(qlore));
        quests.setItemMeta(qmeta);

        // Bouton Améliorations de Guilde
        ItemStack upgrades = new ItemStack(Material.NETHER_STAR);
        ItemMeta upgMeta = upgrades.getItemMeta();
        upgMeta.displayName(PlaceholderUtils.parseToComponent("<gold><bold>Améliorations de Guilde"));
        List<String> upgLore = new ArrayList<>();
        upgLore.add("<gray>Débloquez des bonus permanents pour votre guilde !");
        upgLore.add("<gray>Capacité, claims, bonus métiers, taxe HDV et quêtes.");
        upgLore.add("");
        upgLore.add("<yellow>Clic pour ouvrir les améliorations");
        upgMeta.lore(parseLore(upgLore));
        upgrades.setItemMeta(upgMeta);

        // Bouton Banque de Guilde
        ItemStack bank = new ItemStack(ecoEnabled ? Material.GOLD_INGOT : Material.EXPERIENCE_BOTTLE);
        ItemMeta bankMeta = bank.getItemMeta();
        bankMeta.displayName(PlaceholderUtils.parseToComponent("<green><bold>Banque de Guilde"));
        List<String> bankLore = new ArrayList<>();
        if (ecoEnabled) {
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
        bankMeta.lore(parseLore(bankLore));
        bank.setItemMeta(bankMeta);

        // Bouton Territoire de Guilde (Claims)
        ItemStack claimItem = new ItemStack(Material.GRASS_BLOCK);
        ItemMeta claimItemMeta = claimItem.getItemMeta();
        claimItemMeta.displayName(PlaceholderUtils.parseToComponent("<blue><bold>Territoire de Guilde"));
        List<String> claimLore = new ArrayList<>();
        claimLore.add("<gray>Chunks revendiqués : <white>" + currentClaims + " / " + maxClaims);
        if (ecoEnabled) {
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
        claimItemMeta.lore(parseLore(claimLore));
        claimItem.setItemMeta(claimItemMeta);

        if (isLeader) {
            // Disposition chef de guilde (7 boutons bien répartis sur la rangée inférieure)
            inv.setItem(37, quests);
            inv.setItem(38, upgrades);
            inv.setItem(39, bank);
            inv.setItem(40, claimItem);

            // Bouton Couleur BlueMap
            ItemStack colorItem = new ItemStack(Material.PAINTING);
            ItemMeta colorMeta = colorItem.getItemMeta();
            colorMeta.displayName(PlaceholderUtils.parseToComponent("<light_purple><bold>Couleur BlueMap"));
            List<String> colorLore = new ArrayList<>();
            colorLore.add("<gray>Couleur actuelle : <white>" + team.getColor());
            colorLore.add("<gray>Personnalisez l'affichage de vos");
            colorLore.add("<gray>chunks sur la carte en ligne 3D.");
            colorLore.add("");
            colorLore.add("<yellow>Clic pour choisir une couleur");
            colorMeta.lore(parseLore(colorLore));
            colorItem.setItemMeta(colorMeta);
            inv.setItem(41, colorItem);

            // Paramètres Auto-lock
            ItemStack settings = new ItemStack(Material.REPEATER);
            ItemMeta smeta = settings.getItemMeta();
            smeta.displayName(PlaceholderUtils.parseToComponent("<yellow><bold>Auto-verrouillage"));
            List<String> slore = new ArrayList<>();
            slore.add("<gray>Auto-verrouiller les coffres posés");
            slore.add("<gray>pour les membres de l'équipe :");
            slore.add(team.isAutoLock() ? "<green><bold>ACTIVÉ" : "<red><bold>DÉSACTIVÉ");
            slore.add("");
            slore.add("<yellow>Clic pour basculer");
            smeta.lore(parseLore(slore));
            settings.setItemMeta(smeta);
            inv.setItem(42, settings);

            // Dissoudre
            ItemStack leave = new ItemStack(Material.BARRIER);
            ItemMeta lmeta = leave.getItemMeta();
            lmeta.displayName(PlaceholderUtils.parseToComponent("<red><bold>Dissoudre la guilde"));
            List<String> llore = new ArrayList<>();
            llore.add("<gray>Supprime définitivement votre guilde.");
            llore.add("<dark_red>Action irréversible !");
            lmeta.lore(parseLore(llore));
            leave.setItemMeta(lmeta);
            inv.setItem(44, leave);
        } else {
            // Disposition membre régulier (4 boutons centrés)
            inv.setItem(38, quests);
            inv.setItem(39, upgrades);
            inv.setItem(40, bank);
            inv.setItem(41, claimItem);

            // Quitter la team
            ItemStack leave = new ItemStack(Material.BARRIER);
            ItemMeta lmeta = leave.getItemMeta();
            lmeta.displayName(PlaceholderUtils.parseToComponent("<red><bold>Quitter la guilde"));
            List<String> llore = new ArrayList<>();
            llore.add("<gray>Quitter cette équipe.");
            lmeta.lore(parseLore(llore));
            leave.setItemMeta(lmeta);
            inv.setItem(44, leave);
        }

        plugin.getFoliaLib().getScheduler().runAtEntity(player, task -> player.openInventory(inv));
    }

    public void openTeamQuestGui(Player player, TeamData team) {
        TeamQuestManager tqm = plugin.getTeamQuestManager();
        if (tqm == null) {
            player.sendMessage(PlaceholderUtils.parseToComponent("<red>Quêtes de guilde indisponibles."));
            return;
        }

        if (FloodgateUtil.isBedrockPlayer(player.getUniqueId())) {
            List<BedrockFormManager.BedrockButton> buttons = new ArrayList<>();
            int progress = tqm.getProgress(team.getTeamId());
            int goal = tqm.getGoal();
            String status = progress >= goal ? "§2[TERMINÉE]" : "Progression: " + progress + " / " + goal;

            buttons.add(new BedrockFormManager.BedrockButton("§6Quête Hebdomadaire\n" + status, Material.NETHER_STAR, p -> openTeamQuestGui(p, team)));
            buttons.add(new BedrockFormManager.BedrockButton("§ePoints de Guilde\n§8Hebdo: " + team.getWeeklyPoints() + " | Total: " + team.getTotalPoints(), Material.SUNFLOWER, p -> openTeamQuestGui(p, team)));
            buttons.add(new BedrockFormManager.BedrockButton("§cRetour au menu de guilde\n§r§8Retour", Material.ARROW, this::openTeamGui));

            String content = "Objectif actuel :\n" + tqm.getDesc() +
                    "\n\nProgression : " + progress + " / " + goal +
                    "\nMultiplicateur de gains : " + String.format("%.2f", team.getQuestPointsMultiplier()) + "x";

            BedrockFormManager.openSimpleForm(player, "Quête de Guilde", content, buttons);
            return;
        }

        TeamQuestGuiHolder holder = new TeamQuestGuiHolder();
        Inventory inv = Bukkit.createInventory(holder, 27, PlaceholderUtils.parseToComponent("<blue><bold>Quêtes de Guilde"));
        holder.setInventory(inv);

        ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.displayName(PlaceholderUtils.parseToComponent(" "));
        glass.setItemMeta(glassMeta);
        for (int i = 0; i < 27; i++) inv.setItem(i, glass);

        ItemStack questItem = new ItemStack(Material.NETHER_STAR);
        ItemMeta qmeta = questItem.getItemMeta();
        qmeta.displayName(PlaceholderUtils.parseToComponent("<gold><bold>Quête Hebdomadaire"));
        List<String> lore = new ArrayList<>();
        lore.add("<gray>" + tqm.getDesc());
        lore.add("");
        int progress = tqm.getProgress(team.getTeamId());
        int goal = tqm.getGoal();
        if (progress >= goal) {
            lore.add("<green><bold>✔ OBJECTIF ATTEINT !");
        } else {
            lore.add("<yellow>Progression : <white>" + progress + " <yellow>/ <white>" + goal);
        }
        lore.add("<gray>Multiplicateur actif : <aqua>" + String.format("%.2f", team.getQuestPointsMultiplier()) + "x");
        qmeta.lore(parseLore(lore));
        questItem.setItemMeta(qmeta);
        inv.setItem(11, questItem);

        ItemStack pointsItem = new ItemStack(Material.SUNFLOWER);
        ItemMeta pmeta = pointsItem.getItemMeta();
        pmeta.displayName(PlaceholderUtils.parseToComponent("<yellow><bold>Points de Guilde"));
        List<String> plore = new ArrayList<>();
        plore.add("<gray>Points Hebdomadaires : <white>" + team.getWeeklyPoints() + " pts");
        plore.add("<gray>Points Totaux : <white>" + team.getTotalPoints() + " pts");
        pmeta.lore(parseLore(plore));
        pointsItem.setItemMeta(pmeta);
        inv.setItem(15, pointsItem);

        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta bMeta = back.getItemMeta();
        bMeta.displayName(PlaceholderUtils.parseToComponent("<red><bold>Retour au menu de guilde"));
        back.setItemMeta(bMeta);
        inv.setItem(22, back);

        plugin.getFoliaLib().getScheduler().runAtEntity(player, task -> player.openInventory(inv));
    }

    public void openTeamUpgradesGui(Player player, TeamData team) {
        if (team == null) return;
        boolean isLeader = team.getLeaderUuid().equals(player.getUniqueId());
        EconomyModule ecoMod = (EconomyModule) plugin.getModuleManager().getModule("economy");
        boolean isEco = (ecoMod != null && ecoMod.isEnabled());

        if (FloodgateUtil.isBedrockPlayer(player.getUniqueId())) {
            List<BedrockFormManager.BedrockButton> buttons = new ArrayList<>();

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

                buttons.add(new BedrockFormManager.BedrockButton(btnText, icon, p -> {
                    if (isMax) {
                        p.sendMessage(PlaceholderUtils.parseToComponent("<yellow>Cette amélioration est déjà au niveau maximum."));
                        openTeamUpgradesGui(p, team);
                        return;
                    }
                    if (!isLeader) {
                        p.sendMessage(PlaceholderUtils.parseToComponent("<red>Seul le chef de guilde peut acheter des améliorations."));
                        openTeamUpgradesGui(p, team);
                        return;
                    }
                    boolean bought = plugin.getTeamManager().buyPerk(team, pKey);
                    if (bought) {
                        p.sendMessage(PlaceholderUtils.parseToComponent("<green>Amélioration " + pName + " achetée avec succès via la banque de guilde !"));
                    } else {
                        p.sendMessage(PlaceholderUtils.parseToComponent("<red>Fonds insuffisants dans la banque de guilde !"));
                    }
                    openTeamUpgradesGui(p, team);
                }));
            }

            buttons.add(new BedrockFormManager.BedrockButton("§cRetour au menu de guilde\n§r§8Retour", Material.ARROW, this::openTeamGui));

            String bankStr = isEco ? String.format("%.2f $", team.getBankBalance()) : (team.getBankXp() + " Niveaux XP");
            BedrockFormManager.openSimpleForm(player, "Améliorations de Guilde", "Banque de Guilde : " + bankStr + "\nSélectionnez une amélioration à débloquer :", buttons);
            return;
        }

        TeamUpgradesGuiHolder holder = new TeamUpgradesGuiHolder();
        Inventory inv = Bukkit.createInventory(holder, 27, PlaceholderUtils.parseToComponent("<gold><bold>Améliorations de Guilde"));
        holder.setInventory(inv);

        ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.displayName(PlaceholderUtils.parseToComponent(" "));
        glass.setItemMeta(glassMeta);
        for (int i = 0; i < 27; i++) inv.setItem(i, glass);

        // Slot 4 : Rappel du solde en banque
        ItemStack bankInfo = new ItemStack(isEco ? Material.GOLD_INGOT : Material.EXPERIENCE_BOTTLE);
        ItemMeta biMeta = bankInfo.getItemMeta();
        biMeta.displayName(PlaceholderUtils.parseToComponent("<green><bold>Banque de Guilde"));
        List<String> biLore = new ArrayList<>();
        biLore.add("<gray>Fonds disponibles : <gold>" + (isEco ? String.format("%.2f $", team.getBankBalance()) : team.getBankXp() + " Niveaux XP"));
        biLore.add("<dark_gray>Tous les achats sont prélevés sur ce solde.");
        biMeta.lore(parseLore(biLore));
        bankInfo.setItemMeta(biMeta);
        inv.setItem(4, bankInfo);

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
        bMeta.displayName(PlaceholderUtils.parseToComponent("<red><bold>Retour au menu de guilde"));
        back.setItemMeta(bMeta);
        inv.setItem(22, back);

        plugin.getFoliaLib().getScheduler().runAtEntity(player, task -> player.openInventory(inv));
    }

    public void openTeamColorGui(Player player, TeamData team) {
        if (team == null) return;
        boolean isLeader = team.getLeaderUuid().equals(player.getUniqueId());

        if (FloodgateUtil.isBedrockPlayer(player.getUniqueId())) {
            openTeamColorBedrockForm(player, team);
            return;
        }

        TeamColorGuiHolder holder = new TeamColorGuiHolder();
        Inventory inv = Bukkit.createInventory(holder, 27, PlaceholderUtils.parseToComponent("<dark_purple><bold>Couleur BlueMap"));
        holder.setInventory(inv);

        ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.displayName(PlaceholderUtils.parseToComponent(" "));
        glass.setItemMeta(glassMeta);
        for (int i = 0; i < 27; i++) inv.setItem(i, glass);

        ItemStack info = new ItemStack(Material.PAINTING);
        ItemMeta iMeta = info.getItemMeta();
        iMeta.displayName(PlaceholderUtils.parseToComponent("<light_purple><bold>Couleur du Territoire sur BlueMap"));
        List<String> iLore = new ArrayList<>();
        iLore.add("<gray>Couleur active : <white>" + team.getColor());
        iLore.add("<gray>Cette teinte colore la bordure et la surface");
        iLore.add("<gray>de tous vos chunks sur la carte 3D BlueMap.");
        iLore.add("");
        if (!isLeader) {
            iLore.add("<red>Seul le chef de guilde peut modifier la couleur.");
        }
        iMeta.lore(parseLore(iLore));
        info.setItemMeta(iMeta);
        inv.setItem(4, info);

        ColorEntry[] colors = getColorPalette();
        for (int i = 0; i < colors.length; i++) {
            ColorEntry c = colors[i];
            ItemStack wool = new ItemStack(c.material);
            ItemMeta wMeta = wool.getItemMeta();
            wMeta.displayName(PlaceholderUtils.parseToComponent(c.colorName));
            List<String> wLore = new ArrayList<>();
            wLore.add("<gray>Code HEX : <white>" + c.hex);
            wLore.add("");
            if (team.getColor().equalsIgnoreCase(c.hex)) {
                wLore.add("<green><bold>✔ COULEUR ACTUELLE");
            } else if (isLeader) {
                wLore.add("<yellow>Clic pour sélectionner");
            } else {
                wLore.add("<red>Réservé au chef");
            }
            wMeta.lore(parseLore(wLore));
            wool.setItemMeta(wMeta);
            inv.setItem(10 + i, wool);
        }

        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta bMeta = back.getItemMeta();
        bMeta.displayName(PlaceholderUtils.parseToComponent("<red><bold>Retour au menu de guilde"));
        back.setItemMeta(bMeta);
        inv.setItem(22, back);

        plugin.getFoliaLib().getScheduler().runAtEntity(player, task -> player.openInventory(inv));
    }

    public void openTeamColorBedrockForm(Player player, TeamData team) {
        if (team == null) return;
        boolean isLeader = team.getLeaderUuid().equals(player.getUniqueId());

        List<BedrockFormManager.BedrockButton> buttons = new ArrayList<>();
        ColorEntry[] colors = getColorPalette();

        for (ColorEntry c : colors) {
            boolean active = team.getColor().equalsIgnoreCase(c.hex);
            String label = (active ? "§a[ACTIF] " : "") + c.bedrockName + "\n§r§8" + c.hex;
            buttons.add(new BedrockFormManager.BedrockButton(label, c.material, p -> {
                if (!isLeader) {
                    p.sendMessage(PlaceholderUtils.parseToComponent("<red>Seul le chef de guilde peut changer la couleur du territoire."));
                    openTeamColorBedrockForm(p, team);
                    return;
                }
                team.setColor(c.hex);
                plugin.getTeamManager().saveColorAsync(team);
                BlueMapModule bmm = (BlueMapModule) plugin.getModuleManager().getModule("bluemap");
                if (bmm != null && bmm.isEnabled()) {
                    bmm.updateAllTeamTerritories();
                }
                p.sendMessage(PlaceholderUtils.parseToComponent("<green>Couleur de guilde mise à jour : <yellow>" + c.hex));
                openTeamGui(p);
            }));
        }

        buttons.add(new BedrockFormManager.BedrockButton("§cRetour au menu de guilde\n§r§8Retour", Material.ARROW, this::openTeamGui));

        String content = "Couleur actuelle : " + team.getColor() +
                "\nChoisissez la couleur de vos chunks visibles sur BlueMap :";

        BedrockFormManager.openSimpleForm(player, "Couleur BlueMap", content, buttons);
    }

    public void openTeamBankBedrockForm(Player player, TeamData team) {
        if (team == null) return;
        boolean isLeader = team.getLeaderUuid().equals(player.getUniqueId());
        EconomyModule eco = (EconomyModule) plugin.getModuleManager().getModule("economy");
        boolean isEco = (eco != null && eco.isEnabled());

        List<BedrockFormManager.BedrockButton> buttons = new ArrayList<>();

        if (isEco) {
            double playerBal = eco.getBalance(player.getUniqueId());
            String content = "§7Solde de la banque : §6" + String.format("%.2f $", team.getBankBalance()) +
                    "\n§7Votre solde personnel : §e" + String.format("%.2f $", playerBal) +
                    "\n\n§8Sélectionnez une opération bancaire :";

            // Dépôts
            buttons.add(new BedrockFormManager.BedrockButton("§aDéposer 100 $\n§r§8Verser dans la banque", Material.GOLD_INGOT, p -> {
                handleDepositMoney(p, team, 100.0);
                openTeamBankBedrockForm(p, team);
            }));

            buttons.add(new BedrockFormManager.BedrockButton("§aDéposer 500 $\n§r§8Verser dans la banque", Material.GOLD_INGOT, p -> {
                handleDepositMoney(p, team, 500.0);
                openTeamBankBedrockForm(p, team);
            }));

            buttons.add(new BedrockFormManager.BedrockButton("§aDéposer 2,500 $\n§r§8Verser dans la banque", Material.GOLD_INGOT, p -> {
                handleDepositMoney(p, team, 2500.0);
                openTeamBankBedrockForm(p, team);
            }));

            buttons.add(new BedrockFormManager.BedrockButton("§aDéposer TOUT\n§r§8Tout votre portefeuille", Material.GOLD_BLOCK, p -> {
                double curBal = eco.getBalance(p.getUniqueId());
                if (curBal > 0) {
                    handleDepositMoney(p, team, curBal);
                } else {
                    p.sendMessage(PlaceholderUtils.parseToComponent("<red>Votre portefeuille est vide."));
                }
                openTeamBankBedrockForm(p, team);
            }));

            if (isLeader) {
                buttons.add(new BedrockFormManager.BedrockButton("§eRetirer 100 $\n§r§8Prendre de la banque", Material.IRON_INGOT, p -> {
                    handleWithdrawMoney(p, team, 100.0);
                    openTeamBankBedrockForm(p, team);
                }));

                buttons.add(new BedrockFormManager.BedrockButton("§eRetirer 500 $\n§r§8Prendre de la banque", Material.IRON_INGOT, p -> {
                    handleWithdrawMoney(p, team, 500.0);
                    openTeamBankBedrockForm(p, team);
                }));

                buttons.add(new BedrockFormManager.BedrockButton("§eRetirer 2,500 $\n§r§8Prendre de la banque", Material.IRON_INGOT, p -> {
                    handleWithdrawMoney(p, team, 2500.0);
                    openTeamBankBedrockForm(p, team);
                }));
            }

            buttons.add(new BedrockFormManager.BedrockButton("§cRetour au menu de guilde\n§r§8Retour", Material.ARROW, this::openTeamGui));
            BedrockFormManager.openSimpleForm(player, "Banque de Guilde", content, buttons);
        } else {
            int playerLvl = player.getLevel();
            String content = "§7Solde de la banque : §a" + team.getBankXp() + " Niveaux XP" +
                    "\n§7Vos niveaux d'XP : §e" + playerLvl + " Niveaux XP" +
                    "\n\n§8Sélectionnez une opération bancaire :";

            buttons.add(new BedrockFormManager.BedrockButton("§aDéposer 5 Niveaux XP\n§r§8Verser dans la banque", Material.EXPERIENCE_BOTTLE, p -> {
                handleDepositXp(p, team, 5);
                openTeamBankBedrockForm(p, team);
            }));

            buttons.add(new BedrockFormManager.BedrockButton("§aDéposer 10 Niveaux XP\n§r§8Verser dans la banque", Material.EXPERIENCE_BOTTLE, p -> {
                handleDepositXp(p, team, 10);
                openTeamBankBedrockForm(p, team);
            }));

            buttons.add(new BedrockFormManager.BedrockButton("§aDéposer 25 Niveaux XP\n§r§8Verser dans la banque", Material.EXPERIENCE_BOTTLE, p -> {
                handleDepositXp(p, team, 25);
                openTeamBankBedrockForm(p, team);
            }));

            buttons.add(new BedrockFormManager.BedrockButton("§aDéposer TOUT XP\n§r§8Tous vos niveaux", Material.EXPERIENCE_BOTTLE, p -> {
                int curLvl = p.getLevel();
                if (curLvl > 0) {
                    handleDepositXp(p, team, curLvl);
                } else {
                    p.sendMessage(PlaceholderUtils.parseToComponent("<red>Vous n'avez aucun niveau d'XP."));
                }
                openTeamBankBedrockForm(p, team);
            }));

            if (isLeader) {
                buttons.add(new BedrockFormManager.BedrockButton("§eRetirer 5 Niveaux XP\n§r§8Prendre de la banque", Material.GLASS_BOTTLE, p -> {
                    handleWithdrawXp(p, team, 5);
                    openTeamBankBedrockForm(p, team);
                }));

                buttons.add(new BedrockFormManager.BedrockButton("§eRetirer 10 Niveaux XP\n§r§8Prendre de la banque", Material.GLASS_BOTTLE, p -> {
                    handleWithdrawXp(p, team, 10);
                    openTeamBankBedrockForm(p, team);
                }));

                buttons.add(new BedrockFormManager.BedrockButton("§eRetirer 25 Niveaux XP\n§r§8Prendre de la banque", Material.GLASS_BOTTLE, p -> {
                    handleWithdrawXp(p, team, 25);
                    openTeamBankBedrockForm(p, team);
                }));
            }

            buttons.add(new BedrockFormManager.BedrockButton("§cRetour au menu de guilde\n§r§8Retour", Material.ARROW, this::openTeamGui));
            BedrockFormManager.openSimpleForm(player, "Banque de Guilde", content, buttons);
        }
    }

    private void handleDepositMoney(Player player, TeamData team, double amount) {
        EconomyModule eco = (EconomyModule) plugin.getModuleManager().getModule("economy");
        if (eco == null || !eco.isEnabled()) return;

        if (eco.takeMoneyAtomic(player.getUniqueId(), amount)) {
            team.addBankBalance(amount);
            plugin.getTeamManager().saveBankAsync(team);
            player.sendMessage(PlaceholderUtils.parseToComponent("<green>Vous avez déposé <gold>" + String.format("%.2f", amount) + " $</gold> dans la banque de guilde."));
        } else {
            player.sendMessage(PlaceholderUtils.parseToComponent("<red>Fonds personnels insuffisants."));
        }
    }

    private void handleWithdrawMoney(Player player, TeamData team, double amount) {
        EconomyModule eco = (EconomyModule) plugin.getModuleManager().getModule("economy");
        if (eco == null || !eco.isEnabled()) return;

        if (team.withdrawBankBalance(amount)) {
            eco.giveMoney(player.getUniqueId(), amount);
            plugin.getTeamManager().saveBankAsync(team);
            player.sendMessage(PlaceholderUtils.parseToComponent("<green>Vous avez retiré <gold>" + String.format("%.2f", amount) + " $</gold> de la banque de guilde."));
        } else {
            player.sendMessage(PlaceholderUtils.parseToComponent("<red>Fonds insuffisants dans la banque de guilde."));
        }
    }

    private void handleDepositXp(Player player, TeamData team, int levels) {
        if (player.getLevel() >= levels) {
            player.setLevel(player.getLevel() - levels);
            team.addBankXp(levels);
            plugin.getTeamManager().saveBankAsync(team);
            player.sendMessage(PlaceholderUtils.parseToComponent("<green>Vous avez déposé <aqua>" + levels + " niveaux d'XP</aqua> dans la banque de guilde."));
        } else {
            player.sendMessage(PlaceholderUtils.parseToComponent("<red>Niveaux d'XP insuffisants."));
        }
    }

    private void handleWithdrawXp(Player player, TeamData team, int levels) {
        if (team.withdrawBankXp(levels)) {
            player.setLevel(player.getLevel() + levels);
            plugin.getTeamManager().saveBankAsync(team);
            player.sendMessage(PlaceholderUtils.parseToComponent("<green>Vous avez retiré <aqua>" + levels + " niveaux d'XP</aqua> de la banque de guilde."));
        } else {
            player.sendMessage(PlaceholderUtils.parseToComponent("<red>Niveaux d'XP insuffisants dans la banque de guilde."));
        }
    }

    public static class ColorEntry {
        public final Material material;
        public final String hex;
        public final String colorName;
        public final String bedrockName;

        public ColorEntry(Material material, String hex, String colorName, String bedrockName) {
            this.material = material;
            this.hex = hex;
            this.colorName = colorName;
            this.bedrockName = bedrockName;
        }
    }

    public static ColorEntry[] getColorPalette() {
        return new ColorEntry[]{
                new ColorEntry(Material.LIME_WOOL, "#2ecc71", "<green><bold>Vert Émeraude", "§aVert Émeraude"),
                new ColorEntry(Material.LIGHT_BLUE_WOOL, "#3498db", "<blue><bold>Bleu Azur", "§bBleu Azur"),
                new ColorEntry(Material.RED_WOOL, "#e74c3c", "<red><bold>Rouge Rubis", "§cRouge Rubis"),
                new ColorEntry(Material.YELLOW_WOOL, "#f1c40f", "<yellow><bold>Jaune Soleil", "§eJaune Soleil"),
                new ColorEntry(Material.PURPLE_WOOL, "#9b59b6", "<dark_purple><bold>Violet Royal", "§5Violet Royal"),
                new ColorEntry(Material.ORANGE_WOOL, "#e67e22", "<gold><bold>Orange Flamboyant", "§6Orange Flamboyant"),
                new ColorEntry(Material.CYAN_WOOL, "#1abc9c", "<aqua><bold>Cyan Lagon", "§3Cyan Lagon"),
                new ColorEntry(Material.MAGENTA_WOOL, "#e91e63", "<light_purple><bold>Rose Magenta", "§dRose Magenta")
        };
    }

    private ItemStack buildPerkItem(String perkKey, String perkName, int curLvl, int maxLvl, Material mat,
                                   String currentEffect, String nextEffect, TeamData team, boolean isEco, boolean isLeader) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(PlaceholderUtils.parseToComponent("<gold><bold>" + perkName + " <yellow>[Niv. " + curLvl + "/" + maxLvl + "]"));
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
        meta.lore(parseLore(lore));
        item.setItemMeta(meta);
        return item;
    }

    private List<Component> parseLore(List<String> lore) {
        if (lore == null || lore.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        List<Component> components = new ArrayList<>(lore.size());
        for (String line : lore) {
            components.add(PlaceholderUtils.parseToComponent(line));
        }
        return components;
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

    public static class TeamColorGuiHolder implements org.bukkit.inventory.InventoryHolder {
        private org.bukkit.inventory.Inventory inventory;
        public void setInventory(org.bukkit.inventory.Inventory inv) { this.inventory = inv; }
        @Override public org.bukkit.inventory.Inventory getInventory() { return inventory; }
    }
}
