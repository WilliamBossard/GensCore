package fr.gens.core.modules.perks;

import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.annotations.Command;

public class SoloPerkCommand {

    private final SoloPerkManager manager;
    private final SoloPerkGui gui;

    public SoloPerkCommand(SoloPerkManager manager, SoloPerkGui gui) {
        this.manager = manager;
        this.gui = gui;
    }

    @Command("perks")
    public void executePerks(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Cette commande est reservee aux joueurs.");
            return;
        }
        gui.openMenu(player);
    }

    @Command("bonus")
    public void executeBonus(CommandSender sender) {
        executePerks(sender);
    }

    @Command("passe")
    public void executePasse(CommandSender sender) {
        executePerks(sender);
    }

    @Command("autosmelt")
    public void executeAutoSmelt(CommandSender sender) {
        if (!(sender instanceof Player player)) return;
        if (!manager.hasPerk(player.getUniqueId(), SoloPerkType.AUTO_SMELT)) {
            player.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent(
                    "<red>Vous devez d'abord debloquer la Fonte Instantanee dans le menu <yellow>/perks</yellow> (requis : 80 quetes).</red>"
            ));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        boolean newState = manager.togglePerk(player.getUniqueId(), SoloPerkType.AUTO_SMELT);
        player.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent(
                "<yellow>Fonte Instantanee (Auto-Smelt) : " + (newState ? "<green>ACTIVE</green>" : "<red>DESACTIVE</red>") + ".</yellow>"
        ));
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
    }

    @Command("magnet")
    public void executeMagnet(CommandSender sender) {
        if (!(sender instanceof Player player)) return;
        if (!manager.hasPerk(player.getUniqueId(), SoloPerkType.MAGNET)) {
            player.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent(
                    "<red>Vous devez d'abord debloquer l'Aimant de Recolte dans le menu <yellow>/perks</yellow> (requis : 25 quetes).</red>"
            ));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        boolean newState = manager.togglePerk(player.getUniqueId(), SoloPerkType.MAGNET);
        player.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent(
                "<yellow>Aimant de Recolte (Magnet) : " + (newState ? "<green>ACTIVE</green>" : "<red>DESACTIVE</red>") + ".</yellow>"
        ));
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
    }
}
