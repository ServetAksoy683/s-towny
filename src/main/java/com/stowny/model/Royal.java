package com.stowny.model;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Royal {

    public String name;
    public UUID emperor;
    public final Set<String> memberKingdoms = new HashSet<>();
    public double balance = 0;

    public Royal(String name, UUID emperor, String foundingKingdom) {
        this.name = name;
        this.emperor = emperor;
        this.memberKingdoms.add(foundingKingdom);
    }
}
