package com.alexpsvet.commands.base;

import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract class for sub-commands
 */
public abstract class SubCommand {
    
    /**
     * Get the name of this sub-command
     * @return the name
     */
    public abstract String getName();
    
    /**
     * Get the description of this sub-command
     * @return the description
     */
    public abstract String getDescription();
    
    /**
     * Get the usage syntax of this sub-command
     * @return the syntax
     */
    public abstract String getSyntax();
    
    /**
     * Get the permission required for this sub-command
     * @return the permission, or null if no permission required
     */
    public String getPermission() {
        return null;
    }
    
    /**
     * Get the aliases for this sub-command
     * @return list of aliases
     */
    public List<String> getAliases() {
        return new ArrayList<>();
    }
    
    /**
     * Check if this sub-command can be executed by console
     * @return true if console can execute
     */
    public boolean canConsoleExecute() {
        return false;
    }
    
    /**
     * Execute the sub-command
     * @param sender the command sender
     * @param args the arguments
     */
    public abstract void execute(CommandSender sender, String[] args);
    
    /**
     * Get tab completions for this sub-command
     * @param sender the command sender
     * @param args the current arguments
     * @return list of completions
     */
    public List<String> getTabCompletions(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }
    
    /**
     * Check if a command sender has permission to use this sub-command
     * @param sender the sender
     * @return true if has permission
     */
    public boolean hasPermission(CommandSender sender) {
        String permission = getPermission();
        return permission == null || sender.hasPermission(permission);
    }
    
    /**
     * Check if the name or aliases match the given string
     * @param name the name to check
     * @return true if matches
     */
    public boolean matches(String name) {
        if (getName().equalsIgnoreCase(name)) {
            return true;
        }
        for (String alias : getAliases()) {
            if (alias.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }
}
