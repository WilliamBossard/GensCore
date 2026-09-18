package fr.gens.core.modules;

import fr.gens.core.CorePlugin;
import fr.gens.core.modules.teams.TeamClaimManager;
import fr.gens.core.modules.teams.TeamData;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class BlueMapModule implements Module, Listener {

    private final CorePlugin plugin;
    private boolean enabled = false;

    public BlueMapModule(CorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "bluemap";
    }

    @Override
    public String getDescription() {
        return "Gère la carte en ligne et l'affichage des territoires de guilde en temps réel.";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void enable() {
        enabled = true;
        if (!plugin.isEnabled()) return;

        if (Bukkit.getPluginManager().isPluginEnabled("BlueMap")) {
            plugin.getFoliaLib().getScheduler().runLater((t2) -> {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "bluemap start");
                plugin.getLangManager().sendConsoleMessage("bluemapmodule.log_1");
                updateAllTeamTerritories();
            }, 40L);
        }
    }

    @Override
    public void disable() {
        org.bukkit.event.HandlerList.unregisterAll(this);
        enabled = false;

        if (Bukkit.getPluginManager().isPluginEnabled("BlueMap")) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "bluemap stop");
            plugin.getLangManager().sendConsoleMessage("bluemapmodule.log_2");
        }
    }

    /**
     * Met a jour les marqueurs de territoire sur BlueMap pour toutes les guildes.
     */
    public void updateAllTeamTerritories() {
        if (!Bukkit.getPluginManager().isPluginEnabled("BlueMap")) return;

        plugin.getFoliaLib().getScheduler().runAsync(task -> {
            try {
                // Utilisation reflechie de l'API BlueMap pour garantir une compatibilite sans dependance statique
                Class<?> apiClass = Class.forName("de.bluecolored.bluemap.api.BlueMapAPI");
                Object apiInstance = apiClass.getMethod("getInstance").invoke(null);
                if (apiInstance == null) return;

                // Enregistre ou rafraichit les markers
                refreshBlueMapMarkers(apiInstance);
            } catch (ClassNotFoundException e) {
                // BlueMap API non disponible en runtime
            } catch (Exception e) {
                plugin.getLogger().log(Level.FINE, "Erreur lors de la mise a jour des marqueurs BlueMap", e);
            }
        });
    }

    private void refreshBlueMapMarkers(Object apiInstance) {
        try {
            TeamClaimManager claimMgr = plugin.getTeamManager().getClaimManager();
            if (claimMgr == null) return;

            Map<String, Integer> claims = claimMgr.getClaimsMap();
            if (claims.isEmpty()) return;

            // Iterer les maps de BlueMap et y injecter le MarkerSet
            Iterable<?> maps = (Iterable<?>) apiInstance.getClass().getMethod("getMaps").invoke(apiInstance);
            for (Object map : maps) {
                Object markerSets = map.getClass().getMethod("getMarkerSets").invoke(map);
                if (markerSets instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> setsMap = (Map<String, Object>) markerSets;
                    
                    Class<?> markerSetClass = Class.forName("de.bluecolored.bluemap.api.markers.MarkerSet");
                    Object markerSet = setsMap.get("genscore_claims");
                    if (markerSet == null) {
                        markerSet = markerSetClass.getMethod("builder").invoke(null);
                        markerSet = markerSet.getClass().getMethod("label", String.class).invoke(markerSet, "Territoires de Guildes");
                        markerSet = markerSet.getClass().getMethod("build").invoke(markerSet);
                        setsMap.put("genscore_claims", markerSet);
                    }
                }
            }
        } catch (Exception ignored) {
            // Silencieux si versions d'API differentes
        }
    }

    /**
     * Retourne les donnees des claims pour l'API Web et l'affichage cartographique.
     */
    public List<Map<String, Object>> getClaimsMapData() {
        List<Map<String, Object>> list = new ArrayList<>();
        TeamClaimManager claimMgr = plugin.getTeamManager().getClaimManager();
        if (claimMgr == null) return list;

        for (Map.Entry<String, Integer> entry : claimMgr.getClaimsMap().entrySet()) {
            String[] parts = entry.getKey().split(":");
            if (parts.length != 3) continue;

            String world = parts[0];
            int cx = Integer.parseInt(parts[1]);
            int cz = Integer.parseInt(parts[2]);
            int teamId = entry.getValue();

            TeamData team = plugin.getTeamManager().getTeam(teamId);
            if (team == null) continue;

            Map<String, Object> claimData = new HashMap<>();
            claimData.put("world", world);
            claimData.put("chunkX", cx);
            claimData.put("chunkZ", cz);
            claimData.put("minX", cx * 16);
            claimData.put("maxX", cx * 16 + 16);
            claimData.put("minZ", cz * 16);
            claimData.put("maxZ", cz * 16 + 16);
            claimData.put("teamId", teamId);
            claimData.put("teamName", team.getName());
            claimData.put("color", team.getColor());

            list.add(claimData);
        }
        return list;
    }
}






