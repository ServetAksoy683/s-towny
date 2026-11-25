package com.stowny.model;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Kingdom {

    public String name;
    public UUID king;
    public final Set<String> memberTowns = new HashSet<>();
    public double balance = 0;
    public String royalName;

    public Kingdom(String name, UUID king, String foundingTown) {
        this.name = name;
        this.king = king;
        this.memberTowns.add(foundingTown);
    }
}
