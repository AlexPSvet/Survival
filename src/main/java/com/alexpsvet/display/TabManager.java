package com.alexpsvet.display;

import com.alexpsvet.Survival;
import com.alexpsvet.player.Group;
import com.alexpsvet.player.PlayerStatsManager;
import com.alexpsvet.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Manages player tab list display
 */
public class TabManager {
    private static TabManager instance;
    
    public TabManager() {
        instance = this;
        // Start update task (update every 5 seconds)
        Bukkit.getScheduler().runTaskTimer(Survival.getInstance(), this::updateAll, 100L, 100L);
    }
    
    /**
     * Update tab list for a player
     */
    public void updateTab(Player player) {
        // Set header and footer
        String header = MessageUtil.colorize(
            "\n&6&l✦ PERIQUITO &6&l✦\n" +
            "&7Bienvenue, &e" + player.getName() + "&7!\n"
        );
        
        int online = Bukkit.getOnlinePlayers().size();
        int max = Bukkit.getMaxPlayers();
        
        String footer = MessageUtil.colorize(
            "\n&7Joueurs en ligne: &e" + online + "/" + max + "\n" +
            "&7Discord: &bdiscord.gg/periquito\n" +
            "&7Site: &6play.periquito.net\n "
        );
        
        player.setPlayerListHeaderFooter(header, footer);
        
        // Update player's display name with group prefix
        Group group = PlayerStatsManager.getInstance().getGroup(player.getUniqueId());
        String displayName = MessageUtil.colorize(group.getPrefix() + player.getName());
        player.setPlayerListName(displayName);
    }
    
    /**
     * Update tab list for all online players
     */
    public void updateAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updateTab(player);
        }
    }
    
    public static TabManager getInstance() {
        return instance;
    }
}
