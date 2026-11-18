package com.stowny.listeners;

import com.stowny.STowny;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class JoinListener implements Listener {

    private final STowny plugin;

    public JoinListener(STowny plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (plugin.getConfig().getBoolean("scoreboard.enabled-by-default", true)) {
            plugin.scoreboard().enable(event.getPlayer().getUniqueId());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.scoreboard().disable(event.getPlayer().getUniqueId());
    }
}
