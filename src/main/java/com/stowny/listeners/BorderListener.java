package com.stowny.listeners;

import com.stowny.STowny;
import com.stowny.model.ChunkPos;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Oyuncu bir kasabanin arazisine girip cikinca ekranda baslik gosterir (sadece aktif dunyada). */
public class BorderListener implements Listener {

    private final STowny plugin;
    private final Map<UUID, String> lastTown = new HashMap<>();

    public BorderListener(STowny plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null) {
            return;
        }
        Chunk from = event.getFrom().getChunk();
        Chunk to = event.getTo().getChunk();
        if (from.getX() == to.getX() && from.getZ() == to.getZ() && from.getWorld().equals(to.getWorld())) {
            return;
        }

        Player player = event.getPlayer();
        if (!plugin.worlds().isAllowed(player.getWorld())) {
            return;
        }

        ChunkPos pos = new ChunkPos(to.getWorld().getName(), to.getX(), to.getZ());
        String ownerTownName = plugin.data().getClaimOwner(pos);
        String previous = lastTown.get(player.getUniqueId());

        boolean changed = (ownerTownName == null && previous != null)
                || (ownerTownName != null && !ownerTownName.equalsIgnoreCase(previous));
        if (!changed) {
            return;
        }

        Title.Times times = Title.Times.times(Duration.ofMillis(250), Duration.ofSeconds(2), Duration.ofMillis(500));
        if (ownerTownName != null) {
            lastTown.put(player.getUniqueId(), ownerTownName);
            player.showTitle(Title.title(plugin.lang().get("entering-land", "%town%", ownerTownName),
                    plugin.lang().get("entering-land-sub"), times));
        } else {
            lastTown.remove(player.getUniqueId());
            player.showTitle(Title.title(plugin.lang().get("leaving-land"), Component.empty(), times));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        lastTown.remove(event.getPlayer().getUniqueId());
    }
}
