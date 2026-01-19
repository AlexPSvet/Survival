package com.alexpsvet.commands;

import com.alexpsvet.bounty.menu.BountyMenu;
import com.alexpsvet.commands.base.BaseCommand;
import com.alexpsvet.utils.MessageUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Bounty command
 */
public class BountyCommand extends BaseCommand {
    
    @Override
    public void executeDefault(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(MessageUtil.colorize("&cCette commande ne peut être exécutée que par un joueur!"));
            return;
        }
        
        Player player = (Player) sender;
        BountyMenu.openMainMenu(player);
    }

    @Override
    public String getName() {
        return "bounty";
    }
}
