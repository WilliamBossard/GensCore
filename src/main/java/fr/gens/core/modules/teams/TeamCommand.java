package fr.gens.core.modules.teams;

import fr.gens.core.CorePlugin;
import fr.gens.core.utils.PlaceholderUtils;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;


public class TeamCommand {
    private final CorePlugin plugin;
    private final TeamGui teamGui;
    private final TeamModule module;
    private final Map<UUID, UUID> invites = new ConcurrentHashMap<>(); // invited -> teamLeader

    public TeamCommand(CorePlugin plugin, TeamGui teamGui, TeamModule module) {
        this.plugin = plugin;
        this.teamGui = teamGui;
        this.module = module;
    }

    @Command("team")
    public void executeTeamGui(org.bukkit.command.CommandSender sender) {
        if (!(sender instanceof org.bukkit.entity.Player)) return;
        org.bukkit.entity.Player player = (org.bukkit.entity.Player) sender;
        if (!module.isEnabled()) {
            player.sendMessage(PlaceholderUtils.parseToComponent("<red>Ce module est actuellement désactivé.</red>"));
            return;
        }
        teamGui.openTeamGui(player);
    }

    @Command("team quest")
    public void executeTeamQuest(org.bukkit.command.CommandSender sender) {
        if (!(sender instanceof org.bukkit.entity.Player)) return;
        org.bukkit.entity.Player player = (org.bukkit.entity.Player) sender;
        if (!module.isEnabled()) {
            player.sendMessage(PlaceholderUtils.parseToComponent("<red>Ce module est actuellement désactivé.</red>"));
            return;
        }
        TeamData team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
        if (team == null) {
            plugin.getLangManager().sendMessage(player, "teamgui.msg_1"); // or teamcommand.msg_x
            return;
        }
        teamGui.openTeamQuestGui(player, team);
    }

    @Command("team create <name>")
    public void executeTeamCreate(org.bukkit.command.CommandSender sender, @Argument(value = "name", description = "Nom de la team") String name) {
        if (!(sender instanceof org.bukkit.entity.Player)) return;
        org.bukkit.entity.Player player = (org.bukkit.entity.Player) sender;
        if (!module.isEnabled()) {
            player.sendMessage(PlaceholderUtils.parseToComponent("<red>Ce module est actuellement désactivé.</red>"));
            return;
        }
        TeamData team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
        
        if (team != null) {
            plugin.getLangManager().sendMessage(player, "teamcommand.msg_2");
            return;
        }
        if (name.length() > 16) {
            plugin.getLangManager().sendMessage(player, "teamcommand.msg_3");
            return;
        }
        plugin.getTeamManager().createTeamAsync(name, player.getUniqueId(), newTeam -> {
            if (newTeam == null) {
                plugin.getLangManager().sendMessage(player, "teamcommand.msg_4");
            } else {
                player.sendMessage(PlaceholderUtils.parseToComponent("<green>Guilde " + name + " créée avec succès !"));
            }
        });
    }

    @Command("team invite <target>")
    public void executeTeamInvite(org.bukkit.command.CommandSender sender, @Argument(value = "target", suggestions = "onlinePlayers", description = "Le joueur ciblé") String targetName) {
        if (!(sender instanceof org.bukkit.entity.Player)) return;
        org.bukkit.entity.Player player = (org.bukkit.entity.Player) sender;
        if (!module.isEnabled()) return;
        TeamData team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
        
        if (team == null || !team.isAdminOrLeader(player.getUniqueId())) {
            player.sendMessage(PlaceholderUtils.parseToComponent("<red>Seuls le chef et les administrateurs de la guilde peuvent inviter des joueurs.</red>"));
            return;
        }
        if (team.getMembers().size() >= team.getMaxMembers()) {
            player.sendMessage(plugin.getLangManager().get("teamclaims.msg_max_members",
                    net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("current", String.valueOf(team.getMembers().size())),
                    net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("max", String.valueOf(team.getMaxMembers()))));
            return;
        }
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            plugin.getLangManager().sendMessage(player, "teamcommand.msg_7");
            return;
        }
        if (plugin.getTeamManager().getPlayerTeam(target.getUniqueId()) != null) {
            plugin.getLangManager().sendMessage(player, "teamcommand.msg_8");
            return;
        }
        invites.put(target.getUniqueId(), player.getUniqueId());
        target.sendMessage(PlaceholderUtils.parseToComponent("<green>Vous avez reçu une invitation pour rejoindre la guilde <yellow>" + team.getName() + "<green> !"));
        plugin.getLangManager().sendMessage(target, "teamcommand.msg_9");
        player.sendMessage(PlaceholderUtils.parseToComponent("<green>Invitation envoyée à " + target.getName()));
    }

    @Command("team accept")
    public void executeTeamAccept(org.bukkit.command.CommandSender sender) {
        if (!(sender instanceof org.bukkit.entity.Player)) return;
        org.bukkit.entity.Player player = (org.bukkit.entity.Player) sender;
        if (!module.isEnabled()) return;
        TeamData team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
        
        if (team != null) {
            plugin.getLangManager().sendMessage(player, "teamcommand.msg_10");
            return;
        }
        UUID leaderUuid = invites.get(player.getUniqueId());
        if (leaderUuid == null) {
            plugin.getLangManager().sendMessage(player, "teamcommand.msg_11");
            return;
        }
        TeamData leaderTeam = plugin.getTeamManager().getPlayerTeam(leaderUuid);
        if (leaderTeam != null) {
            if (leaderTeam.getMembers().size() >= leaderTeam.getMaxMembers()) {
                player.sendMessage(plugin.getLangManager().get("teamclaims.msg_target_max_members",
                        net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("max", String.valueOf(leaderTeam.getMaxMembers()))));
                invites.remove(player.getUniqueId());
                return;
            }
            if (plugin.getTeamManager().addMember(leaderTeam, player.getUniqueId())) {
                leaderTeam.broadcast("<yellow>" + player.getName() + " <green>a rejoint la guilde !");
            } else {
                player.sendMessage(plugin.getLangManager().get("teamclaims.msg_target_max_members",
                        net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("max", String.valueOf(leaderTeam.getMaxMembers()))));
            }
        } else {
            plugin.getLangManager().sendMessage(player, "teamcommand.msg_12");
        }
        invites.remove(player.getUniqueId());
    }

    @Command("team upgrades")
    public void executeTeamUpgrades(org.bukkit.command.CommandSender sender) {
        if (!(sender instanceof Player)) return;
        Player player = (Player) sender;
        if (!module.isEnabled()) return;
        TeamData team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
        if (team == null) {
            player.sendMessage(PlaceholderUtils.parseToComponent("<red>Vous n'avez pas de guilde.</red>"));
            return;
        }
        teamGui.openTeamUpgradesGui(player, team);
    }

    @Command("team deposit <amount>")
    public void executeTeamDeposit(org.bukkit.command.CommandSender sender, @Argument(value = "amount", description = "Montant en dollars") double amount) {
        if (!(sender instanceof Player)) return;
        Player player = (Player) sender;
        if (!module.isEnabled()) return;

        TeamData team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
        if (team == null) {
            player.sendMessage(PlaceholderUtils.parseToComponent("<red>Vous n'avez pas de guilde.</red>"));
            return;
        }

        fr.gens.core.modules.EconomyModule eco = (fr.gens.core.modules.EconomyModule) plugin.getModuleManager().getModule("economy");
        if (eco == null || !eco.isEnabled()) {
            player.sendMessage(PlaceholderUtils.parseToComponent("<yellow>Le module d'economie est desactive. Utilisez /team depositxp <niveaux>.</yellow>"));
            return;
        }

        if (!Double.isFinite(amount) || amount <= 0) {
            player.sendMessage(PlaceholderUtils.parseToComponent("<red>Montant invalide.</red>"));
            return;
        }

        if (!eco.takeMoneyAtomic(player.getUniqueId(), amount)) {
            player.sendMessage(PlaceholderUtils.parseToComponent("<red>Vous n'avez pas assez d'argent sur votre compte.</red>"));
            return;
        }

        team.addBankBalance(amount);
        plugin.getTeamManager().saveBankAsync(team);

        team.broadcast("<green>" + player.getName() + " a depose <yellow>" + String.format("%.2f", amount) + " $</yellow> dans la banque de guilde ! (Solde: " + String.format("%.2f", team.getBankBalance()) + " $)");
    }

    @Command("team depositxp <levels>")
    public void executeTeamDepositXp(org.bukkit.command.CommandSender sender, @Argument(value = "levels", description = "Nombre de niveaux d'XP") int levels) {
        if (!(sender instanceof Player)) return;
        Player player = (Player) sender;
        if (!module.isEnabled()) return;

        TeamData team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
        if (team == null) {
            player.sendMessage(PlaceholderUtils.parseToComponent("<red>Vous n'avez pas de guilde.</red>"));
            return;
        }

        if (levels <= 0) {
            player.sendMessage(PlaceholderUtils.parseToComponent("<red>Nombre de niveaux invalide.</red>"));
            return;
        }

        if (player.getLevel() < levels) {
            player.sendMessage(PlaceholderUtils.parseToComponent("<red>Vous n'avez pas assez de niveaux d'XP (Actuel: " + player.getLevel() + ").</red>"));
            return;
        }

        player.setLevel(player.getLevel() - levels);
        team.addBankXp(levels);
        plugin.getTeamManager().saveBankAsync(team);

        team.broadcast("<green>" + player.getName() + " a depose <aqua>" + levels + " niveaux d'XP</aqua> dans la banque de guilde ! (Solde: " + team.getBankXp() + " niveaux)");
    }

    @Command("team withdraw <amount>")
    public void executeTeamWithdraw(org.bukkit.command.CommandSender sender, @Argument(value = "amount", description = "Montant en dollars") double amount) {
        if (!(sender instanceof Player)) return;
        Player player = (Player) sender;
        if (!module.isEnabled()) return;

        TeamData team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
        if (team == null) {
            player.sendMessage(PlaceholderUtils.parseToComponent("<red>Vous n'avez pas de guilde.</red>"));
            return;
        }

        if (!team.isAdminOrLeader(player.getUniqueId())) {
            player.sendMessage(PlaceholderUtils.parseToComponent("<red>Seuls le chef et les administrateurs de la guilde peuvent retirer des fonds de la banque.</red>"));
            return;
        }

        fr.gens.core.modules.EconomyModule eco = (fr.gens.core.modules.EconomyModule) plugin.getModuleManager().getModule("economy");
        if (eco == null || !eco.isEnabled()) {
            player.sendMessage(PlaceholderUtils.parseToComponent("<yellow>Le module d'economie est desactive. Utilisez /team withdrawxp <niveaux>.</yellow>"));
            return;
        }

        if (!Double.isFinite(amount) || amount <= 0) {
            player.sendMessage(PlaceholderUtils.parseToComponent("<red>Montant invalide.</red>"));
            return;
        }

        if (!team.withdrawBankBalance(amount)) {
            player.sendMessage(PlaceholderUtils.parseToComponent("<red>La banque de guilde n'a pas assez d'argent (Solde: " + String.format("%.2f", team.getBankBalance()) + " $).</red>"));
            return;
        }

        eco.giveMoney(player.getUniqueId(), amount);
        plugin.getTeamManager().saveBankAsync(team);

        team.broadcast("<gold>" + player.getName() + " a retire <yellow>" + String.format("%.2f", amount) + " $</yellow> de la banque de guilde.");
    }

    @Command("team withdrawxp <levels>")
    public void executeTeamWithdrawXp(org.bukkit.command.CommandSender sender, @Argument(value = "levels", description = "Nombre de niveaux d'XP") int levels) {
        if (!(sender instanceof Player)) return;
        Player player = (Player) sender;
        if (!module.isEnabled()) return;

        TeamData team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
        if (team == null) {
            player.sendMessage(PlaceholderUtils.parseToComponent("<red>Vous n'avez pas de guilde.</red>"));
            return;
        }

        if (!team.isAdminOrLeader(player.getUniqueId())) {
            player.sendMessage(PlaceholderUtils.parseToComponent("<red>Seuls le chef et les administrateurs de la guilde peuvent retirer de l'XP de la banque.</red>"));
            return;
        }

        if (levels <= 0) {
            player.sendMessage(PlaceholderUtils.parseToComponent("<red>Nombre de niveaux invalide.</red>"));
            return;
        }

        if (!team.withdrawBankXp(levels)) {
            player.sendMessage(PlaceholderUtils.parseToComponent("<red>La banque de guilde n'a pas assez d'XP (Solde: " + team.getBankXp() + " niveaux).</red>"));
            return;
        }

        player.setLevel(player.getLevel() + levels);
        plugin.getTeamManager().saveBankAsync(team);

        team.broadcast("<gold>" + player.getName() + " a retire <aqua>" + levels + " niveaux d'XP</aqua> de la banque de guilde.");
    }

    @Command("team claim")
    public void executeTeamClaim(org.bukkit.command.CommandSender sender) {
        if (!(sender instanceof Player)) return;
        Player player = (Player) sender;
        if (!module.isEnabled()) return;

        TeamData team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
        if (team == null) {
            player.sendMessage(PlaceholderUtils.parseToComponent("<red>Vous devez etre dans une guilde pour revendiquer un territoire.</red>"));
            return;
        }

        org.bukkit.Location loc = player.getLocation();
        String worldName = loc.getWorld() != null ? loc.getWorld().getName() : "world";
        int chunkX = loc.getBlockX() >> 4;
        int chunkZ = loc.getBlockZ() >> 4;

        TeamClaimManager.ClaimResult result = plugin.getTeamManager().getClaimManager().claimChunk(player, team, worldName, chunkX, chunkZ);
        switch (result) {
            case SUCCESS:
                team.broadcast("<green>" + player.getName() + " a revendique ce chunk pour la guilde ! (Territoires: " + plugin.getTeamManager().getClaimManager().getClaimsCount(team.getTeamId()) + "/" + team.getMaxClaims() + ")");
                break;
            case NOT_LEADER:
                player.sendMessage(PlaceholderUtils.parseToComponent("<red>Seul le chef de guilde peut revendiquer un territoire.</red>"));
                break;
            case ALREADY_CLAIMED_BY_SELF:
                player.sendMessage(PlaceholderUtils.parseToComponent("<yellow>Ce chunk appartient deja a votre guilde.</yellow>"));
                break;
            case ALREADY_CLAIMED_BY_OTHER:
                player.sendMessage(PlaceholderUtils.parseToComponent("<red>Ce chunk est deja revendique par une autre guilde.</red>"));
                break;
            case LIMIT_REACHED:
                player.sendMessage(PlaceholderUtils.parseToComponent("<red>Limite de claims atteinte (" + team.getMaxClaims() + " max). Ameliorez votre guilde avec /team upgrades !</red>"));
                break;
            case NOT_ENOUGH_MONEY:
                player.sendMessage(PlaceholderUtils.parseToComponent("<red>Fonds insuffisants dans la banque de guilde ! Cout: 1500.0 $. Deposez des fonds avec /team deposit.</red>"));
                break;
            case NOT_ENOUGH_XP:
                player.sendMessage(PlaceholderUtils.parseToComponent("<red>XP insuffisante dans la banque de guilde ! Cout: 10 niveaux. Deposez de l'XP avec /team depositxp.</red>"));
                break;
            case DATABASE_ERROR:
                player.sendMessage(PlaceholderUtils.parseToComponent("<red>Erreur interne lors de la revendication du territoire.</red>"));
                break;
        }
    }

    @Command("team unclaim")
    public void executeTeamUnclaim(org.bukkit.command.CommandSender sender) {
        if (!(sender instanceof Player)) return;
        Player player = (Player) sender;
        if (!module.isEnabled()) return;

        TeamData team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
        if (team == null) {
            player.sendMessage(PlaceholderUtils.parseToComponent("<red>Vous n'avez pas de guilde.</red>"));
            return;
        }

        org.bukkit.Location unclaimLoc = player.getLocation();
        String unclaimWorld = unclaimLoc.getWorld() != null ? unclaimLoc.getWorld().getName() : "world";
        int unclaimChunkX = unclaimLoc.getBlockX() >> 4;
        int unclaimChunkZ = unclaimLoc.getBlockZ() >> 4;

        TeamClaimManager.UnclaimResult result = plugin.getTeamManager().getClaimManager().unclaimChunk(player, team, unclaimWorld, unclaimChunkX, unclaimChunkZ);
        switch (result) {
            case SUCCESS:
                team.broadcast("<green>" + player.getName() + " a libere un territoire de guilde. (Restants: " + plugin.getTeamManager().getClaimManager().getClaimsCount(team.getTeamId()) + "/" + team.getMaxClaims() + ")");
                break;
            case NOT_LEADER:
                player.sendMessage(PlaceholderUtils.parseToComponent("<red>Seul le chef de guilde peut liberer un territoire.</red>"));
                break;
            case NOT_CLAIMED:
                player.sendMessage(PlaceholderUtils.parseToComponent("<yellow>Ce chunk n'est pas revendique.</yellow>"));
                break;
            case NOT_YOUR_CLAIM:
                player.sendMessage(PlaceholderUtils.parseToComponent("<red>Ce chunk n'appartient pas a votre guilde.</red>"));
                break;
            case DATABASE_ERROR:
                player.sendMessage(PlaceholderUtils.parseToComponent("<red>Erreur interne lors de la liberation du territoire.</red>"));
                break;
        }
    }

    @Command("team color <color>")
    public void executeTeamColor(org.bukkit.command.CommandSender sender, @Argument(value = "color", description = "Code couleur hex (ex: #3498db)") String color) {
        if (!(sender instanceof Player)) return;
        Player player = (Player) sender;
        if (!module.isEnabled()) return;

        TeamData team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
        if (team == null) {
            player.sendMessage(PlaceholderUtils.parseToComponent("<red>Vous n'avez pas de guilde.</red>"));
            return;
        }

        if (!team.isAdminOrLeader(player.getUniqueId())) {
            player.sendMessage(PlaceholderUtils.parseToComponent("<red>Seuls le chef et les administrateurs de la guilde peuvent modifier la couleur de territoire.</red>"));
            return;
        }

        if (!color.startsWith("#")) color = "#" + color;
        if (!color.matches("^#([A-Fa-f0-9]{6})$")) {
            player.sendMessage(PlaceholderUtils.parseToComponent("<red>Format de couleur invalide. Utilisez le format hex (ex: #3498db ou #e74c3c).</red>"));
            return;
        }

        team.setColor(color);
        plugin.getTeamManager().saveColorAsync(team);

        fr.gens.core.modules.BlueMapModule bm = (fr.gens.core.modules.BlueMapModule) plugin.getModuleManager().getModule("bluemap");
        if (bm != null && bm.isEnabled()) {
            bm.updateAllTeamTerritories();
        }

        team.broadcast("<green>La couleur de territoire de la guilde a ete modifiee en <white>" + color + "</white> !");
    }

    @Command("team kick <target>")
    public void executeTeamKick(org.bukkit.command.CommandSender sender, @Argument(value = "target", suggestions = "onlinePlayers", description = "Le joueur a exclure") String targetName) {
        if (!(sender instanceof Player)) return;
        Player player = (Player) sender;
        if (!module.isEnabled()) return;

        TeamData team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
        if (team == null) {
            player.sendMessage(PlaceholderUtils.parseToComponent("<red>Vous n'avez pas de guilde.</red>"));
            return;
        }

        if (!team.isAdminOrLeader(player.getUniqueId())) {
            player.sendMessage(PlaceholderUtils.parseToComponent("<red>Seuls le chef et les administrateurs peuvent exclure des membres.</red>"));
            return;
        }

        UUID targetUuid = null;
        String actualName = targetName;
        for (UUID mUuid : team.getMembers()) {
            org.bukkit.OfflinePlayer op = Bukkit.getOfflinePlayer(mUuid);
            if (op.getName() != null && op.getName().equalsIgnoreCase(targetName)) {
                targetUuid = mUuid;
                actualName = op.getName();
                break;
            }
        }

        if (targetUuid == null) {
            player.sendMessage(PlaceholderUtils.parseToComponent("<red>Ce joueur ne fait pas partie de votre guilde.</red>"));
            return;
        }

        if (team.isLeader(targetUuid)) {
            player.sendMessage(PlaceholderUtils.parseToComponent("<red>Vous ne pouvez pas exclure le chef de guilde !</red>"));
            return;
        }

        if (!team.canManageMembers(player.getUniqueId(), targetUuid)) {
            player.sendMessage(PlaceholderUtils.parseToComponent("<red>Vous n'avez pas les droits suffisants pour exclure cet administrateur.</red>"));
            return;
        }

        plugin.getTeamManager().removeMember(team, targetUuid);
        team.broadcast("<red><bold>" + actualName + "</bold> a ete exclu de la guilde par <yellow>" + player.getName() + "</yellow>.");

        Player targetPlayer = Bukkit.getPlayer(targetUuid);
        if (targetPlayer != null && targetPlayer.isOnline()) {
            targetPlayer.sendMessage(PlaceholderUtils.parseToComponent("<red>Vous avez ete exclu de la guilde " + team.getName() + ".</red>"));
        }
    }

    @Command("team promote <target>")
    public void executeTeamPromote(org.bukkit.command.CommandSender sender, @Argument(value = "target", suggestions = "onlinePlayers", description = "Le membre a promouvoir") String targetName) {
        if (!(sender instanceof Player)) return;
        Player player = (Player) sender;
        if (!module.isEnabled()) return;

        TeamData team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
        if (team == null) {
            player.sendMessage(PlaceholderUtils.parseToComponent("<red>Vous n'avez pas de guilde.</red>"));
            return;
        }

        if (!team.isLeader(player.getUniqueId())) {
            player.sendMessage(PlaceholderUtils.parseToComponent("<red>Seul le chef de guilde peut nommer des administrateurs.</red>"));
            return;
        }

        UUID targetUuid = null;
        String actualName = targetName;
        for (UUID mUuid : team.getMembers()) {
            org.bukkit.OfflinePlayer op = Bukkit.getOfflinePlayer(mUuid);
            if (op.getName() != null && op.getName().equalsIgnoreCase(targetName)) {
                targetUuid = mUuid;
                actualName = op.getName();
                break;
            }
        }

        if (targetUuid == null) {
            player.sendMessage(PlaceholderUtils.parseToComponent("<red>Ce joueur ne fait pas partie de votre guilde.</red>"));
            return;
        }

        if (team.isLeader(targetUuid)) {
            player.sendMessage(PlaceholderUtils.parseToComponent("<red>Vous etes deja le chef de la guilde !</red>"));
            return;
        }

        if (team.isAdmin(targetUuid)) {
            player.sendMessage(PlaceholderUtils.parseToComponent("<yellow>" + actualName + " est deja administrateur de la guilde.</yellow>"));
            return;
        }

        plugin.getTeamManager().promoteAdmin(team, targetUuid);
        team.broadcast("<gold><bold>" + actualName + "</bold> a ete promu <aqua>Administrateur</aqua> de la guilde par <yellow>" + player.getName() + "</yellow> !");

        Player targetPlayer = Bukkit.getPlayer(targetUuid);
        if (targetPlayer != null && targetPlayer.isOnline()) {
            targetPlayer.sendMessage(PlaceholderUtils.parseToComponent("<green><bold>Felicitation !</bold> Vous etes desormais Administrateur de la guilde.</green>"));
        }
    }

    @Command("team demote <target>")
    public void executeTeamDemote(org.bukkit.command.CommandSender sender, @Argument(value = "target", suggestions = "onlinePlayers", description = "L'administrateur a retrograder") String targetName) {
        if (!(sender instanceof Player)) return;
        Player player = (Player) sender;
        if (!module.isEnabled()) return;

        TeamData team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
        if (team == null) {
            player.sendMessage(PlaceholderUtils.parseToComponent("<red>Vous n'avez pas de guilde.</red>"));
            return;
        }

        if (!team.isLeader(player.getUniqueId())) {
            player.sendMessage(PlaceholderUtils.parseToComponent("<red>Seul le chef de guilde peut retrograder des administrateurs.</red>"));
            return;
        }

        UUID targetUuid = null;
        String actualName = targetName;
        for (UUID mUuid : team.getMembers()) {
            org.bukkit.OfflinePlayer op = Bukkit.getOfflinePlayer(mUuid);
            if (op.getName() != null && op.getName().equalsIgnoreCase(targetName)) {
                targetUuid = mUuid;
                actualName = op.getName();
                break;
            }
        }

        if (targetUuid == null) {
            player.sendMessage(PlaceholderUtils.parseToComponent("<red>Ce joueur ne fait pas partie de votre guilde.</red>"));
            return;
        }

        if (!team.isAdmin(targetUuid)) {
            player.sendMessage(PlaceholderUtils.parseToComponent("<yellow>" + actualName + " n'est pas administrateur de la guilde.</yellow>"));
            return;
        }

        plugin.getTeamManager().demoteAdmin(team, targetUuid);
        team.broadcast("<yellow>" + actualName + "</yellow> a ete retrograde au rang de <gray>Membre</gray> par <gold>" + player.getName() + "</gold>.");

        Player targetPlayer = Bukkit.getPlayer(targetUuid);
        if (targetPlayer != null && targetPlayer.isOnline()) {
            targetPlayer.sendMessage(PlaceholderUtils.parseToComponent("<yellow>Vous n'etes plus administrateur de la guilde.</yellow>"));
        }
    }

    @Command("team disband")
    public void executeTeamDisband(org.bukkit.command.CommandSender sender) {
        if (!(sender instanceof Player)) return;
        Player player = (Player) sender;
        if (!module.isEnabled()) return;

        TeamData team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
        if (team == null) {
            player.sendMessage(PlaceholderUtils.parseToComponent("<red>Vous n'avez pas de guilde.</red>"));
            return;
        }

        if (!team.isLeader(player.getUniqueId())) {
            player.sendMessage(PlaceholderUtils.parseToComponent("<red>Seul le chef de guilde peut dissoudre la guilde. Les administrateurs ne peuvent pas detruire la guilde.</red>"));
            return;
        }

        String teamName = team.getName();
        plugin.getTeamManager().disbandTeam(team);
        player.sendMessage(PlaceholderUtils.parseToComponent("<red>Votre guilde " + teamName + " a ete dissoute.</red>"));
    }

    @Command("team leave")
    public void executeTeamLeave(org.bukkit.command.CommandSender sender) {
        if (!(sender instanceof Player)) return;
        Player player = (Player) sender;
        if (!module.isEnabled()) return;

        TeamData team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
        if (team == null) {
            player.sendMessage(PlaceholderUtils.parseToComponent("<red>Vous n'avez pas de guilde.</red>"));
            return;
        }

        if (team.isLeader(player.getUniqueId())) {
            player.sendMessage(PlaceholderUtils.parseToComponent("<red>Le chef ne peut pas quitter sa propre guilde ! Utilisez /team disband pour la dissoudre.</red>"));
            return;
        }

        plugin.getTeamManager().removeMember(team, player.getUniqueId());
        player.sendMessage(PlaceholderUtils.parseToComponent("<yellow>Vous avez quitte la guilde " + team.getName() + ".</yellow>"));
        team.broadcast("<yellow>" + player.getName() + " a quitte la guilde.");
    }

    public void removeInvite(UUID uuid) {
        invites.remove(uuid); // If they were invited
        invites.values().removeIf(val -> val.equals(uuid)); // If they were the inviter
    }

    // ---- GUILD HOME ----
    private final Map<UUID, Long> homeCooldowns = new ConcurrentHashMap<>();

    @Command("team sethome")
    public void executeTeamSetHome(org.bukkit.command.CommandSender sender) {
        if (!(sender instanceof Player)) return;
        Player player = (Player) sender;
        if (!module.isEnabled()) return;

        TeamData team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
        if (team == null) {
            player.sendMessage(PlaceholderUtils.parseToComponent("<red>Vous n'avez pas de guilde."));
            return;
        }
        if (!team.isAdminOrLeader(player.getUniqueId())) {
            player.sendMessage(PlaceholderUtils.parseToComponent("<red>Seuls le chef et les administrateurs peuvent definir le home de la guilde."));
            return;
        }
        if (team.getUpgradeLevel("GUILD_HOME") <= 0) {
            player.sendMessage(PlaceholderUtils.parseToComponent("<red>Debloquez d'abord l'amelioration <gold>Home de Guilde</gold> dans le menu /team."));
            return;
        }
        team.setHomeLocation(player.getLocation());
        plugin.getTeamManager().saveHomeAsync(team);
        player.sendMessage(PlaceholderUtils.parseToComponent("<green>Home de guilde defini a votre position."));
        team.broadcast("<aqua>" + player.getName() + " a defini le home de la guilde.");
    }

    @Command("team home")
    public void executeTeamHome(org.bukkit.command.CommandSender sender) {
        if (!(sender instanceof Player)) return;
        Player player = (Player) sender;
        if (!module.isEnabled()) return;

        TeamData team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
        if (team == null) {
            player.sendMessage(PlaceholderUtils.parseToComponent("<red>Vous n'avez pas de guilde."));
            return;
        }
        int warmup = team.getGuildHomeWarmup();
        if (warmup < 0) {
            player.sendMessage(PlaceholderUtils.parseToComponent("<red>Debloquez d'abord l'amelioration <gold>Home de Guilde</gold> dans le menu /team."));
            return;
        }
        org.bukkit.Location home = team.getHomeLocation();
        if (home == null) {
            player.sendMessage(PlaceholderUtils.parseToComponent("<red>Aucun home de guilde defini. Un admin peut le faire avec /team sethome."));
            return;
        }
        // Check cooldown
        int cooldown = team.getGuildHomeCooldown();
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long lastUsed = homeCooldowns.get(uuid);
        if (lastUsed != null && now - lastUsed < (long) cooldown * 1000L) {
            long remaining = (cooldown * 1000L - (now - lastUsed)) / 1000L;
            player.sendMessage(PlaceholderUtils.parseToComponent("<red>Cooldown : encore " + remaining + "s avant d'utiliser /team home."));
            return;
        }
        homeCooldowns.put(uuid, now);
        fr.gens.core.utils.TeleportUtil.teleportWithCooldown(plugin, player, home, "home de guilde " + team.getName(), "genscore.bypass.cooldown.all");
    }

    // ---- GUILD VAULT ----
    @Command("team vault")
    public void executeTeamVault(org.bukkit.command.CommandSender sender) {
        openVaultForSender(sender);
    }

    @Command("team coffre")
    public void executeTeamCoffre(org.bukkit.command.CommandSender sender) {
        openVaultForSender(sender);
    }

    @Command("team chest")
    public void executeTeamChest(org.bukkit.command.CommandSender sender) {
        openVaultForSender(sender);
    }

    private void openVaultForSender(org.bukkit.command.CommandSender sender) {
        if (!(sender instanceof Player)) return;
        Player player = (Player) sender;
        if (!module.isEnabled()) return;

        TeamData team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
        if (team == null) {
            player.sendMessage(PlaceholderUtils.parseToComponent("<red>Vous n'avez pas de guilde."));
            return;
        }
        int vaultSize = team.getVaultSize();
        if (vaultSize <= 0) {
            player.sendMessage(PlaceholderUtils.parseToComponent("<red>Debloquez d'abord l'amelioration <gold>Coffre de Guilde</gold> dans le menu /team."));
            return;
        }
        teamGui.openTeamVaultGui(player, team);
    }
}



