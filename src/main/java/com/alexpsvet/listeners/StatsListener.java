package com.alexpsvet.listeners;

import com.alexpsvet.display.ScoreboardManager;
import com.alexpsvet.display.TabManager;
import com.alexpsvet.player.PlayerStatsManager;
import com.alexpsvet.player.PlayerStats;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Listener for player stats tracking
 */
public class StatsListener implements Listener {
    
    /**
     * Track player kills and deaths
     */
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        PlayerStats victimStats = PlayerStatsManager.getInstance().getStats(victim.getUniqueId());
        
        if (victimStats != null) {
            victimStats.addDeath();
            PlayerStatsManager.getInstance().saveStats(victimStats);
        }
        
        // Track killer stats
        Player killer = victim.getKiller();
        if (killer != null && killer != victim) {
            PlayerStats killerStats = PlayerStatsManager.getInstance().getStats(killer.getUniqueId());
            if (killerStats != null) {
                killerStats.addKill();
                PlayerStatsManager.getInstance().saveStats(killerStats);
                
                // Update scoreboard
                ScoreboardManager.getInstance().updateAll();
            }
        }
    }
    
    /**
     * Save stats and playtime on quit
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        PlayerStatsManager.getInstance().unloadStats(player.getUniqueId());
    }
}
