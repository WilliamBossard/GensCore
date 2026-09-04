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
                buttons.add(new fr.gens.core.utils.BedrockFormManager.BedrockButton("§e" + (op.getName() != null ? op.getName() : "Inconnu") + "\n§r" + role, org.bukkit.Material.PLAYER_HEAD, p -> {
                    if (isLeader && !team.getLeaderUuid().equals(memberUuid)) {
                        // Exclure
                        team.removeMember(memberUuid);
                        plugin.getLangManager().sendMessage(p, "teamlistener.msg_3");
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
            meta.setOwningPlayer(op);
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
}



