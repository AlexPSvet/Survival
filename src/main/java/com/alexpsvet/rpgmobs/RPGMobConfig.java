package com.alexpsvet.rpgmobs;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents the configuration for an RPG mob
 */
public class RPGMobConfig {
    private final EntityType entityType;
    private boolean enabled;
    private double spawnChance;
    private double healthMultiplier;
    private double damageMultiplier;
    private double speedMultiplier;
    private int minLevel;
    private int maxLevel;
    private String customName;
    private Double explosionPower;
    private final Map<EquipmentSlot, Equipment> equipment;
    private final List<Ability> abilities;
    private final List<PotionEffect> attackEffects;
    private final List<Drop> drops;
    private final List<RPGMobPower> powers;
    private final Map<String, WorldMultiplier> worldMultipliers;
    private final List<HeightMultiplier> heightMultipliers;

    public RPGMobConfig(EntityType entityType) {
        this.entityType = entityType;
        this.enabled = true;
        this.spawnChance = 1.0;
        this.healthMultiplier = 1.0;
        this.damageMultiplier = 1.0;
        this.speedMultiplier = 1.0;
        this.minLevel = 1;
        this.maxLevel = 5;
        this.customName = "";
        this.equipment = new HashMap<>();
        this.abilities = new ArrayList<>();
        this.attackEffects = new ArrayList<>();
        this.drops = new ArrayList<>();
        this.powers = new ArrayList<>();
        this.worldMultipliers = new HashMap<>();
        this.heightMultipliers = new ArrayList<>();
    }

    // Getters and Setters
    public EntityType getEntityType() {
        return entityType;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public double getSpawnChance() {
        return spawnChance;
    }

    public void setSpawnChance(double spawnChance) {
        this.spawnChance = spawnChance;
    }

    public double getHealthMultiplier() {
        return healthMultiplier;
    }

    public void setHealthMultiplier(double healthMultiplier) {
        this.healthMultiplier = healthMultiplier;
    }

    public double getDamageMultiplier() {
        return damageMultiplier;
    }

    public void setDamageMultiplier(double damageMultiplier) {
        this.damageMultiplier = damageMultiplier;
    }

    public double getSpeedMultiplier() {
        return speedMultiplier;
    }

    public void setSpeedMultiplier(double speedMultiplier) {
        this.speedMultiplier = speedMultiplier;
    }

    public int getMinLevel() {
        return minLevel;
    }

    public void setMinLevel(int minLevel) {
        this.minLevel = minLevel;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public void setMaxLevel(int maxLevel) {
        this.maxLevel = maxLevel;
    }

    public String getCustomName() {
        return customName;
    }

    public void setCustomName(String customName) {
        this.customName = customName;
    }

    public Double getExplosionPower() {
        return explosionPower;
    }

    public void setExplosionPower(Double explosionPower) {
        this.explosionPower = explosionPower;
    }

    public Map<EquipmentSlot, Equipment> getEquipment() {
        return equipment;
    }

    public void addEquipment(EquipmentSlot slot, Equipment equipment) {
        this.equipment.put(slot, equipment);
    }

    public List<Ability> getAbilities() {
        return abilities;
    }

    public void addAbility(Ability ability) {
        this.abilities.add(ability);
    }

    public List<PotionEffect> getAttackEffects() {
        return attackEffects;
    }

    public void addAttackEffect(PotionEffect effect) {
        this.attackEffects.add(effect);
    }

    public List<Drop> getDrops() {
        return drops;
    }

    public void addDrop(Drop drop) {
        this.drops.add(drop);
    }

    public List<RPGMobPower> getPowers() {
        return powers;
    }

    public void addPower(RPGMobPower power) {
        this.powers.add(power);
    }

    public Map<String, WorldMultiplier> getWorldMultipliers() {
        return worldMultipliers;
    }

    public void addWorldMultiplier(String world, WorldMultiplier multiplier) {
        this.worldMultipliers.put(world.toLowerCase(), multiplier);
    }

    public List<HeightMultiplier> getHeightMultipliers() {
        return heightMultipliers;
    }

    public void addHeightMultiplier(HeightMultiplier multiplier) {
        this.heightMultipliers.add(multiplier);
    }

    /**
     * Equipment slot enum
     */
    public enum EquipmentSlot {
        HELMET, CHESTPLATE, LEGGINGS, BOOTS, MAINHAND, OFFHAND
    }

    /**
     * Represents an equipment item
     */
    public static class Equipment {
        private final Material material;
        private final double chance;

        public Equipment(Material material, double chance) {
            this.material = material;
            this.chance = chance;
        }

        public Material getMaterial() {
            return material;
        }

        public double getChance() {
            return chance;
        }
    }

    /**
     * Represents an ability (potion effect)
     */
    public static class Ability {
        private final PotionEffectType type;
        private final int duration;
        private final int amplifier;
        private final double chance;

        public Ability(PotionEffectType type, int duration, int amplifier, double chance) {
            this.type = type;
            this.duration = duration;
            this.amplifier = amplifier;
            this.chance = chance;
        }

        public PotionEffectType getType() {
            return type;
        }

        public int getDuration() {
            return duration;
        }

        public int getAmplifier() {
            return amplifier;
        }

        public double getChance() {
            return chance;
        }
    }

    /**
     * Represents a custom drop
     */
    public static class Drop {
        private final Material material;
        private final int minAmount;
        private final int maxAmount;
        private final double chance;

        public Drop(Material material, int minAmount, int maxAmount, double chance) {
            this.material = material;
            this.minAmount = minAmount;
            this.maxAmount = maxAmount;
            this.chance = chance;
        }

        public Material getMaterial() {
            return material;
        }

        public int getMinAmount() {
            return minAmount;
        }

        public int getMaxAmount() {
            return maxAmount;
        }

        public double getChance() {
            return chance;
        }
    }

    /**
     * Represents world-specific multipliers
     */
    public static class WorldMultiplier {
        private final double healthMultiplier;
        private final double damageMultiplier;
        private final double speedMultiplier;

        public WorldMultiplier(double healthMultiplier, double damageMultiplier, double speedMultiplier) {
            this.healthMultiplier = healthMultiplier;
            this.damageMultiplier = damageMultiplier;
            this.speedMultiplier = speedMultiplier;
        }

        public double getHealthMultiplier() {
            return healthMultiplier;
        }

        public double getDamageMultiplier() {
            return damageMultiplier;
        }

        public double getSpeedMultiplier() {
            return speedMultiplier;
        }
    }

    /**
     * Represents height-specific multipliers
     */
    public static class HeightMultiplier {
        private final int minHeight;
        private final int maxHeight;
        private final double healthMultiplier;
        private final double damageMultiplier;
        private final double speedMultiplier;

        public HeightMultiplier(int minHeight, int maxHeight, double healthMultiplier, 
                               double damageMultiplier, double speedMultiplier) {
            this.minHeight = minHeight;
            this.maxHeight = maxHeight;
            this.healthMultiplier = healthMultiplier;
            this.damageMultiplier = damageMultiplier;
            this.speedMultiplier = speedMultiplier;
        }

        public int getMinHeight() {
            return minHeight;
        }

        public int getMaxHeight() {
            return maxHeight;
        }

        public double getHealthMultiplier() {
            return healthMultiplier;
        }

        public double getDamageMultiplier() {
            return damageMultiplier;
        }

        public double getSpeedMultiplier() {
            return speedMultiplier;
        }

        public boolean isInRange(int y) {
            return y >= minHeight && y <= maxHeight;
        }
    }
}
