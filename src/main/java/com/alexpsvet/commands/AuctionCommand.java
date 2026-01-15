package com.alexpsvet.commands;

import com.alexpsvet.Survival;
import com.alexpsvet.auction.AuctionManager;
import com.alexpsvet.auction.menu.AuctionMenu;
import com.alexpsvet.commands.base.BaseCommand;
import com.alexpsvet.commands.base.SubCommand;
import com.alexpsvet.utils.MessageUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Auction House command
 */
public class AuctionCommand extends BaseCommand {
    
    public AuctionCommand() {
        // Register sub-commands
        registerSubCommand(new SellSubCommand());
        registerSubCommand(new CancelSubCommand());
    }
    
    @Override
    public String getName() {
        return "Auction House";
    }
    
    @Override
    protected void executeDefault(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            MessageUtil.sendError(sender, "Cette commande ne peut être exécutée que par un joueur!");
            return;
        }
        
        AuctionMenu.open((Player) sender);
    }
    
    /**
     * Sell sub-command
     */
    private static class SellSubCommand extends SubCommand {
        @Override
        public String getName() {
            return "sell";
        }
        
        @Override
        public String getDescription() {
            return "Vendre un article";
        }
        
        @Override
        public String getSyntax() {
            return "/ah sell <prix> [durée_heures]";
        }
        
        @Override
        public void execute(CommandSender sender, String[] args) {
            if (!(sender instanceof Player)) {
                MessageUtil.sendError(sender, "Cette commande ne peut être exécutée que par un joueur!");
                return;
            }
            
            Player player = (Player) sender;
            
            if (args.length < 1) {
                MessageUtil.sendError(player, "Usage: " + getSyntax());
                return;
            }
            
            double price;
            try {
                price = Double.parseDouble(args[0]);
            } catch (NumberFormatException e) {
                MessageUtil.sendError(player, "Prix invalide!");
                return;
            }
            
            if (price <= 0) {
                MessageUtil.sendError(player, "Le prix doit être positif!");
                return;
            }
            
            long duration = 48; // Default 48 hours
            if (args.length >= 2) {
                try {
                    duration = Long.parseLong(args[1]);
                } catch (NumberFormatException e) {
                    MessageUtil.sendError(player, "Durée invalide!");
                    return;
                }
            }
            
            ItemStack item = player.getInventory().getItemInMainHand();
            if (item == null || item.getType().isAir()) {
                MessageUtil.sendError(player, "Vous devez tenir un article en main!");
                return;
            }
            
            AuctionManager auctionManager = Survival.getInstance().getAuctionManager();
            if (auctionManager.createListing(player, item, price, duration)) {
                player.getInventory().setItemInMainHand(null);
            } else {
                MessageUtil.sendError(player, "Erreur lors de la création de la vente!");
            }
        }
        
        @Override
        public List<String> getTabCompletions(CommandSender sender, String[] args) {
            List<String> completions = new ArrayList<>();
            if (args.length == 1) {
                completions.add("100");
                completions.add("500");
                completions.add("1000");
            } else if (args.length == 2) {
                completions.add("24");
                completions.add("48");
                completions.add("72");
            }
            return completions;
        }
    }
    
    /**
     * Cancel sub-command
     */
    private static class CancelSubCommand extends SubCommand {
        @Override
        public String getName() {
            return "cancel";
        }
        
        @Override
        public String getDescription() {
            return "Annuler une vente";
        }
        
        @Override
        public String getSyntax() {
            return "/ah cancel";
        }
        
        @Override
        public void execute(CommandSender sender, String[] args) {
            if (!(sender instanceof Player)) {
                MessageUtil.sendError(sender, "Cette commande ne peut être exécutée que par un joueur!");
                return;
            }
            
            AuctionMenu.openMyListings((Player) sender);
        }
        
        @Override
        public List<String> getTabCompletions(CommandSender sender, String[] args) {
            return new ArrayList<>();
        }
    }
}
