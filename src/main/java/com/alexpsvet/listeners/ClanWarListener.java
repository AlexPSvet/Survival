package com.alexpsvet.listeners;

import com.alexpsvet.clan.war.ClanWarManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

/**
 * Listener for clan war events
 */
public class ClanWarListener implements Listener {
    
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        ClanWarManager warManager = ClanWarManager.getInstance();
        if (warManager != null) {
            warManager.handlePlayerDeath(event.getEntity().getUniqueId());
        }
    }
}
