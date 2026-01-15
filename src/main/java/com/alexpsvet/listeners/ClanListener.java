package com.alexpsvet.listeners;

import com.alexpsvet.Survival;
import com.alexpsvet.clan.Clan;
import com.alexpsvet.clan.ClanManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Listener for clan-related events
 */
public class ClanListener implements Listener {
    
    @EventHandler
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player) || !(event.getDamager() instanceof Player)) {
            return;
        }
        
        Player victim = (Player) event.getEntity();
        Player attacker = (Player) event.getDamager();
        
        ClanManager clanManager = Survival.getInstance().getClanManager();
        Clan attackerClan = clanManager.getPlayerClan(attacker.getUniqueId());
        Clan victimClan = clanManager.getPlayerClan(victim.getUniqueId());
        
        // Check if both are in the same clan
        if (attackerClan != null && victimClan != null && attackerClan.equals(victimClan)) {
            if (!attackerClan.isFriendlyFire()) {
                event.setCancelled(true);
            }
        }
        
        // Check if they are allies
        if (attackerClan != null && victimClan != null && attackerClan.isAlly(victimClan.getName())) {
            event.setCancelled(true);
        }
    }
}
