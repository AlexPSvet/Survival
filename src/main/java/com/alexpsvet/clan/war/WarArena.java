package com.alexpsvet.clan.war;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.Bukkit;
import com.alexpsvet.Survival;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Manages the war arena world
 */
public class WarArena {
    private static final Logger LOGGER = Logger.getLogger("survival");
    private static final String TEMPLATE_WORLD_NAME = "war_arena_template";
    private static final String WAR_WORLD_NAME = "war_arena";
    
    private World warWorld;
    private Location clan1Spawn;
    private Location clan2Spawn;
    private Location centerLocation;
    
    /**
     * Initialize the arena from template
     */
    public boolean initializeArena() {
        try {
            File templateFolder = new File(Bukkit.getWorldContainer(), TEMPLATE_WORLD_NAME);
            File warFolder = new File(Bukkit.getWorldContainer(), WAR_WORLD_NAME);
            
            // Check if template exists
            if (!templateFolder.exists()) {
                LOGGER.severe("War arena template world '" + TEMPLATE_WORLD_NAME + "' not found!");
                return false;
            }
            
            // Unload existing war world if loaded
            if (Bukkit.getWorld(WAR_WORLD_NAME) != null) {
                unloadArena();
            }
            
            // Delete old war world
            if (warFolder.exists()) {
                FileUtils.deleteDirectory(warFolder);
            }
            
            // Copy template to war world
            FileUtils.copyDirectory(templateFolder, warFolder);
            
            // Load the world
            WorldCreator creator = new WorldCreator(WAR_WORLD_NAME);
            warWorld = Bukkit.createWorld(creator);
            
            if (warWorld == null) {
                LOGGER.severe("Failed to create war arena world!");
                return false;
            }
            
            // Configure world
            warWorld.setAutoSave(false);
            warWorld.setKeepSpawnInMemory(true);
            
            // Load spawn locations from config
            loadSpawnLocations();
            
            LOGGER.info("War arena initialized successfully!");
            return true;
            
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to copy war arena template", e);
            return false;
        }
    }
    
    /**
     * Load spawn locations from config
     */
    private void loadSpawnLocations() {
        if (warWorld == null) return;
        
        // Clan 1 spawn
        int clan1X = Survival.getInstance().getConfig().getInt("clan-wars.arena.clan1-spawn.x", 0);
        int clan1Y = Survival.getInstance().getConfig().getInt("clan-wars.arena.clan1-spawn.y", 70);
        int clan1Z = Survival.getInstance().getConfig().getInt("clan-wars.arena.clan1-spawn.z", -50);
        clan1Spawn = new Location(warWorld, clan1X, clan1Y, clan1Z);
        
        // Clan 2 spawn
        int clan2X = Survival.getInstance().getConfig().getInt("clan-wars.arena.clan2-spawn.x", 0);
        int clan2Y = Survival.getInstance().getConfig().getInt("clan-wars.arena.clan2-spawn.y", 70);
        int clan2Z = Survival.getInstance().getConfig().getInt("clan-wars.arena.clan2-spawn.z", 50);
        clan2Spawn = new Location(warWorld, clan2X, clan2Y, clan2Z);
        
        // Center
        int centerX = Survival.getInstance().getConfig().getInt("clan-wars.arena.center.x", 0);
        int centerY = Survival.getInstance().getConfig().getInt("clan-wars.arena.center.y", 70);
        int centerZ = Survival.getInstance().getConfig().getInt("clan-wars.arena.center.z", 0);
        centerLocation = new Location(warWorld, centerX, centerY, centerZ);
    }
    
    /**
     * Restore the arena to its original state
     */
    public boolean restoreArena() {
        try {
            // Kick all players out
            if (warWorld != null) {
                warWorld.getPlayers().forEach(player -> {
                    Location spawn = Bukkit.getWorlds().get(0).getSpawnLocation();
                    player.teleport(spawn);
                });
            }
            
            // Unload and delete
            unloadArena();
            
            File warFolder = new File(Bukkit.getWorldContainer(), WAR_WORLD_NAME);
            if (warFolder.exists()) {
                FileUtils.deleteDirectory(warFolder);
            }
            
            LOGGER.info("War arena restored successfully!");
            return true;
            
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to restore war arena", e);
            return false;
        }
    }
    
    /**
     * Unload the arena world
     */
    private void unloadArena() {
        if (warWorld != null) {
            Bukkit.unloadWorld(warWorld, false);
            warWorld = null;
        }
    }
    
    public World getWarWorld() {
        return warWorld;
    }
    
    public Location getClan1Spawn() {
        return clan1Spawn;
    }
    
    public Location getClan2Spawn() {
        return clan2Spawn;
    }
    
    public Location getCenterLocation() {
        return centerLocation;
    }
}
