package com.alexpsvet.display;

import com.alexpsvet.Survival;
import com.alexpsvet.clan.Clan;
import com.alexpsvet.clan.ClanManager;
import com.alexpsvet.economy.EconomyManager;
import com.alexpsvet.player.PlayerStats;
import com.alexpsvet.player.PlayerStatsManager;
import com.alexpsvet.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.UUID;

/**
 * Manages player scoreboards
 */
public class ScoreboardManager {
    private static ScoreboardManager instance;
    
    public ScoreboardManager() {
        instance = this;
        // Start update task (update every second)
        Bukkit.getScheduler().runTaskTimer(Survival.getInstance(), this::updateAll, 20L, 20L);
    }
    
    /**
     * Create and set scoreboard for a player
     */
    public void createScoreboard(Player player) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective("sidebar", "dummy", 
            MessageUtil.colorize("&6&lPERIQUITO"));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        
        updateScoreboard(player, scoreboard, objective);
        player.setScoreboard(scoreboard);
    }
    
    /**
     * Update a player's scoreboard
     */
    public void updateScoreboard(Player player, Scoreboard scoreboard, Objective objective) {
        // Clear old scores
        for (String entry : scoreboard.getEntries()) {
            scoreboard.resetScores(entry);
        }
        
        int line = 15;
        
        // Header
        setScore(objective, MessageUtil.colorize("&7&m                    "), line--);
        
        // Player info
        PlayerStats stats = PlayerStatsManager.getInstance().getStats(player.getUniqueId());
        if (stats != null) {
            String rankDisplay = stats.getRank().equals("USER") ? "&7Joueur" : "&6" + stats.getRank();
            setScore(objective, MessageUtil.colorize("&eRang: " + rankDisplay), line--);
        }
        
        setScore(objective, MessageUtil.colorize("&r"), line--);
        
        // Economy
        EconomyManager eco = Survival.getInstance().getEconomyManager();
        double balance = eco.getBalance(player.getUniqueId());
        String currency = Survival.getInstance().getConfig().getString("economy.currency-symbol", "⛁");
        setScore(objective, MessageUtil.colorize("&6Argent: &e" + String.format("%.0f", balance) + " " + currency), line--);
        
        setScore(objective, MessageUtil.colorize(" &r"), line--);
        
        // Stats
        if (stats != null) {
            setScore(objective, MessageUtil.colorize("&aKills: &f" + stats.getKills()), line--);
            setScore(objective, MessageUtil.colorize("&cMorts: &f" + stats.getDeaths()), line--);
            setScore(objective, MessageUtil.colorize("&bK/D: &f" + String.format("%.2f", stats.getKDRatio())), line--);
        }
        
        setScore(objective, MessageUtil.colorize("  &r"), line--);
        
        // Clan
        Clan clan = ClanManager.getInstance().getPlayerClan(player.getUniqueId());
        if (clan != null) {
            setScore(objective, MessageUtil.colorize("&dClan: &f" + clan.getName()), line--);
            setScore(objective, MessageUtil.colorize("&7Membres: &f" + clan.getMemberCount()), line--);
        } else {
            setScore(objective, MessageUtil.colorize("&7Pas de clan"), line--);
        }
        
        setScore(objective, MessageUtil.colorize("   &r"), line--);
        
        // Online players
        setScore(objective, MessageUtil.colorize("&eEn ligne: &f" + Bukkit.getOnlinePlayers().size()), line--);
        
        // Footer
        setScore(objective, MessageUtil.colorize("&7&m                    "), line--);
        setScore(objective, MessageUtil.colorize("&6play.periquito.net"), line--);
    }
    
    /**
     * Set a score for an objective
     */
    private void setScore(Objective objective, String text, int score) {
        Score s = objective.getScore(text);
        s.setScore(score);
    }
    
    /**
     * Update all online players' scoreboards
     */
    public void updateAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Scoreboard scoreboard = player.getScoreboard();
            if (scoreboard == null || scoreboard == Bukkit.getScoreboardManager().getMainScoreboard()) {
                createScoreboard(player);
            } else {
                Objective objective = scoreboard.getObjective("sidebar");
                if (objective != null) {
                    updateScoreboard(player, scoreboard, objective);
                }
            }
        }
    }
    
    /**
     * Remove scoreboard from player
     */
    public void removeScoreboard(Player player) {
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }
    
    public static ScoreboardManager getInstance() {
        return instance;
    }
}
