package com.alexpsvet.home;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.UUID;

/**
 * Represents a player home
 */
public class Home {
    private final UUID owner;
    private final String name;
    private final String worldName;
    private final double x, y, z;
    private final float yaw, pitch;
    
    public Home(UUID owner, String name, Location location) {
        this.owner = owner;
        this.name = name;
        this.worldName = location.getWorld().getName();
        this.x = location.getX();
        this.y = location.getY();
        this.z = location.getZ();
        this.yaw = location.getYaw();
        this.pitch = location.getPitch();
    }
    
    public Home(UUID owner, String name, String worldName, double x, double y, double z, float yaw, float pitch) {
        this.owner = owner;
        this.name = name;
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }
    
    public UUID getOwner() { return owner; }
    public String getName() { return name; }
    public String getWorldName() { return worldName; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }
    
    /**
     * Get the location
     */
    public Location getLocation() {
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;
        return new Location(world, x, y, z, yaw, pitch);
    }
}