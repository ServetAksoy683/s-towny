package com.stowny.listeners;

import com.stowny.STowny;
import com.stowny.model.ChunkPos;
import com.stowny.model.Town;
import org.bukkit.Location;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class ProtectionListener implements Listener {

    private final STowny plugin;

    public ProtectionListener(STowny plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        Town town = ownerTown(event.getBlock().getLocation());
        if (town != null && !bypass(event.getPlayer()) && !town.canBreak(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(plugin.lang().get("protection-denied", "%town%", town.name));
        }
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        Town town = ownerTown(event.getBlock().getLocation());
        if (town != null && !bypass(event.getPlayer()) && !town.canPlace(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(plugin.lang().get("protection-denied", "%town%", town.name));
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
            return;
        }
        if (!(event.getClickedBlock().getState() instanceof Container)) {
            return;
        }
        Town town = ownerTown(event.getClickedBlock().getLocation());
        if (town != null && !bypass(event.getPlayer()) && !town.canBreak(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(plugin.lang().get("protection-denied", "%town%", town.name));
        }
    }

    @EventHandler
    public void onPvp(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim) || !(event.getDamager() instanceof Player)) {
            return;
        }
        Town town = ownerTown(victim.getLocation());
        if (town != null && !town.pvpEnabled) {
            event.setCancelled(true);
        }
    }

    private boolean bypass(Player player) {
        return !plugin.getConfig().getBoolean("protection.enabled", true)
                || player.isOp()
                || player.hasPermission("stowny.admin");
    }

    private Town ownerTown(Location location) {
        ChunkPos pos = new ChunkPos(location.getWorld().getName(), location.getBlockX() >> 4, location.getBlockZ() >> 4);
        String ownerTownName = plugin.data().getClaimOwner(pos);
        return ownerTownName == null ? null : plugin.data().getTown(ownerTownName);
    }
}
