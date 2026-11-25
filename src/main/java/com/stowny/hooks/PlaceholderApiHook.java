package com.stowny.hooks;

import com.stowny.STowny;
import com.stowny.model.ChunkPos;
import com.stowny.model.Kingdom;
import com.stowny.model.Town;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Baska bir scoreboard/tab plugininde Towny satirlari gostermek icin:
 * %stowny_town% %stowny_kingdom% %stowny_royal% %stowny_balance% %stowny_claims% %stowny_pvp% %stowny_here%
 */
public class PlaceholderApiHook extends PlaceholderExpansion {

    private final STowny plugin;

    public PlaceholderApiHook(STowny plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "stowny";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Hadi";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.2.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) {
            return "";
        }
        Town town = plugin.data().getTownOfPlayer(player.getUniqueId());
        Kingdom kingdom = town != null && town.kingdomName != null ? plugin.data().getKingdom(town.kingdomName) : null;

        switch (params.toLowerCase()) {
            case "town":
                return town != null ? town.name : "-";
            case "kingdom":
                return kingdom != null ? kingdom.name : "-";
            case "royal":
                return kingdom != null && kingdom.royalName != null ? kingdom.royalName : "-";
            case "balance":
                return town != null ? plugin.economy().format(town.balance) : "-";
            case "claims":
                return town != null ? String.valueOf(town.claims.size()) : "-";
            case "pvp":
                return town == null ? "-" : (town.pvpEnabled ? "PVP" : "Guvenli");
            case "here": {
                ChunkPos pos = new ChunkPos(player.getWorld().getName(),
                        player.getLocation().getBlockX() >> 4, player.getLocation().getBlockZ() >> 4);
                String owner = plugin.data().getClaimOwner(pos);
                return owner != null ? owner : "Doga";
            }
            default:
                return null;
        }
    }
}
