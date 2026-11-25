package com.stowny.managers;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

/** Vault uzerinden oyuncu bakiyesi (Essentials dahil her ekonomi plugini ile calisir). */
public class EconomyManager {

    private final JavaPlugin plugin;
    private Economy economy;

    public EconomyManager(JavaPlugin plugin) {
        this.plugin = plugin;
        setup();
    }

    private void setup() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().warning("Vault bulunamadi. Ekonomi ozellikleri calismayacak.");
            return;
        }
        RegisteredServiceProvider<Economy> provider = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (provider == null) {
            plugin.getLogger().warning("Vault bulundu ama bir ekonomi plugini kayitli degil.");
            return;
        }
        this.economy = provider.getProvider();
        plugin.getLogger().info("Ekonomi baglandi: " + economy.getName());
    }

    public boolean isAvailable() {
        return economy != null;
    }

    public boolean has(OfflinePlayer player, double amount) {
        return economy != null && economy.has(player, amount);
    }

    public boolean withdraw(OfflinePlayer player, double amount) {
        return economy != null && economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    public void deposit(OfflinePlayer player, double amount) {
        if (economy != null) {
            economy.depositPlayer(player, amount);
        }
    }

    public String format(double amount) {
        return economy != null ? economy.format(amount) : String.format("%.2f", amount);
    }
}
