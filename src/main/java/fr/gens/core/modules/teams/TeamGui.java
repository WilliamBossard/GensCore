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

        Inventory inv = Bukkit.createInventory(null, 45, net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize("<dark_gray>Guilde : " + team.getName()));

        // Ligne de dÃƒÆ’Ã‚Â©cor
        ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.displayName(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(" "));
        glass.setItemMeta(glassMeta);
        for (int i = 0; i < 9; i++) inv.setItem(i, glass);
        for (int i = 37; i < 44; i++) inv.setItem(i, glass);

        // Bouton QuÃƒÆ’Ã‚Âªtes
        ItemStack quests = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta qmeta = quests.getItemMeta();
        qmeta.displayName(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize("<aqua>QuÃƒÆ’Ã‚Âªtes de Guilde"));
        List<String> qlore = new ArrayList<>();
        qlore.add("<gray>Accomplissez des dÃƒÆ’Ã‚Â©fis en coopÃƒÆ’Ã‚Â©ration");
        qlore.add("<gray>pour gagner des Points de Guilde !");
        qlore.add("");
        qlore.add("<yellow>Clic pour ouvrir");
        qmeta.lore(java.util.Optional.ofNullable(qlore).orElse(java.util.Collections.emptyList()).stream().map(s -> net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize((String)s)).collect(java.util.stream.Collectors.toList()));
        quests.setItemMeta(qmeta);
        inv.setItem(36, quests);

        // Afficher les membres
        int slot = 9;
        boolean isLeader = team.getLeaderUuid().equals(player.getUniqueId());

        for (UUID memberUuid : team.getMembers()) {
            OfflinePlayer op = Bukkit.getOfflinePlayer(memberUuid);
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.setOwningPlayer(op);
            meta.displayName(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize("<yellow>" + (op.getName() != null ? op.getName() : "Joueur Inconnu")));
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
            meta.lore(java.util.Optional.ofNullable(lore).orElse(java.util.Collections.emptyList()).stream().map(s -> net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize((String)s)).collect(java.util.stream.Collectors.toList()));
            head.setItemMeta(meta);

            inv.setItem(slot++, head);
            if (slot > 35) break; // Limite de 27 membres pour l'UI simple
        }

        // Bouton ParamÃƒÆ’Ã‚Â¨tres si leader
        if (isLeader) {
            ItemStack settings = new ItemStack(Material.REPEATER);
            ItemMeta smeta = settings.getItemMeta();
            smeta.displayName(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize("<aqua>ParamÃƒÆ’Ã‚Â¨tres de Verrouillage"));
            List<String> slore = new ArrayList<>();
            slore.add("<gray>Auto-verrouiller les coffres posÃƒÆ’Ã‚Â©s");
            slore.add("<gray>pour les membres de l'ÃƒÆ’Ã‚Â©quipe :");
            slore.add(team.isAutoLock() ? "<green><bold>ACTIVÃƒÆ’Ã¢â‚¬Â°" : "<red><bold>DÃƒÆ’Ã¢â‚¬Â°SACTIVÃƒÆ’Ã¢â‚¬Â°");
            slore.add("");
            slore.add("<yellow>Clic pour changer");
            smeta.lore(java.util.Optional.ofNullable(slore).orElse(java.util.Collections.emptyList()).stream().map(s -> net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize((String)s)).collect(java.util.stream.Collectors.toList()));
            settings.setItemMeta(smeta);
            inv.setItem(40, settings);
        }

        // Quitter la team
        ItemStack leave = new ItemStack(Material.BARRIER);
        ItemMeta lmeta = leave.getItemMeta();
        lmeta.displayName(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize("<red>Quitter la guilde"));
        leave.setItemMeta(lmeta);
        inv.setItem(44, leave);

        player.openInventory(inv);
    }
}

