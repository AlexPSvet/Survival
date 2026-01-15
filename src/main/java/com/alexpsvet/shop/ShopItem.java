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
    private final int amount;
    private final String permission; // Optional permission required to buy
    
    public ShopItem(String id, Material material, String displayName, String[] lore, double price, int amount, String permission) {
        this.id = id;
        this.material = material;
        this.displayName = displayName;
        this.lore = lore;
        this.price = price;
        this.amount = amount;
        this.permission = permission;
    }
    
    public String getId() { return id; }
    public Material getMaterial() { return material; }
    public String getDisplayName() { return displayName; }
    public String[] getLore() { return lore; }
    public double getPrice() { return price; }
    public int getAmount() { return amount; }
    public String getPermission() { return permission; }
    
    /**
     * Create an ItemStack from this shop item
     */
    public ItemStack createItemStack() {
        return new ItemStack(material, amount);
    }
}
