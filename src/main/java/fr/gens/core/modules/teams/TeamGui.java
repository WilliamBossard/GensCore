package fr.gens.core.modules.teams;

import fr.gens.core.CorePlugin;
import fr.gens.core.modules.gui.CustomGuiModule;
import fr.gens.core.utils.ItemSerializer;
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

        Inventory inv = Bukkit.createInventory(null, 45, "§8Guilde : " + team.getName());

        // Ligne de décor
        ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.setDisplayName(" ");
        glass.setItemMeta(glassMeta);
        for (int i = 0; i < 9; i++) inv.setItem(i, glass);
        for (int i = 37; i < 44; i++) inv.setItem(i, glass);

        // Bouton Quêtes
        ItemStack quests = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta qmeta = quests.getItemMeta();
        qmeta.setDisplayName("§bQuêtes de Guilde");
        List<String> qlore = new ArrayList<>();
        qlore.add("§7Accomplissez des défis en coopération");
        qlore.add("§7pour gagner des Points de Guilde !");
        qlore.add("");
        qlore.add("§eClic pour ouvrir");
        qmeta.setLore(qlore);
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
            meta.setDisplayName("§e" + (op.getName() != null ? op.getName() : "Joueur Inconnu"));
            List<String> lore = new ArrayList<>();
            if (team.getLeaderUuid().equals(memberUuid)) {
                lore.add("§6 Chef de Guilde");
            } else {
                lore.add("§7Membre");
                if (isLeader) {
                    lore.add("");
                    lore.add("§cClic droit pour exclure");
                }
            }
            meta.setLore(lore);
            head.setItemMeta(meta);

            inv.setItem(slot++, head);
            if (slot > 35) break; // Limite de 27 membres pour l'UI simple
        }

        // Bouton Paramètres si leader
        if (isLeader) {
            ItemStack settings = new ItemStack(Material.REPEATER);
            ItemMeta smeta = settings.getItemMeta();
            smeta.setDisplayName("§bParamètres de Verrouillage");
            List<String> slore = new ArrayList<>();
            slore.add("§7Auto-verrouiller les coffres posés");
            slore.add("§7pour les membres de l'équipe :");
            slore.add(team.isAutoLock() ? "§a§lACTIVÉ" : "§c§lDÉSACTIVÉ");
            slore.add("");
            slore.add("§eClic pour changer");
            smeta.setLore(slore);
            settings.setItemMeta(smeta);
            inv.setItem(40, settings);
        }

        // Quitter la team
        ItemStack leave = new ItemStack(Material.BARRIER);
        ItemMeta lmeta = leave.getItemMeta();
        lmeta.setDisplayName("§cQuitter la guilde");
        leave.setItemMeta(lmeta);
        inv.setItem(44, leave);

        player.openInventory(inv);
    }
}
