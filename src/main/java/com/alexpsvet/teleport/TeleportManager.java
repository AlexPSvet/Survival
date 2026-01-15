package com.alexpsvet.teleport;

import com.alexpsvet.Survival;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages teleport requests
 */
public class TeleportManager {
    private static TeleportManager instance;
    private final Map<UUID, TeleportRequest> requests; // target UUID -> request
    private final Map<UUID, Long> cooldowns; // requester UUID -> cooldown end time
    
    public TeleportManager() {
        instance = this;
        this.requests = new HashMap<>();
        this.cooldowns = new HashMap<>();
    }
    
    /**
     * Send a teleport request
     */
    public boolean sendRequest(Player requester, Player target) {
        // Check cooldown
        if (isOnCooldown(requester.getUniqueId())) {
            long remaining = getCooldownRemaining(requester.getUniqueId());
            return false;
        }
        
        // Check if already has pending request
        if (requests.containsKey(target.getUniqueId())) {
            return false;
        }
        
        long expireTime = Survival.getInstance().getConfig().getLong("teleport.request-expire-seconds", 30);
        TeleportRequest request = new TeleportRequest(requester.getUniqueId(), target.getUniqueId(), expireTime);
        requests.put(target.getUniqueId(), request);
        
        // Auto-expire after time
        Bukkit.getScheduler().runTaskLater(Survival.getInstance(), () -> {
            if (requests.get(target.getUniqueId()) == request) {
                requests.remove(target.getUniqueId());
            }
        }, expireTime * 20L);
        
        return true;
    }
    
    /**
     * Accept a teleport request
     */
    public boolean acceptRequest(Player target) {
        TeleportRequest request = requests.remove(target.getUniqueId());
        if (request == null || request.isExpired()) {
            return false;
        }
        
        Player requester = Bukkit.getPlayer(request.getRequester());
        if (requester == null || !requester.isOnline()) {
            return false;
        }
        
        // Start teleport with delay
        int delay = Survival.getInstance().getConfig().getInt("teleport.delay-seconds", 3);
        startTeleport(requester, target.getLocation(), delay);
        
        // Set cooldown
        int cooldown = Survival.getInstance().getConfig().getInt("teleport.cooldown-seconds", 60);
        setCooldown(requester.getUniqueId(), cooldown);
        
        return true;
    }
    
    /**
     * Deny a teleport request
     */
    public boolean denyRequest(Player target) {
        TeleportRequest request = requests.remove(target.getUniqueId());
        return request != null;
    }
    
    /**
     * Get pending request for a player
     */
    public TeleportRequest getRequest(UUID targetUuid) {
        return requests.get(targetUuid);
    }
    
    /**
     * Start teleport with delay
     */
    public void startTeleport(Player player, Location destination, int delaySeconds) {
        Location startLocation = player.getLocation().clone();
        
        new BukkitRunnable() {
            int countdown = delaySeconds;
            
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }
                
                // Check if player moved
                if (player.getLocation().distance(startLocation) > 0.5) {
                    player.sendMessage("§cTéléportation annulée car vous avez bougé!");
                    cancel();
                    return;
                }
                
                if (countdown <= 0) {
                    player.teleport(destination);
                    player.sendMessage("§aTéléportation effectuée!");
                    cancel();
                    return;
                }
                
                player.sendMessage("§eTéléportation dans §6" + countdown + "§e seconde(s)...");
                countdown--;
            }
        }.runTaskTimer(Survival.getInstance(), 0L, 20L);
    }
    
    /**
     * Check if player is on cooldown
     */
    public boolean isOnCooldown(UUID uuid) {
        Long endTime = cooldowns.get(uuid);
        if (endTime == null) return false;
        
        if (System.currentTimeMillis() >= endTime) {
            cooldowns.remove(uuid);
            return false;
        }
        return true;
    }
    
    /**
     * Get cooldown remaining time in seconds
     */
    public long getCooldownRemaining(UUID uuid) {
        Long endTime = cooldowns.get(uuid);
        if (endTime == null) return 0;
        
        long remaining = endTime - System.currentTimeMillis();
        return Math.max(0, remaining / 1000);
    }
    
    /**
     * Set cooldown for a player
     */
    public void setCooldown(UUID uuid, int seconds) {
        cooldowns.put(uuid, System.currentTimeMillis() + (seconds * 1000L));
    }
    
    public static TeleportManager getInstance() {
        return instance;
    }
}
