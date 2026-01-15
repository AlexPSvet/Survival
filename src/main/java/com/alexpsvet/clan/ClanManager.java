package com.alexpsvet.clan;

import com.alexpsvet.Survival;
import com.alexpsvet.database.Database;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manager for clans
 */
public class ClanManager {
    private static final Logger LOGGER = Logger.getLogger("survival");
    private static ClanManager instance;
    private final Database database;
    private final Map<String, Clan> clans;
    private final Map<UUID, String> playerClans;
    private final Map<UUID, Long> invitations;
    private final int maxMembers;
    private final int maxAllies;
    
    public ClanManager(Database database) {
        instance = this;
        this.database = database;
        this.clans = new HashMap<>();
        this.playerClans = new HashMap<>();
        this.invitations = new HashMap<>();
        this.maxMembers = Survival.getInstance().getConfig().getInt("clans.max-members", 20);
        this.maxAllies = Survival.getInstance().getConfig().getInt("clans.max-allies", 3);
        createTables();
        loadClans();
    }
    
    /**
     * Create the clan tables
     */
    private void createTables() {
        String clansTable = "CREATE TABLE IF NOT EXISTS clans (" +
                "name VARCHAR(16) PRIMARY KEY," +
                "tag VARCHAR(6) NOT NULL," +
                "leader VARCHAR(36) NOT NULL," +
                "description TEXT," +
                "friendly_fire BOOLEAN DEFAULT 0," +
                "home_world VARCHAR(50)," +
                "home_x DOUBLE," +
                "home_y DOUBLE," +
                "home_z DOUBLE," +
                "home_yaw FLOAT," +
                "home_pitch FLOAT," +
                "created_at BIGINT NOT NULL" +
                ")";
        database.executeUpdate(clansTable);
        
        String membersTable = "CREATE TABLE IF NOT EXISTS clan_members (" +
                "clan_name VARCHAR(16) NOT NULL," +
                "uuid VARCHAR(36) NOT NULL," +
                "rank VARCHAR(20) NOT NULL," +
                "joined_at BIGINT NOT NULL," +
                "PRIMARY KEY (clan_name, uuid)" +
                ")";
        database.executeUpdate(membersTable);
        
        String alliesTable = "CREATE TABLE IF NOT EXISTS clan_allies (" +
                "clan_name VARCHAR(16) NOT NULL," +
                "ally_name VARCHAR(16) NOT NULL," +
                "created_at BIGINT NOT NULL," +
                "PRIMARY KEY (clan_name, ally_name)" +
                ")";
        database.executeUpdate(alliesTable);
        
        String enemiesTable = "CREATE TABLE IF NOT EXISTS clan_enemies (" +
                "clan_name VARCHAR(16) NOT NULL," +
                "enemy_name VARCHAR(16) NOT NULL," +
                "created_at BIGINT NOT NULL," +
                "PRIMARY KEY (clan_name, enemy_name)" +
                ")";
        database.executeUpdate(enemiesTable);
        
        LOGGER.info("Clan tables created/verified");
    }
    
    /**
     * Load all clans from database
     */
    private void loadClans() {
        ResultSet rs = database.executeQuery("SELECT * FROM clans");
        try {
            while (rs != null && rs.next()) {
                String name = rs.getString("name");
                String tag = rs.getString("tag");
                UUID leader = UUID.fromString(rs.getString("leader"));
                
                Clan clan = new Clan(name, tag, leader);
                clan.setDescription(rs.getString("description"));
                clan.setFriendlyFire(rs.getBoolean("friendly_fire"));
                
                // Load home
                String homeWorld = rs.getString("home_world");
                if (homeWorld != null) {
                    Location home = new Location(
                        Bukkit.getWorld(homeWorld),
                        rs.getDouble("home_x"),
                        rs.getDouble("home_y"),
                        rs.getDouble("home_z"),
                        rs.getFloat("home_yaw"),
                        rs.getFloat("home_pitch")
                    );
                    clan.setHome(home);
                }
                
                clans.put(name, clan);
                loadClanMembers(clan);
                loadClanAllies(clan);
                loadClanEnemies(clan);
            }
            if (rs != null) rs.close();
            LOGGER.info("Loaded " + clans.size() + " clans");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to load clans", e);
        }
    }
    
    /**
     * Load clan members
     */
    private void loadClanMembers(Clan clan) {
        ResultSet rs = database.executeQuery("SELECT uuid, rank FROM clan_members WHERE clan_name = ?", clan.getName());
        try {
            while (rs != null && rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                ClanRank rank = ClanRank.valueOf(rs.getString("rank"));
                clan.addMember(uuid, rank);
                playerClans.put(uuid, clan.getName());
            }
            if (rs != null) rs.close();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to load members for clan " + clan.getName(), e);
        }
    }
    
    /**
     * Load clan allies
     */
    private void loadClanAllies(Clan clan) {
        ResultSet rs = database.executeQuery("SELECT ally_name FROM clan_allies WHERE clan_name = ?", clan.getName());
        try {
            while (rs != null && rs.next()) {
                clan.addAlly(rs.getString("ally_name"));
            }
            if (rs != null) rs.close();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to load allies for clan " + clan.getName(), e);
        }
    }
    
    /**
     * Load clan enemies
     */
    private void loadClanEnemies(Clan clan) {
        ResultSet rs = database.executeQuery("SELECT enemy_name FROM clan_enemies WHERE clan_name = ?", clan.getName());
        try {
            while (rs != null && rs.next()) {
                clan.addEnemy(rs.getString("enemy_name"));
            }
            if (rs != null) rs.close();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to load enemies for clan " + clan.getName(), e);
        }
    }
    
    /**
     * Create a new clan
     */
    public boolean createClan(String name, String tag, UUID leader) {
        if (clans.containsKey(name)) {
            return false;
        }
        
        Clan clan = new Clan(name, tag, leader);
        clans.put(name, clan);
        playerClans.put(leader, name);
        
        // Save to database
        database.executeUpdate(
            "INSERT INTO clans (name, tag, leader, created_at) VALUES (?, ?, ?, ?)",
            name, tag, leader.toString(), System.currentTimeMillis()
        );
        
        database.executeUpdate(
            "INSERT INTO clan_members (clan_name, uuid, rank, joined_at) VALUES (?, ?, ?, ?)",
            name, leader.toString(), ClanRank.LEADER.name(), System.currentTimeMillis()
        );
        
        return true;
    }
    
    /**
     * Disband a clan
     */
    public void disbandClan(String name) {
        Clan clan = clans.get(name);
        if (clan == null) return;
        
        // Remove from player mappings
        for (UUID uuid : clan.getMembers().keySet()) {
            playerClans.remove(uuid);
        }
        
        clans.remove(name);
        
        // Remove from database
        database.executeUpdate("DELETE FROM clans WHERE name = ?", name);
        database.executeUpdate("DELETE FROM clan_members WHERE clan_name = ?", name);
        database.executeUpdate("DELETE FROM clan_allies WHERE clan_name = ? OR ally_name = ?", name, name);
        database.executeUpdate("DELETE FROM clan_enemies WHERE clan_name = ? OR enemy_name = ?", name, name);
    }
    
    /**
     * Add a member to a clan
     */
    public boolean addMember(String clanName, UUID uuid) {
        Clan clan = clans.get(clanName);
        if (clan == null || clan.getMemberCount() >= maxMembers) {
            return false;
        }
        
        clan.addMember(uuid, ClanRank.MEMBER);
        playerClans.put(uuid, clanName);
        
        database.executeUpdate(
            "INSERT INTO clan_members (clan_name, uuid, rank, joined_at) VALUES (?, ?, ?, ?)",
            clanName, uuid.toString(), ClanRank.MEMBER.name(), System.currentTimeMillis()
        );
        
        return true;
    }
    
    /**
     * Remove a member from a clan
     */
    public void removeMember(String clanName, UUID uuid) {
        Clan clan = clans.get(clanName);
        if (clan == null) return;
        
        clan.removeMember(uuid);
        playerClans.remove(uuid);
        
        database.executeUpdate("DELETE FROM clan_members WHERE clan_name = ? AND uuid = ?", clanName, uuid.toString());
    }
    
    /**
     * Set a member's rank
     */
    public void setRank(String clanName, UUID uuid, ClanRank rank) {
        Clan clan = clans.get(clanName);
        if (clan == null) return;
        
        clan.setRank(uuid, rank);
        database.executeUpdate("UPDATE clan_members SET rank = ? WHERE clan_name = ? AND uuid = ?", 
            rank.name(), clanName, uuid.toString());
    }
    
    /**
     * Set clan home
     */
    public void setHome(String clanName, Location location) {
        Clan clan = clans.get(clanName);
        if (clan == null) return;
        
        clan.setHome(location);
        database.executeUpdate(
            "UPDATE clans SET home_world = ?, home_x = ?, home_y = ?, home_z = ?, home_yaw = ?, home_pitch = ? WHERE name = ?",
            location.getWorld().getName(),
            location.getX(),
            location.getY(),
            location.getZ(),
            location.getYaw(),
            location.getPitch(),
            clanName
        );
    }
    
    /**
     * Update clan settings (friendly fire, description, etc.)
     */
    public void updateClanSettings(String clanName) {
        Clan clan = clans.get(clanName);
        if (clan == null) return;
        
        database.executeUpdate(
            "UPDATE clans SET friendly_fire = ?, description = ? WHERE name = ?",
            clan.isFriendlyFire(),
            clan.getDescription(),
            clanName
        );
    }
    
    /**
     * Add alliance
     */
    public boolean addAlly(String clanName, String allyName) {
        Clan clan = clans.get(clanName);
        Clan ally = clans.get(allyName);
        
        if (clan == null || ally == null || clan.getAllies().size() >= maxAllies) {
            return false;
        }
        
        clan.addAlly(allyName);
        ally.addAlly(clanName);
        
        long now = System.currentTimeMillis();
        database.executeUpdate("INSERT INTO clan_allies (clan_name, ally_name, created_at) VALUES (?, ?, ?)", 
            clanName, allyName, now);
        database.executeUpdate("INSERT INTO clan_allies (clan_name, ally_name, created_at) VALUES (?, ?, ?)", 
            allyName, clanName, now);
        
        return true;
    }
    
    /**
     * Remove alliance
     */
    public void removeAlly(String clanName, String allyName) {
        Clan clan = clans.get(clanName);
        Clan ally = clans.get(allyName);
        
        if (clan != null) clan.removeAlly(allyName);
        if (ally != null) ally.removeAlly(clanName);
        
        database.executeUpdate("DELETE FROM clan_allies WHERE (clan_name = ? AND ally_name = ?) OR (clan_name = ? AND ally_name = ?)", 
            clanName, allyName, allyName, clanName);
    }
    
    public Clan getClan(String name) {
        return clans.get(name);
    }
    
    public Clan getPlayerClan(UUID uuid) {
        String clanName = playerClans.get(uuid);
        return clanName != null ? clans.get(clanName) : null;
    }
    
    public boolean isInClan(UUID uuid) {
        return playerClans.containsKey(uuid);
    }
    
    public Collection<Clan> getAllClans() {
        return clans.values();
    }
    
    public void invitePlayer(UUID uuid, String clanName) {
        invitations.put(uuid, System.currentTimeMillis());
    }
    
    public void removeInvitation(UUID uuid) {
        invitations.remove(uuid);
    }
    
    public boolean hasInvitation(UUID uuid) {
        return invitations.containsKey(uuid);
    }
    
    public static ClanManager getInstance() {
        return instance;
    }
}
