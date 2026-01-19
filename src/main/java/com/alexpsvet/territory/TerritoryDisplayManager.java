package com.alexpsvet.territory;

import com.alexpsvet.Survival;
import com.alexpsvet.chat.ChatManager;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manager for territory display features (titles and boss bars)
 * Uses an optimized approach with periodic checks instead of move events
 */
public class TerritoryDisplayManager {
    
    private final Survival plugin;
    private final TerritoryManager territoryManager;
    private final ChatManager chatManager;
    
    // Track which territory each player is currently in
    private final Map<UUID, Territory> playerTerritories;
    
    // Track boss bars for each player
    private final Map<UUID, BossBar> playerBossBars;
    
    // Task for periodic checking
    private BukkitRunnable checkTask;
    
    public TerritoryDisplayManager(Survival plugin, TerritoryManager territoryManager) {
        this.plugin = plugin;
        this.territoryManager = territoryManager;
        this.chatManager = ChatManager.getInstance();
        this.playerTerritories = new HashMap<>();
        this.playerBossBars = new HashMap<>();
        
        startCheckTask();
    }
    
    /**
     * Start the periodic task to check player locations
     * Runs every 10 ticks (0.5 seconds) for responsive detection while being optimized
     */
    private void startCheckTask() {
        checkTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    checkPlayerTerritory(player);
                }
            }
        };
        
        // Run every 10 ticks (0.5 seconds)
        checkTask.runTaskTimer(plugin, 0L, 10L);
    }
    
    /**
     * Check if a player has entered or left a territory
     */
    private void checkPlayerTerritory(Player player) {
        UUID playerId = player.getUniqueId();
        Territory currentTerritory = territoryManager.getTerritoryAt(player.getLocation());
        Territory previousTerritory = playerTerritories.get(playerId);
        
        // Player entered a new territory
        if (currentTerritory != null && !currentTerritory.equals(previousTerritory)) {
            onTerritoryEnter(player, currentTerritory);
            playerTerritories.put(playerId, currentTerritory);
        }
        // Player left a territory
        else if (currentTerritory == null && previousTerritory != null) {
            onTerritoryLeave(player, previousTerritory);
            playerTerritories.remove(playerId);
        }
    }
    
    /**
     * Handle player entering a territory
     */
    private void onTerritoryEnter(Player player, Territory territory) {
        // Show title
        String title = chatManager.getMessage("territory.enter-title");
        String subtitle = chatManager.getMessage("territory.enter-subtitle",
                "{owner}", territory.getOwnerName());
        
        player.sendTitle(
                ChatColor.translateAlternateColorCodes('&', title),
                ChatColor.translateAlternateColorCodes('&', subtitle),
                10, 70, 20
        );
        
        // Create and show boss bar
        createBossBar(player, territory);
        
        // Show territory boundaries with particles
        showTerritoryBoundaries(player, territory);
    }
    
    /**
     * Handle player leaving a territory
     */
    private void onTerritoryLeave(Player player, Territory territory) {
        // Show exit title
        String title = chatManager.getMessage("territory.exit-title");
        String subtitle = chatManager.getMessage("territory.exit-subtitle",
                "{owner}", territory.getOwnerName());
        
        player.sendTitle(
                ChatColor.translateAlternateColorCodes('&', title),
                ChatColor.translateAlternateColorCodes('&', subtitle),
                10, 50, 20
        );
        
        // Remove boss bar
        removeBossBar(player);
    }
    
    /**
     * Show territory boundaries with particles for 3 seconds
     * Only shows particles near the player's Y level (not extreme vertical boundaries)
     */
    private void showTerritoryBoundaries(Player player, Territory territory) {
        Location center = territory.getCenter();
        int radius = territory.getRadius();
        Location playerLoc = player.getLocation();
        
        // Particle display task - runs for 60 ticks (3 seconds) every 5 ticks
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= 60 || !player.isOnline()) {
                    cancel();
                    return;
                }
                
                // Show particles on the cylindrical boundary at player's approximate Y level
                int minY = playerLoc.getBlockY() - 5;
                int maxY = playerLoc.getBlockY() + 15;
                
                // Draw vertical lines at cardinal and intercardinal directions
                double[] angles = {0, 45, 90, 135, 180, 225, 270, 315};
                
                for (double angle : angles) {
                    double radians = Math.toRadians(angle);
                    double x = center.getX() + (radius * Math.cos(radians));
                    double z = center.getZ() + (radius * Math.sin(radians));
                    
                    // Draw vertical line with particles
                    for (int y = minY; y <= maxY; y += 2) {
                        Location particleLoc = new Location(center.getWorld(), x, y, z);
                        
                        // Only show if within reasonable distance from player
                        if (particleLoc.distance(playerLoc) < 50) {
                            player.spawnParticle(
                                Particle.VILLAGER_HAPPY,
                                particleLoc,
                                1,
                                0, 0, 0,
                                0
                            );
                        }
                    }
                }
                
                // Draw horizontal circles at min and max Y
                for (int y : new int[]{minY, maxY}) {
                    for (double angle = 0; angle < 360; angle += 15) {
                        double radians = Math.toRadians(angle);
                        double x = center.getX() + (radius * Math.cos(radians));
                        double z = center.getZ() + (radius * Math.sin(radians));
                        
                        Location particleLoc = new Location(center.getWorld(), x, y, z);
                        
                        if (particleLoc.distance(playerLoc) < 50) {
                            player.spawnParticle(
                                Particle.VILLAGER_HAPPY,
                                particleLoc,
                                1,
                                0, 0, 0,
                                0
                            );
                        }
                    }
                }
                
                ticks += 5;
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }
    
    /**
     * Create and show a boss bar for a territory
     */
    private void createBossBar(Player player, Territory territory) {
        // Remove existing boss bar if any
        removeBossBar(player);
        
        // Determine the owner display text
        String ownerText;
        if (territory.getClanName() != null) {
            ownerText = chatManager.getMessage("territory.bossbar-clan",
                    "{clan}", territory.getClanName());
        } else {
            ownerText = chatManager.getMessage("territory.bossbar-player",
                    "{owner}", territory.getOwnerName());
        }
        
        // Create boss bar
        String bossBarText = chatManager.getMessage("territory.bossbar",
                "{owner}", ownerText);
        
        BossBar bossBar = Bukkit.createBossBar(
                ChatColor.translateAlternateColorCodes('&', bossBarText),
                BarColor.BLUE,
                BarStyle.SOLID
        );
        
        bossBar.addPlayer(player);
        bossBar.setProgress(1.0);
        
        playerBossBars.put(player.getUniqueId(), bossBar);
    }
    
    /**
     * Remove a player's boss bar
     */
    private void removeBossBar(Player player) {
        UUID playerId = player.getUniqueId();
        BossBar bossBar = playerBossBars.remove(playerId);
        
        if (bossBar != null) {
            bossBar.removePlayer(player);
            bossBar.removeAll();
        }
    }
    
    /**
     * Clean up when a player quits
     */
    public void onPlayerQuit(Player player) {
        UUID playerId = player.getUniqueId();
        playerTerritories.remove(playerId);
        removeBossBar(player);
    }
    
    /**
     * Stop the check task and clean up all boss bars
     */
    public void shutdown() {
        if (checkTask != null) {
            checkTask.cancel();
        }
        
        // Remove all boss bars
        for (BossBar bossBar : playerBossBars.values()) {
            bossBar.removeAll();
        }
        
        playerBossBars.clear();
        playerTerritories.clear();
    }
}
