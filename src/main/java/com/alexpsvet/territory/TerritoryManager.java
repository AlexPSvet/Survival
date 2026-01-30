package com.alexpsvet.territory;

import com.alexpsvet.Survival;
import com.alexpsvet.clan.Clan;
import com.alexpsvet.clan.ClanManager;
import com.alexpsvet.database.Database;
import org.bukkit.Location;
import org.bukkit.block.Block;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manager for protected territories
 */
public class TerritoryManager {
    private static final Logger LOGGER = Logger.getLogger("survival");
    private static TerritoryManager instance;
    
    private final Database database;
    private final Map<Integer, Territory> territories;
    private final Map<Block, Integer> protectionBlocks; // Protection stone blocks
    private int nextId = 1;
    
    public TerritoryManager(Database database) {
        instance = this;
        this.database = database;
        this.territories = new HashMap<>();
        this.protectionBlocks = new HashMap<>();
        createTables();
        loadTerritories();
    }
    
    /**
     * Create territory tables
     */
    private void createTables() {
        String territoriesTable = "CREATE TABLE IF NOT EXISTS territories (" +
                "id INTEGER PRIMARY KEY " + (database.getType().name().equals("SQLITE") ? "AUTOINCREMENT" : "AUTO_INCREMENT") + "," +
                "owner_uuid VARCHAR(36) NOT NULL," +
                "owner_name VARCHAR(16) NOT NULL," +
                "clan_name VARCHAR(16)," +
                "world VARCHAR(50) NOT NULL," +
                "center_x INTEGER NOT NULL," +
                "center_y INTEGER NOT NULL," +
                "center_z INTEGER NOT NULL," +
                "radius INTEGER NOT NULL," +
                "created_at BIGINT NOT NULL," +
                "flag_pvp BOOLEAN DEFAULT 0," +
                "flag_explosions BOOLEAN DEFAULT 0," +
                "flag_mob_spawning BOOLEAN DEFAULT 1," +
                "flag_mob_griefing BOOLEAN DEFAULT 0," +
                "flag_fire_spread BOOLEAN DEFAULT 0" +
                ")";
        database.executeUpdate(territoriesTable);
        
        String trustedTable = "CREATE TABLE IF NOT EXISTS territory_trusted (" +
                "territory_id INTEGER NOT NULL," +
                "player_uuid VARCHAR(36) NOT NULL," +
                "PRIMARY KEY (territory_id, player_uuid)" +
                ")";
        database.executeUpdate(trustedTable);
        
        LOGGER.info("Territory tables created/verified");
    }
    
    /**
     * Load all territories from database
     */
    private void loadTerritories() {
        ResultSet rs = database.executeQuery("SELECT * FROM territories");
        try {
            while (rs != null && rs.next()) {
                int id = rs.getInt("id");
                UUID owner = UUID.fromString(rs.getString("owner_uuid"));
                String ownerName = rs.getString("owner_name");
                String world = rs.getString("world");
                int x = rs.getInt("center_x");
                int y = rs.getInt("center_y");
                int z = rs.getInt("center_z");
                int radius = rs.getInt("radius");
                long createdAt = rs.getLong("created_at");
                String clanName = rs.getString("clan_name");
                
                Location center = new Location(Survival.getInstance().getServer().getWorld(world), x, y, z);
                Territory territory = new Territory(id, owner, ownerName, center, radius, createdAt);
                territory.setClanName(clanName);
                
                // Load flags
                TerritoryFlags flags = new TerritoryFlags();
                flags.setPvp(rs.getBoolean("flag_pvp"));
                flags.setExplosions(rs.getBoolean("flag_explosions"));
                flags.setMobSpawning(rs.getBoolean("flag_mob_spawning"));
                flags.setMobGriefing(rs.getBoolean("flag_mob_griefing"));
                flags.setFireSpread(rs.getBoolean("flag_fire_spread"));
                territory.setFlags(flags);
                
                territories.put(id, territory);
                loadTrustedPlayers(territory);
                
                if (id >= nextId) {
                    nextId = id + 1;
                }
            }
            if (rs != null) rs.close();
            LOGGER.info("Loaded " + territories.size() + " territories");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to load territories", e);
        }
    }
    
    /**
     * Load trusted players for a territory
     */
    private void loadTrustedPlayers(Territory territory) {
        ResultSet rs = database.executeQuery("SELECT player_uuid FROM territory_trusted WHERE territory_id = ?", territory.getId());
        try {
            while (rs != null && rs.next()) {
                UUID player = UUID.fromString(rs.getString("player_uuid"));
                territory.addTrusted(player);
            }
            if (rs != null) rs.close();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to load trusted players for territory " + territory.getId(), e);
        }
    }
    
    /**
     * Create a new territory
     */
    public Territory createTerritory(UUID owner, String ownerName, Location center, int radius) {
        database.executeUpdate(
            "INSERT INTO territories (owner_uuid, owner_name, world, center_x, center_y, center_z, radius, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            owner.toString(), ownerName, center.getWorld().getName(),
            center.getBlockX(), center.getBlockY(), center.getBlockZ(),
            radius, System.currentTimeMillis()
        );
        
        ResultSet rs = database.executeQuery("SELECT MAX(id) as max_id FROM territories");
        try {
            if (rs != null && rs.next()) {
                int id = rs.getInt("max_id");
                Territory territory = new Territory(id, owner, ownerName, center, radius, System.currentTimeMillis());
                territories.put(id, territory);
                rs.close();
                return territory;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to get territory ID", e);
        }
        
        return null;
    }
    
    /**
     * Create a territory from a protection block
     */
    public Territory createTerritoryFromBlock(Block block, UUID owner, String ownerName, int radius) {
        Territory territory = createTerritory(owner, ownerName, block.getLocation(), radius);
        if (territory != null) {
            protectionBlocks.put(block, territory.getId());
        }
        return territory;
    }
    
    /**
     * Remove a territory
     */
    public void removeTerritory(int territoryId) {
        territories.remove(territoryId);
        database.executeUpdate("DELETE FROM territories WHERE id = ?", territoryId);
        database.executeUpdate("DELETE FROM territory_trusted WHERE territory_id = ?", territoryId);
        
        // Remove protection block mapping
        protectionBlocks.entrySet().removeIf(entry -> entry.getValue() == territoryId);
    }
    
    /**
     * Claim a territory for a clan
     */
    public boolean claimForClan(int territoryId, String clanName) {
        Territory territory = territories.get(territoryId);
        if (territory == null) {
            return false;
        }
        
        territory.setClanName(clanName);
        database.executeUpdate("UPDATE territories SET clan_name = ? WHERE id = ?", clanName, territoryId);
        return true;
    }
    
    /**
     * Unclaim a territory from a clan
     */
    public void unclaimFromClan(int territoryId) {
        Territory territory = territories.get(territoryId);
        if (territory != null) {
            territory.setClanName(null);
            database.executeUpdate("UPDATE territories SET clan_name = NULL WHERE id = ?", territoryId);
        }
    }
    
    /**
     * Add a trusted player to a territory
     */
    public void addTrusted(int territoryId, UUID player) {
        Territory territory = territories.get(territoryId);
        if (territory != null) {
            territory.addTrusted(player);
            database.executeUpdate(
                "INSERT INTO territory_trusted (territory_id, player_uuid) VALUES (?, ?)",
                territoryId, player.toString()
            );
        }
    }
    
    /**
     * Remove a trusted player from a territory
     */
    public void removeTrusted(int territoryId, UUID player) {
        Territory territory = territories.get(territoryId);
        if (territory != null) {
            territory.removeTrusted(player);
            database.executeUpdate(
                "DELETE FROM territory_trusted WHERE territory_id = ? AND player_uuid = ?",
                territoryId, player.toString()
            );
        }
    }
    
    /**
     * Check if a new territory would collide with any existing territories
     * @param center the center of the new territory
     * @param radius the radius of the new territory
     * @return true if there would be a collision
     */
    public boolean wouldCollide(Location center, int radius) {
        for (Territory territory : territories.values()) {
            if (!territory.getCenter().getWorld().equals(center.getWorld())) {
                continue;
            }
            
            // Calculate distance between centers
            double dx = territory.getCenter().getBlockX() - center.getBlockX();
            double dy = territory.getCenter().getBlockY() - center.getBlockY();
            double dz = territory.getCenter().getBlockZ() - center.getBlockZ();
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            
            // Collision if distance between centers is less than sum of radii
            if (distance < (territory.getRadius() + radius)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Get territory at a location
     */
    public Territory getTerritoryAt(Location location) {
        for (Territory territory : territories.values()) {
            if (territory.contains(location)) {
                return territory;
            }
        }
        return null;
    }
    
    /**
     * Get territory by protection block
     */
    public Territory getTerritoryByBlock(Block block) {
        Integer territoryId = protectionBlocks.get(block);
        return territoryId != null ? territories.get(territoryId) : null;
    }
    
    /**
     * Check if a player can build at a location
     */
    public boolean canBuild(UUID player, Location location) {
        Territory territory = getTerritoryAt(location);
        if (territory == null) {
            return true; // No territory, can build
        }
        
        // Check if player has direct permission
        if (territory.hasPermission(player)) {
            return true;
        }
        
        // Check if territory is claimed by a clan
        if (territory.getClanName() != null) {
            ClanManager clanManager = Survival.getInstance().getClanManager();
            Clan clan = clanManager.getClan(territory.getClanName());
            if (clan != null && clan.isMember(player)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Update territory flags
     */
    public void updateFlags(int territoryId) {
        Territory territory = territories.get(territoryId);
        if (territory == null) return;
        
        TerritoryFlags flags = territory.getFlags();
        database.executeUpdate(
            "UPDATE territories SET flag_pvp = ?, flag_explosions = ?, flag_mob_spawning = ?, flag_mob_griefing = ?, flag_fire_spread = ? WHERE id = ?",
            flags.isPvp(), flags.isExplosions(), flags.isMobSpawning(), flags.isMobGriefing(), flags.isFireSpread(), territoryId
        );
    }
    
    public Collection<Territory> getAllTerritories() {
        return territories.values();
    }
    
    public Territory getTerritory(int id) {
        return territories.get(id);
    }
    
    public static TerritoryManager getInstance() {
        return instance;
    }
}
