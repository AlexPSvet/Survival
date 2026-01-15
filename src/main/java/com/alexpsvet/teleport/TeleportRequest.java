package com.alexpsvet.teleport;

import java.util.UUID;

/**
 * Represents a teleport request
 */
public class TeleportRequest {
    private final UUID requester;
    private final UUID target;
    private final long timestamp;
    private final long expiresAt;
    
    public TeleportRequest(UUID requester, UUID target, long expiresInSeconds) {
        this.requester = requester;
        this.target = target;
        this.timestamp = System.currentTimeMillis();
        this.expiresAt = timestamp + (expiresInSeconds * 1000);
    }
    
    public UUID getRequester() { return requester; }
    public UUID getTarget() { return target; }
    public long getTimestamp() { return timestamp; }
    
    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt;
    }
    
    public long getSecondsRemaining() {
        long remaining = expiresAt - System.currentTimeMillis();
        return Math.max(0, remaining / 1000);
    }
}
