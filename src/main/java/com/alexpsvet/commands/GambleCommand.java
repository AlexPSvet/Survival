package com.alexpsvet.commands;

import com.alexpsvet.Survival;
import com.alexpsvet.gamble.GambleMenu;
import com.alexpsvet.commands.base.BaseCommand;
import com.alexpsvet.utils.MessageUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Gamble command
 */
public class GambleCommand extends BaseCommand {

    
    @Override
    public void executeDefault(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(MessageUtil.colorize("&cCette commande ne peut être exécutée que par un joueur!"));
            return;
        }
        
        // Check if gambling is enabled
        if (!Survival.getInstance().getConfig().getBoolean("gamble.enabled", true)) {
            sender.sendMessage(MessageUtil.colorize("&cLe jeu est actuellement désactivé!"));
            return;
        }
        
        Player player = (Player) sender;
        GambleMenu.openMainMenu(player);
    }

    @Override
    public String getName() {
        return "gamble";
    }
}
