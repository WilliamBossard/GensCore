package fr.gens.core.modules.headdrop;

import fr.gens.core.CorePlugin;
import fr.gens.core.modules.Module;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

public class HeadDropModule implements Module, Listener {

    private final CorePlugin plugin;
    private final Random random = new Random();
    private double dropChance;
    private boolean enabled = false;

    public HeadDropModule(CorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "HeadDrop";
    }

    @Override
    public String getDescription() {
        return "Drop de têtes lors de la mort d'un joueur";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void enable() {
        enabled = true;
        // Ajouter la configuration si elle n'existe pas
        if (!plugin.getConfig().contains("headdrop.chance")) {
            plugin.getConfig().set("headdrop.chance", 10.0); // 10% par défaut
            plugin.saveConfig();
        }
        this.dropChance = plugin.getConfig().getDouble("headdrop.chance", 10.0);
        
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getLogger().info("[HeadDrop] Module activé avec " + dropChance + "% de chance de drop.");
    }

    @Override
    public void disable() {
        enabled = false;
        plugin.getLogger().info("[HeadDrop] Module désactivé.");
    }

    public void setDropChance(double dropChance) {
        this.dropChance = dropChance;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!isEnabled()) return;

        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        // Vérifie si on est dans les probabilités de drop
        if (random.nextDouble() * 100.0 <= dropChance) {
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            
            if (meta != null) {
                meta.setOwningPlayer(victim);
                meta.setDisplayName("§eTête de §6" + victim.getName());

                List<String> lore = new ArrayList<>();
                lore.add("§8§m------------------------");
                
                if (killer != null) {
                    lore.add("§c⚔ Tué par : §f" + killer.getName());
                    
                    ItemStack weapon = killer.getInventory().getItemInMainHand();
                    String weaponName = "§7A mains nues";
                    if (weapon != null && weapon.getType() != Material.AIR) {
                        weaponName = "§7" + weapon.getType().name().replace("_", " ");
                        if (weapon.hasItemMeta() && weapon.getItemMeta().hasDisplayName()) {
                            weaponName = weapon.getItemMeta().getDisplayName();
                        }
                    }
                    lore.add("§c🗡 Arme : §f" + weaponName);
                } else {
                    lore.add("§c💀 Cause : §fMorts naturelles / Environnement");
                }
                
                String date = new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date());
                lore.add("§e📅 Date : §f" + date);
                lore.add("§8§m------------------------");
                
                meta.setLore(lore);
                head.setItemMeta(meta);
            }
            
            // Ajouter la tête au butin
            event.getDrops().add(head);
        }
    }
}
