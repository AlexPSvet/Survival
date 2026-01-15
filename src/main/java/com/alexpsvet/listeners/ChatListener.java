package com.alexpsvet.listeners;

import com.alexpsvet.Survival;
import com.alexpsvet.chat.ChatManager;
import com.alexpsvet.clan.Clan;
import com.alexpsvet.clan.ClanManager;
import com.alexpsvet.utils.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Listener for chat events
 */
public class ChatListener implements Listener {
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        ChatManager chatManager = ChatManager.getInstance();
        
        if (!player.hasPlayedBefore()) {
            // First join
            String message = chatManager.getMessage("welcome.first-join", "{player}", player.getName());
            event.setJoinMessage(MessageUtil.colorize(message));
        } else {
            String message = chatManager.getMessage("welcome.join", "{player}", player.getName());
            event.setJoinMessage(MessageUtil.colorize(message));
        }
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        ChatManager chatManager = ChatManager.getInstance();
        String message = chatManager.getMessage("welcome.quit", "{player}", event.getPlayer().getName());
        event.setQuitMessage(MessageUtil.colorize(message));
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        ChatManager chatManager = ChatManager.getInstance();
        
        // Check if chat is enabled for player
        if (!chatManager.isChatEnabled(player.getUniqueId())) {
            event.setCancelled(true);
            MessageUtil.sendError(player, chatManager.getMessage("chat.muted"));
            return;
        }
        
        // Check if player is in a clan
        ClanManager clanManager = Survival.getInstance().getClanManager();
        Clan clan = clanManager.getPlayerClan(player.getUniqueId());
        
        String format;
        if (clan != null) {
            format = chatManager.getMessage("chat.format-clan", 
                "{tag}", clan.getTag(),
                "{player}", player.getDisplayName(),
                "{message}", "%2$s");
        } else {
            format = chatManager.getMessage("chat.format",
                "{player}", player.getDisplayName(),
                "{message}", "%2$s");
        }
        
        event.setFormat(MessageUtil.colorize(format));
    }
}
