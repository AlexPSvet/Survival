package com.alexpsvet.shop;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a category in the shop
 */
public class ShopCategory {
    private final String id;
    private final String name;
    private final Material icon;
    private final int slot;
    private final List<ShopItem> items;
    
    public ShopCategory(String id, String name, Material icon, int slot) {
        this.id = id;
        this.name = name;
        this.icon = icon;
        this.slot = slot;
        this.items = new ArrayList<>();
    }
    
    public String getId() { return id; }
    public String getName() { return name; }
    public Material getIcon() { return icon; }
    public int getSlot() { return slot; }
    public List<ShopItem> getItems() { return items; }
    
    public void addItem(ShopItem item) {
        items.add(item);
    }
}
