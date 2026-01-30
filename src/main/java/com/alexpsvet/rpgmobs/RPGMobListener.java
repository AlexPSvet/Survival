package com.alexpsvet.rpgmobs;

import com.alexpsvet.economy.EconomyManager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.List;
import java.util.Random;

/**
 * Listener for RPG mobs events
 */
public class RPGMobListener implements Listener {
    private final RPGMobManager mobManager;
    private long currentTick = 0;

    public RPGMobListener(RPGMobManager mobManager) {
        this.mobManager = mobManager;
    }

    /**
     * Handle mob spawning - customize mobs when they spawn
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!mobManager.isEnabled()) return;
        if (event.isCancelled()) return;

        LivingEntity entity = event.getEntity();
        
        // Don't customize named mobs (likely custom NPCs or special mobs)
        if (entity.getCustomName() != null && !entity.getCustomName().isEmpty()) {
            return;
        }

        // Ignore certain spawn reasons (spawner, egg, etc. if desired)
        CreatureSpawnEvent.SpawnReason reason = event.getSpawnReason();
        if (reason == CreatureSpawnEvent.SpawnReason.SPAWNER_EGG ||
            reason == CreatureSpawnEvent.SpawnReason.SPAWNER ||
            reason == CreatureSpawnEvent.SpawnReason.BREEDING ||
            reason == CreatureSpawnEvent.SpawnReason.CUSTOM) {
            // Optionally skip these, or customize them too
            // For now, we'll customize all natural spawns
        }

        // Check spawn chance before customizing
        RPGMob rpgMob = mobManager.customizeMob(entity);
        if (rpgMob != null) {
            // Successfully created an RPG mob
            currentTick = entity.getWorld().getFullTime();
        }
    }

    /**
     * Handle entity damage - apply custom damage and attack effects
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!mobManager.isEnabled()) return;

        Entity damager = event.getDamager();
        Entity victim = event.getEntity();

        // RPG mob attacking a player
        if (damager instanceof LivingEntity && victim instanceof Player) {
            RPGMob rpgMob = mobManager.getRPGMob(damager.getUniqueId());
            if (rpgMob != null) {
                // Apply damage multiplier
                double damage = event.getDamage();
                double multiplier = rpgMob.getConfig().getDamageMultiplier();
                double levelBonus = 1 + (rpgMob.getLevel() - 1) * 0.05;
                event.setDamage(damage * multiplier * levelBonus);

                // Apply attack effects to victim
                Player player = (Player) victim;
                for (PotionEffect effect : rpgMob.getConfig().getAttackEffects()) {
                    player.addPotionEffect(effect);
                }
            }
        }

        // Player attacking RPG mob - update health display
        if (damager instanceof Player && victim instanceof LivingEntity) {
            RPGMob rpgMob = mobManager.getRPGMob(victim.getUniqueId());
            if (rpgMob != null) {
                mobManager.updateHealthDisplay(rpgMob);
            }
        }
    }

    /**
     * Handle mob targeting - trigger special powers
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityTarget(EntityTargetLivingEntityEvent event) {
        if (!mobManager.isEnabled()) return;
        if (event.isCancelled()) return;
        
        Entity mob = event.getEntity();
        Entity target = event.getTarget();
        
        if (!(mob instanceof LivingEntity) || target == null) return;
        
        RPGMob rpgMob = mobManager.getRPGMob(mob.getUniqueId());
        if (rpgMob == null) return;
        
        currentTick = mob.getWorld().getFullTime();
        
        // Try to use powers
        for (RPGMobPower power : rpgMob.getConfig().getPowers()) {
            if (power.canUse(mob.getUniqueId(), currentTick)) {
                power.use((LivingEntity) mob, target, currentTick);
                break; // Only use one power at a time
            }
        }
    }

    /**
     * Update health display when mob takes damage from any source
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDamageMonitor(EntityDamageEvent event) {
        if (!mobManager.isEnabled()) return;
        if (event.isCancelled()) return;

        if (event.getEntity() instanceof LivingEntity) {
            RPGMob rpgMob = mobManager.getRPGMob(event.getEntity().getUniqueId());
            if (rpgMob != null) {
                mobManager.updateHealthDisplay(rpgMob);
            }
        }
    }

    /**
     * Handle mob death - custom drops and rewards
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDeath(EntityDeathEvent event) {
        if (!mobManager.isEnabled()) return;

        LivingEntity entity = event.getEntity();
        RPGMob rpgMob = mobManager.getRPGMob(entity.getUniqueId());

        if (rpgMob == null) return;

        Player killer = entity.getKiller();
        
        // Add custom drops
        List<ItemStack> customDrops = mobManager.getCustomDrops(rpgMob);
        event.getDrops().addAll(customDrops);

        // Modify experience
        int baseExp = event.getDroppedExp();
        int newExp = (int) (baseExp * mobManager.getExpMultiplier() * (1 + rpgMob.getLevel() * 0.1));
        event.setDroppedExp(newExp);

        // Give money reward to killer
        if (killer != null) {
            EconomyManager economyManager = EconomyManager.getInstance();
            if (economyManager != null) {
                // Calculate money based on mob level
                double baseReward = 10.0 + (rpgMob.getLevel() * 5.0);
                double finalReward = baseReward * mobManager.getMoneyMultiplier();
                economyManager.addBalance(killer.getUniqueId(), finalReward);
                
                killer.sendMessage("§a+§e" + String.format("%.1f", finalReward) + 
                    " §a⛁ §7(RPG Mob Kill)");
            }
        }

        // Remove from active mobs
        mobManager.removeMob(entity.getUniqueId());
    }
}
