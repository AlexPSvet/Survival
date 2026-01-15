package com.alexpsvet.utils.menu;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

/**
 * Represents a clickable button in a menu
 */
public class Button {
    private final int slot;
    private final ItemStack itemStack;
    private final List<BiConsumer<Player, ClickType>> clickActions;
    private BiPredicate<Player, ClickType> canClickPredicate;
    
    private Button(Builder builder) {
        this.slot = builder.slot;
        this.itemStack = builder.itemStack;
        this.clickActions = builder.clickActions;
        this.canClickPredicate = builder.canClickPredicate;
    }
    
    /**
     * Get the slot position of this button
     * @return slot number (0-53 for double chest)
     */
    public int getSlot() {
        return slot;
    }
    
    /**
     * Get the ItemStack displayed for this button
     * @return the ItemStack
     */
    public ItemStack getItemStack() {
        return itemStack;
    }
    
    /**
     * Check if a player can click this button
     * @param player the player attempting to click
     * @param clickType the type of click
     * @return true if the player can click, false otherwise
     */
    public boolean canClick(Player player, ClickType clickType) {
        if (canClickPredicate == null) {
            return true;
        }
        return canClickPredicate.test(player, clickType);
    }
    
    /**
     * Execute all click actions for this button
     * @param player the player who clicked
     * @param clickType the type of click
     */
    public void onClick(Player player, ClickType clickType) {
        if (!canClick(player, clickType)) {
            return;
        }
        
        for (BiConsumer<Player, ClickType> action : clickActions) {
            action.accept(player, clickType);
        }
    }
    
    /**
     * Builder pattern for creating buttons
     */
    public static class Builder {
        private int slot;
        private ItemStack itemStack;
        private List<BiConsumer<Player, ClickType>> clickActions = new ArrayList<>();
        private BiPredicate<Player, ClickType> canClickPredicate;
        
        /**
         * Set the slot position
         * @param slot the slot number (0-53)
         * @return this builder
         */
        public Builder slot(int slot) {
            this.slot = slot;
            return this;
        }
        
        /**
         * Set the item to display
         * @param material the material type
         * @return this builder
         */
        public Builder item(Material material) {
            this.itemStack = new ItemStack(material);
            return this;
        }
        
        /**
         * Set the item to display
         * @param itemStack the ItemStack
         * @return this builder
         */
        public Builder item(ItemStack itemStack) {
            this.itemStack = itemStack;
            return this;
        }
        
        /**
         * Set the display name of the item
         * @param name the display name
         * @return this builder
         */
        public Builder name(String name) {
            if (itemStack == null) {
                itemStack = new ItemStack(Material.STONE);
            }
            ItemMeta meta = itemStack.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(name);
                itemStack.setItemMeta(meta);
            }
            return this;
        }
        
        /**
         * Set the lore (description) of the item
         * @param lore the lore lines
         * @return this builder
         */
        public Builder lore(List<String> lore) {
            if (itemStack == null) {
                itemStack = new ItemStack(Material.STONE);
            }
            ItemMeta meta = itemStack.getItemMeta();
            if (meta != null) {
                meta.setLore(lore);
                itemStack.setItemMeta(meta);
            }
            return this;
        }
        
        /**
         * Set the lore (description) of the item
         * @param lore the lore lines
         * @return this builder
         */
        public Builder lore(String... lore) {
            List<String> loreList = new ArrayList<>();
            for (String line : lore) {
                loreList.add(line);
            }
            return lore(loreList);
        }
        
        /**
         * Add a click action to be executed when the button is clicked
         * @param action the action to execute
         * @return this builder
         */
        public Builder onClick(BiConsumer<Player, ClickType> action) {
            this.clickActions.add(action);
            return this;
        }
        
        /**
         * Set a predicate to check if the player can click this button
         * @param predicate the predicate (returns true if click is allowed)
         * @return this builder
         */
        public Builder canClick(BiPredicate<Player, ClickType> predicate) {
            this.canClickPredicate = predicate;
            return this;
        }
        
        /**
         * Require a specific permission to click this button
         * @param permission the permission node
         * @return this builder
         */
        public Builder requirePermission(String permission) {
            this.canClickPredicate = (player, clickType) -> player.hasPermission(permission);
            return this;
        }
        
        /**
         * Build the button
         * @return the constructed Button
         */
        public Button build() {
            if (itemStack == null) {
                throw new IllegalStateException("ItemStack must be set");
            }
            return new Button(this);
        }
    }
}
