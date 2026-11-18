package com.stowny.managers;

import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/** config.yml -> world-restriction ayarina gore plugin'in hangi dunyalarda aktif oldugunu belirler. */
public class WorldManager {

    private final JavaPlugin plugin;

    public WorldManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isAllowed(World world) {
        if (world == null) {
            return false;
        }
        if (!plugin.getConfig().getBoolean("world-restriction.enabled", false)) {
            return true;
        }
        List<String> worlds = plugin.getConfig().getStringList("world-restriction.worlds");
        for (String w : worlds) {
            if (w.equalsIgnoreCase(world.getName())) {
                return true;
            }
        }
        return false;
    }
}
