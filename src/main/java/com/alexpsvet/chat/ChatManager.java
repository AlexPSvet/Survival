package com.alexpsvet.chat;

import com.alexpsvet.Survival;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manager for chat messages and private messages
 */
public class ChatManager {
    private static final Logger LOGGER = Logger.getLogger("survival");
    private static ChatManager instance;
    
    private final Map<UUID, UUID> replyTargets; // Who each player last talked to
    private final Map<UUID, Boolean> toggledChat; // Players who toggled chat
    
    private FileConfiguration messagesConfig;
    private File messagesFile;
    
    public ChatManager() {
        instance = this;
        this.replyTargets = new HashMap<>();
        this.toggledChat = new HashMap<>();
        loadMessagesConfig();
    }
    
    /**
     * Load custom messages configuration
     */
    private void loadMessagesConfig() {
        messagesFile = new File(Survival.getInstance().getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            Survival.getInstance().saveResource("messages.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }
    
    /**
     * Get a custom message
     * @param path the path in messages.yml
     * @return the message
     */
    public String getMessage(String path) {
        return messagesConfig.getString(path, "&cMessage not found: " + path);
    }
    
    /**
     * Get a custom message with placeholders
     * @param path the path in messages.yml
     * @param placeholders pairs of placeholder and value
     * @return the formatted message
     */
    public String getMessage(String path, String... placeholders) {
        String message = getMessage(path);
        for (int i = 0; i < placeholders.length - 1; i += 2) {
            message = message.replace(placeholders[i], placeholders[i + 1]);
        }
        return message;
    }
    
    /**
     * Set reply target for a player
     * @param player the player UUID
     * @param target the target UUID
     */
    public void setReplyTarget(UUID player, UUID target) {
        replyTargets.put(player, target);
    }
    
    /**
     * Get reply target for a player
     * @param player the player UUID
     * @return the target UUID, or null
     */
    public UUID getReplyTarget(UUID player) {
        return replyTargets.get(player);
    }
    
    /**
     * Toggle chat for a player
     * @param player the player UUID
     * @return true if chat is now enabled
     */
    public boolean toggleChat(UUID player) {
        boolean enabled = !toggledChat.getOrDefault(player, false);
        toggledChat.put(player, enabled);
        return enabled;
    }
    
    /**
     * Check if player has chat enabled
     * @param player the player UUID
     * @return true if enabled
     */
    public boolean isChatEnabled(UUID player) {
        return !toggledChat.getOrDefault(player, false);
    }
    
    /**
     * Save messages configuration
     */
    public void saveMessages() {
        try {
            messagesConfig.save(messagesFile);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to save messages.yml", e);
        }
    }
    
    public static ChatManager getInstance() {
        return instance;
    }
}
