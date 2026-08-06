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
        Inventory inv = Bukkit.createInventory(null, 36, "§8■ §x§f§f§b§7§0§3Métiers §8■");

        // Fill background
        ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.setDisplayName(" ");
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
            meta.setDisplayName(type.getColor() + "§l" + type.getDisplayName());
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("§7Niveau: §f" + level);
            lore.add("§7XP: §f" + String.format("%.1f", xp) + " §8/ §f" + nextXp);
            
            int percent = (int) ((xp / nextXp) * 100);
            String progressBar = createProgressBar(percent);
            
            lore.add("§8" + progressBar + " §8(§7" + percent + "%§8)");
            lore.add("");
            if (hasJob) {
                lore.add("§a■ §7Statut : §a§lActif");
                lore.add("§8▶ §cCliquez pour démissionner");
            } else {
                lore.add("§c■ §7Statut : §c§lInactif");
                lore.add("§8▶ §aCliquez pour rejoindre");
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
            
            inv.setItem(slots[i], item);
        }

        player.openInventory(inv);
    }

    private String createProgressBar(int percent) {
        int totalBars = 20;
        int activeBars = (percent * totalBars) / 100;
        StringBuilder sb = new StringBuilder("§a");
        for (int i = 0; i < totalBars; i++) {
            if (i == activeBars) sb.append("§7");
            sb.append("|");
        }
        return sb.toString();
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.getView().getTitle().equals("§8■ §x§f§f§b§7§0§3Métiers §8■")) {
            e.setCancelled(true);
            if (!(e.getWhoClicked() instanceof Player)) return;
            Player p = (Player) e.getWhoClicked();
            
            ItemStack clicked = e.getCurrentItem();
            if (clicked == null || !clicked.hasItemMeta()) return;
            
            String name = clicked.getItemMeta().getDisplayName();
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
                    p.sendMessage("§cVous avez quitté le métier " + targetJob.getDisplayName() + " !");
                } else {
                    if (jobsModule.getActiveJobsCount(p.getUniqueId()) >= 2) {
                        p.sendMessage("§cVous avez déjà 2 métiers actifs ! Quittez-en un d'abord.");
                    } else {
                        jobsModule.joinJob(p.getUniqueId(), targetJob);
                        p.sendMessage("§aVous avez rejoint le métier " + targetJob.getDisplayName() + " !");
                    }
                }
                openGUI(p);
            }
        }
    }
}
