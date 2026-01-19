package com.alexpsvet.commands;

import com.alexpsvet.Survival;
import com.alexpsvet.territory.Territory;
import com.alexpsvet.territory.TerritoryManager;
import com.alexpsvet.territory.menu.ProtectionBlockMenu;
import com.alexpsvet.territory.menu.TerritoryConfigMenu;
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
 * Command to manage protection
 */
public class ProtectionCommand implements CommandExecutor, TabCompleter {
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Cette commande ne peut être exécutée que par un joueur!");
            return true;
        }
        
        Player player = (Player) sender;
        
        // No args or "shop" - open shop menu
        if (args.length == 0 || (args.length == 1 && args[0].equalsIgnoreCase("shop"))) {
            ProtectionBlockMenu.open(player);
            return true;
        }
        
        // "config" or "configure" - open config menu if in territory
        if (args[0].equalsIgnoreCase("config") || args[0].equalsIgnoreCase("configure")) {
            TerritoryManager territoryManager = Survival.getInstance().getTerritoryManager();
            Territory territory = territoryManager.getTerritoryAt(player.getLocation());
            
            if (territory == null) {
                MessageUtil.sendError(player, "Vous devez être dans une zone protégée pour utiliser cette commande!");
                return true;
            }
            
            if (!territory.hasPermission(player.getUniqueId())) {
                MessageUtil.sendError(player, "Vous n'avez pas la permission de configurer ce territoire!");
                return true;
            }
            
            TerritoryConfigMenu.open(player, territory);
            return true;
        }
        
        // "trust <player>" - add trusted player
        if (args[0].equalsIgnoreCase("trust")) {
            if (args.length < 2) {
                MessageUtil.sendError(player, "Usage: /protection trust <joueur>");
                return true;
            }
            
            TerritoryManager territoryManager = Survival.getInstance().getTerritoryManager();
            Territory territory = territoryManager.getTerritoryAt(player.getLocation());
            
            if (territory == null) {
                MessageUtil.sendError(player, "Vous devez être dans une zone protégée!");
                return true;
            }
            
            if (!territory.hasPermission(player.getUniqueId())) {
                MessageUtil.sendError(player, "Vous n'avez pas la permission de gérer ce territoire!");
                return true;
            }
            
            Player targetPlayer = Bukkit.getPlayer(args[1]);
            if (targetPlayer == null) {
                MessageUtil.sendError(player, "Le joueur " + args[1] + " n'est pas en ligne!");
                return true;
            }
            
            if (territory.isTrusted(targetPlayer.getUniqueId())) {
                MessageUtil.sendError(player, targetPlayer.getName() + " est déjà dans la liste de confiance!");
                return true;
            }
            
            territoryManager.addTrusted(territory.getId(), targetPlayer.getUniqueId());
            MessageUtil.sendSuccess(player, targetPlayer.getName() + " ajouté à la liste de confiance!");
            MessageUtil.sendMessage(targetPlayer, MessageUtil.colorize("&aVous avez été ajouté à la protection de &e" + player.getName()));
            return true;
        }
        
        // "untrust <player>" - remove trusted player
        if (args[0].equalsIgnoreCase("untrust") || args[0].equalsIgnoreCase("distrust")) {
            if (args.length < 2) {
                MessageUtil.sendError(player, "Usage: /protection untrust <joueur>");
                return true;
            }
            
            TerritoryManager territoryManager = Survival.getInstance().getTerritoryManager();
            Territory territory = territoryManager.getTerritoryAt(player.getLocation());
            
            if (territory == null) {
                MessageUtil.sendError(player, "Vous devez être dans une zone protégée!");
                return true;
            }
            
            if (!territory.hasPermission(player.getUniqueId())) {
                MessageUtil.sendError(player, "Vous n'avez pas la permission de gérer ce territoire!");
                return true;
            }
            
            Player targetPlayer = Bukkit.getPlayer(args[1]);
            if (targetPlayer == null) {
                MessageUtil.sendError(player, "Le joueur " + args[1] + " n'est pas en ligne!");
                return true;
            }
            
            if (!territory.isTrusted(targetPlayer.getUniqueId())) {
                MessageUtil.sendError(player, targetPlayer.getName() + " n'est pas dans la liste de confiance!");
                return true;
            }
            
            territoryManager.removeTrusted(territory.getId(), targetPlayer.getUniqueId());
            MessageUtil.sendSuccess(player, targetPlayer.getName() + " retiré de la liste de confiance!");
            MessageUtil.sendMessage(targetPlayer, MessageUtil.colorize("&cVous avez été retiré de la protection de &e" + player.getName()));
            return true;
        }
        
        MessageUtil.sendError(player, "Usage: /protection [shop|config|trust|untrust] [joueur]");
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.addAll(Arrays.asList("shop", "config", "configure", "trust", "untrust"));
            return completions;
        }
        
        if (args.length == 2 && (args[0].equalsIgnoreCase("trust") || args[0].equalsIgnoreCase("untrust"))) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                completions.add(online.getName());
            }
        }
        
        return completions;
    }
}
