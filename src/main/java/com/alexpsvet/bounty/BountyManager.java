package com.alexpsvet.bounty;

import com.alexpsvet.Survival;
import com.alexpsvet.database.Database;
import com.alexpsvet.economy.EconomyManager;
import com.alexpsvet.economy.TransactionType;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages player bounties
 */
public class BountyManager {
    private static final Logger LOGGER = Logger.getLogger("survival");
    private static BountyManager instance;
    private final Database database;
    private final Map<UUID, List<Bounty>> activeBounties; // target UUID -> bounties
    private final double minimumBounty;
    private final double maximumBounty;
    
    public BountyManager(Database database) {
        instance = this;
        this.database = database;
        this.activeBounties = new HashMap<>();
        this.minimumBounty = Survival.getInstance().getConfig().getDouble("bounty.minimum-amount", 100.0);
        this.maximumBounty = Survival.getInstance().getConfig().getDouble("bounty.maximum-amount", 100000.0);
        createTables();
        loadBounties();
    }
    
    public static BountyManager getInstance() {
        return instance;
    }
    
    /**
     * Create the bounty tables
     */
    private void createTables() {
        String query = "CREATE TABLE IF NOT EXISTS bounties (" +
                "id VARCHAR(36) PRIMARY KEY," +
                "target_uuid VARCHAR(36) NOT NULL," +
                "target_name VARCHAR(16) NOT NULL," +
                "issuer_uuid VARCHAR(36) NOT NULL," +
                "issuer_name VARCHAR(16) NOT NULL," +
                "amount DOUBLE NOT NULL," +
                "created_at BIGINT NOT NULL," +
                "active BOOLEAN NOT NULL DEFAULT 1" +
                ")";
        database.executeUpdate(query);
        
        String indexQuery = "CREATE INDEX IF NOT EXISTS idx_bounties_target ON bounties(target_uuid, active)";
        database.executeUpdate(indexQuery);
        
        LOGGER.info("Bounty tables created/verified");
    }
    
    /**
     * Load all active bounties from the database
     */
    private void loadBounties() {
        ResultSet rs = database.executeQuery("SELECT * FROM bounties WHERE active = 1");
        try {
            while (rs != null && rs.next()) {
                Bounty bounty = new Bounty(
                    UUID.fromString(rs.getString("id")),
                    UUID.fromString(rs.getString("target_uuid")),
                    rs.getString("target_name"),
                    UUID.fromString(rs.getString("issuer_uuid")),
                    rs.getString("issuer_name"),
                    rs.getDouble("amount"),
                    rs.getLong("created_at"),
                    rs.getBoolean("active")
                );
                
                activeBounties.computeIfAbsent(bounty.getTargetUuid(), k -> new ArrayList<>()).add(bounty);
            }
            if (rs != null) rs.close();
            LOGGER.info("Loaded " + activeBounties.values().stream().mapToInt(List::size).sum() + " active bounties");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to load bounties", e);
        }
    }
    
    /**
     * Place a bounty on a player
     * @param issuerUuid UUID of the player placing the bounty
     * @param issuerName Name of the issuer
     * @param targetUuid UUID of the target player
     * @param targetName Name of the target
     * @param amount Bounty amount
     * @return true if successful, false otherwise
     */
    public boolean placeBounty(UUID issuerUuid, String issuerName, UUID targetUuid, String targetName, double amount) {
        if (issuerUuid.equals(targetUuid)) {
            return false; // Can't place bounty on yourself
        }
        
        if (amount < minimumBounty || amount > maximumBounty) {
            return false; // Invalid amount
        }
        
        EconomyManager eco = Survival.getInstance().getEconomyManager();
        if (eco.getBalance(issuerUuid) < amount) {
            return false; // Not enough money
        }
        
        // Deduct money from issuer
        eco.removeBalance(issuerUuid, amount);
        
        // Create bounty
        Bounty bounty = new Bounty(
            UUID.randomUUID(),
            targetUuid,
            targetName,
            issuerUuid,
            issuerName,
            amount,
            System.currentTimeMillis(),
            true
        );
        
        // Save to database
        database.executeUpdate(
            "INSERT INTO bounties (id, target_uuid, target_name, issuer_uuid, issuer_name, amount, created_at, active) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            bounty.getId().toString(),
            bounty.getTargetUuid().toString(),
            bounty.getTargetName(),
            bounty.getIssuerUuid().toString(),
            bounty.getIssuerName(),
            bounty.getAmount(),
            bounty.getCreatedAt(),
            bounty.isActive()
        );
        
        // Add to cache
        activeBounties.computeIfAbsent(targetUuid, k -> new ArrayList<>()).add(bounty);
        
        return true;
    }
    
    /**
     * Claim a bounty when a player is killed
     * @param killerUuid UUID of the killer
     * @param killerName Name of the killer
     * @param victimUuid UUID of the victim
     * @return total bounty amount claimed
     */
    public double claimBounty(UUID killerUuid, String killerName, UUID victimUuid) {
        List<Bounty> bounties = activeBounties.get(victimUuid);
        if (bounties == null || bounties.isEmpty()) {
            return 0.0;
        }
        
        double totalAmount = 0.0;
        EconomyManager eco = Survival.getInstance().getEconomyManager();
        
        for (Bounty bounty : new ArrayList<>(bounties)) {
            if (bounty.isActive()) {
                // Don't let issuer claim their own bounty
                if (bounty.getIssuerUuid().equals(killerUuid)) {
                    continue;
                }
                
                totalAmount += bounty.getAmount();
                
                // Give money to killer
                eco.addBalance(killerUuid, bounty.getAmount());
                
                // Mark as inactive
                bounty.setActive(false);
                database.executeUpdate("UPDATE bounties SET active = 0 WHERE id = ?", bounty.getId().toString());
            }
        }
        
        // Remove inactive bounties from cache
        bounties.removeIf(b -> !b.isActive());
        if (bounties.isEmpty()) {
            activeBounties.remove(victimUuid);
        }
        
        return totalAmount;
    }
    
    /**
     * Get all active bounties for a player
     * @param targetUuid UUID of the target
     * @return list of bounties
     */
    public List<Bounty> getBounties(UUID targetUuid) {
        return activeBounties.getOrDefault(targetUuid, new ArrayList<>());
    }
    
    /**
     * Get total bounty amount on a player
     * @param targetUuid UUID of the target
     * @return total amount
     */
    public double getTotalBounty(UUID targetUuid) {
        List<Bounty> bounties = activeBounties.get(targetUuid);
        if (bounties == null || bounties.isEmpty()) {
            return 0.0;
        }
        
        return bounties.stream()
                .filter(Bounty::isActive)
                .mapToDouble(Bounty::getAmount)
                .sum();
    }
    
    /**
     * Get all active bounties
     * @return map of target UUID to bounties
     */
    public Map<UUID, List<Bounty>> getAllBounties() {
        return new HashMap<>(activeBounties);
    }
    
    /**
     * Cancel a bounty (refund issuer)
     * @param bountyId ID of the bounty
     * @param issuerUuid UUID of the issuer (for verification)
     * @return true if successful
     */
    public boolean cancelBounty(UUID bountyId, UUID issuerUuid) {
        for (List<Bounty> bounties : activeBounties.values()) {
            for (Bounty bounty : bounties) {
                if (bounty.getId().equals(bountyId) && bounty.getIssuerUuid().equals(issuerUuid) && bounty.isActive()) {
                    // Refund issuer
                    EconomyManager eco = Survival.getInstance().getEconomyManager();
                    eco.addBalance(issuerUuid, bounty.getAmount());
                    
                    // Mark as inactive
                    bounty.setActive(false);
                    database.executeUpdate("UPDATE bounties SET active = 0 WHERE id = ?", bounty.getId().toString());
                    
                    // Remove from cache
                    bounties.remove(bounty);
                    if (bounties.isEmpty()) {
                        activeBounties.remove(bounty.getTargetUuid());
                    }
                    
                    return true;
                }
            }
        }
        
        return false;
    }
    
    public double getMinimumBounty() {
        return minimumBounty;
    }
    
    public double getMaximumBounty() {
        return maximumBounty;
    }
}
