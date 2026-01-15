package com.alexpsvet.utils.menu;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages all active menus and tracks which players have which menus open
 */
public class MenuManager {
    private static MenuManager instance;
    
    private final Map<UUID, Menu> openMenus; // Player UUID -> Menu
    private final Map<UUID, Menu> registeredMenus; // Menu UUID -> Menu
    
    private MenuManager() {
        this.openMenus = new HashMap<>();
        this.registeredMenus = new HashMap<>();
    }
    
    /**
     * Get the singleton instance of MenuManager
     * @return the MenuManager instance
     */
    public static MenuManager getInstance() {
        if (instance == null) {
            instance = new MenuManager();
        }
        return instance;
    }
    
    /**
     * Register a menu when it's opened by a player
     * @param player the player opening the menu
     * @param menu the menu being opened
     */
    public void registerOpenMenu(Player player, Menu menu) {
        openMenus.put(player.getUniqueId(), menu);
        registeredMenus.put(menu.getMenuId(), menu);
    }
    
    /**
     * Get the menu currently open for a player
     * @param player the player
     * @return the Menu, or null if no menu is open
     */
    public Menu getOpenMenu(Player player) {
        return openMenus.get(player.getUniqueId());
    }
    
    /**
     * Get a registered menu by its ID
     * @param menuId the menu UUID
     * @return the Menu, or null if not found
     */
    public Menu getMenu(UUID menuId) {
        return registeredMenus.get(menuId);
    }
    
    /**
     * Check if a player has a menu open
     * @param player the player
     * @return true if the player has a menu open, false otherwise
     */
    public boolean hasMenuOpen(Player player) {
        return openMenus.containsKey(player.getUniqueId());
    }
    
    /**
     * Close and unregister a player's menu
     * @param player the player
     */
    public void closeMenu(Player player) {
        Menu menu = openMenus.remove(player.getUniqueId());
        if (menu != null) {
            // Note: We keep the menu in registeredMenus in case it's reused
            // You might want to add logic to clean up unused menus
        }
    }
    
    /**
     * Unregister a menu completely
     * @param menuId the menu UUID
     */
    public void unregisterMenu(UUID menuId) {
        registeredMenus.remove(menuId);
        // Remove from all players who have it open
        openMenus.values().removeIf(menu -> menu.getMenuId().equals(menuId));
    }
    
    /**
     * Clear all open menus (useful for plugin reload/disable)
     */
    public void clearAll() {
        openMenus.clear();
        registeredMenus.clear();
    }
}
