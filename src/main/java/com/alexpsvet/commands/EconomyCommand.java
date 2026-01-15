package com.alexpsvet.commands;

import com.alexpsvet.Survival;
import com.alexpsvet.economy.EconomyManager;
import com.alexpsvet.economy.TransactionType;
import com.alexpsvet.economy.menu.EconomyMenu;
import com.alexpsvet.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Economy commands: balance, pay, etc.
 */
public class EconomyCommand implements CommandExecutor {
    private final EconomyManager economyManager;
    private final String currencySymbol;
    
    public EconomyCommand() {
        this.economyManager = Survival.getInstance().getEconomyManager();
        this.currencySymbol = Survival.getInstance().getConfig().getString("economy.currency-symbol", "⛁");
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String cmd = command.getName().toLowerCase();
        
        switch (cmd) {
            case "balance":
            case "bal":
                return handleBalance(sender, args);
            case "pay":
                return handlePay(sender, args);
            case "economy":
            case "eco":
                return handleEconomyMenu(sender);
            case "ecoadmin":
                return handleEcoAdmin(sender, args);
            default:
                return false;
        }
    }
    
    /**
     * Handle balance command
     */
    private boolean handleBalance(CommandSender sender, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                MessageUtil.sendError(sender, "Cette commande ne peut être exécutée que par un joueur!");
                return true;
            }
            
            Player player = (Player) sender;
            double balance = economyManager.getBalance(player.getUniqueId());
            MessageUtil.sendMessage(sender, 
                MessageUtil.format("&aVotre solde: &e{amount} {currency}",
                    "{amount}", String.format("%.2f", balance),
                    "{currency}", currencySymbol));
            return true;
        }
        
        // Check other player's balance
        if (!sender.hasPermission("survival.economy.balance.others")) {
            MessageUtil.sendError(sender, "Vous n'avez pas la permission!");
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            MessageUtil.sendError(sender, "Joueur introuvable!");
            return true;
        }
        
        double balance = economyManager.getBalance(target.getUniqueId());
        MessageUtil.sendMessage(sender, 
            MessageUtil.format("&aSolde de &e{player}&a: &e{amount} {currency}",
                "{player}", target.getName(),
                "{amount}", String.format("%.2f", balance),
                "{currency}", currencySymbol));
        return true;
    }
    
    /**
     * Handle pay command
     */
    private boolean handlePay(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            MessageUtil.sendError(sender, "Cette commande ne peut être exécutée que par un joueur!");
            return true;
        }
        
        if (args.length < 2) {
            MessageUtil.sendError(sender, "Usage: /pay <joueur> <montant>");
            return true;
        }
        
        Player player = (Player) sender;
        Player target = Bukkit.getPlayer(args[0]);
        
        if (target == null) {
            MessageUtil.sendError(sender, "Joueur introuvable!");
            return true;
        }
        
        if (target.equals(player)) {
            MessageUtil.sendError(sender, "Vous ne pouvez pas vous payer vous-même!");
            return true;
        }
        
        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            MessageUtil.sendError(sender, "Montant invalide!");
            return true;
        }
        
        if (amount <= 0) {
            MessageUtil.sendError(sender, "Le montant doit être positif!");
            return true;
        }
        
        if (economyManager.transfer(player.getUniqueId(), target.getUniqueId(), amount)) {
            MessageUtil.sendMessage(sender, 
                MessageUtil.format("&aVous avez envoyé &e{amount} {currency} &aà &e{player}",
                    "{amount}", String.format("%.2f", amount),
                    "{currency}", currencySymbol,
                    "{player}", target.getName()));
            MessageUtil.sendMessage(target, 
                MessageUtil.format("&aVous avez reçu &e{amount} {currency} &ade &e{player}",
                    "{amount}", String.format("%.2f", amount),
                    "{currency}", currencySymbol,
                    "{player}", player.getName()));
        } else {
            MessageUtil.sendMessage(sender, 
                MessageUtil.format("&cFonds insuffisants! Vous avez besoin de &e{amount} {currency}",
                    "{amount}", String.format("%.2f", amount),
                    "{currency}", currencySymbol));
        }
        
        return true;
    }
    
    /**
     * Handle economy menu command
     */
    private boolean handleEconomyMenu(CommandSender sender) {
        if (!(sender instanceof Player)) {
            MessageUtil.sendError(sender, "Cette commande ne peut être exécutée que par un joueur!");
            return true;
        }
        
        EconomyMenu.open((Player) sender);
        return true;
    }
    
    /**
     * Handle economy admin commands
     */
    private boolean handleEcoAdmin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("survival.economy.admin")) {
            MessageUtil.sendError(sender, "Vous n'avez pas la permission!");
            return true;
        }
        
        if (args.length < 3) {
            MessageUtil.sendError(sender, "Usage: /ecoadmin <give|take|set> <joueur> <montant>");
            return true;
        }
        
        String action = args[0].toLowerCase();
        Player target = Bukkit.getPlayer(args[1]);
        
        if (target == null) {
            MessageUtil.sendError(sender, "Joueur introuvable!");
            return true;
        }
        
        double amount;
        try {
            amount = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            MessageUtil.sendError(sender, "Montant invalide!");
            return true;
        }
        
        switch (action) {
            case "give":
                economyManager.addBalance(target.getUniqueId(), amount);
                economyManager.addTransaction(target.getUniqueId(), TransactionType.ADMIN_ADD, amount, "Admin give by " + sender.getName());
                MessageUtil.sendSuccess(sender, "Ajouté " + amount + " " + currencySymbol + " à " + target.getName());
                MessageUtil.sendSuccess(target, "Vous avez reçu " + amount + " " + currencySymbol);
                break;
            case "take":
                if (economyManager.removeBalance(target.getUniqueId(), amount)) {
                    economyManager.addTransaction(target.getUniqueId(), TransactionType.ADMIN_REMOVE, -amount, "Admin take by " + sender.getName());
                    MessageUtil.sendSuccess(sender, "Retiré " + amount + " " + currencySymbol + " de " + target.getName());
                    MessageUtil.sendWarning(target, amount + " " + currencySymbol + " ont été retirés de votre compte");
                } else {
                    MessageUtil.sendError(sender, "Le joueur n'a pas assez d'argent!");
                }
                break;
            case "set":
                economyManager.setBalance(target.getUniqueId(), target.getName(), amount);
                MessageUtil.sendSuccess(sender, "Solde de " + target.getName() + " défini à " + amount + " " + currencySymbol);
                break;
            default:
                MessageUtil.sendError(sender, "Action invalide! Utilisez: give, take ou set");
                break;
        }
        
        return true;
    }
}
