package com.alexpsvet.utils.menu;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a clickable GUI menu
 */
public class Menu implements InventoryHolder {
    private final String title;
    private final int size;
    private final Inventory inventory;
    private final Map<Integer, Button> buttons;
    private final Map<Integer, ItemStack> background;
    private final UUID menuId;
    
    private Menu(Builder builder) {
        this.title = builder.title;
        this.size = builder.size;
        this.buttons = builder.buttons;
        this.background = builder.background;
        this.menuId = UUID.randomUUID();
    
        this.inventory = Bukkit.createInventory(this, size, title);
        
        // Place all buttons in the inventory
        for (Button button : buttons.values()) {
            inventory.setItem(button.getSlot(), button.getItemStack());
        }

        // Fill background items
        for (Map.Entry<Integer, ItemStack> entry : background.entrySet()) {
            int slot = entry.getKey();
            ItemStack itemStack = entry.getValue();
            if (!buttons.containsKey(slot)) {
                inventory.setItem(slot, itemStack);
            }
        }
    }
    
    /**
     * Get the unique ID of this menu
     * @return the menu UUID
     */
    public UUID getMenuId() {
        return menuId;
    }
    
    /**
     * Get the title of this menu
     * @return the title
     */
    public String getTitle() {
        return title;
    }
    
    /**
     * Get the size of this menu
     * @return the size (number of slots)
     */
    public int getSize() {
        return size;
    }
    
    /**
     * Get the button at a specific slot
     * @param slot the slot number
     * @return the Button, or null if no button at that slot
     */
    public Button getButton(int slot) {
        return buttons.get(slot);
    }
    
    /**
     * Check if a button exists at the given slot
     * @param slot the slot number
     * @return true if a button exists, false otherwise
     */
    public boolean hasButton(int slot) {
        return buttons.containsKey(slot);
    }
    
    /**
     * Handle a click on this menu
     * @param player the player who clicked
     * @param slot the slot that was clicked
     * @param clickType the type of click
     * @return true if the click was handled, false otherwise
     */
    public boolean handleClick(Player player, int slot, ClickType clickType) {
        Button button = getButton(slot);
        if (button == null) {
            return false;
        }
        
        if (!button.canClick(player, clickType)) {
            return false;
        }
        
        button.onClick(player, clickType);
        return true;
    }
    
    /**
     * Open this menu for a player
     * @param player the player to open the menu for
     */
    public void open(Player player) {
        MenuManager.getInstance().registerOpenMenu(player, this);
        player.openInventory(inventory);
    }
    
    /**
     * Refresh the menu by updating all item stacks
     */
    public void refresh() {
        inventory.clear();
        for (Button button : buttons.values()) {
            inventory.setItem(button.getSlot(), button.getItemStack());
        }
        for (Map.Entry<Integer, ItemStack> entry : background.entrySet()) {
            int slot = entry.getKey();
            ItemStack itemStack = entry.getValue();
            if (!buttons.containsKey(slot)) {
                inventory.setItem(slot, itemStack);
            }
        }
    }
    
    /**
     * Update a specific slot with a new item
     * @param slot the slot to update
     * @param item the new item
     */
    public void updateSlot(int slot, ItemStack item) {
        inventory.setItem(slot, item);
    }
    
    /**
     * Add or replace a button dynamically
     * @param button the button to add
     */
    public void setButton(Button button) {
        buttons.put(button.getSlot(), button);
        inventory.setItem(button.getSlot(), button.getItemStack());
    }
    
    /**
     * Remove a button from a slot
     * @param slot the slot to clear
     */
    public void removeButton(int slot) {
        buttons.remove(slot);
        inventory.setItem(slot, null);
    }
    
    @Override
    public Inventory getInventory() {
        return inventory;
    }
    
    /**
     * Builder pattern for creating menus
     */
    public static class Builder {
        private String title = "Menu";
        private int size = 27; // Default to 3 rows
        private Map<Integer, Button> buttons = new HashMap<>();
        private Map<Integer, ItemStack> background = new HashMap<>();
        
        /**
         * Set the title of the menu
         * @param title the title
         * @return this builder
         */
        public Builder title(String title) {
            this.title = title;
            return this;
        }
        
        /**
         * Set the size of the menu (must be multiple of 9, max 54)
         * @param size the size
         * @return this builder
         */
        public Builder size(int size) {
            if (size <= 0 || size > 54 || size % 9 != 0) {
                throw new IllegalArgumentException("Size must be a positive multiple of 9 and <= 54");
            }
            this.size = size;
            return this;
        }
        
        /**
         * Set the number of rows (1-6)
         * @param rows the number of rows
         * @return this builder
         */
        public Builder rows(int rows) {
            if (rows < 1 || rows > 6) {
                throw new IllegalArgumentException("Rows must be between 1 and 6");
            }
            this.size = rows * 9;
            return this;
        }
        
        /**
         * Add a button to this menu
         * @param button the button to add
         * @return this builder
         */
        public Builder button(Button button) {
            if (button.getSlot() >= size) {
                throw new IllegalArgumentException("Button slot " + button.getSlot() + " exceeds menu size " + size);
            }
            this.buttons.put(button.getSlot(), button);
            return this;
        }
        
        /**
         * Add multiple buttons to this menu
         * @param buttons the buttons to add
         * @return this builder
         */
        public Builder buttons(Button... buttons) {
            for (Button button : buttons) {
                button(button);
            }
            return this;
        }

        /**
         * Set background item for the menu
         * @param slot the slot number
         * @param itemStack the background item
         * @return this builder
         */
        public Builder setBackgroundItem(int slot, ItemStack itemStack) {
            if (slot < 0 || slot >= size) {
                throw new IllegalArgumentException("Invalid slot " + slot + " for menu size " + size);
            }
            this.background.put(slot, itemStack);
            return this;
        }

        /**
         * Set a background item at specific slots
         * @param itemStack the background item
         * @param slotsToFill the slot numbers to fill with the background item
         * @return this builder
         */
        public Builder backgroundItem(ItemStack itemStack, int... slotsToFill) {
            for (int slot : slotsToFill) {
                if (slot < 0 || slot >= size) {
                    throw new IllegalArgumentException("Invalid slot " + slot + " for menu size " + size);
                }
                this.background.put(slot, itemStack);
            }
            return this;
        }
        
        /**
         * Build the menu
         * @return the constructed Menu
         */
        public Menu build() {
            return new Menu(this);
        }
    }
}
