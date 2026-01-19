package com.alexpsvet.home;

import com.alexpsvet.Survival;
import com.alexpsvet.database.Database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages player homes
 */
public class HomeManager {
    private static final Logger LOGGER = Logger.getLogger("survival");
    private static HomeManager instance;
    
    private final Database database;
    private final Map<UUID, Map<String, Home>> playerHomes; // UUID -> (name -> Home)
    private final Map<UUID, Long> teleportCooldowns;
    
    public HomeManager(Database database) {
        instance = this;
        this.database = database;
        this.playerHomes = new HashMap<>();
        this.teleportCooldowns = new HashMap<>();
        
        // Initialize database table
        initDatabase();
    }
    
    /**
     * Initialize homes database table
     */
    private void initDatabase() {
        String query = "CREATE TABLE IF NOT EXISTS homes (" +
                "owner VARCHAR(36) NOT NULL, " +
                "name VARCHAR(32) NOT NULL, " +
                "world VARCHAR(64) NOT NULL, " +
                "x DOUBLE NOT NULL, " +
                "y DOUBLE NOT NULL, " +
                "z DOUBLE NOT NULL, " +
                "yaw FLOAT NOT NULL, " +
                "pitch FLOAT NOT NULL, " +
                "PRIMARY KEY (owner, name))";
        database.executeUpdate(query);
    }
    
    /**
     * Load homes for a player
     */
    public void loadHomes(UUID uuid) {
        Map<String, Home> homes = new HashMap<>();
        
        ResultSet rs = database.executeQuery(
            "SELECT * FROM homes WHERE owner = ?",
            uuid.toString()
        );
        
        try {
            while (rs != null && rs.next()) {
                String name = rs.getString("name");
                String world = rs.getString("world");
                double x = rs.getDouble("x");
                double y = rs.getDouble("y");
                double z = rs.getDouble("z");
                float yaw = rs.getFloat("yaw");
                float pitch = rs.getFloat("pitch");
                
                homes.put(name.toLowerCase(), new Home(uuid, name, world, x, y, z, yaw, pitch));
            }
            if (rs != null) rs.close();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to load homes for " + uuid, e);
        }
        
        playerHomes.put(uuid, homes);
        LOGGER.info("Loaded " + homes.size() + " homes for " + uuid);
    }
    
    /**
     * Save a home
     */
    public boolean saveHome(Home home) {
        int result = database.executeUpdate(
            "INSERT OR REPLACE INTO homes (owner, name, world, x, y, z, yaw, pitch) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            home.getOwner().toString(),
            home.getName(),
            home.getWorldName(),
            home.getX(),
            home.getY(),
            home.getZ(),
            home.getYaw(),
            home.getPitch()
        );
        
        if (result > 0) {
            // Update cache
            playerHomes.computeIfAbsent(home.getOwner(), k -> new HashMap<>())
                .put(home.getName().toLowerCase(), home);
            return true;
        }
        return false;
    }
    
    /**
     * Delete a home
     */
    public boolean deleteHome(UUID owner, String name) {
        int result = database.executeUpdate(
            "DELETE FROM homes WHERE owner = ? AND LOWER(name) = LOWER(?)",
            owner.toString(),
            name
        );
        
        if (result > 0) {
            // Update cache
            Map<String, Home> homes = playerHomes.get(owner);
            if (homes != null) {
                homes.remove(name.toLowerCase());
            }
            return true;
        }
        return false;
    }
    
    /**
     * Get a home
     */
    public Home getHome(UUID owner, String name) {
        Map<String, Home> homes = playerHomes.get(owner);
        if (homes == null) {
            loadHomes(owner);
            homes = playerHomes.get(owner);
        }
        return homes != null ? homes.get(name.toLowerCase()) : null;
    }
    
    /**
     * Get all homes for a player
     */
    public Collection<Home> getHomes(UUID owner) {
        Map<String, Home> homes = playerHomes.get(owner);
        if (homes == null) {
            loadHomes(owner);
            homes = playerHomes.get(owner);
        }
        return homes != null ? homes.values() : Collections.emptyList();
    }
    
    /**
     * Get home count for a player
     */
    public int getHomeCount(UUID owner) {
        return getHomes(owner).size();
    }
    
    /**
     * Check if player is on cooldown
     */
    public boolean isOnCooldown(UUID owner) {
        Long endTime = teleportCooldowns.get(owner);
        if (endTime == null) return false;
        
        if (System.currentTimeMillis() >= endTime) {
            teleportCooldowns.remove(owner);
            return false;
        }
        return true;
    }
    
    /**
     * Get cooldown remaining time in seconds
     */
    public long getCooldownRemaining(UUID owner) {
        Long endTime = teleportCooldowns.get(owner);
        if (endTime == null) return 0;
        
        long remaining = endTime - System.currentTimeMillis();
        return Math.max(0, remaining / 1000);
    }
    
    /**
     * Set cooldown for a player
     */
    public void setCooldown(UUID owner) {
        int seconds = Survival.getInstance().getConfig().getInt("home.cooldown-seconds", 300);
        teleportCooldowns.put(owner, System.currentTimeMillis() + (seconds * 1000L));
    }
    
    /**
     * Unload homes for a player
     */
    public void unloadHomes(UUID owner) {
        playerHomes.remove(owner);
    }
    
    public static HomeManager getInstance() {
        return instance;
    }
}