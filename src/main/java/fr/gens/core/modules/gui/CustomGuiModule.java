package fr.gens.core.modules.gui;

import fr.gens.core.CorePlugin;
import fr.gens.core.modules.Module;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import fr.gens.core.utils.PlaceholderUtils;

import java.io.File;
import java.lang.reflect.Field;
import org.bukkit.command.CommandMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomGuiModule implements Module, Listener, CommandExecutor {

    private final CorePlugin plugin;
    private boolean enabled = false;
    private final Map<String, CustomMenu> menus = new HashMap<>();

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
        
        plugin.getCommand("menu").setExecutor(this);
        plugin.getLogger().info("[CustomGui] Module activé, " + menus.size() + " menus loaded.");
    }

    @Override
    public void disable() {
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
                
                CustomMenu menu = new CustomMenu(title, rows * 9, menuName, customCommand);

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
            if (itemName != null) meta.displayName(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(itemName));
            List<String> lore = section.getStringList("lore");
            if (lore != null && !lore.isEmpty()) meta.lore(java.util.Optional.ofNullable(lore).orElse(java.util.Collections.emptyList()).stream().map(s -> net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize((String)s)).collect(java.util.stream.Collectors.toList()));
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;
        
        String menuName = "principal";
        if (args.length > 0) {
            menuName = args[0];
        }

        CustomMenu menu = menus.get(menuName);
        if (menu != null) {
            openMenu(p, menu);
        } else {
            p.sendMessage("<red>Menu introuvable : " + menuName);
        }
        return true;
    }

    public void openMenu(Player player, CustomMenu menu) {
        String parsedTitle = PlaceholderUtils.setPlaceholders(plugin, player, menu.getTitle());
        Inventory inv = Bukkit.createInventory(null, menu.getSize(), net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(parsedTitle));

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
                    String name = PlaceholderUtils.setPlaceholders(plugin, player, meta.getDisplayName());
                    meta.displayName(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(name));
                }
                if (meta.hasLore()) {
                    List<String> newLore = new ArrayList<>();
                    for (String line : meta.getLore()) {
                        String parsedLine = PlaceholderUtils.setPlaceholders(plugin, player, line);
                        if (parsedLine != null && !parsedLine.contains("REMOVE_LINE")) {
                            newLore.add(parsedLine);
                        }
                    }
                    meta.lore(java.util.Optional.ofNullable(newLore).orElse(java.util.Collections.emptyList()).stream().map(s -> net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize((String)s)).collect(java.util.stream.Collectors.toList()));
                }
                item.setItemMeta(meta);
            }
            inv.setItem(entry.getKey(), item);
        }
        player.openInventory(inv);
    }

    private void registerDynamicCommand(String cmd, CustomMenu menu) {
        try {
            Field bukkitCommandMap = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            bukkitCommandMap.setAccessible(true);
            CommandMap commandMap = (CommandMap) bukkitCommandMap.get(Bukkit.getServer());

            Command command = new Command(cmd) {
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

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!enabled) return;
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player p = (Player) event.getWhoClicked();

        String title = event.getView().getTitle();
        CustomMenu clickedMenu = null;
        for (CustomMenu menu : menus.values()) {
            String parsedTitle = PlaceholderUtils.setPlaceholders(plugin, p, menu.getTitle());
            if (parsedTitle.equals(title)) {
                clickedMenu = menu;
                break;
            }
        }

        if (clickedMenu != null) {
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
        private final String id;
        private final String customCommand;
        private final Map<Integer, MenuItem> items = new HashMap<>();

        public CustomMenu(String title, int size, String id, String customCommand) {
            this.title = title;
            this.size = size;
            this.id = id;
            this.customCommand = customCommand;
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
