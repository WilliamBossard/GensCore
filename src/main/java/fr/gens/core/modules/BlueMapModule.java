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
            registerBlueMapListener();
            plugin.getFoliaLib().getScheduler().runLater((t2) -> {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "bluemap start");
                plugin.getLangManager().sendConsoleMessage("bluemapmodule.log_1");
                updateAllTeamTerritories();
            }, 40L);
        }
    }

    private void registerBlueMapListener() {
        try {
            Class<?> apiClass = Class.forName("de.bluecolored.bluemap.api.BlueMapAPI");
            java.lang.reflect.Method onEnableMethod = apiClass.getMethod("onEnable", java.util.function.Consumer.class);
            onEnableMethod.invoke(null, (java.util.function.Consumer<Object>) api -> {
                plugin.getLogger().info("[BlueMap] API active détectée, synchronisation des territoires de guildes...");
                refreshBlueMapMarkers(api);
            });
        } catch (Throwable ignored) {
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
                Class<?> apiClass = Class.forName("de.bluecolored.bluemap.api.BlueMapAPI");
                Object optInstance = apiClass.getMethod("getInstance").invoke(null);
                if (optInstance instanceof java.util.Optional) {
                    java.util.Optional<?> opt = (java.util.Optional<?>) optInstance;
                    if (!opt.isPresent()) return;
                    refreshBlueMapMarkers(opt.get());
                } else if (optInstance != null) {
                    refreshBlueMapMarkers(optInstance);
                }
            } catch (ClassNotFoundException e) {
                // BlueMap API non disponible en runtime
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "[BlueMap] Erreur lors de la mise à jour des marqueurs", e);
            }
        });
    }

    private void refreshBlueMapMarkers(Object apiInstance) {
        try {
            if (plugin.getTeamManager() == null) return;
            TeamClaimManager claimMgr = plugin.getTeamManager().getClaimManager();
            if (claimMgr == null) return;

            Map<String, Integer> claims = claimMgr.getClaimsMap();

            Class<?> markerSetClass = Class.forName("de.bluecolored.bluemap.api.markers.MarkerSet");
            Class<?> extrudeMarkerClass = Class.forName("de.bluecolored.bluemap.api.markers.ExtrudeMarker");
            Class<?> shapeClass = Class.forName("de.bluecolored.bluemap.api.math.Shape");
            Class<?> colorClass = Class.forName("de.bluecolored.bluemap.api.math.Color");

            java.lang.reflect.Constructor<?> colorStrConstructor = colorClass.getConstructor(String.class);
            java.lang.reflect.Constructor<?> colorRgbaConstructor = colorClass.getConstructor(int.class, int.class, int.class, float.class);
            java.lang.reflect.Method createRectMethod = shapeClass.getMethod("createRect", double.class, double.class, double.class, double.class);

            // Iterer les maps de BlueMap et y injecter les markers
            Iterable<?> maps = (Iterable<?>) apiInstance.getClass().getMethod("getMaps").invoke(apiInstance);
            for (Object map : maps) {
                String mapId = (String) map.getClass().getMethod("getId").invoke(map);
                String mapWorldId = "";
                try {
                    Object blueMapWorld = map.getClass().getMethod("getWorld").invoke(map);
                    if (blueMapWorld != null) {
                        mapWorldId = (String) blueMapWorld.getClass().getMethod("getId").invoke(blueMapWorld);
                    }
                } catch (Exception ignored) {}

                Object markerSets = map.getClass().getMethod("getMarkerSets").invoke(map);
                if (markerSets instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> setsMap = (Map<String, Object>) markerSets;
                    
                    Object markerSet = setsMap.get("genscore_claims");
                    if (markerSet == null) {
                        Object setBuilder = markerSetClass.getMethod("builder").invoke(null);
                        setBuilder.getClass().getMethod("label", String.class).invoke(setBuilder, "Territoires de Guildes");
                        invokeBoolean(setBuilder, "defaultHidden", false);
                        invokeBoolean(setBuilder, "toggleable", true);
                        markerSet = setBuilder.getClass().getMethod("build").invoke(setBuilder);
                        setsMap.put("genscore_claims", markerSet);
                    }

                    @SuppressWarnings("unchecked")
                    Map<String, Object> markers = (Map<String, Object>) markerSetClass.getMethod("getMarkers").invoke(markerSet);
                    markers.clear(); // Reinitialiser pour supprimer les chunks libérés

                    for (Map.Entry<String, Integer> entry : claims.entrySet()) {
                        String[] parts = entry.getKey().split(":");
                        if (parts.length != 3) continue;

                        String chunkWorld = parts[0];
                        int cx = Integer.parseInt(parts[1]);
                        int cz = Integer.parseInt(parts[2]);
                        int teamId = entry.getValue();

                        String cw = chunkWorld.toLowerCase();
                        String mw = mapWorldId.toLowerCase();
                        String mi = mapId.toLowerCase();

                        boolean worldMatches = mw.isEmpty()
                            || mw.equals(cw)
                            || mw.contains(cw)
                            || mi.equals(cw)
                            || mi.contains(cw)
                            || (cw.equals("world") && (mw.contains("overworld") || mi.contains("overworld")))
                            || (cw.contains("nether") && (mw.contains("nether") || mi.contains("nether")))
                            || ((cw.contains("the_end") || cw.contains("end")) && (mw.contains("end") || mi.contains("end")));

                        if (!worldMatches) continue;

                        TeamData team = plugin.getTeamManager().getTeam(teamId);
                        if (team == null) continue;

                        String hexColor = team.getColor();
                        if (hexColor == null || !hexColor.startsWith("#") || hexColor.length() != 7) {
                            hexColor = "#2ecc71"; // Couleur verte par defaut
                        }

                        // Construction des couleurs de bordure et de remplissage
                        Object lineColor = colorStrConstructor.newInstance(hexColor);
                        int r = (int) colorClass.getMethod("getRed").invoke(lineColor);
                        int g = (int) colorClass.getMethod("getGreen").invoke(lineColor);
                        int b = (int) colorClass.getMethod("getBlue").invoke(lineColor);
                        Object fillColor = colorRgbaConstructor.newInstance(r, g, b, 0.35f);

                        double minX = cx * 16.0;
                        double minZ = cz * 16.0;
                        double maxX = minX + 16.0;
                        double maxZ = minZ + 16.0;

                        Object shape = createRectMethod.invoke(null, minX, minZ, maxX, maxZ);

                        Object markerBuilder = extrudeMarkerClass.getMethod("builder").invoke(null);
                        markerBuilder.getClass().getMethod("label", String.class).invoke(markerBuilder, "Guilde : " + team.getName());
                        markerBuilder.getClass().getMethod("detail", String.class).invoke(markerBuilder,
                            "<div style='font-family: sans-serif; padding: 6px; min-width: 160px;'>" +
                            "<b style='font-size: 15px; color:" + hexColor + ";'>" + team.getName() + "</b><br>" +
                            "<span style='color: #aaa; font-size: 12px;'>Territoire revendiqué</span><br>" +
                            "<span style='font-size: 12px;'>Chunk: [" + cx + ", " + cz + "]</span><br>" +
                            "<span style='font-size: 11px; color: #888;'>X: " + (int)minX + ".." + (int)maxX + " | Z: " + (int)minZ + ".." + (int)maxZ + "</span>" +
                            "</div>"
                        );
                        invokeShape(markerBuilder, shape, shapeClass);
                        markerBuilder.getClass().getMethod("lineColor", colorClass).invoke(markerBuilder, lineColor);
                        markerBuilder.getClass().getMethod("fillColor", colorClass).invoke(markerBuilder, fillColor);
                        invokeInt(markerBuilder, "lineWidth", 2);
                        invokeBoolean(markerBuilder, "depthTestEnabled", false);
                        try {
                            markerBuilder.getClass().getMethod("centerPosition").invoke(markerBuilder);
                        } catch (Exception ignored) {}

                        Object marker = markerBuilder.getClass().getMethod("build").invoke(markerBuilder);
                        markers.put("claim_" + chunkWorld + "_" + cx + "_" + cz, marker);
                    }
                    if (!markers.isEmpty()) {
                        plugin.getLogger().info("[BlueMap] " + markers.size() + " territoire(s) synchronisé(s) sur la carte '" + mapId + "'.");
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "[BlueMap] Erreur lors du rendu des territoires", e);
        }
    }

    private void invokeBoolean(Object target, String methodName, boolean value) {
        try {
            target.getClass().getMethod(methodName, Boolean.class).invoke(target, Boolean.valueOf(value));
            return;
        } catch (NoSuchMethodException ignored) {
            try {
                target.getClass().getMethod(methodName, boolean.class).invoke(target, value);
            } catch (Exception ignored2) {}
        } catch (Exception ignored) {}
    }

    private void invokeInt(Object target, String methodName, int value) {
        try {
            target.getClass().getMethod(methodName, Integer.class).invoke(target, Integer.valueOf(value));
            return;
        } catch (NoSuchMethodException ignored) {
            try {
                target.getClass().getMethod(methodName, int.class).invoke(target, value);
            } catch (Exception ignored2) {}
        } catch (Exception ignored) {}
    }

    private void invokeShape(Object markerBuilder, Object shape, Class<?> shapeClass) {
        try {
            markerBuilder.getClass().getMethod("shape", shapeClass, float.class, float.class)
                    .invoke(markerBuilder, shape, -64f, 320f);
            return;
        } catch (NoSuchMethodException ignored) {
            try {
                markerBuilder.getClass().getMethod("shape", shapeClass, double.class, double.class)
                        .invoke(markerBuilder, shape, -64.0, 320.0);
            } catch (Exception ignored2) {}
        } catch (Exception ignored) {}
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






