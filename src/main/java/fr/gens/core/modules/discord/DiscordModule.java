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
import net.dv8tion.jda.api.interactions.commands.build.Commands;
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
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.Webhook;

import java.awt.Color;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class DiscordModule extends ListenerAdapter implements Module, CommandExecutor, TabCompleter, Listener {

    private final CorePlugin plugin;
    private boolean enabled = false;
    private JDA jda;
    private WebhookClient webhookClient;
    
    // Map code -> UUID of player
    private final Map<String, UUID> pendingLinks = new HashMap<>();

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
        if (!plugin.getConfig().contains("discord.bot_token")) {
            plugin.getConfig().set("discord.bot_token", "VOTRE_TOKEN_ICI");
            configChanged = true;
        }
        if (!plugin.getConfig().contains("discord.chat_channel_id")) {
            plugin.getConfig().set("discord.chat_channel_id", "VOTRE_CHANNEL_ID_ICI");
            configChanged = true;
        }
        if (!plugin.getConfig().contains("discord.linked_role_id")) {
            plugin.getConfig().set("discord.linked_role_id", "Linked"); // Role name or ID
            configChanged = true;
        }
        if (!plugin.getConfig().contains("discord.log_channel_id")) {
            plugin.getConfig().set("discord.log_channel_id", "");
            configChanged = true;
        }
        if (configChanged) {
            plugin.saveConfig();
        }

        String token = plugin.getConfig().getString("discord.bot_token", "");
        if (token == null || token.isEmpty() || token.equals("VOTRE_TOKEN_ICI")) {
            plugin.getLogger().warning("[Discord] Token invalide ! Configurez le token dans config.yml via l'interface Web.");
            return;
        }

        plugin.getCommand("discord").setExecutor(this);
        plugin.getCommand("discord").setTabCompleter(this);

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
                plugin.getLogger().info("[Discord] Bot connecté avec succès !");
                
                jda.upsertCommand("resetpassword", "Changer le mot de passe de votre compte Minecraft")
                   .addOption(OptionType.STRING, "nouveau_mdp", "Votre nouveau mot de passe", true)
                   .queue();
                
                plugin.getServer().getPluginManager().registerEvents(this, plugin);

                sendBotMessage("**Le serveur a démarré !**");
                
            } catch (Exception e) {
                plugin.getLogger().severe("[Discord] Erreur lors de la connexion du bot : " + e.getMessage());
            }
        });
    }

    @Override
    public void disable() {
        enabled = false;
        if (jda != null) {
            sendBotMessage(" **Le serveur est maintenant éteint.**");
            
            if (webhookClient != null) {
                webhookClient.close();
            }
            
            jda.shutdown();
            jda = null;
        }
        pendingLinks.clear();
        plugin.getLogger().info("[Discord] Module désactivé.");
    }

    public void sendBotMessage(String content) {
        if (jda == null) return;
        String channelId = plugin.getConfig().getString("discord.chat_channel_id");
        if (channelId == null || channelId.isEmpty()) return;
        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel != null) {
            channel.sendMessage(content).queue();
        }
    }

    public void sendBotEmbed(String title, String imageUrl, Color color) {
        if (jda == null || !enabled) return;
        String channelId = plugin.getConfig().getString("discord.chat_channel_id", "");
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
        String channelId = plugin.getConfig().getString("discord.log_channel_id", "");
        if (channelId == null || channelId.isEmpty()) channelId = plugin.getConfig().getString("discord.chat_channel_id", "");
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
        String logChannelId = plugin.getConfig().getString("discord.log_channel_id");
        String chatChannelId = plugin.getConfig().getString("discord.chat_channel_id");
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
        String channelId = plugin.getConfig().getString("discord.chat_channel_id");
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
    public void onChat(AsyncPlayerChatEvent event) {
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
        
        sendChatWebhook(event.getMessage(), "https://mc-heads.net/avatar/" + event.getPlayer().getName(), username);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!enabled) return;
        Player p = event.getPlayer();
        sendBotEmbed(p.getName() + " a rejoint le serveur", "https://mc-heads.net/avatar/" + p.getName(), Color.GREEN);
        
        // Synchroniser le badge Discord
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String discordId = plugin.getDatabaseManager().getDiscordId(p.getUniqueId());
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
                plugin.getLogger().warning("Erreur lors de la synchro LuckPerms Discord pour " + p.getName());
            }
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (!enabled) return;
        sendBotEmbed(event.getPlayer().getName() + " a quitté le serveur", "https://mc-heads.net/avatar/" + event.getPlayer().getName(), Color.RED);
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
            if (p.hasPermission("genscore.discord.linked") && plugin.getDatabaseManager().getDiscordId(p.getUniqueId()) != null) {
                p.sendMessage("§cVotre compte est déjà lié !");
                return true;
            }

            String code = String.format("%04d", new Random().nextInt(10000));
            pendingLinks.put(code, p.getUniqueId());
            
            p.sendMessage("§a§l[Discord] §7Pour lier votre compte, allez sur le serveur Discord et tapez :");
            p.sendMessage("§e!link " + code);
            p.sendMessage("§7(Ce code expire au prochain redémarrage)");
            return true;
        }
        
        p.sendMessage("§cUtilisation: /discord link");
        return true;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot() || !event.isFromGuild()) return;

        String[] args = event.getMessage().getContentRaw().split(" ");
        if (args[0].equalsIgnoreCase("!link") && args.length == 2) {
            String code = args[1];

            if (!pendingLinks.containsKey(code)) {
                event.getChannel().sendMessage("Code invalide ou expiré.").queue();
                return;
            }

            UUID uuid = pendingLinks.remove(code);
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
                            p.sendMessage("§a§l[Discord] §aVotre compte a été lié avec succès !");
                        }
                    }
                    plugin.getDatabaseManager().setDiscordId(uuid, event.getAuthor().getId());
                } catch (Exception e) {
                    plugin.getLogger().warning("Erreur lors de la liaison Discord: " + e.getMessage());
                }

                // Essayer de donner le rôle sur Discord (Asynchrone)
                if (finalTargetGuild != null) {
                    String roleId = plugin.getConfig().getString("discord.linked_role_id", "Linked");
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
        } else if (event.getChannel().getId().equals(plugin.getConfig().getString("discord.chat_channel_id"))) {
            // Discord -> Minecraft Chat
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                UUID uuid = plugin.getDatabaseManager().getUuidFromDiscord(event.getAuthor().getId());
                String prefix = "§7[Joueur] ";
                String guild = "";
                String playerName = event.getAuthor().getName();
                
                if (uuid != null) {
                    try {
                        net.luckperms.api.model.user.User user = net.luckperms.api.LuckPermsProvider.get().getUserManager().loadUser(uuid).get();
                        if (user != null) {
                            String lpPrefix = user.getCachedData().getMetaData().getPrefix();
                            if (lpPrefix != null) prefix = org.bukkit.ChatColor.translateAlternateColorCodes('&', lpPrefix) + " ";
                            playerName = user.getUsername() != null ? user.getUsername() : playerName;
                        }
                    } catch (Exception ignored) {}
                    
                    fr.gens.core.modules.teams.TeamData team = plugin.getTeamManager().getPlayerTeam(uuid);
                    if (team != null) guild = "§e[" + team.getName() + "] ";
                }
                
                String finalMessage = "§9[Discord] " + prefix + guild + "§f" + playerName + " §8» §7" + event.getMessage().getContentDisplay();
                Bukkit.getServer().broadcastMessage(finalMessage);
            });
        }
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getName().equals("resetpassword")) {
            String newPassword = event.getOption("nouveau_mdp").getAsString();
            String discordId = event.getUser().getId();
            UUID uuid = plugin.getDatabaseManager().getUuidFromDiscord(discordId);
            
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
            plugin.getDatabaseManager().updatePassword(uuid, hash, salt);
            
            fr.gens.core.modules.auth.AuthModule authModule = (fr.gens.core.modules.auth.AuthModule) plugin.getModuleManager().getModule("auth");
            if (authModule != null) {
                authModule.forceLogout(uuid);
            }
            
            event.reply("Votre mot de passe Minecraft a été changé avec succès ! Vous pouvez maintenant vous connecter en jeu avec `/login`.").setEphemeral(true).queue();
        }
    }
}
