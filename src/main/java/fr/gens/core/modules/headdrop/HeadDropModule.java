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
        if (!plugin.getConfigManager().getConfig("modules/headdrop.yml").contains("headdrop.chance")) {
            plugin.getConfigManager().getConfig("modules/headdrop.yml").set("headdrop.chance", 10.0); // 10% par défaut
            plugin.getConfigManager().saveConfig("modules/headdrop.yml");
        }
        this.dropChance = plugin.getConfigManager().getConfig("modules/headdrop.yml").getDouble("headdrop.chance", 10.0);
        
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getLogger().info("[HeadDrop] Module activé avec " + dropChance + "% drop chance.");
    }

    @Override
    public void disable() {
        org.bukkit.event.HandlerList.unregisterAll(this);
        enabled = false;
        plugin.getLangManager().sendConsoleMessage("headdropmodule.log_1");
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
                meta.setPlayerProfile(victim.getPlayerProfile());
                meta.displayName(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<yellow>Tête de <gold>" + victim.getName()));

                List<String> lore = new ArrayList<>();
                lore.add("<dark_gray><strikethrough>------------------------");
                
                if (killer != null) {
                    lore.add("<red> Tué par : <white>" + killer.getName());
                    
                    ItemStack weapon = killer.getInventory().getItemInMainHand();
                    String weaponName = "<gray>A mains nues";
                    if (weapon != null && weapon.getType() != Material.AIR) {
                        weaponName = "<gray>" + weapon.getType().name().replace("_", " ");
                        if (weapon.hasItemMeta() && weapon.getItemMeta().hasDisplayName()) {
                            weaponName = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(weapon.getItemMeta().displayName());
                        }
                    }
                    lore.add("<red> Arme : <white>" + weaponName);
                } else {
                    lore.add("<red> Cause : <white>Morts naturelles / Environnement");
                }
                
                String date = new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date());
                lore.add("<yellow> Date : <white>" + date);
                lore.add("<dark_gray><strikethrough>------------------------");
                
                meta.lore(java.util.Optional.ofNullable(lore).orElse(java.util.Collections.emptyList()).stream().map(s -> fr.gens.core.utils.PlaceholderUtils.parseToComponent((String)s)).collect(java.util.stream.Collectors.toList()));
                head.setItemMeta(meta);
            }
            
            // Ajouter la tête au butin
            event.getDrops().add(head);
        }
    }
}




