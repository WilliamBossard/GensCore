package fr.gens.core.modules.perks;

import fr.gens.core.CorePlugin;
import fr.gens.core.modules.GuiModule.GensGuiHolder;
import fr.gens.core.modules.quests.QuestModule;
import fr.gens.core.utils.BedrockFormManager;
import fr.gens.core.utils.FloodgateUtil;
import fr.gens.core.utils.PlaceholderUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class SoloPerkGui {

    private final CorePlugin plugin;
    private final SoloPerkManager manager;
    private final NamespacedKey perkKey;

    public SoloPerkGui(CorePlugin plugin, SoloPerkManager manager) {
        this.plugin = plugin;
        this.manager = manager;
        this.perkKey = new NamespacedKey(plugin, "solo_perk_id");
    }

    public void openMenu(Player player) {
        if (FloodgateUtil.isBedrockPlayer(player.getUniqueId())) {
            openBedrockMenu(player);
            return;
        }

        UUID uuid = player.getUniqueId();
        int completedQuests = manager.getQuestsCompletedTotal(uuid);
        boolean eco = manager.isEconomyEnabled();
        Set<String> unlocked = manager.getUnlockedPerks(uuid);

        PerkGuiHolder holder = new PerkGuiHolder();
        Inventory inv = Bukkit.createInventory(holder, 54, PlaceholderUtils.parseToComponent("<dark_gray>Bonus & Maitrises de Quetes"));
        holder.setInventory(inv);

        // Vitres de séparation
        ItemStack grayGlass = createGlass(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < 9; i++) inv.setItem(i, grayGlass);
        for (int i = 18; i < 27; i++) inv.setItem(i, grayGlass);
        for (int i = 45; i < 54; i++) inv.setItem(i, grayGlass);

        // Séparateur informatif au centre de la séparation (Slot 22)
        ItemStack sepItem = new ItemStack(Material.NETHER_STAR);
        ItemMeta sepMeta = sepItem.getItemMeta();
        if (sepMeta != null) {
            sepMeta.displayName(PlaceholderUtils.parseToComponent("<gold><bold>--- Maitrises Majeures Payantes ---</bold></gold>"));
            List<String> sepLore = new ArrayList<>();
            sepLore.add("<dark_gray>Competences exclusives a acheter avec vos gains de quetes");
            sepLore.add("");
            sepLore.add("<gray>Requièrent un palier de quetes terminees");
            if (eco) {
                sepLore.add("<gray>ET un paiement en <gold><bold>dollars ($)</bold></gold> sur votre compte.");
            } else {
                sepLore.add("<gray>ET un paiement en <green><bold>niveaux d'experience (XP)</bold></green>.");
            }
            sepMeta.lore(sepLore.stream().map(PlaceholderUtils::parseToComponent).toList());
            sepItem.setItemMeta(sepMeta);
        }
        inv.setItem(22, sepItem);

        // Header : Profil joueur (Slot 4)
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta headMeta = head.getItemMeta();
        if (headMeta != null) {
            headMeta.displayName(PlaceholderUtils.parseToComponent("<gold><bold>" + player.getName() + " - Progression"));
            List<String> lore = new ArrayList<>();
            lore.add("<gray>Quetes terminees a vie : <yellow><bold>" + completedQuests);
            if (eco) {
                fr.gens.core.modules.EconomyModule ecoMod = (fr.gens.core.modules.EconomyModule) plugin.getModuleManager().getModule("economy");
                double bal = ecoMod != null ? ecoMod.getBalance(uuid) : 0.0;
                lore.add("<gray>Solde personnel : <green>" + String.format("%.2f", bal) + " $");
            } else {
                lore.add("<gray>Niveaux d'experience : <green>" + player.getLevel() + " XP");
            }
            lore.add("<dark_gray>Terminez des quetes pour debloquer des bonus !");
            headMeta.lore(lore.stream().map(PlaceholderUtils::parseToComponent).toList());
            head.setItemMeta(headMeta);
        }
        inv.setItem(4, head);

        // Paliers Gratuits (Ligne 2 : slots 10, 11, 12, 14, 15, 16)
        SoloPerkType[] freePerks = {
                SoloPerkType.FREE_REROLL,
                SoloPerkType.EXTRA_HOME,
                SoloPerkType.SPEED_BOOST,
                SoloPerkType.JOBS_XP,
                SoloPerkType.WARMUP_REDUCTION,
                SoloPerkType.FEED_ACCESS
        };
        int[] freeSlots = {10, 11, 12, 14, 15, 16};

        for (int i = 0; i < freePerks.length; i++) {
            SoloPerkType perk = freePerks[i];
            int slot = freeSlots[i];
            inv.setItem(slot, buildPerkItem(player, perk, completedQuests, unlocked));
        }

        // Maîtrises Majeures Payantes (Ligne 4 : slots 29, 30, 31, 32, 33)
        SoloPerkType[] majorPerks = {
                SoloPerkType.MAGNET,
                SoloPerkType.DOUBLE_DROP,
                SoloPerkType.PORTABLE_WORKBENCH,
                SoloPerkType.AUTO_SMELT,
                SoloPerkType.KEEP_EXP
        };
        int[] majorSlots = {29, 30, 31, 32, 33};

        for (int i = 0; i < majorPerks.length; i++) {
            SoloPerkType perk = majorPerks[i];
            int slot = majorSlots[i];
            inv.setItem(slot, buildPerkItem(player, perk, completedQuests, unlocked));
        }

        // Bouton retour aux quêtes (Slot 49)
        ItemStack backItem = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backItem.getItemMeta();
        if (backMeta != null) {
            backMeta.displayName(PlaceholderUtils.parseToComponent("<yellow><bold>Retour aux Quetes"));
            backItem.setItemMeta(backMeta);
        }
        inv.setItem(49, backItem);

        plugin.getFoliaLib().getScheduler().runAtEntity(player, task -> player.openInventory(inv));
    }

    private ItemStack buildPerkItem(Player player, SoloPerkType perk, int completedQuests, Set<String> unlocked) {
        UUID uuid = player.getUniqueId();
        boolean isUnlocked = unlocked.contains(perk.getId());
        boolean eco = manager.isEconomyEnabled();
        fr.gens.core.modules.EconomyModule ecoMod = (fr.gens.core.modules.EconomyModule) plugin.getModuleManager().getModule("economy");
        double balance = (eco && ecoMod != null) ? ecoMod.getBalance(uuid) : 0.0;
        int xp = player.getLevel();

        Material mat = isUnlocked ? perk.getIcon() : (completedQuests >= perk.getRequiredQuests() ? Material.EMERALD : Material.GRAY_DYE);
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        String name = perk.getNameFr();
        String costFormatted = eco ? String.format("%.0f $", perk.getCostMoney()) : (perk.getCostXp() + " XP");
        if (perk.isMajor()) {
            meta.displayName(PlaceholderUtils.parseToComponent("<gold><bold>" + name + "</bold> <gray>(" + costFormatted + " & " + perk.getRequiredQuests() + " quetes)</gray>"));
        } else {
            meta.displayName(PlaceholderUtils.parseToComponent("<yellow><bold>" + name + "</bold> <gray>(Gratuit - " + perk.getRequiredQuests() + " quetes)</gray>"));
        }

        List<String> lore = new ArrayList<>();
        lore.add("<dark_gray>" + perk.getDescriptionFr());
        lore.add("");

        if (perk.isMajor()) {
            lore.add("<gray>Type : <gold><bold>Maitrise Majeure Payante</bold></gold>");
            if (eco) {
                lore.add("<gray>Cout d'achat : <gold><bold>" + String.format("%.0f", perk.getCostMoney()) + " $</bold></gold>");
            } else {
                lore.add("<gray>Cout d'achat : <green><bold>" + perk.getCostXp() + " niveaux d'XP</bold></green>");
            }
        } else {
            lore.add("<gray>Type : <aqua><bold>Palier Gratuit</bold></aqua>");
            lore.add("<gray>Cout d'achat : <green><bold>GRATUIT</bold></green>");
        }

        boolean hasQuests = completedQuests >= perk.getRequiredQuests();
        int remaining = perk.getRequiredQuests() - completedQuests;
        if (hasQuests) {
            lore.add("<gray>Condition quetes : <green><bold>VALIDEE</bold> (" + completedQuests + "/" + perk.getRequiredQuests() + " quetes)</green>");
        } else {
            lore.add("<gray>Condition quetes : <red><bold>INSUFFISANT</bold> (" + remaining + " manquante(s))</red>");
        }
        lore.add("");

        if (isUnlocked) {
            if (perk.isToggleable()) {
                boolean active = manager.isPerkActive(uuid, perk);
                if (active) {
                    lore.add("<green><bold>[ACTIF]</bold> <gray>- Clic pour desactiver</gray>");
                } else {
                    lore.add("<red><bold>[DESACTIVE]</bold> <gray>- Clic pour activer</gray>");
                }
            } else {
                lore.add("<green><bold>[ACQUIS ET ACTIF EN PERMANENCE]</bold></green>");
            }
        } else {
            if (hasQuests) {
                if (perk.isMajor()) {
                    boolean hasFunds = eco ? (balance >= perk.getCostMoney()) : (xp >= perk.getCostXp());
                    if (hasFunds) {
                        lore.add("<yellow><bold>[CLIQUEZ POUR ACHETER (" + costFormatted + ")]</bold></yellow>");
                    } else {
                        double missingMoney = perk.getCostMoney() - balance;
                        int missingXp = perk.getCostXp() - xp;
                        lore.add("<red><bold>[FONDS INSUFFISANTS]</bold> <gray>(Il vous manque " + (eco ? String.format("%.0f $", missingMoney) : (missingXp + " XP")) + ")</gray></red>");
                    }
                } else {
                    lore.add("<green><bold>[CLIQUEZ POUR RECLAMER GRATUITEMENT]</bold></green>");
                }
            } else {
                lore.add("<red><bold>[VERROUILLE]</bold> <gray>(Atteignez d'abord " + perk.getRequiredQuests() + " quetes terminees)</gray></red>");
            }
        }

        meta.lore(lore.stream().map(PlaceholderUtils::parseToComponent).toList());
        meta.getPersistentDataContainer().set(perkKey, PersistentDataType.STRING, perk.getId());
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createGlass(Material mat) {
        ItemStack glass = new ItemStack(mat);
        ItemMeta meta = glass.getItemMeta();
        if (meta != null) {
            meta.displayName(PlaceholderUtils.parseToComponent(" "));
            glass.setItemMeta(meta);
        }
        return glass;
    }

    private void openBedrockMenu(Player player) {
        UUID uuid = player.getUniqueId();
        int completed = manager.getQuestsCompletedTotal(uuid);
        Set<String> unlocked = manager.getUnlockedPerks(uuid);
        boolean eco = manager.isEconomyEnabled();

        String title = "Bonus de Quetes";
        StringBuilder content = new StringBuilder("Quetes terminees : " + completed + "\n\nChoisissez un bonus :");

        List<BedrockFormManager.BedrockButton> buttons = new ArrayList<>();

        for (SoloPerkType perk : SoloPerkType.values()) {
            boolean isUnlocked = unlocked.contains(perk.getId());
            String btnText;

            if (isUnlocked) {
                if (perk.isToggleable()) {
                    boolean active = manager.isPerkActive(uuid, perk);
                    btnText = perk.getNameFr() + "\n" + (active ? "[ACTIF - Clic toggle]" : "[DESACTIVE - Clic toggle]");
                } else {
                    btnText = perk.getNameFr() + "\n[ACQUIS]";
                }
            } else {
                String costStr = perk.isMajor() ? (eco ? (String.format("%.0f", perk.getCostMoney()) + " $") : (perk.getCostXp() + " XP")) : "GRATUIT";
                if (completed >= perk.getRequiredQuests()) {
                    if (perk.isMajor()) {
                        btnText = perk.getNameFr() + "\n[ACHETER: " + costStr + "]";
                    } else {
                        btnText = perk.getNameFr() + "\n[RECLAMER GRATUIT]";
                    }
                } else {
                    btnText = perk.getNameFr() + "\n[VERROUILLE: " + perk.getRequiredQuests() + " quetes | Cout: " + costStr + "]";
                }
            }

            buttons.add(new BedrockFormManager.BedrockButton(btnText, perk.getIcon(), p -> {
                handlePerkAction(p, perk);
            }));
        }

        buttons.add(new BedrockFormManager.BedrockButton("Retour aux Quetes", Material.ARROW, p -> {
            QuestModule qm = (QuestModule) plugin.getModuleManager().getModule("quests");
            if (qm != null) qm.openQuestsMenu(p);
        }));

        BedrockFormManager.openSimpleForm(player, title, content.toString(), buttons);
    }

    private void handlePerkAction(Player player, SoloPerkType perk) {
        UUID uuid = player.getUniqueId();
        if (manager.hasPerk(uuid, perk)) {
            if (perk.isToggleable()) {
                boolean newState = manager.togglePerk(uuid, perk);
                player.sendMessage(PlaceholderUtils.parseToComponent(
                        "<yellow>" + perk.getNameFr() + " est desormais " + (newState ? "<green>ACTIVE</green>" : "<red>DESACTIVE</red>") + ".</yellow>"
                ));
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                openMenu(player);
            }
            return;
        }

        SoloPerkManager.UnlockResult result = manager.unlockPerk(player, perk);
        switch (result) {
            case SUCCESS -> {
                player.sendMessage(PlaceholderUtils.parseToComponent("<green><bold>Felicitations !</bold> Vous avez debloque : <yellow>" + perk.getNameFr() + "</yellow> !</green>"));
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
                openMenu(player);
            }
            case NOT_ENOUGH_QUESTS -> {
                player.sendMessage(PlaceholderUtils.parseToComponent("<red>Vous n'avez pas accompli assez de quetes pour ce bonus (" + perk.getRequiredQuests() + " requises).</red>"));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            }
            case NOT_ENOUGH_FUNDS -> {
                player.sendMessage(PlaceholderUtils.parseToComponent("<red>Fonds insuffisants ! Il vous faut " + String.format("%.0f", perk.getCostMoney()) + " $.</red>"));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            }
            case NOT_ENOUGH_XP -> {
                player.sendMessage(PlaceholderUtils.parseToComponent("<red>Niveaux d'experience insuffisants ! Il vous faut " + perk.getCostXp() + " niveaux d'XP.</red>"));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            }
            case ALREADY_UNLOCKED -> {
                player.sendMessage(PlaceholderUtils.parseToComponent("<yellow>Vous possedez deja ce bonus.</yellow>"));
            }
            default -> {
                player.sendMessage(PlaceholderUtils.parseToComponent("<red>Une erreur est survenue.</red>"));
            }
        }
    }

    private class PerkGuiHolder implements GensGuiHolder {
        private Inventory inventory;

        public void setInventory(Inventory inv) {
            this.inventory = inv;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }

        @Override
        public void onClick(InventoryClickEvent event) {
            event.setCancelled(true);
            Player player = (Player) event.getWhoClicked();
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;

            if (clicked.getType() == Material.ARROW) {
                QuestModule qm = (QuestModule) plugin.getModuleManager().getModule("quests");
                if (qm != null) qm.openQuestsMenu(player);
                return;
            }

            ItemMeta meta = clicked.getItemMeta();
            if (meta == null) return;

            if (meta.getPersistentDataContainer().has(perkKey, PersistentDataType.STRING)) {
                String perkId = meta.getPersistentDataContainer().get(perkKey, PersistentDataType.STRING);
                SoloPerkType perk = SoloPerkType.fromId(perkId);
                if (perk != null) {
                    handlePerkAction(player, perk);
                }
            }
        }
    }
}
