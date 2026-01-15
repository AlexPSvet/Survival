package com.alexpsvet.player;

import com.alexpsvet.database.Database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages player statistics and ranks
 */
public class PlayerStatsManager {
    private static final Logger LOGGER = Logger.getLogger("survival");
    private static PlayerStatsManager instance;
    private final Database database;
    private final Map<UUID, PlayerStats> statsCache;
    
    public PlayerStatsManager(Database database) {
        instance = this;
        this.database = database;
        this.statsCache = new HashMap<>();
        createTables();
    }
    
    /**
     * Create the player stats tables
     */
    private void createTables() {
        String query = "CREATE TABLE IF NOT EXISTS player_stats (" +
                "uuid VARCHAR(36) PRIMARY KEY," +
                "player_name VARCHAR(16) NOT NULL," +
                "kills INTEGER NOT NULL DEFAULT 0," +
                "deaths INTEGER NOT NULL DEFAULT 0," +
                "playtime BIGINT NOT NULL DEFAULT 0," +
                "rank VARCHAR(20) NOT NULL DEFAULT 'USER'," +
                "group_name VARCHAR(20) NOT NULL DEFAULT 'USER'," +
                "last_join BIGINT NOT NULL," +
                "created_at BIGINT NOT NULL" +
                ")";
        database.executeUpdate(query);
        
        LOGGER.info("Player stats tables created/verified");
    }
    
    /**
     * Load player stats from database
     */
    public PlayerStats loadStats(UUID uuid, String playerName) {
        if (statsCache.containsKey(uuid)) {
            return statsCache.get(uuid);
        }
        
        ResultSet rs = database.executeQuery("SELECT * FROM player_stats WHERE uuid = ?", uuid.toString());
        try {
            if (rs != null && rs.next()) {
                PlayerStats stats = new PlayerStats(
                    uuid,
                    rs.getString("player_name"),
                    rs.getInt("kills"),
                    rs.getInt("deaths"),
                    rs.getLong("playtime"),
                    rs.getString("rank"),
                    rs.getString("group_name"),
                    rs.getLong("last_join")
                );
                statsCache.put(uuid, stats);
                rs.close();
                return stats;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to load stats for " + uuid, e);
        }
        
        // Create new stats
        PlayerStats stats = new PlayerStats(uuid, playerName);
        saveStats(stats);
        statsCache.put(uuid, stats);
        return stats;
    }
    
    /**
     * Save player stats to database
     */
    public void saveStats(PlayerStats stats) {
        stats.updatePlaytime(); // Update playtime before saving
        
        int result = database.executeUpdate(
            "UPDATE player_stats SET player_name = ?, kills = ?, deaths = ?, playtime = ?, " +
            "rank = ?, group_name = ?, last_join = ? WHERE uuid = ?",
            stats.getPlayerName(), stats.getKills(), stats.getDeaths(), stats.getPlaytime(),
            stats.getRank(), stats.getGroup(), stats.getLastJoin(), stats.getUuid().toString()
        );
        
        if (result == 0) {
            // Insert new record
            database.executeUpdate(
                "INSERT INTO player_stats (uuid, player_name, kills, deaths, playtime, rank, group_name, last_join, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                stats.getUuid().toString(), stats.getPlayerName(), stats.getKills(), stats.getDeaths(),
                stats.getPlaytime(), stats.getRank(), stats.getGroup(), stats.getLastJoin(), System.currentTimeMillis()
            );
        }
    }
    
    /**
     * Get cached stats
     */
    public PlayerStats getStats(UUID uuid) {
        return statsCache.get(uuid);
    }
    
    /**
     * Remove stats from cache
     */
    public void unloadStats(UUID uuid) {
        PlayerStats stats = statsCache.remove(uuid);
        if (stats != null) {
            saveStats(stats);
        }
    }
    
    /**
     * Get player's group
     */
    public Group getGroup(UUID uuid) {
        PlayerStats stats = statsCache.get(uuid);
        if (stats == null) return Group.Defaults.USER;
        return Group.Defaults.getByName(stats.getGroup());
    }
    
    /**
     * Set player's group
     */
    public void setGroup(UUID uuid, String groupName) {
        PlayerStats stats = statsCache.get(uuid);
        if (stats != null) {
            stats.setGroup(groupName);
            saveStats(stats);
        }
    }
    
    /**
     * Set player's rank
     */
    public void setRank(UUID uuid, String rank) {
        PlayerStats stats = statsCache.get(uuid);
        if (stats != null) {
            stats.setRank(rank);
            saveStats(stats);
        }
    }
    
    /**
     * Save all cached stats
     */
    public void saveAll() {
        for (PlayerStats stats : statsCache.values()) {
            saveStats(stats);
        }
    }
    
    public static PlayerStatsManager getInstance() {
        return instance;
    }
}
