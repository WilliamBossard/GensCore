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
import org.incendo.cloud.annotations.Command;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;


public class DiscordModule extends ListenerAdapter implements Module, Listener {

    private final CorePlugin plugin;
    private boolean enabled = false;
    private JDA jda;
    private WebhookClient webhookClient;

    // Code liaison Discord → {UUID, timestamp d'expiration}
    private record PendingLink(UUID uuid, long expiresAt) {}
    private final Map<String, PendingLink> pendingLinks = new ConcurrentHashMap<>();
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
        return "Gère le bot Discord intégré et la liaison des comptes (Remplace DiscordSRV).";
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


        // Désactivation des logs intempestifs de JDA
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

                sendBotMessage("🟢 **Le serveur a démarré !**");
                
            } catch (Exception e) {
                plugin.getLogger().severe("[Discord] Erreur lors de la connexion du bot : " + e.getMessage());
                plugin.getLangManager().sendConsoleError("discordmodule.log_1");
            }
        });
    }

    @Override
    public void registerCommands(fr.gens.core.CorePlugin plugin) {
        if (plugin.getCommandManager() != null && plugin.getCommandManager().getAnnotationParser() != null) {
            plugin.getCommandManager().getAnnotationParser().parse(this);
        }
    }

    @Override
    public void disable() {
        if (!enabled) return;
        enabled = false;
        org.bukkit.event.HandlerList.unregisterAll(this);
        
        if (jda != null) {
            String channelId = plugin.getConfigManager().getConfig("modules/discord.yml").getString("discord.chat_channel_id");
            if (channelId != null && !channelId.trim().isEmpty()) {
                TextChannel channel = jda.getTextChannelById(channelId);
                if (channel != null) {
                    try {
                        channel.sendMessage("🔴 **Le serveur est maintenant éteint.**").complete();
                    } catch (Exception ignored) {}
                }
            }
            
            if (webhookClient != null) {
                webhookClient.close();
            }
            
            jda.shutdown();
            
            try {
                // Attente pour laisser le temps à JDA de déconnecter ses WebSockets proprement
                // afin d'éviter l'erreur "zip file closed" lors de la fermeture du serveur.
                Thread.sleep(1500);
            } catch (InterruptedException ignored) {}
            
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
            embed.setDescription("**" + playerName + "** s'est authentifié avec succès.");
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

    private String getAvatarUrl(Player p) {
        if (p == null) return "https://crafthead.net/helm/Steve.png";
        
        fr.gens.core.modules.BedrockSkinModule skinModule = (fr.gens.core.modules.BedrockSkinModule) plugin.getModuleManager().getModule("bedrockskin");
        if (skinModule != null) {
            return skinModule.getHeadUrl(p.getUniqueId(), p.getName());
        }
        
        if (fr.gens.core.utils.FloodgateUtil.isBedrockPlayer(p.getUniqueId())) {
            return "https://crafthead.net/helm/" + p.getUniqueId().toString() + ".png";
        }
        return "https://crafthead.net/helm/" + p.getName() + ".png";
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        if (!enabled) return;
        
        String prefix = fr.gens.core.utils.FloodgateUtil.getJavaDiscordPrefix();
        if (fr.gens.core.utils.FloodgateUtil.isBedrockPlayer(event.getPlayer().getUniqueId())) {
            prefix = fr.gens.core.utils.FloodgateUtil.getBedrockDiscordPrefix();
        }

        // Récupérer le préfixe LuckPerms (ex: Owner, Admin)
        String platformPrefix = "";
        try {
            LuckPerms api = LuckPermsProvider.get();
            User user = api.getUserManager().getUser(event.getPlayer().getUniqueId());
            if (user != null) {
                String lpPrefix = user.getCachedData().getMetaData().getPrefix();
                if (lpPrefix != null) {
                    // On retire les codes couleurs MiniMessage
                    platformPrefix = net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().stripTags(lpPrefix).trim() + " ";
                }
            }
        } catch (Exception ignored) {}

        fr.gens.core.modules.teams.TeamData team = plugin.getTeamManager().getPlayerTeam(event.getPlayer().getUniqueId());
        String guild = (team != null) ? "[" + team.getName() + "] " : "";

        String username = platformPrefix + prefix + guild + event.getPlayer().getName();
        String messageText = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(event.message());
        
        sendChatWebhook(messageText, getAvatarUrl(event.getPlayer()), username);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!enabled) return;
        Player p = event.getPlayer();
        sendBotEmbed(p.getName() + " a rejoint le serveur", getAvatarUrl(p), Color.GREEN);
        
        // Synchroniser le badge Discord
        plugin.getFoliaLib().getScheduler().runAsync((wrappedTask) -> {
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
        sendBotEmbed(event.getPlayer().getName() + " a quitté le serveur", getAvatarUrl(event.getPlayer()), Color.RED);
    }

    @EventHandler
    public void onAdvancementDone(org.bukkit.event.player.PlayerAdvancementDoneEvent event) {
        if (!enabled) return;
        org.bukkit.advancement.Advancement advancement = event.getAdvancement();
        if (advancement.getDisplay() == null) return;
        
        // Only announce advancements that should be announced in chat (no recipes, no hidden ones)
        if (!advancement.getDisplay().doesAnnounceToChat()) return;

        String title = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(advancement.getDisplay().title());
        String message = event.getPlayer().getName() + " a accompli le progrès [" + title + "] !";
        
        sendBotEmbed(message, getAvatarUrl(event.getPlayer()), Color.YELLOW);
    }

    @Command("discord [subcommand]")
    public void executeDiscord(org.bukkit.command.CommandSender sender, @org.incendo.cloud.annotations.Argument(value = "subcommand") @org.incendo.cloud.annotations.Default(" ") String subcommand) {
        if (!(sender instanceof org.bukkit.entity.Player)) return;
        org.bukkit.entity.Player p = (org.bukkit.entity.Player) sender;
        if ("link".equalsIgnoreCase(subcommand)) {
            fr.gens.core.modules.stats.StatsModule statsModule = (fr.gens.core.modules.stats.StatsModule) plugin.getModuleManager().getModule("stats");
            if (p.hasPermission("genscore.discord.linked") && statsModule != null && statsModule.getStatsDAO().getDiscordId(p.getUniqueId()) != null) {
                plugin.getLangManager().sendMessage(p, "discordmodule.msg_1");
                return;
            }
            String code = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
            pendingLinks.put(code, new PendingLink(p.getUniqueId(), System.currentTimeMillis() + LINK_EXPIRY_MS));
            plugin.getLangManager().sendMessage(p, "discordmodule.msg_2");
            p.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<yellow>!link " + code));
            plugin.getLangManager().sendMessage(p, "discordmodule.msg_3");
        } else {
            plugin.getLangManager().sendMessage(p, "discordmodule.msg_4");
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot() || !event.isFromGuild()) return;

        String[] args = event.getMessage().getContentRaw().split(" ");
        if (args[0].equalsIgnoreCase("!link") && args.length == 2) {
            try {
                event.getMessage().delete().queue(null, (error) -> {});
            } catch (Exception ignored) {}
            
            String code = args[1];

            // Purger les codes expirés
            pendingLinks.entrySet().removeIf(e -> System.currentTimeMillis() > e.getValue().expiresAt());

            PendingLink link = pendingLinks.remove(code);
            if (link == null || System.currentTimeMillis() > link.expiresAt()) {
                event.getChannel().sendMessage("Code invalide ou expiré (10 min max). Utilisez `/discord link` en jeu pour en générer un nouveau.").queue();
                return;
            }

            UUID uuid = link.uuid();
            Guild finalTargetGuild = event.getGuild();

            plugin.getFoliaLib().getScheduler().runNextTick((t2) -> {
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

            event.getChannel().sendMessage("Ton compte Minecraft a été lié avec succès !").queue();
        } else if (event.getChannel().getId().equals(plugin.getConfigManager().getConfig("modules/discord.yml").getString("discord.chat_channel_id"))) {
            // Discord -> Minecraft Chat
            plugin.getFoliaLib().getScheduler().runAsync((wrappedTask) -> {
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
                    
                    String platformPrefix = !fr.gens.core.utils.FloodgateUtil.isFloodgateInstalled() ? "" : 
                            (fr.gens.core.utils.FloodgateUtil.isBedrockPlayer(uuid) 
                            ? fr.gens.core.utils.FloodgateUtil.getBedrockPrefix() : fr.gens.core.utils.FloodgateUtil.getJavaPrefix());
                    prefix = platformPrefix + prefix;
                }
                
                String finalMessage = "<blue>[Discord] " + prefix + guild + "<white>" + playerName + " <dark_gray>» <gray>" + event.getMessage().getContentDisplay();
                Bukkit.getServer().broadcast(fr.gens.core.utils.PlaceholderUtils.parseToComponent(finalMessage.replace("§", "").replace("&", "")));
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
                event.reply("Vous n'avez lié aucun compte Minecraft à ce compte Discord.").setEphemeral(true).queue();
                return;
            }
            
            if (newPassword.length() < 4) {
                event.reply("Le mot de passe doit faire au moins 4 caractères.").setEphemeral(true).queue();
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
            
            event.reply("Votre mot de passe Minecraft a été changé avec succès ! Vous pouvez maintenant vous connecter en jeu avec `/login`.").setEphemeral(true).queue();
        }
    }
}





