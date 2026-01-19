package com.alexpsvet.commands;

import com.alexpsvet.commands.base.SubCommand;
import com.alexpsvet.chat.ChatManager;
import com.alexpsvet.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Private message sub-command
 */
public class MessageSubCommand extends SubCommand {
    
    @Override
    public String getName() {
        return "msg";
    }
    
    @Override
    public String getDescription() {
        return "Envoyer un message privé";
    }
    
    @Override
    public String getSyntax() {
        return "/msg <joueur> <message>";
    }
    
    @Override
    public List<String> getAliases() {
        List<String> aliases = new ArrayList<>();
        aliases.add("tell");
        aliases.add("whisper");
        aliases.add("w");
        return aliases;
    }
    
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            MessageUtil.sendError(sender, "Cette commande ne peut être exécutée que par un joueur!");
            return;
        }
        
        if (args.length < 2) {
            MessageUtil.sendError(sender, "Usage: " + getSyntax());
            return;
        }
        
        Player player = (Player) sender;
        Player target = Bukkit.getPlayer(args[0]);
        
        if (target == null) {
            ChatManager chatManager = ChatManager.getInstance();
            MessageUtil.sendMessage(player, chatManager.getMessage("private-message.offline"));
            return;
        }
        
        if (target.equals(player)) {
            ChatManager chatManager = ChatManager.getInstance();
            MessageUtil.sendMessage(player, chatManager.getMessage("private-message.self"));
            return;
        }
        
        // Build message
        StringBuilder message = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            message.append(args[i]).append(" ");
        }
        String finalMessage = message.toString().trim();
        
        // Send messages
        ChatManager chatManager = ChatManager.getInstance();
        String senderFormat = chatManager.getMessage("private-message.format-sender",
            "{to}", target.getName(),
            "{message}", finalMessage);
        String receiverFormat = chatManager.getMessage("private-message.format-receiver",
            "{from}", player.getName(),
            "{message}", finalMessage);
        
        MessageUtil.sendMessage(player, senderFormat);
        MessageUtil.sendMessage(target, receiverFormat);
        
        // Set reply targets
        chatManager.setReplyTarget(player.getUniqueId(), target.getUniqueId());
        chatManager.setReplyTarget(target.getUniqueId(), player.getUniqueId());
    }
    
    @Override
    public List<String> getTabCompletions(CommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> players = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                players.add(player.getName());
            }
            return players;
        }
        return new ArrayList<>();
    }
}
