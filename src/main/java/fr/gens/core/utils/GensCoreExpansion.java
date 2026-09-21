package fr.gens.core.utils;

import fr.gens.core.CorePlugin;
import fr.gens.core.modules.EconomyModule;
import fr.gens.core.modules.teams.TeamData;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Extension officielle de GensCore pour PlaceholderAPI.
 * Fournit des variables globales et specifiques aux joueurs pour d'autres plugins
 * tels que TAB, DeluxeMenus, Scoreboards et Chat, incluant la version et le protocole ViaVersion.
 */
public class GensCoreExpansion extends PlaceholderExpansion {

    private final CorePlugin plugin;

    public GensCoreExpansion(CorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "genscore";
    }

    @Override
    public @NotNull String getAuthor() {
        return "William Bossard";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer offlinePlayer, @NotNull String params) {
        if (offlinePlayer == null) return null;
        Player player = offlinePlayer.getPlayer();
        UUID uuid = offlinePlayer.getUniqueId();

        switch (params.toLowerCase()) {
            case "client_version":
                return ViaVersionUtil.getPlayerVersionName(uuid);
            case "client_protocol":
                return String.valueOf(ViaVersionUtil.getPlayerProtocolVersion(uuid));
            case "client_type":
                return FloodgateUtil.isBedrockPlayer(uuid) ? "Bedrock" : "Java";
            case "is_legacy":
                return String.valueOf(ViaVersionUtil.isLegacyClient(uuid));
            case "is_bedrock":
                return String.valueOf(FloodgateUtil.isBedrockPlayer(uuid));
            case "ping":
                return player != null ? String.valueOf(player.getPing()) : "0";
            case "balance":
            case "money": {
                EconomyModule eco = (EconomyModule) plugin.getModuleManager().getModule("economy");
                return (eco != null && eco.isEnabled()) ? String.format("%.0f", eco.getBalance(uuid)) : "0";
            }
            case "guild":
            case "team": {
                if (plugin.getTeamManager() != null) {
                    TeamData team = plugin.getTeamManager().getPlayerTeam(uuid);
                    return team != null ? team.getName() : "Aucune";
                }
                return "Aucune";
            }
            case "guild_role": {
                if (plugin.getTeamManager() != null) {
                    TeamData team = plugin.getTeamManager().getPlayerTeam(uuid);
                    if (team != null) {
                        return team.getRoleName(uuid);
                    }
                }
                return "Aucun";
            }
            default:
                return null;
        }
    }
}
