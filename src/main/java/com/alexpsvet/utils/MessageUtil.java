package com.alexpsvet.utils;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class for message formatting and coloring
 */
public class MessageUtil {
    
    /**
     * Translate color codes in a string
     * @param message The message with & color codes
     * @return The message with Minecraft color codes
     */
    public static String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
    
    /**
     * Translate color codes in a list of strings
     * @param messages The list of messages with & color codes
     * @return The list of messages with Minecraft color codes
     */
    public static List<String> colorize(List<String> messages) {
        return messages.stream()
                .map(MessageUtil::colorize)
                .collect(Collectors.toList());
    }
    
    /**
     * Send a colored message to a player
     * @param player The player
     * @param message The message with & color codes
     */
    public static void sendMessage(Player player, String message) {
        player.sendMessage(colorize(message));
    }
    
    /**
     * Send a colored message to a command sender
     * @param sender The command sender
     * @param message The message with & color codes
     */
    public static void sendMessage(CommandSender sender, String message) {
        sender.sendMessage(colorize(message));
    }
    
    /**
     * Send multiple colored messages to a player
     * @param player The player
     * @param messages The messages with & color codes
     */
    public static void sendMessages(Player player, List<String> messages) {
        messages.forEach(msg -> sendMessage(player, msg));
    }
    
    /**
     * Send multiple colored messages to a command sender
     * @param sender The command sender
     * @param messages The messages with & color codes
     */
    public static void sendMessages(CommandSender sender, List<String> messages) {
        messages.forEach(msg -> sendMessage(sender, msg));
    }
    
    /**
     * Format a message with placeholders
     * @param message The message template
     * @param placeholders Pairs of placeholder and value (placeholder1, value1, placeholder2, value2, ...)
     * @return The formatted message
     */
    public static String format(String message, String... placeholders) {
        String result = message;
        for (int i = 0; i < placeholders.length - 1; i += 2) {
            result = result.replace(placeholders[i], placeholders[i + 1]);
        }
        return result;
    }
    
    /**
     * Send a success message (green)
     * @param sender The command sender
     * @param message The message
     */
    public static void sendSuccess(CommandSender sender, String message) {
        sendMessage(sender, "&a" + message);
    }
    
    /**
     * Send an error message (red)
     * @param sender The command sender
     * @param message The message
     */
    public static void sendError(CommandSender sender, String message) {
        sendMessage(sender, "&c" + message);
    }
    
    /**
     * Send a warning message (yellow)
     * @param sender The command sender
     * @param message The message
     */
    public static void sendWarning(CommandSender sender, String message) {
        sendMessage(sender, "&e" + message);
    }
    
    /**
     * Send an info message (blue)
     * @param sender The command sender
     * @param message The message
     */
    public static void sendInfo(CommandSender sender, String message) {
        sendMessage(sender, "&b" + message);
    }
    
    /**
     * Create a centered message
     * @param message The message
     * @return The centered message
     */
    public static String center(String message) {
        int maxWidth = 80;
        int spaces = (maxWidth - ChatColor.stripColor(colorize(message)).length()) / 2;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < spaces; i++) {
            sb.append(" ");
        }
        sb.append(message);
        return sb.toString();
    }
    
    /**
     * Strip all color codes from a message
     * @param message The message
     * @return The message without color codes
     */
    public static String stripColor(String message) {
        return ChatColor.stripColor(message);
    }
}
