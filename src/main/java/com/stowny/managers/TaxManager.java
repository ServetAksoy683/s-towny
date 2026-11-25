package com.stowny.managers;

import com.stowny.STowny;
import com.stowny.model.Kingdom;
import com.stowny.model.Royal;
import com.stowny.model.Town;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.UUID;

/** Periyodik sakin vergisi, arazi bakimi, krallik ve kraliyet bakimi. */
public class TaxManager {

    private final STowny plugin;

    public TaxManager(STowny plugin) {
        this.plugin = plugin;
    }

    public void startTask() {
        int intervalMinutes = plugin.getConfig().getInt("economy.tax-interval-minutes", 1440);
        long ticks = Math.max(1200L, intervalMinutes * 60L * 20L);
        Bukkit.getScheduler().runTaskTimer(plugin, this::runCycle, ticks, ticks);
    }

    private void runCycle() {
        double upkeepPerClaim = plugin.getConfig().getDouble("economy.upkeep-per-claim-per-day", 0);
        double kingdomUpkeep = plugin.getConfig().getDouble("economy.kingdom-upkeep-per-day", 0);
        double royalUpkeep = plugin.getConfig().getDouble("economy.royal-upkeep-per-day", 0);

        for (Town town : new ArrayList<>(plugin.data().allTowns().values())) {
            if (town.dailyTax > 0) {
                for (UUID residentId : new ArrayList<>(town.residents)) {
                    OfflinePlayer resident = Bukkit.getOfflinePlayer(residentId);
                    if (plugin.economy().has(resident, town.dailyTax)) {
                        plugin.economy().withdraw(resident, town.dailyTax);
                        town.balance += town.dailyTax;
                        notifyIfOnline(residentId, "tax-charged", "%amount%", plugin.economy().format(town.dailyTax));
                    }
                }
            }

            double claimUpkeep = town.claims.size() * upkeepPerClaim;
            if (claimUpkeep > 0) {
                if (town.balance >= claimUpkeep) {
                    town.balance -= claimUpkeep;
                } else {
                    town.balance = 0;
                    notifyIfOnline(town.mayor, "upkeep-failed-warning", "%name%", town.name);
                }
            }

            if (town.kingdomName != null) {
                Kingdom kingdom = plugin.data().getKingdom(town.kingdomName);
                if (kingdom != null && kingdomUpkeep > 0) {
                    if (town.balance >= kingdomUpkeep) {
                        town.balance -= kingdomUpkeep;
                        kingdom.balance += kingdomUpkeep;
                        notifyIfOnline(town.mayor, "upkeep-charged", "%amount%", plugin.economy().format(kingdomUpkeep));
                    } else {
                        notifyIfOnline(town.mayor, "upkeep-failed-warning", "%name%", town.name);
                    }
                }
            }
        }

        for (Kingdom kingdom : new ArrayList<>(plugin.data().allKingdoms().values())) {
            if (kingdom.royalName == null || royalUpkeep <= 0) continue;
            Royal royal = plugin.data().getRoyal(kingdom.royalName);
            if (royal == null) continue;
            if (kingdom.balance >= royalUpkeep) {
                kingdom.balance -= royalUpkeep;
                royal.balance += royalUpkeep;
                notifyIfOnline(kingdom.king, "upkeep-charged", "%amount%", plugin.economy().format(royalUpkeep));
            } else {
                notifyIfOnline(kingdom.king, "upkeep-failed-warning", "%name%", kingdom.name);
            }
        }

        plugin.data().save();
    }

    private void notifyIfOnline(UUID uuid, String key, String placeholder, String value) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) {
            player.sendMessage(plugin.lang().get(key, placeholder, value));
            plugin.sounds().play(player, "tax");
        }
    }
}
