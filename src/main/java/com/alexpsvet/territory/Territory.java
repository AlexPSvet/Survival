package com.alexpsvet.territory;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Represents a protected territory
 */
public class Territory {
    private final int id;
    private final UUID owner;
    private final String ownerName;
    private final Location center;
    private final int radius;
    private final long createdAt;
    private String clanName; // Optional clan ownership
    private Set<UUID> trusted; // Trusted players
    private TerritoryFlags flags;
    private boolean showingParticleBorder; // Whether particle border is currently showing
    
    public Territory(int id, UUID owner, String ownerName, Location center, int radius, long createdAt) {
        this.id = id;
        this.owner = owner;
        this.ownerName = ownerName;
        this.center = center;
        this.radius = radius;
        this.createdAt = createdAt;
        this.trusted = new HashSet<>();
        this.flags = new TerritoryFlags();
        this.showingParticleBorder = false;
    }
    
    public int getId() {
        return id;
    }
    
    public UUID getOwner() {
        return owner;
    }
    
    public String getOwnerName() {
        return ownerName;
    }
    
    public Location getCenter() {
        return center;
    }
    
    public int getRadius() {
        return radius;
    }
    
    public long getCreatedAt() {
        return createdAt;
    }
    
    public String getClanName() {
        return clanName;
    }
    
    public void setClanName(String clanName) {
        this.clanName = clanName;
    }
    
    public Set<UUID> getTrusted() {
        return trusted;
    }
    
    public void addTrusted(UUID uuid) {
        trusted.add(uuid);
    }
    
    public void removeTrusted(UUID uuid) {
        trusted.remove(uuid);
    }
    
    public boolean isTrusted(UUID uuid) {
        return trusted.contains(uuid);
    }
    
    public TerritoryFlags getFlags() {
        return flags;
    }
    
    public void setFlags(TerritoryFlags flags) {
        this.flags = flags;
    }
    
    /**
     * Check if a location is within this territory
     */
    public boolean contains(Location location) {
        if (!location.getWorld().equals(center.getWorld())) {
            return false;
        }
        
        int dx = location.getBlockX() - center.getBlockX();
        int dy = location.getBlockY() - center.getBlockY();
        int dz = location.getBlockZ() - center.getBlockZ();
        
        return (dx * dx + dy * dy + dz * dz) <= (radius * radius);
    }
    
    /**
     * Check if a block is within this territory
     */
    public boolean contains(Block block) {
        return contains(block.getLocation());
    }
    
    /**
     * Check if a player has permission in this territory
     */
    public boolean hasPermission(UUID player) {
        return owner.equals(player) || isTrusted(player);
    }
    
    /**
     * Get the size (number of blocks) covered by this territory
     */
    public int getSize() {
        return (int) ((4.0 / 3.0) * Math.PI * radius * radius * radius);
    }
    
    /**
     * Check if particle border is showing
     */
    public boolean isShowingParticleBorder() {
        return showingParticleBorder;
    }
    
    /**
     * Set whether particle border should be showing
     */
    public void setShowingParticleBorder(boolean showing) {
        this.showingParticleBorder = showing;
    }
}
