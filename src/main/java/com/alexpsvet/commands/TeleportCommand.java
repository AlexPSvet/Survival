package com.alexpsvet.commands;

import com.alexpsvet.Survival;
import com.alexpsvet.teleport.TeleportManager;
import com.alexpsvet.teleport.TeleportRequest;
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
 * Teleport commands: /tpa, /tpaccept, /tpdeny
 */
public class TeleportCommand implements CommandExecutor, TabCompleter {
    private final TeleportManager teleportManager;
    
    public TeleportCommand() {
        this.teleportManager = Survival.getInstance().getTeleportManager();
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            MessageUtil.sendError(sender, "Cette commande ne peut être exécutée que par un joueur!");
            return true;
        }
        
        Player player = (Player) sender;
        String cmd = command.getName().toLowerCase();
        
        switch (cmd) {
            case "tpa":
                return handleTpa(player, args);
            case "tpaccept":
                return handleTpaccept(player);
            case "tpdeny":
                return handleTpdeny(player);
            default:
                return false;
        }
    }
    
    private boolean handleTpa(Player player, String[] args) {
        if (args.length < 1) {
            MessageUtil.sendError(player, "Usage: /tpa <joueur>");
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            MessageUtil.sendError(player, "Joueur introuvable!");
            return true;
        }
        
        if (target.equals(player)) {
            MessageUtil.sendError(player, "Vous ne pouvez pas vous téléporter à vous-même!");
            return true;
        }
        
        if (teleportManager.isOnCooldown(player.getUniqueId())) {
            long remaining = teleportManager.getCooldownRemaining(player.getUniqueId());
            MessageUtil.sendError(player, "Vous devez attendre encore " + remaining + " seconde(s)!");
            return true;
        }
        
        if (teleportManager.sendRequest(player, target)) {
            MessageUtil.sendSuccess(player, "Demande de téléportation envoyée à " + target.getName());
            MessageUtil.sendMessage(target, 
                MessageUtil.colorize("&e" + player.getName() + " &7demande à se téléporter à vous!"));
            MessageUtil.sendMessage(target, 
                MessageUtil.colorize("&aCliquez ou tapez &e/tpaccept &apour accepter, &c/tpdeny &apour refuser"));
        } else {
            MessageUtil.sendError(player, "Impossible d'envoyer la demande. Le joueur a déjà une demande en attente.");
        }
        
        return true;
    }
    
    private boolean handleTpaccept(Player player) {
        TeleportRequest request = teleportManager.getRequest(player.getUniqueId());
        if (request == null) {
            MessageUtil.sendError(player, "Vous n'avez aucune demande de téléportation en attente!");
            return true;
        }
        
        Player requester = Bukkit.getPlayer(request.getRequester());
        if (requester == null) {
            MessageUtil.sendError(player, "Le joueur n'est plus en ligne!");
            teleportManager.denyRequest(player);
            return true;
        }
        
        if (teleportManager.acceptRequest(player)) {
            MessageUtil.sendSuccess(player, "Demande de téléportation acceptée!");
            MessageUtil.sendSuccess(requester, player.getName() + " a accepté votre demande!");
        } else {
            MessageUtil.sendError(player, "La demande a expiré ou n'est plus valide!");
        }
        
        return true;
    }
    
    private boolean handleTpdeny(Player player) {
        TeleportRequest request = teleportManager.getRequest(player.getUniqueId());
        if (request == null) {
            MessageUtil.sendError(player, "Vous n'avez aucune demande de téléportation en attente!");
            return true;
        }
        
        Player requester = Bukkit.getPlayer(request.getRequester());
        
        if (teleportManager.denyRequest(player)) {
            MessageUtil.sendSuccess(player, "Demande de téléportation refusée!");
            if (requester != null) {
                MessageUtil.sendError(requester, player.getName() + " a refusé votre demande de téléportation.");
            }
        }
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (command.getName().equalsIgnoreCase("tpa") && args.length == 1) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!player.equals(sender)) {
                    completions.add(player.getName());
                }
            }
        }
        
        return completions;
    }
}
