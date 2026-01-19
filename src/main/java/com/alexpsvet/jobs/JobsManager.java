package com.alexpsvet.jobs;

import com.alexpsvet.Survival;
import com.alexpsvet.database.Database;
import com.alexpsvet.economy.EconomyManager;
import com.alexpsvet.economy.TransactionType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages jobs system
 */
public class JobsManager {
    private static final Logger LOGGER = Logger.getLogger("survival");
    private static JobsManager instance;
    private final Database database;
    private final Map<String, Job> jobs; // jobId -> Job
    private final Map<UUID, PlayerJob> playerJobs; // playerUuid -> PlayerJob
    private final Map<UUID, BossBar> activeBossBars; // playerUuid -> BossBar for level up notifications
    
    public JobsManager(Database database) {
        instance = this;
        this.database = database;
        this.jobs = new HashMap<>();
        this.playerJobs = new HashMap<>();
        this.activeBossBars = new HashMap<>();
        createTables();
        loadJobs();
        loadPlayerJobs();
    }
    
    public static JobsManager getInstance() {
        return instance;
    }
    
    /**
     * Create the jobs tables
     */
    private void createTables() {
        String query = "CREATE TABLE IF NOT EXISTS player_jobs (" +
                "player_uuid VARCHAR(36) PRIMARY KEY," +
                "job_id VARCHAR(50) NOT NULL," +
                "level INTEGER NOT NULL DEFAULT 1," +
                "experience DOUBLE NOT NULL DEFAULT 0," +
                "joined_at BIGINT NOT NULL" +
                ")";
        database.executeUpdate(query);
        
        LOGGER.info("Jobs tables created/verified");
    }
    
    /**
     * Load jobs from jobs.yml
     */
    private void loadJobs() {
        File jobsFile = new File(Survival.getInstance().getDataFolder(), "jobs.yml");
        if (!jobsFile.exists()) {
            Survival.getInstance().saveResource("jobs.yml", false);
        }
        
        FileConfiguration config = YamlConfiguration.loadConfiguration(jobsFile);
        ConfigurationSection jobsSection = config.getConfigurationSection("jobs");
        
        if (jobsSection == null) {
            LOGGER.warning("No jobs defined in jobs.yml");
            return;
        }
        
        for (String jobId : jobsSection.getKeys(false)) {
            ConfigurationSection jobSection = jobsSection.getConfigurationSection(jobId);
            if (jobSection == null) continue;
            
            String name = jobSection.getString("name", jobId);
            String description = jobSection.getString("description", "");
            Material icon = Material.valueOf(jobSection.getString("icon", "BOOK"));
            int maxLevel = jobSection.getInt("max-level", 100);
            
            Job job = new Job(jobId, name, description, icon, maxLevel);
            
            // Load rewards
            ConfigurationSection rewardsSection = jobSection.getConfigurationSection("rewards");
            if (rewardsSection != null) {
                for (String actionName : rewardsSection.getKeys(false)) {
                    try {
                        JobAction action = JobAction.valueOf(actionName.toUpperCase());
                        double reward = rewardsSection.getDouble(actionName);
                        job.addReward(action, reward);
                    } catch (IllegalArgumentException e) {
                        LOGGER.warning("Unknown job action: " + actionName);
                    }
                }
            }
            
            // Load level multipliers
            ConfigurationSection levelsSection = jobSection.getConfigurationSection("levels");
            if (levelsSection != null) {
                for (String levelStr : levelsSection.getKeys(false)) {
                    try {
                        int level = Integer.parseInt(levelStr);
                        ConfigurationSection levelSection = levelsSection.getConfigurationSection(levelStr);
                        if (levelSection != null) {
                            double multiplier = levelSection.getDouble("multiplier", 1.0);
                            double expRequired = levelSection.getDouble("experience", 1000.0);
                            job.setLevelMultiplier(level, multiplier);
                            job.setExperienceRequired(level, expRequired);
                        }
                    } catch (NumberFormatException e) {
                        LOGGER.warning("Invalid level number: " + levelStr);
                    }
                }
            }
            
            jobs.put(jobId, job);
            LOGGER.info("Loaded job: " + name + " (ID: " + jobId + ")");
        }
        
        LOGGER.info("Loaded " + jobs.size() + " jobs");
    }
    
    /**
     * Load player jobs from database
     */
    private void loadPlayerJobs() {
        ResultSet rs = database.executeQuery("SELECT * FROM player_jobs");
        try {
            while (rs != null && rs.next()) {
                UUID playerUuid = UUID.fromString(rs.getString("player_uuid"));
                String jobId = rs.getString("job_id");
                int level = rs.getInt("level");
                double experience = rs.getDouble("experience");
                long joinedAt = rs.getLong("joined_at");
                
                PlayerJob playerJob = new PlayerJob(playerUuid, jobId, level, experience, joinedAt);
                playerJobs.put(playerUuid, playerJob);
            }
            if (rs != null) rs.close();
            LOGGER.info("Loaded " + playerJobs.size() + " player jobs");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to load player jobs", e);
        }
    }
    
    /**
     * Get all available jobs
     */
    public Map<String, Job> getJobs() {
        return new HashMap<>(jobs);
    }
    
    /**
     * Get a job by ID
     */
    public Job getJob(String jobId) {
        return jobs.get(jobId);
    }
    
    /**
     * Get a player's current job
     */
    public PlayerJob getPlayerJob(UUID playerUuid) {
        return playerJobs.get(playerUuid);
    }
    
    /**
     * Check if a player has a job
     */
    public boolean hasJob(UUID playerUuid) {
        return playerJobs.containsKey(playerUuid);
    }
    
    /**
     * Join a job
     */
    public boolean joinJob(UUID playerUuid, String jobId) {
        if (hasJob(playerUuid)) {
            return false; // Already has a job
        }
        
        if (!jobs.containsKey(jobId)) {
            return false; // Invalid job
        }
        
        PlayerJob playerJob = new PlayerJob(playerUuid, jobId, 1, 0, System.currentTimeMillis());
        playerJobs.put(playerUuid, playerJob);
        
        database.executeUpdate(
            "INSERT INTO player_jobs (player_uuid, job_id, level, experience, joined_at) VALUES (?, ?, ?, ?, ?)",
            playerUuid.toString(), jobId, 1, 0.0, System.currentTimeMillis()
        );
        
        return true;
    }
    
    /**
     * Leave current job
     */
    public boolean leaveJob(UUID playerUuid) {
        if (!hasJob(playerUuid)) {
            return false;
        }
        
        playerJobs.remove(playerUuid);
        database.executeUpdate("DELETE FROM player_jobs WHERE player_uuid = ?", playerUuid.toString());
        
        return true;
    }
    
    /**
     * Grant experience and reward for a job action
     */
    public void performAction(Player player, JobAction action) {
        PlayerJob playerJob = getPlayerJob(player.getUniqueId());
        if (playerJob == null) {
            return; // No job
        }
        
        Job job = getJob(playerJob.getJobId());
        if (job == null) {
            return; // Invalid job
        }
        
        double reward = job.calculateReward(action, playerJob.getLevel());
        if (reward == 0.0) {
            return; // No reward for this action in this job
        }
        
        // Give money
        EconomyManager eco = Survival.getInstance().getEconomyManager();
        eco.addBalance(player.getUniqueId(), reward);
        
        // Give experience (same as reward for simplicity)
        double expGained = reward;
        playerJob.addExperience(expGained);
        
        // Check for level up
        checkLevelUp(player, playerJob, job);
        
        // Save to database
        savePlayerJob(playerJob);
    }
    
    /**
     * Check if player should level up
     */
    private void checkLevelUp(Player player, PlayerJob playerJob, Job job) {
        int currentLevel = playerJob.getLevel();
        if (currentLevel >= job.getMaxLevel()) {
            return; // Max level
        }
        
        int nextLevel = currentLevel + 1;
        double requiredExp = job.getExperienceRequired(nextLevel);
        
        if (playerJob.getExperience() >= requiredExp) {
            // Level up!
            playerJob.setLevel(nextLevel);
            playerJob.setExperience(playerJob.getExperience() - requiredExp);
            
            // Show boss bar notification
            showLevelUpNotification(player, job, nextLevel);
            
            // Save
            savePlayerJob(playerJob);
            
            // Check for another level up (recursive)
            checkLevelUp(player, playerJob, job);
        }
    }
    
    /**
     * Show boss bar notification for level up
     */
    private void showLevelUpNotification(Player player, Job job, int newLevel) {
        // Remove existing boss bar if any
        BossBar existingBar = activeBossBars.remove(player.getUniqueId());
        if (existingBar != null) {
            existingBar.removePlayer(player);
        }
        
        // Create new boss bar
        String title = "§6§l✦ " + job.getName() + " Niveau " + newLevel + " §6§l✦";
        BossBar bossBar = Bukkit.createBossBar(title, BarColor.YELLOW, BarStyle.SOLID);
        bossBar.setProgress(1.0);
        bossBar.addPlayer(player);
        
        activeBossBars.put(player.getUniqueId(), bossBar);
        
        // Remove after 5 seconds
        Bukkit.getScheduler().runTaskLater(Survival.getInstance(), () -> {
            bossBar.removePlayer(player);
            activeBossBars.remove(player.getUniqueId());
        }, 100L); // 5 seconds (20 ticks per second)
    }
    
    /**
     * Save player job to database
     */
    private void savePlayerJob(PlayerJob playerJob) {
        database.executeUpdate(
            "UPDATE player_jobs SET level = ?, experience = ? WHERE player_uuid = ?",
            playerJob.getLevel(),
            playerJob.getExperience(),
            playerJob.getPlayerUuid().toString()
        );
    }
    
    /**
     * Get player's progress percentage to next level
     */
    public double getProgressToNextLevel(UUID playerUuid) {
        PlayerJob playerJob = getPlayerJob(playerUuid);
        if (playerJob == null) {
            return 0.0;
        }
        
        Job job = getJob(playerJob.getJobId());
        if (job == null) {
            return 0.0;
        }
        
        int currentLevel = playerJob.getLevel();
        if (currentLevel >= job.getMaxLevel()) {
            return 100.0; // Max level
        }
        
        double requiredExp = job.getExperienceRequired(currentLevel + 1);
        double currentExp = playerJob.getExperience();
        
        return (currentExp / requiredExp) * 100.0;
    }
}
