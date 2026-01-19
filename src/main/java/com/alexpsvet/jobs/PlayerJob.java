package com.alexpsvet.jobs;

import java.util.UUID;

/**
 * Represents a player's job progress
 */
public class PlayerJob {
    private final UUID playerUuid;
    private final String jobId;
    private int level;
    private double experience;
    private long joinedAt;
    
    public PlayerJob(UUID playerUuid, String jobId, int level, double experience, long joinedAt) {
        this.playerUuid = playerUuid;
        this.jobId = jobId;
        this.level = level;
        this.experience = experience;
        this.joinedAt = joinedAt;
    }
    
    public UUID getPlayerUuid() {
        return playerUuid;
    }
    
    public String getJobId() {
        return jobId;
    }
    
    public int getLevel() {
        return level;
    }
    
    public void setLevel(int level) {
        this.level = level;
    }
    
    public double getExperience() {
        return experience;
    }
    
    public void setExperience(double experience) {
        this.experience = experience;
    }
    
    public void addExperience(double amount) {
        this.experience += amount;
    }
    
    public long getJoinedAt() {
        return joinedAt;
    }
    
    public void setJoinedAt(long joinedAt) {
        this.joinedAt = joinedAt;
    }
}
