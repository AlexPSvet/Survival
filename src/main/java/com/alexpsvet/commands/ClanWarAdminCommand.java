package com.alexpsvet.commands;

import com.alexpsvet.Survival;
import com.alexpsvet.clan.war.ClanWarManager;
import com.alexpsvet.utils.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Admin commands for managing/testing the war arena
 */
public class ClanWarAdminCommand implements CommandExecutor {

    private final ClanWarManager warManager;

    public ClanWarAdminCommand() {
        this.warManager = Survival.getInstance().getClanWarManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command");
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("survival.admin")) {
            MessageUtil.sendError(player, "Vous n'avez pas la permission!");
            return true;
        }

        if (args.length == 0) {
            MessageUtil.sendMessage(player, "Usage: /waradmin <load|tp1|tp2|test>");
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "load":
                if (warManager.loadArena()) {
                    MessageUtil.sendSuccess(player, "Arena loaded successfully.");
                } else {
                    MessageUtil.sendError(player, "Failed to load arena. Check server logs.");
                }
                break;
            case "tp1":
                if (warManager.teleportToClanSpawn(1, player)) {
                    MessageUtil.sendSuccess(player, "Téléporté au spawn clan1.");
                } else {
                    MessageUtil.sendError(player, "Impossible de téléporter: spawn non disponible.");
                }
                break;
            case "tp2":
                if (warManager.teleportToClanSpawn(2, player)) {
                    MessageUtil.sendSuccess(player, "Téléporté au spawn clan2.");
                } else {
                    MessageUtil.sendError(player, "Impossible de téléporter: spawn non disponible.");
                }
                break;
            case "test":
                warManager.startTestWar(player);
                MessageUtil.sendSuccess(player, "Test war started. You are the only participant.");
                break;
            default:
                MessageUtil.sendMessage(player, "Usage: /waradmin <load|tp1|tp2|test>");
        }

        return true;
    }
}
