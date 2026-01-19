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
        return levelMultipliers.getOrDefault(level, 1.0);
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
