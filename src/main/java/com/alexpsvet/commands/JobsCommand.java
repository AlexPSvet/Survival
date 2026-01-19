package com.alexpsvet.commands;

import com.alexpsvet.jobs.menu.JobsMenu;
import com.alexpsvet.commands.base.BaseCommand;
import com.alexpsvet.utils.MessageUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Jobs command
 */
public class JobsCommand extends BaseCommand {

    @Override
    public void executeDefault(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(MessageUtil.colorize("&cCette commande ne peut être exécutée que par un joueur!"));
            return;
        }
        
        Player player = (Player) sender;
        JobsMenu.openMainMenu(player);
    }

    @Override
    public String getName() {
        return "jobs";
    }
}
