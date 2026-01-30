package com.alexpsvet.rpgmobs;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Represents a special power that RPG mobs can use
 */
public class RPGMobPower {
    private final PowerType type;
    private final double chance;
    private final int cooldown; // in ticks
    private final Map<UUID, Long> lastUseTime;
    private final Random random;

    public RPGMobPower(PowerType type, double chance, int cooldown) {
        this.type = type;
        this.chance = chance;
        this.cooldown = cooldown;
        this.lastUseTime = new HashMap<>();
        this.random = new Random();
    }

    /**
     * Check if the power can be used (cooldown and chance)
     */
    public boolean canUse(UUID mobUuid, long currentTick) {
        // Check cooldown
        if (lastUseTime.containsKey(mobUuid)) {
            long lastUse = lastUseTime.get(mobUuid);
            if (currentTick - lastUse < cooldown) {
                return false;
            }
        }

        // Check random chance
        return random.nextDouble() <= chance;
    }

    /**
     * Use the power
     */
    public void use(LivingEntity mob, Entity target, long currentTick) {
        lastUseTime.put(mob.getUniqueId(), currentTick);
        
        switch (type) {
            case TELEPORT_STRIKE:
                teleportStrike(mob, target);
                break;
            case LEAP_ATTACK:
                leapAttack(mob, target);
                break;
            case SUMMON_MINIONS:
                summonMinions(mob);
                break;
            case LIGHTNING_STRIKE:
                lightningStrike(mob, target);
                break;
            case FIREBALL:
                fireball(mob, target);
                break;
        }
    }

    /**
     * Teleport behind the target and strike
     */
    private void teleportStrike(LivingEntity mob, Entity target) {
        if (!(target instanceof LivingEntity)) return;
        
        Location targetLoc = target.getLocation();
        Vector direction = targetLoc.getDirection().multiply(-1).normalize();
        Location teleportLoc = targetLoc.clone().add(direction.multiply(2));
        teleportLoc.setY(targetLoc.getY());
        
        // Ensure the location is safe
        if (teleportLoc.getBlock().getType().isSolid()) {
            teleportLoc.add(0, 1, 0);
        }
        
        // Particle effect at old location
        mob.getWorld().spawnParticle(Particle.PORTAL, mob.getLocation(), 50, 0.5, 1, 0.5, 0.1);
        mob.getWorld().playSound(mob.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
        
        // Teleport
        mob.teleport(teleportLoc);
        
        // Particle effect at new location
        mob.getWorld().spawnParticle(Particle.PORTAL, teleportLoc, 50, 0.5, 1, 0.5, 0.1);
        mob.getWorld().playSound(teleportLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
        
        // Face the target
        Location lookAt = target.getLocation();
        Vector lookVector = lookAt.toVector().subtract(teleportLoc.toVector());
        Location newLoc = teleportLoc.clone();
        newLoc.setDirection(lookVector);
        mob.teleport(newLoc);
    }

    /**
     * Leap towards the target
     */
    private void leapAttack(LivingEntity mob, Entity target) {
        if (!(target instanceof LivingEntity)) return;
        
        Location mobLoc = mob.getLocation();
        Location targetLoc = target.getLocation();
        
        // Calculate leap vector
        Vector direction = targetLoc.toVector().subtract(mobLoc.toVector()).normalize();
        direction.setY(0.5); // Add upward component
        direction.multiply(1.5); // Leap strength
        
        // Apply velocity
        mob.setVelocity(direction);
        
        // Effects
        mob.getWorld().spawnParticle(Particle.CLOUD, mobLoc, 20, 0.5, 0.5, 0.5, 0.1);
        mob.getWorld().playSound(mobLoc, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.5f, 1.5f);
    }

    /**
     * Summon minions around the mob
     */
    private void summonMinions(LivingEntity mob) {
        Location loc = mob.getLocation();
        
        // Spawn 2-3 minions of the same type
        int count = random.nextInt(2) + 2;
        for (int i = 0; i < count; i++) {
            double angle = (2 * Math.PI * i) / count;
            double x = loc.getX() + Math.cos(angle) * 2;
            double z = loc.getZ() + Math.sin(angle) * 2;
            Location spawnLoc = new Location(loc.getWorld(), x, loc.getY(), z);
            
            // Spawn minion
            LivingEntity minion = (LivingEntity) loc.getWorld().spawnEntity(spawnLoc, mob.getType());
            
            // Make it slightly weaker
            minion.setMaxHealth(mob.getMaxHealth() * 0.5);
            minion.setHealth(minion.getMaxHealth());
            minion.setCustomName("§7Minion");
            minion.setCustomNameVisible(true);
            
            // Particle effect
            mob.getWorld().spawnParticle(Particle.SMOKE_LARGE, spawnLoc, 30, 0.5, 0.5, 0.5, 0.05);
        }
        
        mob.getWorld().playSound(loc, Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 1.0f, 0.8f);
    }

    /**
     * Strike lightning at target
     */
    private void lightningStrike(LivingEntity mob, Entity target) {
        if (!(target instanceof LivingEntity)) return;
        
        Location targetLoc = target.getLocation();
        
        // Visual lightning (no damage from actual lightning)
        mob.getWorld().strikeLightningEffect(targetLoc);
        
        // Apply damage
        LivingEntity livingTarget = (LivingEntity) target;
        livingTarget.damage(10.0, mob);
        
        // Particle effects
        mob.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, targetLoc, 50, 1, 2, 1, 0.1);
    }

    /**
     * Shoot a fireball at the target
     */
    private void fireball(LivingEntity mob, Entity target) {
        if (!(target instanceof LivingEntity)) return;
        
        Location mobLoc = mob.getEyeLocation();
        Vector direction = target.getLocation().toVector().subtract(mobLoc.toVector()).normalize();
        
        // Spawn fireball
        org.bukkit.entity.Fireball fireball = mob.getWorld().spawn(
            mobLoc.add(direction), 
            org.bukkit.entity.Fireball.class
        );
        
        fireball.setShooter(mob);
        fireball.setDirection(direction);
        fireball.setYield(2.0f); // Explosion power
        
        mob.getWorld().playSound(mobLoc, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 1.0f);
    }

    /**
     * Clean up old cooldown data for removed mobs
     */
    public void cleanupOldData() {
        lastUseTime.entrySet().removeIf(entry -> 
            System.currentTimeMillis() - entry.getValue() > 300000); // 5 minutes
    }

    // Getters
    public PowerType getType() {
        return type;
    }

    public double getChance() {
        return chance;
    }

    public int getCooldown() {
        return cooldown;
    }

    /**
     * Types of special powers
     */
    public enum PowerType {
        TELEPORT_STRIKE,    // Teleport behind target
        LEAP_ATTACK,        // Leap towards target
        SUMMON_MINIONS,     // Summon weaker versions
        LIGHTNING_STRIKE,   // Strike lightning
        FIREBALL            // Shoot fireball
    }
}
