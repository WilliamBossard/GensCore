package fr.gens.core.modules.teams;

import fr.gens.core.CorePlugin;
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
            player.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<red>Ce module est actuellement désactivé.</red>"));
            return;
        }
        teamGui.openTeamGui(player);
    }

    @Command("team quest")
    public void executeTeamQuest(org.bukkit.command.CommandSender sender) {
        if (!(sender instanceof org.bukkit.entity.Player)) return;
        org.bukkit.entity.Player player = (org.bukkit.entity.Player) sender;
        if (!module.isEnabled()) {
            player.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<red>Ce module est actuellement désactivé.</red>"));
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
            player.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<red>Ce module est actuellement désactivé.</red>"));
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
                player.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<green>Guilde " + name + " créée avec succès !"));
            }
        });
    }

    @Command("team invite <target>")
    public void executeTeamInvite(org.bukkit.command.CommandSender sender, @Argument(value = "target", suggestions = "onlinePlayers", description = "Le joueur ciblé") String targetName) {
        if (!(sender instanceof org.bukkit.entity.Player)) return;
        org.bukkit.entity.Player player = (org.bukkit.entity.Player) sender;
        if (!module.isEnabled()) return;
        TeamData team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
        
        if (team == null || !team.getLeaderUuid().equals(player.getUniqueId())) {
            plugin.getLangManager().sendMessage(player, "teamcommand.msg_5");
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
        target.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<green>Vous avez reçu une invitation pour rejoindre la guilde <yellow>" + team.getName() + "<green> !"));
        plugin.getLangManager().sendMessage(target, "teamcommand.msg_9");
        player.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<green>Invitation envoyée à " + target.getName()));
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
            plugin.getTeamManager().addMember(leaderTeam, player.getUniqueId());
            leaderTeam.broadcast("<yellow>" + player.getName() + " <green>a rejoint la guilde !");
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
            player.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<red>Vous n'avez pas de guilde.</red>"));
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
            player.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<red>Vous n'avez pas de guilde.</red>"));
            return;
        }

        fr.gens.core.modules.EconomyModule eco = (fr.gens.core.modules.EconomyModule) plugin.getModuleManager().getModule("economy");
        if (eco == null || !eco.isEnabled()) {
            player.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<yellow>Le module d'economie est desactive. Utilisez /team depositxp <niveaux>.</yellow>"));
            return;
        }

        if (!Double.isFinite(amount) || amount <= 0) {
            player.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<red>Montant invalide.</red>"));
            return;
        }

        if (!eco.takeMoneyAtomic(player.getUniqueId(), amount)) {
            player.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<red>Vous n'avez pas assez d'argent sur votre compte.</red>"));
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
            player.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<red>Vous n'avez pas de guilde.</red>"));
            return;
        }

        if (levels <= 0) {
            player.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<red>Nombre de niveaux invalide.</red>"));
            return;
        }

        if (player.getLevel() < levels) {
            player.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<red>Vous n'avez pas assez de niveaux d'XP (Actuel: " + player.getLevel() + ").</red>"));
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
            player.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<red>Vous n'avez pas de guilde.</red>"));
            return;
        }

        if (!team.getLeaderUuid().equals(player.getUniqueId())) {
            player.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<red>Seul le chef de guilde peut retirer des fonds de la banque.</red>"));
            return;
        }

        fr.gens.core.modules.EconomyModule eco = (fr.gens.core.modules.EconomyModule) plugin.getModuleManager().getModule("economy");
        if (eco == null || !eco.isEnabled()) {
            player.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<yellow>Le module d'economie est desactive. Utilisez /team withdrawxp <niveaux>.</yellow>"));
            return;
        }

        if (!Double.isFinite(amount) || amount <= 0) {
            player.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<red>Montant invalide.</red>"));
            return;
        }

        if (!team.withdrawBankBalance(amount)) {
            player.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<red>La banque de guilde n'a pas assez d'argent (Solde: " + String.format("%.2f", team.getBankBalance()) + " $).</red>"));
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
            player.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<red>Vous n'avez pas de guilde.</red>"));
            return;
        }

        if (!team.getLeaderUuid().equals(player.getUniqueId())) {
            player.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<red>Seul le chef de guilde peut retirer de l'XP de la banque.</red>"));
            return;
        }

        if (levels <= 0) {
            player.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<red>Nombre de niveaux invalide.</red>"));
            return;
        }

        if (!team.withdrawBankXp(levels)) {
            player.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<red>La banque de guilde n'a pas assez d'XP (Solde: " + team.getBankXp() + " niveaux).</red>"));
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
            player.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<red>Vous devez etre dans une guilde pour revendiquer un territoire.</red>"));
            return;
        }

        TeamClaimManager.ClaimResult result = plugin.getTeamManager().getClaimManager().claimChunk(player, team, player.getLocation().getChunk());
        switch (result) {
            case SUCCESS:
                team.broadcast("<green>" + player.getName() + " a revendique ce chunk pour la guilde ! (Territoires: " + plugin.getTeamManager().getClaimManager().getClaimsCount(team.getTeamId()) + "/" + team.getMaxClaims() + ")");
                break;
            case NOT_LEADER:
                player.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<red>Seul le chef de guilde peut revendiquer un territoire.</red>"));
                break;
            case ALREADY_CLAIMED_BY_SELF:
                player.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<yellow>Ce chunk appartient deja a votre guilde.</yellow>"));
                break;
            case ALREADY_CLAIMED_BY_OTHER:
                player.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<red>Ce chunk est deja revendique par une autre guilde.</red>"));
                break;
            case LIMIT_REACHED:
                player.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<red>Limite de claims atteinte (" + team.getMaxClaims() + " max). Ameliorez votre guilde avec /team upgrades !</red>"));
                break;
            case NOT_ENOUGH_MONEY:
                player.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<red>Fonds insuffisants dans la banque de guilde ! Cout: 1500.0 $. Deposez des fonds avec /team deposit.</red>"));
                break;
            case NOT_ENOUGH_XP:
                player.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<red>XP insuffisante dans la banque de guilde ! Cout: 10 niveaux. Deposez de l'XP avec /team depositxp.</red>"));
                break;
            case DATABASE_ERROR:
                player.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<red>Erreur interne lors de la revendication du territoire.</red>"));
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
            player.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<red>Vous n'avez pas de guilde.</red>"));
            return;
        }

        TeamClaimManager.UnclaimResult result = plugin.getTeamManager().getClaimManager().unclaimChunk(player, team, player.getLocation().getChunk());
        switch (result) {
            case SUCCESS:
                team.broadcast("<green>" + player.getName() + " a libere un territoire de guilde. (Restants: " + plugin.getTeamManager().getClaimManager().getClaimsCount(team.getTeamId()) + "/" + team.getMaxClaims() + ")");
                break;
            case NOT_LEADER:
                player.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<red>Seul le chef de guilde peut liberer un territoire.</red>"));
                break;
            case NOT_CLAIMED:
                player.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<yellow>Ce chunk n'est pas revendique.</yellow>"));
                break;
            case NOT_YOUR_CLAIM:
                player.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<red>Ce chunk n'appartient pas a votre guilde.</red>"));
                break;
            case DATABASE_ERROR:
                player.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<red>Erreur interne lors de la liberation du territoire.</red>"));
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
            player.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<red>Vous n'avez pas de guilde.</red>"));
            return;
        }

        if (!team.getLeaderUuid().equals(player.getUniqueId())) {
            player.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<red>Seul le chef de guilde peut modifier la couleur de territoire.</red>"));
            return;
        }

        if (!color.startsWith("#")) color = "#" + color;
        if (!color.matches("^#([A-Fa-f0-9]{6})$")) {
            player.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<red>Format de couleur invalide. Utilisez le format hex (ex: #3498db ou #e74c3c).</red>"));
            return;
        }

        team.setColor(color);
        plugin.getTeamManager().saveColorAsync(team);

        team.broadcast("<green>La couleur de territoire de la guilde a ete modifiee en <white>" + color + "</white> !");
    }

    public void removeInvite(UUID uuid) {
        invites.remove(uuid); // If they were invited
        invites.values().removeIf(val -> val.equals(uuid)); // If they were the inviter
    }
}



