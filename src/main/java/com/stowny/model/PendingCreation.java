package com.stowny.model;

public class PendingCreation {

    public enum Type {TOWN, KINGDOM, ROYAL}

    public final Type type;
    public final String name;
    public final double cost;

    public PendingCreation(Type type, String name, double cost) {
        this.type = type;
        this.name = name;
        this.cost = cost;
    }
}
