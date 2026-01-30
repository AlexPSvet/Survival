package com.alexpsvet.commands;

import com.alexpsvet.trade.TradeManager;
import com.alexpsvet.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Command for player-to-player trading
 */
public class TradeCommand implements CommandExecutor, TabCompleter {
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Cette commande ne peut être exécutée que par un joueur!");
            return true;
        }
        
        Player player = (Player) sender;
        
        // /trade <player> - send trade request
        if (args.length == 0) {
            sender.sendMessage(MessageUtil.colorize("&cUsage: /trade <joueur>"));
            sender.sendMessage(MessageUtil.colorize("&7/trade accept <joueur> - Accepter une demande"));
            sender.sendMessage(MessageUtil.colorize("&7/trade deny <joueur> - Refuser une demande"));
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        // /trade accept <player>
        if (subCommand.equals("accept") || subCommand.equals("accepter")) {
            if (args.length < 2) {
                MessageUtil.sendError(player, "Usage: /trade accept <joueur>");
                return true;
            }
            
            Player requester = Bukkit.getPlayer(args[1]);
            if (requester == null) {
                MessageUtil.sendError(player, "Le joueur " + args[1] + " n'est pas en ligne!");
                return true;
            }
            
            TradeManager.getInstance().acceptTradeRequest(player, requester);
            return true;
        }
        
        // /trade deny <player>
        if (subCommand.equals("deny") || subCommand.equals("refuse") || subCommand.equals("refuser")) {
            if (args.length < 2) {
                MessageUtil.sendError(player, "Usage: /trade deny <joueur>");
                return true;
            }
            
            Player requester = Bukkit.getPlayer(args[1]);
            if (requester == null) {
                MessageUtil.sendError(player, "Le joueur " + args[1] + " n'est pas en ligne!");
                return true;
            }
            
            TradeManager.getInstance().denyTradeRequest(player, requester);
            return true;
        }
        
        // /trade <player> - send request
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            MessageUtil.sendError(player, "Le joueur " + args[0] + " n'est pas en ligne!");
            return true;
        }
        
        if (target.equals(player)) {
            MessageUtil.sendError(player, "Vous ne pouvez pas trader avec vous-même!");
            return true;
        }
        
        TradeManager.getInstance().sendTradeRequest(player, target);
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // First argument - subcommand or player name
            completions.add("accept");
            completions.add("deny");
            
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!player.equals(sender)) {
                    completions.add(player.getName());
                }
            }
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("accept") || args[0].equalsIgnoreCase("deny"))) {
            // Second argument for accept/deny - player name
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!player.equals(sender)) {
                    completions.add(player.getName());
                }
            }
        }
        
        return completions;
    }
}
