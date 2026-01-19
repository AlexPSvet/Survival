package com.alexpsvet.listeners;

import com.alexpsvet.Survival;
import com.alexpsvet.display.ScoreboardManager;
import com.alexpsvet.display.TabManager;
import com.alexpsvet.economy.EconomyManager;
import com.alexpsvet.player.PlayerStatsManager;
import com.alexpsvet.territory.TerritoryDisplayManager;
import com.alexpsvet.utils.MessageUtil;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;

/**
 * Listener for player join events
 */
public class PlayerJoinListener implements Listener {
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        EconomyManager economyManager = Survival.getInstance().getEconomyManager();
        PlayerStatsManager statsManager = Survival.getInstance().getStatsManager();
        FileConfiguration messages = Survival.getInstance().getConfig();
        
        // Create economy account if doesn't exist
        boolean isNewPlayer = !economyManager.hasAccount(event.getPlayer().getUniqueId());
        if (isNewPlayer) {
            economyManager.createAccount(event.getPlayer().getUniqueId(), event.getPlayer().getName());
        }
        
        // Load player stats
        statsManager.loadStats(event.getPlayer().getUniqueId(), event.getPlayer().getName());
        
        // Create scoreboard and tab
        ScoreboardManager.getInstance().createScoreboard(event.getPlayer());
        TabManager.getInstance().updateTab(event.getPlayer());
        
        // Welcome title and fireworks
        sendWelcomeEffects(event.getPlayer(), isNewPlayer);
        
        // Custom join messages
        if (isNewPlayer) {
            String msg = MessageUtil.format(
                Survival.getInstance().getConfig().getString("messages.welcome.first-join", ""),
                "{player}", event.getPlayer().getName()
            );
            event.setJoinMessage(MessageUtil.colorize(msg));
        } else {
            String msg = MessageUtil.format(
                Survival.getInstance().getConfig().getString("messages.welcome.join", ""),
                "{player}", event.getPlayer().getName()
            );
            event.setJoinMessage(MessageUtil.colorize(msg));
        }
    }
    
    /**
     * Send welcome title and spawn fireworks
     */
    private void sendWelcomeEffects(org.bukkit.entity.Player player, boolean isNewPlayer) {
        // Send title with colors and effects
        String title = MessageUtil.colorize("&6&lBienvenido a &e&lPeriquito");
        String subtitle = isNewPlayer ? 
            MessageUtil.colorize("&a¡Bienvenido por primera vez!") : 
            MessageUtil.colorize("&7¡Bienvenido de vuelta!");
        
        player.sendTitle(title, subtitle, 10, 70, 20);
        
        // Play sound
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        
        // Spawn fireworks
        Bukkit.getScheduler().runTaskLater(Survival.getInstance(), () -> {
            spawnFirework(player.getLocation());
        }, 20L);
        
        Bukkit.getScheduler().runTaskLater(Survival.getInstance(), () -> {
            spawnFirework(player.getLocation().add(2, 0, 2));
        }, 30L);
        
        Bukkit.getScheduler().runTaskLater(Survival.getInstance(), () -> {
            spawnFirework(player.getLocation().add(-2, 0, -2));
        }, 40L);
    }
    
    /**
     * Spawn a firework at location
     */
    private void spawnFirework(Location location) {
        Firework firework = (Firework) location.getWorld().spawnEntity(location, EntityType.FIREWORK);
        FireworkMeta meta = firework.getFireworkMeta();
        
        // Mark as welcome firework (won't cause damage)
        firework.setMetadata("welcome_firework", new FixedMetadataValue(Survival.getInstance(), true));
        
        // Create random firework effect
        FireworkEffect.Builder effectBuilder = FireworkEffect.builder();
        effectBuilder.with(FireworkEffect.Type.BALL_LARGE);
        
        // Random colors
        Color[] colors = {Color.RED, Color.YELLOW, Color.LIME, Color.AQUA, Color.FUCHSIA, Color.ORANGE};
        effectBuilder.withColor(colors[(int) (Math.random() * colors.length)]);
        effectBuilder.withFade(colors[(int) (Math.random() * colors.length)]);
        
        effectBuilder.withFlicker();
        effectBuilder.withTrail();
        
        meta.addEffect(effectBuilder.build());
        meta.setPower(1);
        
        firework.setFireworkMeta(meta);
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Clean up territory display manager data
        TerritoryDisplayManager territoryDisplayManager = Survival.getInstance().getTerritoryDisplayManager();
        if (territoryDisplayManager != null) {
            territoryDisplayManager.onPlayerQuit(event.getPlayer());
        }
    }
    
    /**
     * Prevent damage from welcome fireworks
     */
    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // Check if damage is caused by a firework
        if (event.getDamager() instanceof Firework) {
            Firework firework = (Firework) event.getDamager();
            
            // Check if it's a welcome firework
            if (firework.hasMetadata("welcome_firework")) {
                event.setCancelled(true);
            }
        }
    }
}
