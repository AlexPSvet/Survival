package com.alexpsvet.utils.menu;

import com.alexpsvet.gamble.GambleMenu;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryHolder;

/**
 * Listener that handles all menu interactions
 */
public class MenuListener implements Listener {
    
    /**
     * Handle clicks in menu inventories
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMenuClick(InventoryClickEvent event) {
        // Check if the clicked inventory is a menu
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof Menu)) {
            return;
        }
        
        Menu menu = (Menu) holder;
        
        // Cancel the event by default to prevent item manipulation
        event.setCancelled(true);
        
        // Ensure the clicker is a player
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        
        // Get the clicked slot
        int slot = event.getRawSlot();
        
        // Ignore clicks outside the menu
        if (slot < 0 || slot >= menu.getSize()) {
            return;
        }
        
        // Check if there's a button at this slot
        if (!menu.hasButton(slot)) {
            return;
        }
        
        // Handle the click
        menu.handleClick(player, slot, event.getClick());
    }
    
    /**
     * Handle dragging items in menus (prevent it)
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMenuDrag(InventoryDragEvent event) {
        // Check if the inventory is a menu
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof Menu) {
            // Cancel drag events in menus to prevent item manipulation
            event.setCancelled(true);
        }
    }
    
    /**
     * Handle menu close events
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onMenuClose(InventoryCloseEvent event) {
        // Check if the closed inventory is a menu
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof Menu)) {
            return;
        }
        
        // Ensure the player is actually a player
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getPlayer();
        Menu menu = (Menu) holder;
        
        // Check if this is a gamble menu and player is in active game
        if (GambleMenu.isPlayerInActiveGame(player)) {
            // Remove the bet and send message to player
            GambleMenu.handlePlayerLeaveGame(player);
            player.sendMessage("§cYou have left the gamble game and lost your bet.");
            return;
        }
        
        // Unregister the menu from the player
        MenuManager.getInstance().closeMenu(player);
    }
}
