package com.alexpsvet.jobs;

import org.bukkit.Material;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a job that players can have
 */
public class Job {
    private final String id;
    private final String name;
    private final String description;
    private final Material icon;
    private final int maxLevel;
    private final Map<JobAction, Double> baseRewards; // Base rewards for each action
    private final Map<Integer, Double> levelMultipliers; // Level -> multiplier
    private final Map<Integer, Double> experienceRequired; // Level -> experience needed
    
    public Job(String id, String name, String description, Material icon, int maxLevel) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.maxLevel = maxLevel;
        this.baseRewards = new HashMap<>();
        this.levelMultipliers = new HashMap<>();
        this.experienceRequired = new HashMap<>();
    }
    
    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public Material getIcon() {
        return icon;
    }
    
    public int getMaxLevel() {
        return maxLevel;
    }
    
    public Map<JobAction, Double> getBaseRewards() {
        return baseRewards;
    }
    
    public void addReward(JobAction action, double amount) {
        baseRewards.put(action, amount);
    }
    
    public double getBaseReward(JobAction action) {
        return baseRewards.getOrDefault(action, 0.0);
    }
    
    public void setLevelMultiplier(int level, double multiplier) {
        levelMultipliers.put(level, multiplier);
    }
    
    public double getLevelMultiplier(int level) {
        // If we have an exact multiplier for this level, return it
        if (levelMultipliers.containsKey(level)) {
            return levelMultipliers.get(level);
        }
        
        // Otherwise, interpolate between the closest defined levels
        // Find the two surrounding levels
        int lowerLevel = 1;
        int upperLevel = maxLevel;
        double lowerMultiplier = 1.0;
        double upperMultiplier = 1.0;
        
        for (int definedLevel : levelMultipliers.keySet()) {
            if (definedLevel <= level && definedLevel > lowerLevel) {
                lowerLevel = definedLevel;
                lowerMultiplier = levelMultipliers.get(definedLevel);
            }
            if (definedLevel > level && definedLevel < upperLevel) {
                upperLevel = definedLevel;
                upperMultiplier = levelMultipliers.get(definedLevel);
            }
        }
        
        // If we're at or below the lowest defined level, return that multiplier
        if (level <= lowerLevel) {
            return lowerMultiplier;
        }
        
        // If we're at or above the highest defined level, return that multiplier
        if (level >= upperLevel) {
            return upperMultiplier;
        }
        
        // Linear interpolation between the two surrounding levels
        double ratio = (double) (level - lowerLevel) / (upperLevel - lowerLevel);
        return lowerMultiplier + ratio * (upperMultiplier - lowerMultiplier);
    }
    
    public void setExperienceRequired(int level, double experience) {
        experienceRequired.put(level, experience);
    }
    
    public double getExperienceRequired(int level) {
        return experienceRequired.getOrDefault(level, 1000.0);
    }
    
    /**
     * Calculate reward for an action at a specific level
     */
    public double calculateReward(JobAction action, int level) {
        double baseReward = getBaseReward(action);
        if (baseReward == 0.0) {
            return 0.0;
        }
        
        double multiplier = getLevelMultiplier(level);
        return baseReward * multiplier;
    }
}
