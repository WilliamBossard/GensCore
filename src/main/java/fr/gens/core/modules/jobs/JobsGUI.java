package fr.gens.core.modules.jobs;

import fr.gens.core.CorePlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;


public class JobsGUI implements Listener {

    private final CorePlugin plugin;
    private final JobsModule jobsModule;

    public JobsGUI(CorePlugin plugin, JobsModule jobsModule) {
        this.plugin = plugin;
        this.jobsModule = jobsModule;
    }

    public void openGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 36, net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize("<dark_gray>ÃƒÂ¢Ã¢â‚¬â€œÃ‚Â  Ãƒâ€šÃ‚Â§x<white><white><aqua><gray><black><dark_aqua>MÃƒÆ’Ã‚Â©tiers <dark_gray>ÃƒÂ¢Ã¢â‚¬â€œÃ‚Â "));

        // Fill background
        ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.displayName(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(" "));
        glass.setItemMeta(glassMeta);
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, glass);
        }

        int[] slots = {10, 12, 14, 16};
        JobType[] types = JobType.values();

        for (int i = 0; i < types.length; i++) {
            JobType type = types[i];
            boolean hasJob = jobsModule.hasJob(player.getUniqueId(), type);
            int level = jobsModule.getLevel(player.getUniqueId(), type);
            double xp = jobsModule.getXp(player.getUniqueId(), type);
            double nextXp = jobsModule.getXpNeededForNextLevel(level);
            
            Material mat = Material.IRON_PICKAXE;
            if (type == JobType.BUCHERON) mat = Material.IRON_AXE;
            if (type == JobType.FERMIER) mat = Material.IRON_HOE;
            if (type == JobType.CHASSEUR) mat = Material.IRON_SWORD;
            
            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(type.getColor() + "<bold>" + type.getDisplayName()));
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("<gray>Niveau: <white>" + level);
            lore.add("<gray>XP: <white>" + String.format("%.1f", xp) + " <dark_gray>/ <white>" + nextXp);
            
            int percent = (int) ((xp / nextXp) * 100);
            String progressBar = createProgressBar(percent);
            
            lore.add("<dark_gray>" + progressBar + " <dark_gray>(<gray>" + percent + "%<dark_gray>)");
            lore.add("");
            if (hasJob) {
                lore.add("<green>ÃƒÂ¢Ã¢â‚¬â€œÃ‚Â  <gray>Statut : <green><bold>Actif");
                lore.add("<dark_gray>ÃƒÂ¢Ã¢â‚¬â€œÃ‚Â¶ <red>Cliquez pour dÃƒÆ’Ã‚Â©missionner");
            } else {
                lore.add("<red>ÃƒÂ¢Ã¢â‚¬â€œÃ‚Â  <gray>Statut : <red><bold>Inactif");
                lore.add("<dark_gray>ÃƒÂ¢Ã¢â‚¬â€œÃ‚Â¶ <green>Cliquez pour rejoindre");
            }
            meta.lore(java.util.Optional.ofNullable(lore).orElse(java.util.Collections.emptyList()).stream().map(s -> net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize((String)s)).collect(java.util.stream.Collectors.toList()));
            item.setItemMeta(meta);
            
            inv.setItem(slots[i], item);
        }

        player.openInventory(inv);
    }

    private String createProgressBar(int percent) {
        int totalBars = 20;
        int activeBars = (percent * totalBars) / 100;
        StringBuilder sb = new StringBuilder("<green>");
        for (int i = 0; i < totalBars; i++) {
            if (i == activeBars) sb.append("<gray>");
            sb.append("|");
        }
        return sb.toString();
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(e.getView().title()).equals("■ Métiers ■")) {
            e.setCancelled(true);
            if (!(e.getWhoClicked() instanceof Player)) return;
            Player p = (Player) e.getWhoClicked();
            
            ItemStack clicked = e.getCurrentItem();
            if (clicked == null || !clicked.hasItemMeta()) return;
            
            if (!clicked.getItemMeta().hasDisplayName()) return;
            String name = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(clicked.getItemMeta().displayName());
            JobType targetJob = null;
            for (JobType type : JobType.values()) {
                if (name.contains(type.getDisplayName())) {
                    targetJob = type;
                    break;
                }
            }
            
            if (targetJob != null) {
                if (jobsModule.hasJob(p.getUniqueId(), targetJob)) {
                    jobsModule.leaveJob(p.getUniqueId(), targetJob);
                    p.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize("<red>Vous avez quittÃƒÆ’Ã‚Â© le mÃƒÆ’Ã‚Â©tier " + targetJob.getDisplayName() + " !"));
                } else {
                    if (jobsModule.getActiveJobsCount(p.getUniqueId()) >= 2) {
                        plugin.getLangManager().sendMessage(p, "jobsgui.msg_1");
                    } else {
                        jobsModule.joinJob(p.getUniqueId(), targetJob);
                        p.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize("<green>Vous avez rejoint le mÃƒÆ’Ã‚Â©tier " + targetJob.getDisplayName() + " !"));
                    }
                }
                openGUI(p);
            }
        }
    }
}

