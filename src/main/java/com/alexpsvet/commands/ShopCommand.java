package com.alexpsvet.commands;

import com.alexpsvet.shop.menu.ShopMenu;
import com.alexpsvet.utils.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Shop command
 */
public class ShopCommand implements CommandExecutor {
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            MessageUtil.sendError(sender, "Cette commande ne peut être exécutée que par un joueur!");
            return true;
        }
        
        Player player = (Player) sender;
        ShopMenu.openMainMenu(player);
        return true;
    }
}
