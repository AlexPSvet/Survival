package com.alexpsvet.commands;

import com.alexpsvet.chat.ChatManager;
import com.alexpsvet.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Private message and reply command
 */
public class MessageCommand implements CommandExecutor, TabCompleter {
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            MessageUtil.sendError(sender, "Cette commande ne peut être exécutée que par un joueur!");
            return true;
        }
        
        Player player = (Player) sender;
        
        // Handle /r (reply) command
        if (command.getName().equalsIgnoreCase("r")) {
            return handleReply(player, args);
        }
        
        // Handle /msg command
        return handleMessage(player, args);
    }
    
    private boolean handleMessage(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtil.sendError(player, "Usage: /msg <joueur> <message>");
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[0]);
        
        if (target == null) {
            ChatManager chatManager = ChatManager.getInstance();
            MessageUtil.sendMessage(player, chatManager.getMessage("private-message.offline"));
            return true;
        }
        
        if (target.equals(player)) {
            ChatManager chatManager = ChatManager.getInstance();
            MessageUtil.sendMessage(player, chatManager.getMessage("private-message.self"));
            return true;
        }
        
        // Build message
        StringBuilder message = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            message.append(args[i]).append(" ");
        }
        String finalMessage = message.toString().trim();
        
        // Send messages
        sendPrivateMessage(player, target, finalMessage);
        
        return true;
    }
    
    private boolean handleReply(Player player, String[] args) {
        if (args.length < 1) {
            MessageUtil.sendError(player, "Usage: /r <message>");
            return true;
        }
        
        ChatManager chatManager = ChatManager.getInstance();
        Player target = chatManager.getReplyTarget(player.getUniqueId());
        
        if (target == null) {
            MessageUtil.sendMessage(player, chatManager.getMessage("private-message.no-reply"));
            return true;
        }
        
        // Build message
        StringBuilder message = new StringBuilder();
        for (String arg : args) {
            message.append(arg).append(" ");
        }
        String finalMessage = message.toString().trim();
        
        // Send messages
        sendPrivateMessage(player, target, finalMessage);
        
        return true;
    }
    
    private void sendPrivateMessage(Player sender, Player receiver, String message) {
        ChatManager chatManager = ChatManager.getInstance();
        
        String senderFormat = chatManager.getMessage("private-message.format-sender",
            "{to}", receiver.getName(),
            "{message}", message);
        String receiverFormat = chatManager.getMessage("private-message.format-receiver",
            "{from}", sender.getName(),
            "{message}", message);
        
        MessageUtil.sendMessage(sender, senderFormat);
        MessageUtil.sendMessage(receiver, receiverFormat);
        
        // Set reply targets
        chatManager.setReplyTarget(sender.getUniqueId(), receiver.getUniqueId());
        chatManager.setReplyTarget(receiver.getUniqueId(), sender.getUniqueId());
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        // Only provide tab completion for /msg command (first argument is player name)
        if (command.getName().equalsIgnoreCase("msg") && args.length == 1) {
            List<String> players = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player != sender) {
                    players.add(player.getName());
                }
            }
            return players;
        }
        
        return new ArrayList<>();
    }
}
