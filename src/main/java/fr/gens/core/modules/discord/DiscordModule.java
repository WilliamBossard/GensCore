package fr.gens.core.modules.discord;

import fr.gens.core.CorePlugin;
import fr.gens.core.modules.Module;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.Webhook;

import java.awt.Color;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;


public class DiscordModule extends ListenerAdapter implements Module, CommandExecutor, TabCompleter, Listener {

    private final CorePlugin plugin;
    private boolean enabled = false;
    private JDA jda;
    private WebhookClient webhookClient;

    // Code liaison Discord ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ {UUID, timestamp d'expiration}
    private record PendingLink(UUID uuid, long expiresAt) {}
    private final Map<String, PendingLink> pendingLinks = new HashMap<>();
    private static final long LINK_EXPIRY_MS = 10L * 60 * 1000; // 10 minutes
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public DiscordModule(CorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "Discord";
    }

    @Override
    public String getDescription() {
        return "GÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¨re le bot Discord intÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©grÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â© et la liaison des comptes (Remplace DiscordSRV).";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void enable() {
        enabled = true;
        
        // Initialisation de la config
        boolean configChanged = false;
        if (!plugin.getConfigManager().getConfig("modules/discord.yml").contains("discord.bot_token")) {
            plugin.getConfigManager().getConfig("modules/discord.yml").set("discord.bot_token", "VOTRE_TOKEN_ICI");
            configChanged = true;
        }
        if (!plugin.getConfigManager().getConfig("modules/discord.yml").contains("discord.chat_channel_id")) {
            plugin.getConfigManager().getConfig("modules/discord.yml").set("discord.chat_channel_id", "VOTRE_CHANNEL_ID_ICI");
            configChanged = true;
        }
        if (!plugin.getConfigManager().getConfig("modules/discord.yml").contains("discord.linked_role_id")) {
            plugin.getConfigManager().getConfig("modules/discord.yml").set("discord.linked_role_id", "Linked"); // Role name or ID
            configChanged = true;
        }
        if (!plugin.getConfigManager().getConfig("modules/discord.yml").contains("discord.log_channel_id")) {
            plugin.getConfigManager().getConfig("modules/discord.yml").set("discord.log_channel_id", "");
            configChanged = true;
        }
        if (configChanged) {
            plugin.getConfigManager().saveConfig("modules/discord.yml");
        }

        String token = plugin.getConfigManager().getConfig("modules/discord.yml").getString("discord.bot_token", "");
        if (token == null || token.isEmpty() || token.equals("VOTRE_TOKEN_ICI")) {
            plugin.getLangManager().sendConsoleWarning("discordmodule.log_1");
            return;
        }

        org.bukkit.command.PluginCommand cmd_discord = plugin.getCommand("discord");
        if (cmd_discord != null) { cmd_discord.setExecutor(this); cmd_discord.setTabCompleter(this); }

        // DÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©sactivation des logs intempestifs de JDA
        java.util.logging.Logger.getLogger("net.dv8tion").setLevel(java.util.logging.Level.OFF);
        java.util.logging.Logger.getLogger("JDA").setLevel(java.util.logging.Level.OFF);

        // Forcer le chargement de la classe JDA dans le main thread pour initialiser SLF4J correctement
        try { Class.forName("net.dv8tion.jda.api.JDABuilder"); } catch (Exception ignored) {}

        // Lancement asynchrone pour ne pas bloquer le serveur
        ClassLoader pluginClassLoader = getClass().getClassLoader();
        CompletableFuture.runAsync(() -> {
            Thread.currentThread().setContextClassLoader(pluginClassLoader);
            try {
                jda = JDABuilder.createDefault(token)
                        .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                        .addEventListeners(this)
                        .build();
                jda.awaitReady();
                plugin.getLangManager().sendConsoleMessage("discordmodule.log_2");
                
                jda.upsertCommand("resetpassword", "Changer le mot de passe de votre compte Minecraft")
                   .addOption(OptionType.STRING, "nouveau_mdp", "Votre nouveau mot de passe", true)
                   .queue();
                
                plugin.getServer().getPluginManager().registerEvents(this, plugin);

                sendBotMessage("**Le serveur a dÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©marrÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â© !**");
                
            } catch (Exception e) {
                plugin.getLogger().severe("[Discord] Erreur lors de la connexion du bot : " + e.getMessage());
            }
        });
    }

    @Override
    public void disable() {
        org.bukkit.event.HandlerList.unregisterAll(this);
        enabled = false;
        if (jda != null) {
            sendBotMessage(" **Le serveur est maintenant ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©teint.**");
            
            if (webhookClient != null) {
                webhookClient.close();
            }
            
            jda.shutdown();
            jda = null;
        }
        pendingLinks.clear();
        plugin.getLangManager().sendConsoleMessage("discordmodule.log_3");
    }

    public void sendBotMessage(String content) {
        if (jda == null) return;
        String channelId = plugin.getConfigManager().getConfig("modules/discord.yml").getString("discord.chat_channel_id");
        if (channelId == null || channelId.isEmpty()) return;
        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel != null) {
            channel.sendMessage(content).queue();
        }
    }

    public void sendBotEmbed(String title, String imageUrl, Color color) {
        if (jda == null || !enabled) return;
        String channelId = plugin.getConfigManager().getConfig("modules/discord.yml").getString("discord.chat_channel_id", "");
        if (channelId.isEmpty()) return;
        
        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel != null) {
            EmbedBuilder embed = new EmbedBuilder();
            if (imageUrl != null) {
                embed.setAuthor(title, null, imageUrl);
            } else {
                embed.setTitle(title);
            }
            embed.setColor(color);
            channel.sendMessageEmbeds(embed.build()).queue();
        }
    }

    public void sendBotLogEmbed(String title, String desc, Color color) {
        if (jda == null || !enabled) return;
        String channelId = plugin.getConfigManager().getConfig("modules/discord.yml").getString("discord.log_channel_id", "");
        if (channelId == null || channelId.isEmpty()) channelId = plugin.getConfigManager().getConfig("modules/discord.yml").getString("discord.chat_channel_id", "");
        if (channelId.isEmpty()) return;
        
        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel != null) {
            EmbedBuilder embed = new EmbedBuilder();
            embed.setTitle("Logs Moderation: " + title);
            embed.setDescription(desc);
            embed.setColor(color);
            channel.sendMessageEmbeds(embed.build()).queue();
        }
    }
    
    public void logAuthEvent(String playerName, String action, Color color) {
        if (jda == null) return;
        String logChannelId = plugin.getConfigManager().getConfig("modules/discord.yml").getString("discord.log_channel_id");
        String chatChannelId = plugin.getConfigManager().getConfig("modules/discord.yml").getString("discord.chat_channel_id");
        String channelId = (logChannelId != null && !logChannelId.isEmpty()) ? logChannelId : chatChannelId;
        
        if (channelId == null || channelId.isEmpty()) return;
        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel != null) {
            EmbedBuilder embed = new EmbedBuilder();
            embed.setTitle("\uD83D\uDD12 " + action);
            embed.setDescription("**" + playerName + "** s'est authentifiÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â© avec succÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¨s.");
            embed.setColor(color);
            embed.setTimestamp(java.time.Instant.now());
            channel.sendMessageEmbeds(embed.build()).queue();
        }
    }

    private void sendChatWebhook(String message, String avatarUrl, String username) {
        if (jda == null) return;
        String channelId = plugin.getConfigManager().getConfig("modules/discord.yml").getString("discord.chat_channel_id");
        if (channelId == null || channelId.isEmpty()) return;

        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) return;

        if (webhookClient != null) {
            WebhookMessageBuilder builder = new WebhookMessageBuilder()
                    .setUsername(username)
                    .setAvatarUrl(avatarUrl)
                    .setContent(message);
            webhookClient.send(builder.build());
            return;
        }

        channel.retrieveWebhooks().queue(webhooks -> {
            Webhook hook = webhooks.stream().filter(w -> w.getName().equals("GensBotHook")).findFirst().orElse(null);
            if (hook == null) {
                channel.createWebhook("GensBotHook").queue(newHook -> {
                    webhookClient = WebhookClient.withUrl(newHook.getUrl());
                    sendChatWebhook(message, avatarUrl, username);
                });
            } else {
                webhookClient = WebhookClient.withUrl(hook.getUrl());
                sendChatWebhook(message, avatarUrl, username);
            }
        });
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        if (!enabled) return;
        
        String prefix = "";
        try {
            net.luckperms.api.model.user.User user = net.luckperms.api.LuckPermsProvider.get().getUserManager().getUser(event.getPlayer().getUniqueId());
            if (user != null) {
                String lpPrefix = user.getCachedData().getMetaData().getPrefix();
                if (lpPrefix != null) {
                    net.kyori.adventure.text.Component prefixComp = fr.gens.core.utils.PlaceholderUtils.parseToComponent(lpPrefix);
                    prefix = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(prefixComp) + " ";
                }
            }
        } catch (Exception ignored) {}
        
        String guild = "";
        fr.gens.core.modules.teams.TeamData team = plugin.getTeamManager().getPlayerTeam(event.getPlayer().getUniqueId());
        if (team != null) guild = "[" + team.getName() + "] ";
        
        String username = prefix + guild + event.getPlayer().getName();
        String messageText = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(event.message());
        
        sendChatWebhook(messageText, "https://mc-heads.net/avatar/" + event.getPlayer().getName(), username);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!enabled) return;
        Player p = event.getPlayer();
        sendBotEmbed(p.getName() + " a rejoint le serveur", "https://mc-heads.net/avatar/" + p.getName(), Color.GREEN);
        
        // Synchroniser le badge Discord
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            fr.gens.core.modules.stats.StatsModule statsModule = (fr.gens.core.modules.stats.StatsModule) plugin.getModuleManager().getModule("stats");
            String discordId = statsModule != null ? statsModule.getStatsDAO().getDiscordId(p.getUniqueId()) : null;
            boolean isLinked = (discordId != null && !discordId.isEmpty());
            
            try {
                LuckPerms luckPerms = LuckPermsProvider.get();
                User user = luckPerms.getUserManager().getUser(p.getUniqueId());
                if (user != null) {
                    boolean hasPerm = p.hasPermission("genscore.discord.linked");
                    boolean changed = false;
                    
                    if (isLinked && !hasPerm) {
                        user.data().add(Node.builder("genscore.discord.linked").build());
                        changed = true;
                    } else if (!isLinked && hasPerm) {
                        user.data().remove(Node.builder("genscore.discord.linked").build());
                        // Aussi retirer l'ancien groupe DiscordSRV au cas ou
                        user.data().remove(Node.builder("group.linked").build());
                        changed = true;
                    }
                    
                    if (changed) {
                        luckPerms.getUserManager().saveUser(user);
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Error synchronizing LuckPerms Discord for " + p.getName());
            }
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (!enabled) return;
        sendBotEmbed(event.getPlayer().getName() + " a quittÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â© le serveur", "https://mc-heads.net/avatar/" + event.getPlayer().getName(), Color.RED);
    }

    @EventHandler
    public void onAdvancementDone(org.bukkit.event.player.PlayerAdvancementDoneEvent event) {
        if (!enabled) return;
        org.bukkit.advancement.Advancement advancement = event.getAdvancement();
        if (advancement.getDisplay() == null) return;
        
        // Only announce advancements that should be announced in chat (no recipes, no hidden ones)
        if (!advancement.getDisplay().doesAnnounceToChat()) return;

        String title = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(advancement.getDisplay().title());
        String message = event.getPlayer().getName() + " a accompli le progrÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¨s [" + title + "] !";
        
        sendBotEmbed(message, "https://mc-heads.net/avatar/" + event.getPlayer().getName(), Color.YELLOW);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            if ("link".startsWith(args[0].toLowerCase())) completions.add("link");
        }
        return completions;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;

        if (args.length > 0 && args[0].equalsIgnoreCase("link")) {
            fr.gens.core.modules.stats.StatsModule statsModule = (fr.gens.core.modules.stats.StatsModule) plugin.getModuleManager().getModule("stats");
            if (p.hasPermission("genscore.discord.linked") && statsModule != null && statsModule.getStatsDAO().getDiscordId(p.getUniqueId()) != null) {
                plugin.getLangManager().sendMessage(p, "discordmodule.msg_1");
                return true;
            }

            // Code sÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©curisÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â© : 6 chiffres via SecureRandom = 1 000 000 combinaisons
            String code = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
            pendingLinks.put(code, new PendingLink(p.getUniqueId(), System.currentTimeMillis() + LINK_EXPIRY_MS));
            
            plugin.getLangManager().sendMessage(p, "discordmodule.msg_2");
            p.sendMessage("<yellow>!link " + code);
            plugin.getLangManager().sendMessage(p, "discordmodule.msg_3");
            return true;
        }
        
        plugin.getLangManager().sendMessage(p, "discordmodule.msg_4");
        return true;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot() || !event.isFromGuild()) return;

        String[] args = event.getMessage().getContentRaw().split(" ");
        if (args[0].equalsIgnoreCase("!link") && args.length == 2) {
            String code = args[1];

            // Purger les codes expirÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©s
            pendingLinks.entrySet().removeIf(e -> System.currentTimeMillis() > e.getValue().expiresAt());

            PendingLink link = pendingLinks.remove(code);
            if (link == null || System.currentTimeMillis() > link.expiresAt()) {
                event.getChannel().sendMessage("Code invalide ou expirÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â© (10 min max). Utilisez `/discord link` en jeu pour en gÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©nÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©rer un nouveau.").queue();
                return;
            }

            UUID uuid = link.uuid();
            Guild finalTargetGuild = event.getGuild();

            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    LuckPerms api = LuckPermsProvider.get();
                    User user = api.getUserManager().getUser(uuid);
                    if (user != null) {
                        user.data().add(Node.builder("genscore.discord.linked").build());
                        api.getUserManager().saveUser(user);
                        
                        Player p = Bukkit.getPlayer(uuid);
                        if (p != null) {
                            plugin.getLangManager().sendMessage(p, "discordmodule.msg_5");
                        }
                    }
                    fr.gens.core.modules.stats.StatsModule statsModule = (fr.gens.core.modules.stats.StatsModule) plugin.getModuleManager().getModule("stats");
                    if (statsModule != null) statsModule.getStatsDAO().setDiscordId(uuid, event.getAuthor().getId());
                } catch (Exception e) {
                    plugin.getLogger().warning("Error linking Discord: " + e.getMessage());
                }

                // Essayer de donner le rÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â´le sur Discord (Asynchrone)
                if (finalTargetGuild != null) {
                    String roleId = plugin.getConfigManager().getConfig("modules/discord.yml").getString("discord.linked_role_id", "Linked");
                    if (roleId != null && !roleId.isEmpty() && !roleId.equals("ID_DU_ROLE_JOUEUR")) {
                        Role role = null;
                        try {
                            role = finalTargetGuild.getRoleById(roleId);
                        } catch (NumberFormatException ignored) {}
                        
                        if (role == null) {
                            var roles = finalTargetGuild.getRolesByName(roleId, true);
                            if (!roles.isEmpty()) {
                                role = roles.get(0);
                            }
                        }
                        
                        Member member = event.getMember();
                        if (role != null && member != null) {
                            finalTargetGuild.addRoleToMember(member, role).queue();
                        }
                    }
                }
            });

            event.getChannel().sendMessage("Ton compte Minecraft a ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©tÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â© liÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â© avec succÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¨s !").queue();
        } else if (event.getChannel().getId().equals(plugin.getConfigManager().getConfig("modules/discord.yml").getString("discord.chat_channel_id"))) {
            // Discord -> Minecraft Chat
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                fr.gens.core.modules.stats.StatsModule statsModule = (fr.gens.core.modules.stats.StatsModule) plugin.getModuleManager().getModule("stats");
                UUID uuid = statsModule != null ? statsModule.getStatsDAO().getUuidFromDiscord(event.getAuthor().getId()) : null;
                String prefix = "<gray>[Joueur] ";
                String guild = "";
                String playerName = event.getAuthor().getName();
                
                if (uuid != null) {
                    try {
                        net.luckperms.api.model.user.User user = net.luckperms.api.LuckPermsProvider.get().getUserManager().loadUser(uuid).get();
                        if (user != null) {
                            String lpPrefix = user.getCachedData().getMetaData().getPrefix();
                            if (lpPrefix != null) prefix = lpPrefix + " ";
                            playerName = user.getUsername() != null ? user.getUsername() : playerName;
                        }
                    } catch (Exception ignored) {}
                    
                    fr.gens.core.modules.teams.TeamData team = plugin.getTeamManager().getPlayerTeam(uuid);
                    if (team != null) guild = "<yellow>[" + team.getName() + "] ";
                }
                
                String finalMessage = "<blue>[Discord] " + prefix + guild + "<white>" + playerName + " <dark_gray>ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â» <gray>" + event.getMessage().getContentDisplay();
                Bukkit.getServer().broadcast(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(finalMessage.replace("ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â§", "").replace("&", "")));
            });
        }
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getName().equals("resetpassword")) {
            String newPassword = event.getOption("nouveau_mdp").getAsString();
            String discordId = event.getUser().getId();
            fr.gens.core.modules.stats.StatsModule statsModule = (fr.gens.core.modules.stats.StatsModule) plugin.getModuleManager().getModule("stats");
            UUID uuid = statsModule != null ? statsModule.getStatsDAO().getUuidFromDiscord(discordId) : null;
            
            if (uuid == null) {
                event.reply("Vous n'avez liÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â© aucun compte Minecraft ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â  ce compte Discord.").setEphemeral(true).queue();
                return;
            }
            
            if (newPassword.length() < 4) {
                event.reply("Le mot de passe doit faire au moins 4 caractÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¨res.").setEphemeral(true).queue();
                return;
            }

            String salt = fr.gens.core.modules.auth.AuthModule.generateSalt();
            String hash = fr.gens.core.modules.auth.AuthModule.hashPassword(newPassword, salt);
            fr.gens.core.modules.auth.AuthModule authModule = (fr.gens.core.modules.auth.AuthModule) plugin.getModuleManager().getModule("auth");
            if (authModule != null) {
                authModule.getAuthDAO().updatePassword(uuid, hash, salt);
            }
            
            if (authModule != null) {
                authModule.forceLogout(uuid);
            }
            
            event.reply("Votre mot de passe Minecraft a ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©tÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â© changÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â© avec succÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¨s ! Vous pouvez maintenant vous connecter en jeu avec `/login`.").setEphemeral(true).queue();
        }
    }
}


