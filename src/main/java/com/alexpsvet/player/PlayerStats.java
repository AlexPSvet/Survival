package com.alexpsvet.player;

import java.util.UUID;

/**
 * Represents player statistics
 */
public class PlayerStats {
    private final UUID uuid;
    private String playerName;
    private int kills;
    private int deaths;
    private long playtime; // in milliseconds
    private String rank; // premium rank: USER, RANK1, RANK2, RANK3
    private String group; // permission group: USER, RANK1, RANK2, RANK3, ADMIN, OWNER
    private long lastJoin;
    private long sessionStart;
    
    public PlayerStats(UUID uuid, String playerName) {
        this.uuid = uuid;
        this.playerName = playerName;
        this.kills = 0;
        this.deaths = 0;
        this.playtime = 0;
        this.rank = "USER";
        this.group = "USER";
        this.lastJoin = System.currentTimeMillis();
        this.sessionStart = System.currentTimeMillis();
    }
    
    public PlayerStats(UUID uuid, String playerName, int kills, int deaths, long playtime, 
                      String rank, String group, long lastJoin) {
        this.uuid = uuid;
        this.playerName = playerName;
        this.kills = kills;
        this.deaths = deaths;
        this.playtime = playtime;
        this.rank = rank;
        this.group = group;
        this.lastJoin = lastJoin;
        this.sessionStart = System.currentTimeMillis();
    }
    
    // Getters
    public UUID getUuid() { return uuid; }
    public String getPlayerName() { return playerName; }
    public int getKills() { return kills; }
    public int getDeaths() { return deaths; }
    public long getPlaytime() { return playtime; }
    public String getRank() { return rank; }
    public String getGroup() { return group; }
    public long getLastJoin() { return lastJoin; }
    public long getSessionStart() { return sessionStart; }
    
    // Setters
    public void setPlayerName(String playerName) { this.playerName = playerName; }
    public void setKills(int kills) { this.kills = kills; }
    public void setDeaths(int deaths) { this.deaths = deaths; }
    public void setPlaytime(long playtime) { this.playtime = playtime; }
    public void setRank(String rank) { this.rank = rank; }
    public void setGroup(String group) { this.group = group; }
    public void setLastJoin(long lastJoin) { this.lastJoin = lastJoin; }
    public void setSessionStart(long sessionStart) { this.sessionStart = sessionStart; }
    
    // Utility methods
    public void addKill() { this.kills++; }
    public void addDeath() { this.deaths++; }
    
    public double getKDRatio() {
        if (deaths == 0) return kills;
        return (double) kills / deaths;
    }
    
    /**
     * Update playtime with current session
     */
    public void updatePlaytime() {
        long sessionTime = System.currentTimeMillis() - sessionStart;
        this.playtime += sessionTime;
        this.sessionStart = System.currentTimeMillis();
    }
    
    /**
     * Get formatted playtime string
     */
    public String getFormattedPlaytime() {
        long seconds = playtime / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) {
            return days + "j " + (hours % 24) + "h";
        } else if (hours > 0) {
            return hours + "h " + (minutes % 60) + "m";
        } else if (minutes > 0) {
            return minutes + "m " + (seconds % 60) + "s";
        } else {
            return seconds + "s";
        }
    }
}
