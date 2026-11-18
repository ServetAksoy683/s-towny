package com.stowny.managers;

import com.stowny.model.ChunkPos;
import com.stowny.model.Town;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.dynmap.DynmapAPI;
import org.dynmap.markers.AreaMarker;
import org.dynmap.markers.MarkerAPI;
import org.dynmap.markers.MarkerSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Dynmap kuruluysa kasaba arazilerini haritada gosterir; degilse sessizce pasif kalir. */
public class DynmapManager {

    private final JavaPlugin plugin;
    private MarkerSet markerSet;
    private boolean enabled = false;
    private final Map<String, List<String>> markerIdsByTown = new HashMap<>();

    public DynmapManager(JavaPlugin plugin) {
        this.plugin = plugin;
        setup();
    }

    private void setup() {
        if (!plugin.getConfig().getBoolean("dynmap.enabled", true)) {
            return;
        }
        Plugin dynmapPlugin = Bukkit.getPluginManager().getPlugin("dynmap");
        if (!(dynmapPlugin instanceof DynmapAPI dynmapApi)) {
            plugin.getLogger().info("Dynmap bulunamadi, harita entegrasyonu pasif.");
            return;
        }
        MarkerAPI markerApi = dynmapApi.getMarkerAPI();
        if (markerApi == null) {
            return;
        }
        String setName = plugin.getConfig().getString("dynmap.marker-set-name", "S-Towny");
        this.markerSet = markerApi.getMarkerSet("s-towny.towns");
        if (markerSet == null) {
            this.markerSet = markerApi.createMarkerSet("s-towny.towns", setName, null, false);
        }
        this.enabled = markerSet != null;
        if (enabled) {
            plugin.getLogger().info("Dynmap entegrasyonu aktif.");
        }
    }

    public void refreshTown(Town town) {
        if (!enabled) return;
        removeTown(town.name);

        int color = parseColor(plugin.getConfig().getString("dynmap.fill-color", "#55ff55"));
        int lineColor = parseColor(plugin.getConfig().getString("dynmap.line-color", "#00aa00"));
        double fillOpacity = plugin.getConfig().getDouble("dynmap.fill-opacity", 0.35);
        double lineOpacity = plugin.getConfig().getDouble("dynmap.line-opacity", 0.8);

        List<String> ids = new ArrayList<>();
        int i = 0;
        for (ChunkPos pos : town.claims) {
            String id = "s-towny_" + town.name.toLowerCase() + "_" + (i++);
            double[] xs = {pos.x() * 16.0, pos.x() * 16.0 + 16, pos.x() * 16.0 + 16, pos.x() * 16.0};
            double[] zs = {pos.z() * 16.0, pos.z() * 16.0, pos.z() * 16.0 + 16, pos.z() * 16.0 + 16};
            AreaMarker marker = markerSet.createAreaMarker(id, town.name, false, pos.world(), xs, zs, false);
            if (marker != null) {
                marker.setFillStyle(fillOpacity, color);
                marker.setLineStyle(2, lineOpacity, lineColor);
                ids.add(id);
            }
        }
        markerIdsByTown.put(town.name.toLowerCase(), ids);
    }

    public void removeTown(String townName) {
        if (!enabled) return;
        List<String> ids = markerIdsByTown.remove(townName.toLowerCase());
        if (ids == null) return;
        for (String id : ids) {
            AreaMarker marker = markerSet.findAreaMarker(id);
            if (marker != null) marker.deleteMarker();
        }
    }

    private int parseColor(String hex) {
        try {
            return Integer.parseInt(hex.replace("#", ""), 16);
        } catch (NumberFormatException e) {
            return 0x55ff55;
        }
    }
}
