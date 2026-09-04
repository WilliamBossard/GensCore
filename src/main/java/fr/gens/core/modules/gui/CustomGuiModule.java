package fr.gens.core.modules.gui;

import fr.gens.core.CorePlugin;
import fr.gens.core.modules.Module;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import cloud.commandframework.annotations.Argument;
import cloud.commandframework.annotations.CommandMethod;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import fr.gens.core.utils.PlaceholderUtils;

import java.io.File;
import java.lang.reflect.Field;
import org.bukkit.command.CommandMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.Map;


public class CustomGuiModule implements Module, Listener {

    private final CorePlugin plugin;
    private boolean enabled = false;
    private final Map<String, CustomMenu> menus = new ConcurrentHashMap<>();

    public CustomGuiModule(CorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "CustomGui";
    }

    @Override
    public String getDescription() {
        return "Génère des menus interactifs depuis les fichiers YAML.";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void enable() {
        enabled = true;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        loadMenus();
        plugin.getLogger().info("[CustomGui] Module activé, " + menus.size() + " menus loaded.");
    }

    @Override
    public void registerCommands(fr.gens.core.CorePlugin plugin) {
        if (plugin.getCommandManager() != null && plugin.getCommandManager().getAnnotationParser() != null) {
            plugin.getCommandManager().getAnnotationParser().parse(this);
        }
    }

    @Override
    public void disable() {
        org.bukkit.event.HandlerList.unregisterAll(this);
        enabled = false;
        menus.clear();
        plugin.getLangManager().sendConsoleMessage("customguimodule.log_1");
    }

    public void loadMenus() {
        menus.clear();
        File menuDir = new File(plugin.getDataFolder(), "menus");
        if (!menuDir.exists()) {
            menuDir.mkdirs();
        }

        File[] files = menuDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                String menuName = file.getName().replace(".yml", "");
                
                ConfigurationSection settings = config.getConfigurationSection("menu-settings");
                if (settings == null) continue;

                String title = settings.getString("name", "Menu");
                int rows = settings.getInt("rows", 3);
                String customCommand = settings.getString("command");
                
                CustomMenu menu = new CustomMenu(title, rows * 9);

                // Register dynamically for autocompletion
                if (customCommand != null && !customCommand.isEmpty()) {
                    registerDynamicCommand(customCommand, menu);
                }

                for (String key : config.getKeys(false)) {
                    if (key.equals("menu-settings")) continue;
                    ConfigurationSection itemConfig = config.getConfigurationSection(key);
                    if (itemConfig == null) continue;

                    int slot = itemConfig.getInt("slot", -1);
                    if (slot < 0 || slot >= menu.getSize()) continue;

                    String type = itemConfig.getString("type", "");
                    if (type.equals("predicate")) {
                        ConfigurationSection req = itemConfig.getConfigurationSection("view-requirement");
                        String perm = null;
                        if (req != null) {
                            for (String k : req.getKeys(false)) {
                                perm = req.getString(k + ".permission");
                                if (perm != null) break;
                            }
                        }
                        
                        ItemStack btnItem = buildItem(itemConfig.getConfigurationSection("button"));
                        String btnCmd = itemConfig.getString("button.command", "");
                        
                        ItemStack fbItem = buildItem(itemConfig.getConfigurationSection("fallback"));
                        String fbCmd = itemConfig.getString("fallback.command", "");
                        
                        menu.addPredicateItem(slot, perm, btnItem, btnCmd, fbItem, fbCmd);
                    } else {
                        ItemStack item = buildItem(itemConfig);
                        String command = itemConfig.getString("command", "");
                        menu.addItem(slot, item, command);
                    }
                }
                menus.put(menuName, menu);
            }
        }
    }
    
    private ItemStack buildItem(ConfigurationSection section) {
        if (section == null) return new ItemStack(Material.STONE);
        String matName = section.getString("material", "STONE");
        Material material = Material.matchMaterial(matName);
        if (material == null) material = Material.STONE;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String itemName = section.getString("name");
            if (itemName != null) meta.displayName(fr.gens.core.utils.PlaceholderUtils.parseToComponent(itemName));
            List<String> lore = section.getStringList("lore");
            if (lore != null && !lore.isEmpty()) meta.lore(java.util.Optional.ofNullable(lore).orElse(java.util.Collections.emptyList()).stream().map(s -> fr.gens.core.utils.PlaceholderUtils.parseToComponent((String)s)).collect(java.util.stream.Collectors.toList()));
            item.setItemMeta(meta);
        }
        return item;
    }

    @CommandMethod("menu [name]")
    public void executeMenu(org.bukkit.command.CommandSender sender, @Argument(value = "name", defaultValue = "principal") String menuName) {
        if (!(sender instanceof org.bukkit.entity.Player)) return;
        org.bukkit.entity.Player p = (org.bukkit.entity.Player) sender;
        if (menuName.equals("principal") && !menus.containsKey("principal")) {
            String menuList = String.join(", ", menus.keySet());
            p.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<gold>Menus disponibles : <yellow>" + (menuList.isEmpty() ? "Aucun" : menuList)));
            return;
        }
        CustomMenu menu = menus.get(menuName);
        if (menu != null) {
            openMenu(p, menu);
        } else {
            p.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<red>Menu introuvable : " + menuName));
        }
    }

    public void openMenu(Player player, CustomMenu menu) {
        if (fr.gens.core.utils.FloodgateUtil.isBedrockPlayer(player.getUniqueId())) {
            java.util.List<fr.gens.core.utils.BedrockFormManager.BedrockButton> buttons = new java.util.ArrayList<>();
            // Sort by slot to keep order
            java.util.List<Map.Entry<Integer, CustomMenu.MenuItem>> sortedItems = new java.util.ArrayList<>(menu.getItems().entrySet());
            sortedItems.sort(Map.Entry.comparingByKey());

            for (Map.Entry<Integer, CustomMenu.MenuItem> entry : sortedItems) {
                CustomMenu.MenuItem mi = entry.getValue();
                ItemStack baseItem = mi.item;
                String cmdToRun = mi.command;
                
                if (mi.isPredicate) {
                    if (mi.permission != null && player.hasPermission(mi.permission)) {
                        baseItem = mi.item;
                        cmdToRun = mi.command;
                    } else {
                        baseItem = mi.fallbackItem;
                        cmdToRun = mi.fallbackCommand;
                    }
                }
                
                if (baseItem == null) continue;
                // Ignorer les vitres de décoration sans commande
                if ((cmdToRun == null || cmdToRun.isEmpty()) && baseItem.getType().name().contains("GLASS_PANE")) continue;
                
                ItemMeta meta = baseItem.getItemMeta();
                String btnText = "Objet";
                if (meta != null && meta.hasDisplayName()) {
                    btnText = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(meta.displayName());
                } else {
                    btnText = baseItem.getType().name();
                }
                
                if (meta != null && meta.hasLore()) {
                    for (net.kyori.adventure.text.Component loreLine : meta.lore()) {
                        String lineText = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(loreLine);
                        if (!lineText.contains("REMOVE_LINE")) {
                            btnText += "\n" + lineText;
                        }
                    }
                }
                
                final String finalCmd = cmdToRun;
                
                fr.gens.core.utils.BedrockFormManager.BedrockButton btn;
                if (baseItem.getType() == Material.PLAYER_HEAD) {
                    String headUrl;
                    if (fr.gens.core.utils.FloodgateUtil.isBedrockPlayer(player.getUniqueId())) {
                        headUrl = "https://crafthead.net/helm/" + player.getUniqueId().toString() + ".png";
                    } else {
                        headUrl = "https://crafthead.net/helm/" + player.getName() + ".png";
                    }
                    btn = new fr.gens.core.utils.BedrockFormManager.BedrockButton(btnText, headUrl, p -> {
                        executeCommand(p, finalCmd);
                    });
                } else {
                    btn = new fr.gens.core.utils.BedrockFormManager.BedrockButton(btnText, baseItem.getType(), p -> {
                        executeCommand(p, finalCmd);
                    });
                }
                buttons.add(btn);
            }
            fr.gens.core.utils.BedrockFormManager.openSimpleForm(player, menu.getTitle(), "Sélectionnez une option :", buttons);
            return;
        }

        net.kyori.adventure.text.Component parsedTitle = PlaceholderUtils.setPlaceholdersComponent(plugin, player, menu.getTitle());
        Inventory inv = Bukkit.createInventory(null, menu.getSize(), parsedTitle);

        for (Map.Entry<Integer, CustomMenu.MenuItem> entry : menu.getItems().entrySet()) {
            CustomMenu.MenuItem mi = entry.getValue();
            ItemStack baseItem = mi.item;
            
            if (mi.isPredicate) {
                if (mi.permission != null && player.hasPermission(mi.permission)) {
                    baseItem = mi.item; // button
                } else {
                    baseItem = mi.fallbackItem; // fallback
                }
            }
            if (baseItem == null) continue;
            
            ItemStack item = baseItem.clone();
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                if (meta.hasDisplayName()) {
                    net.kyori.adventure.text.Component name = PlaceholderUtils.setPlaceholdersComponent(plugin, player, net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(meta.displayName()));
                    meta.displayName(name);
                }
                if (meta.hasLore()) {
                    List<net.kyori.adventure.text.Component> newLore = new java.util.ArrayList<>();
                    for (net.kyori.adventure.text.Component compLine : meta.lore()) {
                        if (compLine == null) continue;
                        String line = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(compLine);
                        if (!line.contains("REMOVE_LINE")) {
                            newLore.add(PlaceholderUtils.setPlaceholdersComponent(plugin, player, line));
                        }
                    }
                    meta.lore(newLore);
                }
                item.setItemMeta(meta);
            }
            inv.setItem(entry.getKey(), item);
        }
        player.openInventory(inv);
    }
    
    private void executeCommand(Player p, String cmdToRun) {
        if (cmdToRun != null && !cmdToRun.isEmpty()) {
            if (cmdToRun.equalsIgnoreCase("close")) return;
            if (cmdToRun.startsWith("player: ")) {
                String cmd = cmdToRun.substring(8).replace("%player_name%", p.getName());
                if (cmd.equals("profil") || cmd.equals("tuto")) {
                    openMenu(p, menus.get(cmd));
                } else {
                    p.performCommand(cmd);
                }
            } else if (cmdToRun.startsWith("console: ")) {
                String cmd = cmdToRun.substring(9).replace("%player_name%", p.getName());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            } else {
                p.performCommand(cmdToRun);
            }
        }
    }

    private void registerDynamicCommand(String cmd, CustomMenu menu) {
        if (plugin.getCommandManager() != null) {
            try {
                cloud.commandframework.paper.PaperCommandManager<CommandSender> mgr = plugin.getCommandManager().getPaperCommandManager();
                mgr.command(
                    mgr.commandBuilder(cmd)
                        .senderType(Player.class)
                        .handler(context -> {
                            openMenu((Player) context.getSender(), menu);
                        })
                );
                plugin.getLogger().info("[Gui] Commande dynamique enregistrée via Cloud : /" + cmd);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to register dynamic command /" + cmd + " via Cloud");
            }
        } else {
            try {
                Field bukkitCommandMap = Bukkit.getServer().getClass().getDeclaredField("commandMap");
                bukkitCommandMap.setAccessible(true);
                CommandMap commandMap = (CommandMap) bukkitCommandMap.get(Bukkit.getServer());

                org.bukkit.command.Command command = new org.bukkit.command.Command(cmd) {
                    @Override
                    public boolean execute(CommandSender sender, String label, String[] args) {
                        if (sender instanceof Player) {
                            openMenu((Player) sender, menu);
                        }
                        return true;
                    }
                };
                commandMap.register("genscore", command);
                plugin.getLogger().info("[Gui] Commande dynamique enregistrée : /" + cmd);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to register dynamic command /" + cmd);
            }
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!enabled) return;
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player p = (Player) event.getWhoClicked();

        String title = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        CustomMenu clickedMenu = null;
        for (CustomMenu menu : menus.values()) {
            net.kyori.adventure.text.Component parsedTitle = PlaceholderUtils.setPlaceholdersComponent(plugin, p, menu.getTitle());
            String parsedTitleStr = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(parsedTitle);
            if (parsedTitleStr.equals(title)) {
                clickedMenu = menu;
                break;
            }
        }

        if (clickedMenu != null) {
            if (event.getClickedInventory() == null) return;
            if (!event.getClickedInventory().equals(event.getView().getTopInventory())) {
                if (event.getAction() == org.bukkit.event.inventory.InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                    event.setCancelled(true);
                }
                return;
            }
            
            event.setCancelled(true);
            int slot = event.getSlot();
            CustomMenu.MenuItem item = clickedMenu.getItems().get(slot);
            if (item != null) {
                String cmdToRun = item.command;
                if (item.isPredicate) {
                    if (item.permission != null && p.hasPermission(item.permission)) {
                        cmdToRun = item.command;
                    } else {
                        cmdToRun = item.fallbackCommand;
                    }
                }
                
                if (cmdToRun != null && !cmdToRun.isEmpty()) {
                    p.closeInventory();
                    if (cmdToRun.equalsIgnoreCase("close")) {
                        return;
                    }
                    if (cmdToRun.startsWith("player: ")) {
                        String cmd = cmdToRun.substring(8).replace("%player_name%", p.getName());
                        if (cmd.equals("profil") || cmd.equals("tuto")) {
                            openMenu(p, menus.get(cmd));
                        } else {
                            p.performCommand(cmd);
                        }
                    } else if (cmdToRun.startsWith("console: ")) {
                        String cmd = cmdToRun.substring(9).replace("%player_name%", p.getName());
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                    } else {
                        p.performCommand(cmdToRun);
                    }
                }
            }
        }
    }

    public static class CustomMenu {
        private final String title;
        private final int size;
        private final Map<Integer, MenuItem> items = new ConcurrentHashMap<>();

        public CustomMenu(String title, int size) {
            this.title = title;
            this.size = size;
        }

        public String getTitle() { return title; }
        public int getSize() { return size; }
        public void addItem(int slot, ItemStack item, String command) {
            items.put(slot, new MenuItem(item, command));
        }

        public void addPredicateItem(int slot, String permission, ItemStack btn, String btnCmd, ItemStack fb, String fbCmd) {
            items.put(slot, new MenuItem(permission, btn, btnCmd, fb, fbCmd));
        }

        public Map<Integer, MenuItem> getItems() { return items; }

        public static class MenuItem {
            public final ItemStack item;
            public final String command;
            
            public boolean isPredicate = false;
            public String permission;
            public ItemStack fallbackItem;
            public String fallbackCommand;

            public MenuItem(ItemStack item, String command) {
                this.item = item;
                this.command = command;
            }
            
            public MenuItem(String permission, ItemStack btn, String btnCmd, ItemStack fb, String fbCmd) {
                this.isPredicate = true;
                this.permission = permission;
                this.item = btn;
                this.command = btnCmd;
                this.fallbackItem = fb;
                this.fallbackCommand = fbCmd;
            }
        }
    }
}




