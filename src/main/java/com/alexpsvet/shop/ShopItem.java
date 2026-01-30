package com.alexpsvet.shop;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * Represents an item in the shop
 */
public class ShopItem {
    private final String id;
    private final Material material;
    private final String displayName;
    private final String[] lore;
    private final double price;
    private final double sellPrice; // Price at which players can sell this item (0 = can't sell)
    private final int amount;
    private final String permission; // Optional permission required to buy
    
    public ShopItem(String id, Material material, String displayName, String[] lore, double price, double sellPrice, int amount, String permission) {
        this.id = id;
        this.material = material;
        this.displayName = displayName;
        this.lore = lore;
        this.price = price;
        this.sellPrice = sellPrice;
        this.amount = amount;
        this.permission = permission;
    }
    
    // Constructor for backward compatibility
    public ShopItem(String id, Material material, String displayName, String[] lore, double price, int amount, String permission) {
        this(id, material, displayName, lore, price, price * 0.5, amount, permission); // Default sell price is 50% of buy price
    }
    
    public String getId() { return id; }
    public Material getMaterial() { return material; }
    public String getDisplayName() { return displayName; }
    public String[] getLore() { return lore; }
    public double getPrice() { return price; }
    public double getSellPrice() { return sellPrice; }
    public int getAmount() { return amount; }
    public String getPermission() { return permission; }
    public boolean canSell() { return sellPrice > 0; }
    
    /**
     * Get the total price for a specific quantity multiplier
     */
    public double getPriceFor(int quantityMultiplier) {
        return price * quantityMultiplier;
    }
    
    /**
     * Create an ItemStack from this shop item with specific quantity
     */
    public ItemStack createItemStack(int quantityMultiplier) {
        return new ItemStack(material, amount * quantityMultiplier);
    }
    
    /**
     * Create an ItemStack from this shop item
     */
    public ItemStack createItemStack() {
        return new ItemStack(material, amount);
    }
}