package com.alexpsvet.rpgmobs;

import org.bukkit.entity.LivingEntity;

/**
 * Represents an active RPG mob in the world
 */
public class RPGMob {
    private final LivingEntity entity;
    private final RPGMobConfig config;
    private final int level;
    private final double maxHealth;
    private final String baseName;

    public RPGMob(LivingEntity entity, RPGMobConfig config, int level, String baseName) {
        this.entity = entity;
        this.config = config;
        this.level = level;
        this.maxHealth = entity.getMaxHealth();
        this.baseName = baseName;
    }

    public LivingEntity getEntity() {
        return entity;
    }

    public RPGMobConfig getConfig() {
        return config;
    }

    public int getLevel() {
        return level;
    }

    public double getMaxHealth() {
        return maxHealth;
    }

    public String getBaseName() {
        return baseName;
    }

    /**
     * Check if this RPG mob is still valid (entity exists and is alive)
     */
    public boolean isValid() {
        return entity != null && entity.isValid() && !entity.isDead();
    }
}
