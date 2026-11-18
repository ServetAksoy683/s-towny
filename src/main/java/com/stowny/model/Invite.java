package com.stowny.model;

import java.util.UUID;

public class Invite {

    public enum Type {TOWN, KINGDOM, ROYAL}

    public final Type type;
    public final String targetOrgName;
    public final UUID inviter;

    public Invite(Type type, String targetOrgName, UUID inviter) {
        this.type = type;
        this.targetOrgName = targetOrgName;
        this.inviter = inviter;
    }
}
