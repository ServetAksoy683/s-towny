package com.stowny.managers;

import com.stowny.model.ChunkPos;
import com.stowny.model.Kingdom;
import com.stowny.model.Royal;
import com.stowny.model.Town;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Tum kasaba/krallik/kraliyet verisinin tek kaynagi; towns.yml'e kaydeder. */
public class DataManager {

    private final JavaPlugin plugin;
    private final File file;

    private final Map<String, Town> towns = new HashMap<>();
    private final Map<String, Kingdom> kingdoms = new HashMap<>();
    private final Map<String, Royal> royals = new HashMap<>();
    private final Map<UUID, String> playerTownIndex = new HashMap<>();
    private final Map<ChunkPos, String> claimIndex = new HashMap<>();

    public DataManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "towns.yml");
        load();
    }

    public Town getTown(String name) { return towns.get(name.toLowerCase()); }
    public Kingdom getKingdom(String name) { return kingdoms.get(name.toLowerCase()); }
    public Royal getRoyal(String name) { return royals.get(name.toLowerCase()); }

    public Town getTownOfPlayer(UUID uuid) {
        String name = playerTownIndex.get(uuid);
        return name == null ? null : towns.get(name);
    }

    public String getClaimOwner(ChunkPos pos) { return claimIndex.get(pos); }

    public Map<String, Town> allTowns() { return towns; }
    public Map<String, Kingdom> allKingdoms() { return kingdoms; }
    public Map<String, Royal> allRoyals() { return royals; }

    public Kingdom getKingdomOfKing(UUID uuid) {
        for (Kingdom k : kingdoms.values()) {
            if (k.king.equals(uuid)) return k;
        }
        return null;
    }

    public Royal getRoyalOfEmperor(UUID uuid) {
        for (Royal r : royals.values()) {
            if (r.emperor.equals(uuid)) return r;
        }
        return null;
    }

    public boolean nameTaken(String name) {
        String lower = name.toLowerCase();
        return towns.containsKey(lower) || kingdoms.containsKey(lower) || royals.containsKey(lower);
    }

    public void addTown(Town town) {
        towns.put(town.name.toLowerCase(), town);
        for (UUID resident : town.residents) {
            playerTownIndex.put(resident, town.name.toLowerCase());
        }
        for (ChunkPos pos : town.claims) {
            claimIndex.put(pos, town.name);
        }
        save();
    }

    public void removeTown(Town town) {
        towns.remove(town.name.toLowerCase());
        playerTownIndex.values().removeIf(v -> v.equalsIgnoreCase(town.name));
        claimIndex.entrySet().removeIf(e -> e.getValue().equalsIgnoreCase(town.name));
        save();
    }

    public void addKingdom(Kingdom kingdom) {
        kingdoms.put(kingdom.name.toLowerCase(), kingdom);
        save();
    }

    public void addRoyal(Royal royal) {
        royals.put(royal.name.toLowerCase(), royal);
        save();
    }

    public void removeKingdom(Kingdom kingdom) {
        kingdoms.remove(kingdom.name.toLowerCase());
        save();
    }

    public void removeRoyal(Royal royal) {
        royals.remove(royal.name.toLowerCase());
        save();
    }

    public void addResident(Town town, UUID uuid) {
        town.residents.add(uuid);
        playerTownIndex.put(uuid, town.name.toLowerCase());
        save();
    }

    public void removeResident(Town town, UUID uuid) {
        town.residents.remove(uuid);
        town.assistants.remove(uuid);
        town.canBreak.remove(uuid);
        town.canPlace.remove(uuid);
        playerTownIndex.remove(uuid);
        save();
    }

    public void addClaim(Town town, ChunkPos pos) {
        town.claims.add(pos);
        claimIndex.put(pos, town.name);
        save();
    }

    public void removeClaim(Town town, ChunkPos pos) {
        town.claims.remove(pos);
        claimIndex.remove(pos);
        save();
    }

    private void load() {
        if (!file.exists()) {
            plugin.getDataFolder().mkdirs();
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);

        ConfigurationSection townsSection = yaml.getConfigurationSection("towns");
        if (townsSection != null) {
            for (String key : townsSection.getKeys(false)) {
                ConfigurationSection s = townsSection.getConfigurationSection(key);
                if (s == null) continue;

                Town town = new Town(s.getString("display-name", key), UUID.fromString(s.getString("mayor")));
                town.residents.clear();
                for (String r : s.getStringList("residents")) town.residents.add(UUID.fromString(r));
                for (String a : s.getStringList("assistants")) town.assistants.add(UUID.fromString(a));
                for (String c : s.getStringList("claims")) {
                    ChunkPos pos = ChunkPos.parse(c);
                    town.claims.add(pos);
                    claimIndex.put(pos, town.name);
                }
                town.balance = s.getDouble("balance", 0);
                town.dailyTax = s.getDouble("daily-tax", 0);
                town.kingdomName = s.getString("kingdom", null);
                town.pvpEnabled = s.getBoolean("pvp-enabled", false);
                for (String u : s.getStringList("can-break")) town.canBreak.add(UUID.fromString(u));
                for (String u : s.getStringList("can-place")) town.canPlace.add(UUID.fromString(u));

                String spawnRaw = s.getString("spawn");
                if (spawnRaw != null) town.spawn = deserializeLocation(spawnRaw);

                towns.put(key, town);
                for (UUID resident : town.residents) playerTownIndex.put(resident, key);
            }
        }

        ConfigurationSection kingdomsSection = yaml.getConfigurationSection("kingdoms");
        if (kingdomsSection != null) {
            for (String key : kingdomsSection.getKeys(false)) {
                ConfigurationSection s = kingdomsSection.getConfigurationSection(key);
                if (s == null) continue;
                Kingdom kingdom = new Kingdom(s.getString("display-name", key), UUID.fromString(s.getString("king")), "");
                kingdom.memberTowns.clear();
                kingdom.memberTowns.addAll(s.getStringList("member-towns"));
                kingdom.balance = s.getDouble("balance", 0);
                kingdom.royalName = s.getString("royal", null);
                kingdoms.put(key, kingdom);
            }
        }

        ConfigurationSection royalsSection = yaml.getConfigurationSection("royals");
        if (royalsSection != null) {
            for (String key : royalsSection.getKeys(false)) {
                ConfigurationSection s = royalsSection.getConfigurationSection(key);
                if (s == null) continue;
                Royal royal = new Royal(s.getString("display-name", key), UUID.fromString(s.getString("emperor")), "");
                royal.memberKingdoms.clear();
                royal.memberKingdoms.addAll(s.getStringList("member-kingdoms"));
                royal.balance = s.getDouble("balance", 0);
                royals.put(key, royal);
            }
        }
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();

        for (Town town : towns.values()) {
            String path = "towns." + town.name.toLowerCase();
            yaml.set(path + ".display-name", town.name);
            yaml.set(path + ".mayor", town.mayor.toString());
            yaml.set(path + ".residents", town.residents.stream().map(UUID::toString).toList());
            yaml.set(path + ".assistants", town.assistants.stream().map(UUID::toString).toList());
            yaml.set(path + ".claims", town.claims.stream().map(ChunkPos::serialize).toList());
            yaml.set(path + ".balance", town.balance);
            yaml.set(path + ".daily-tax", town.dailyTax);
            yaml.set(path + ".pvp-enabled", town.pvpEnabled);
            yaml.set(path + ".can-break", town.canBreak.stream().map(UUID::toString).toList());
            yaml.set(path + ".can-place", town.canPlace.stream().map(UUID::toString).toList());
            if (town.kingdomName != null) yaml.set(path + ".kingdom", town.kingdomName);
            if (town.spawn != null) yaml.set(path + ".spawn", serializeLocation(town.spawn));
        }

        for (Kingdom kingdom : kingdoms.values()) {
            String path = "kingdoms." + kingdom.name.toLowerCase();
            yaml.set(path + ".display-name", kingdom.name);
            yaml.set(path + ".king", kingdom.king.toString());
            yaml.set(path + ".member-towns", new java.util.ArrayList<>(kingdom.memberTowns));
            yaml.set(path + ".balance", kingdom.balance);
            if (kingdom.royalName != null) yaml.set(path + ".royal", kingdom.royalName);
        }

        for (Royal royal : royals.values()) {
            String path = "royals." + royal.name.toLowerCase();
            yaml.set(path + ".display-name", royal.name);
            yaml.set(path + ".emperor", royal.emperor.toString());
            yaml.set(path + ".member-kingdoms", new java.util.ArrayList<>(royal.memberKingdoms));
            yaml.set(path + ".balance", royal.balance);
        }

        try {
            plugin.getDataFolder().mkdirs();
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("towns.yml kaydedilemedi: " + e.getMessage());
        }
    }

    private String serializeLocation(Location loc) {
        return loc.getWorld().getName() + "," + loc.getX() + "," + loc.getY() + "," + loc.getZ()
                + "," + loc.getYaw() + "," + loc.getPitch();
    }

    private Location deserializeLocation(String raw) {
        String[] p = raw.split(",");
        World world = Bukkit.getWorld(p[0]);
        if (world == null) return null;
        return new Location(world, Double.parseDouble(p[1]), Double.parseDouble(p[2]), Double.parseDouble(p[3]),
                Float.parseFloat(p[4]), Float.parseFloat(p[5]));
    }
}
