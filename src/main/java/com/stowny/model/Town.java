package com.stowny.model;

import org.bukkit.Location;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Town {

    public String name;
    public UUID mayor;
    public final Set<UUID> assistants = new HashSet<>();
    public final Set<UUID> residents = new HashSet<>();
    public final Set<ChunkPos> claims = new HashSet<>();
    public double balance = 0;
    public double dailyTax = 0;
    public Location spawn;
    public String kingdomName;
    public boolean pvpEnabled = false;
    public final Set<UUID> canBreak = new HashSet<>();
    public final Set<UUID> canPlace = new HashSet<>();

    public Town(String name, UUID mayor) {
        this.name = name;
        this.mayor = mayor;
        this.residents.add(mayor);
    }

    public boolean isMayorOrAssistant(UUID uuid) {
        return uuid.equals(mayor) || assistants.contains(uuid);
    }

    public boolean isResident(UUID uuid) {
        return residents.contains(uuid);
    }

    public boolean canBreak(UUID uuid) {
        return isMayorOrAssistant(uuid) || canBreak.contains(uuid);
    }

    public boolean canPlace(UUID uuid) {
        return isMayorOrAssistant(uuid) || canPlace.contains(uuid);
    }
}
