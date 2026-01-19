package com.alexpsvet.bounty;

import java.util.UUID;

/**
 * Represents a bounty placed on a player
 */
public class Bounty {
    private final UUID id;
    private final UUID targetUuid;
    private final String targetName;
    private final UUID issuerUuid;
    private final String issuerName;
    private final double amount;
    private final long createdAt;
    private boolean active;
    
    public Bounty(UUID id, UUID targetUuid, String targetName, UUID issuerUuid, String issuerName, double amount, long createdAt, boolean active) {
        this.id = id;
        this.targetUuid = targetUuid;
        this.targetName = targetName;
        this.issuerUuid = issuerUuid;
        this.issuerName = issuerName;
        this.amount = amount;
        this.createdAt = createdAt;
        this.active = active;
    }
    
    public UUID getId() {
        return id;
    }
    
    public UUID getTargetUuid() {
        return targetUuid;
    }
    
    public String getTargetName() {
        return targetName;
    }
    
    public UUID getIssuerUuid() {
        return issuerUuid;
    }
    
    public String getIssuerName() {
        return issuerName;
    }
    
    public double getAmount() {
        return amount;
    }
    
    public long getCreatedAt() {
        return createdAt;
    }
    
    public boolean isActive() {
        return active;
    }
    
    public void setActive(boolean active) {
        this.active = active;
    }
}
