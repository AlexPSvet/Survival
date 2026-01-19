package com.alexpsvet.bounty;

import com.alexpsvet.Survival;
import com.alexpsvet.utils.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

/**
 * Listener for bounty-related events
 */
public class BountyListener implements Listener {
    
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        
        if (killer == null) {
            return; // Not killed by a player
        }
        
        BountyManager bountyManager = BountyManager.getInstance();
        double totalBounty = bountyManager.claimBounty(killer.getUniqueId(), killer.getName(), victim.getUniqueId());
        
        if (totalBounty > 0) {
            String currency = Survival.getInstance().getConfig().getString("economy.currency-symbol", "⛁");
            
            // Notify killer
            killer.sendMessage(MessageUtil.colorize("&a&l✓ Vous avez réclamé une prime de &e" + totalBounty + " " + currency + " &a&lpour avoir tué &6" + victim.getName() + "&a&l!"));
            
            // Notify victim
            victim.sendMessage(MessageUtil.colorize("&c&l✗ &6" + killer.getName() + " &c&la réclamé la prime sur votre tête de &e" + totalBounty + " " + currency + "&c&l!"));
            
            // Broadcast if high bounty
            if (totalBounty >= 10000) {
                Survival.getInstance().getServer().broadcastMessage(
                    MessageUtil.colorize("&6&l[PRIME] &6" + killer.getName() + " &ea réclamé une prime de &c" + totalBounty + " " + currency + " &epour avoir tué &6" + victim.getName() + "&e!")
                );
            }
        }
    }
}
