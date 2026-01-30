package com.alexpsvet.jobs.menu;

import com.alexpsvet.Survival;
import com.alexpsvet.jobs.Job;
import com.alexpsvet.jobs.JobAction;
import com.alexpsvet.jobs.JobsManager;
import com.alexpsvet.jobs.PlayerJob;
import com.alexpsvet.utils.MessageUtil;
import com.alexpsvet.utils.menu.Button;
import com.alexpsvet.utils.menu.Menu;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Jobs menu system
 */
public class JobsMenu {
    
    /**
     * Open main jobs menu
     */
    public static void openMainMenu(Player player) {
        PlayerJob playerJob = JobsManager.getInstance().getPlayerJob(player.getUniqueId());
        
        if (playerJob == null) {
            // No job, show job selection
            openJobSelectionMenu(player);
        } else {
            // Has job, show job info
            openJobInfoMenu(player);
        }
    }
    
    /**
     * Open job selection menu
     */
    public static void openJobSelectionMenu(Player player) {
        Menu.Builder builder = new Menu.Builder()
            .title(MessageUtil.colorize("&6&lChoisir un Métier"))
            .rows(6);
        
        Map<String, Job> jobs = JobsManager.getInstance().getJobs();
        int slot = 10;
        
        for (Job job : jobs.values()) {
            if (slot == 17) slot = 19; // Skip to next row
            if (slot == 26) slot = 28; // Skip to next row
            if (slot >= 35) break;
            
            ItemStack icon = new ItemStack(job.getIcon());
            List<String> lore = new ArrayList<>();
            lore.add(MessageUtil.colorize("&7" + job.getDescription()));
            lore.add("");
            lore.add(MessageUtil.colorize("&e&lRécompenses:"));
            
            // Show top 5 rewards
            int count = 0;
            for (Map.Entry<JobAction, Double> entry : job.getBaseRewards().entrySet()) {
                if (count >= 5) break;
                lore.add(MessageUtil.colorize("  &7" + entry.getKey().getDisplayName() + ": &a+" + entry.getValue() + " ⛁"));
                count++;
            }
            
            if (job.getBaseRewards().size() > 5) {
                lore.add(MessageUtil.colorize("  &7... et " + (job.getBaseRewards().size() - 5) + " autres"));
            }
            
            lore.add("");
            lore.add(MessageUtil.colorize("&7Niveau maximum: &e" + job.getMaxLevel()));
            lore.add("");
            lore.add(MessageUtil.colorize("&eCliquez pour rejoindre!"));
            
            builder.button(new Button.Builder()
                .slot(slot++)
                .item(icon)
                .name(MessageUtil.colorize("&6&l" + job.getName()))
                .lore(lore)
                .onClick((p, clickType) -> {
                    if (JobsManager.getInstance().joinJob(p.getUniqueId(), job.getId())) {
                        p.sendMessage(MessageUtil.colorize("&aVous avez rejoint le métier de &6" + job.getName() + "&a!"));
                        p.closeInventory();
                        openJobInfoMenu(p);
                    } else {
                        p.sendMessage(MessageUtil.colorize("&cVous avez déjà un métier!"));
                    }
                })
                .build());
        }
        
        // Close button
        ItemStack close = new ItemStack(Material.BARRIER);
        builder.button(new Button.Builder()
            .slot(49)
            .item(close)
            .name(MessageUtil.colorize("&cFermer"))
            .onClick((p, clickType) -> p.closeInventory())
            .build());
        
        builder.build().open(player);
    }
    
    /**
     * Open job info menu for player's current job
     */
    public static void openJobInfoMenu(Player player) {
        PlayerJob playerJob = JobsManager.getInstance().getPlayerJob(player.getUniqueId());
        if (playerJob == null) {
            openJobSelectionMenu(player);
            return;
        }
        
        Job job = JobsManager.getInstance().getJob(playerJob.getJobId());
        if (job == null) {
            player.sendMessage(MessageUtil.colorize("&cErreur: métier invalide!"));
            return;
        }
        
        Menu.Builder builder = new Menu.Builder()
            .title(MessageUtil.colorize("&6&l" + job.getName()))
            .rows(6);
        
        // Job info item
        ItemStack jobIcon = new ItemStack(job.getIcon());
        List<String> jobLore = new ArrayList<>();
        jobLore.add(MessageUtil.colorize("&7" + job.getDescription()));
        jobLore.add("");
        jobLore.add(MessageUtil.colorize("&e&lVotre Niveau: &6" + playerJob.getLevel() + " &7/ &e" + job.getMaxLevel()));
        
        double progress = JobsManager.getInstance().getProgressToNextLevel(player.getUniqueId());
        if (playerJob.getLevel() < job.getMaxLevel()) {
            int nextLevel = playerJob.getLevel() + 1;
            double requiredExp = job.getExperienceRequired(nextLevel);
            double currentExp = playerJob.getExperience();
            
            jobLore.add(MessageUtil.colorize("&e&lExpérience: &a" + String.format("%.1f", currentExp) + " &7/ &e" + String.format("%.0f", requiredExp)));
            jobLore.add(MessageUtil.colorize("&e&lProgrès: &a" + String.format("%.1f", progress) + "%"));
            
            // Progress bar
            int progressBars = (int) (progress / 5); // 20 bars total
            StringBuilder progressBar = new StringBuilder("&7[");
            for (int i = 0; i < 20; i++) {
                if (i < progressBars) {
                    progressBar.append("&a█");
                } else {
                    progressBar.append("&8█");
                }
            }
            progressBar.append("&7]");
            jobLore.add(MessageUtil.colorize(progressBar.toString()));
        } else {
            jobLore.add(MessageUtil.colorize("&a&l✓ NIVEAU MAXIMUM ATTEINT"));
        }
        
        jobLore.add("");
        double currentMultiplier = job.getLevelMultiplier(playerJob.getLevel());
        jobLore.add(MessageUtil.colorize("&7Multiplicateur actuel: &ax" + String.format("%.2f", currentMultiplier)));
        
        builder.button(new Button.Builder()
            .slot(13)
            .item(jobIcon)
            .name(MessageUtil.colorize("&6&l" + job.getName()))
            .lore(jobLore)
            .build());
        
        // Rewards list
        ItemStack rewardsIcon = new ItemStack(Material.GOLD_INGOT);
        List<String> rewardsLore = new ArrayList<>();
        rewardsLore.add(MessageUtil.colorize("&7Vos récompenses actuelles:"));
        rewardsLore.add("");
        
        for (Map.Entry<JobAction, Double> entry : job.getBaseRewards().entrySet()) {
            double reward = job.calculateReward(entry.getKey(), playerJob.getLevel());
            rewardsLore.add(MessageUtil.colorize("  &7" + entry.getKey().getDisplayName() + ": &a+" + String.format("%.2f", reward) + " ⛁"));
        }
        
        builder.button(new Button.Builder()
            .slot(29)
            .item(rewardsIcon)
            .name(MessageUtil.colorize("&e&lRécompenses"))
            .lore(rewardsLore)
            .build());
        
        // Level progression
        ItemStack levelIcon = new ItemStack(Material.EXPERIENCE_BOTTLE);
        List<String> levelLore = new ArrayList<>();
        levelLore.add(MessageUtil.colorize("&7Progression des niveaux:"));
        levelLore.add("");
        
        int currentLevel = playerJob.getLevel();
        for (int i = Math.max(1, currentLevel - 2); i <= Math.min(job.getMaxLevel(), currentLevel + 3); i++) {
            String prefix = i == currentLevel ? "&a➤ " : "&7  ";
            double multiplier = job.getLevelMultiplier(i);
            String multiplierStr = String.format("%.2f", multiplier);
            levelLore.add(MessageUtil.colorize(prefix + "Niveau " + i + ": &ex" + multiplierStr));
        }
        
        builder.button(new Button.Builder()
            .slot(31)
            .item(levelIcon)
            .name(MessageUtil.colorize("&6&lProgression"))
            .lore(levelLore)
            .build());
        
        // Statistics
        ItemStack statsIcon = new ItemStack(Material.BOOK);
        List<String> statsLore = new ArrayList<>();
        statsLore.add(MessageUtil.colorize("&7Statistiques:"));
        statsLore.add("");
        
        long daysSince = (System.currentTimeMillis() - playerJob.getJoinedAt()) / (1000 * 60 * 60 * 24);
        statsLore.add(MessageUtil.colorize("&7Membre depuis: &e" + daysSince + " jour(s)"));
        statsLore.add(MessageUtil.colorize("&7Niveau actuel: &e" + playerJob.getLevel()));
        statsLore.add(MessageUtil.colorize("&7Expérience totale: &e" + String.format("%.1f", playerJob.getExperience())));
        
        builder.button(new Button.Builder()
            .slot(33)
            .item(statsIcon)
            .name(MessageUtil.colorize("&b&lStatistiques"))
            .lore(statsLore)
            .build());
        
        // Leave job button
        ItemStack leaveIcon = new ItemStack(Material.BARRIER);
        List<String> leaveLore = new ArrayList<>();
        leaveLore.add(MessageUtil.colorize("&cQuitter ce métier"));
        leaveLore.add("");
        leaveLore.add(MessageUtil.colorize("&7Vous perdrez tous vos progrès!"));
        leaveLore.add(MessageUtil.colorize("&eCliquez pour quitter"));
        
        builder.button(new Button.Builder()
            .slot(49)
            .item(leaveIcon)
            .name(MessageUtil.colorize("&c&lQuitter le Métier"))
            .lore(leaveLore)
            .onClick((p, clickType) -> {
                if (JobsManager.getInstance().leaveJob(p.getUniqueId())) {
                    p.sendMessage(MessageUtil.colorize("&cVous avez quitté le métier de &6" + job.getName()));
                    p.closeInventory();
                } else {
                    p.sendMessage(MessageUtil.colorize("&cErreur lors de la désinscription!"));
                }
            })
            .build());
        
        // Back to job selection
        ItemStack backIcon = new ItemStack(Material.ARROW);
        builder.button(new Button.Builder()
            .slot(45)
            .item(backIcon)
            .name(MessageUtil.colorize("&7Changer de Métier"))
            .onClick((p, clickType) -> openJobSelectionMenu(p))
            .build());
        
        builder.build().open(player);
    }
}
