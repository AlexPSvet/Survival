package com.alexpsvet.commands.base;

import com.alexpsvet.utils.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Base command class with sub-command support
 */
public abstract class BaseCommand implements CommandExecutor, TabCompleter {
    
    private final List<SubCommand> subCommands = new ArrayList<>();
    
    /**
     * Get the name of this command
     * @return the command name
     */
    public abstract String getName();
    
    /**
     * Register a sub-command
     * @param subCommand the sub-command to register
     */
    protected void registerSubCommand(SubCommand subCommand) {
        subCommands.add(subCommand);
    }
    
    /**
     * Get all registered sub-commands
     * @return list of sub-commands
     */
    protected List<SubCommand> getSubCommands() {
        return subCommands;
    }
    
    /**
     * Execute when no sub-command is provided
     * @param sender the sender
     * @param args the arguments
     */
    protected abstract void executeDefault(CommandSender sender, String[] args);
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // No sub-command specified
        if (args.length == 0) {
            executeDefault(sender, args);
            return true;
        }
        
        // Find matching sub-command
        String subCommandName = args[0];
        SubCommand subCommand = findSubCommand(subCommandName);
        
        if (subCommand == null) {
            MessageUtil.sendError(sender, "Sous-commande inconnue: " + subCommandName);
            sendHelp(sender);
            return true;
        }
        
        // Check if console can execute
        if (!(sender instanceof Player) && !subCommand.canConsoleExecute()) {
            MessageUtil.sendError(sender, "Cette commande ne peut être exécutée que par un joueur!");
            return true;
        }
        
        // Check permission
        if (!subCommand.hasPermission(sender)) {
            MessageUtil.sendError(sender, "Vous n'avez pas la permission!");
            return true;
        }
        
        // Execute sub-command
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
        try {
            subCommand.execute(sender, subArgs);
        } catch (Exception e) {
            MessageUtil.sendError(sender, "Erreur lors de l'exécution de la commande: " + e.getMessage());
            e.printStackTrace();
        }
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        
        // Tab complete sub-command names
        if (args.length == 1) {
            for (SubCommand subCommand : subCommands) {
                if (subCommand.hasPermission(sender)) {
                    completions.add(subCommand.getName());
                    completions.addAll(subCommand.getAliases());
                }
            }
            return completions.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        
        // Tab complete sub-command arguments
        if (args.length > 1) {
            SubCommand subCommand = findSubCommand(args[0]);
            if (subCommand != null && subCommand.hasPermission(sender)) {
                String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
                completions = subCommand.getTabCompletions(sender, subArgs);
                
                // Filter based on current input
                String currentArg = args[args.length - 1];
                return completions.stream()
                        .filter(s -> s.toLowerCase().startsWith(currentArg.toLowerCase()))
                        .collect(Collectors.toList());
            }
        }
        
        return completions;
    }
    
    /**
     * Find a sub-command by name or alias
     * @param name the name to search for
     * @return the sub-command, or null if not found
     */
    private SubCommand findSubCommand(String name) {
        for (SubCommand subCommand : subCommands) {
            if (subCommand.matches(name)) {
                return subCommand;
            }
        }
        return null;
    }
    
    /**
     * Send help message listing all sub-commands
     * @param sender the sender
     */
    protected void sendHelp(CommandSender sender) {
        MessageUtil.sendMessage(sender, "&6&l===== Commandes " + getName() + " =====");
        for (SubCommand subCommand : subCommands) {
            if (subCommand.hasPermission(sender)) {
                MessageUtil.sendMessage(sender, 
                    "&e" + subCommand.getSyntax() + " &7- " + subCommand.getDescription());
            }
        }
    }
}
