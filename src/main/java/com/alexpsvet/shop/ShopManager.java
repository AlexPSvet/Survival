package com.alexpsvet.shop;

import com.alexpsvet.Survival;
import com.alexpsvet.shop.menu.ShopMenu;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Manages the shop system
 */
public class ShopManager {
    private static final Logger LOGGER = Logger.getLogger("survival");
    private static ShopManager instance;
    
    private final ShopMenu shopMenu;
    private final Map<String, ShopCategory> categories;
    
    public ShopManager() {
        instance = this;
        this.categories = new HashMap<>();
        loadShopConfig();
        this.shopMenu = new ShopMenu(categories.values());
    }
    
    /**
     * Load shop configuration from shop.yml
     */
    private void loadShopConfig() {
        File shopFile = new File(Survival.getInstance().getDataFolder(), "shop.yml");
        if (!shopFile.exists()) {
            Survival.getInstance().saveResource("shop.yml", false);
        }
        
        FileConfiguration config = YamlConfiguration.loadConfiguration(shopFile);
        
        ConfigurationSection categoriesSection = config.getConfigurationSection("categories");
        if (categoriesSection == null) {
            LOGGER.warning("No categories found in shop.yml!");
            return;
        }
        
        for (String categoryId : categoriesSection.getKeys(false)) {
            ConfigurationSection catSection = categoriesSection.getConfigurationSection(categoryId);
            if (catSection == null) continue;
            
            String name = catSection.getString("name", categoryId);
            Material icon = Material.valueOf(catSection.getString("icon", "CHEST"));
            int slot = catSection.getInt("slot", 0);
            
            ShopCategory category = new ShopCategory(categoryId, name, icon, slot);
            
            // Load items
            ConfigurationSection itemsSection = catSection.getConfigurationSection("items");
            if (itemsSection != null) {
                for (String itemId : itemsSection.getKeys(false)) {
                    ConfigurationSection itemSection = itemsSection.getConfigurationSection(itemId);
                    if (itemSection == null) continue;
                    
                    try {
                        Material material = Material.valueOf(itemSection.getString("material", "STONE"));
                        String displayName = itemSection.getString("name", itemId);
                        List<String> loreList = itemSection.getStringList("lore");
                        String[] lore = loreList.toArray(new String[0]);
                        double price = itemSection.getDouble("price", 0);
                        int amount = itemSection.getInt("amount", 1);
                        String permission = itemSection.getString("permission", null);
                        
                        ShopItem item = new ShopItem(itemId, material, displayName, lore, price, amount, permission);
                        category.addItem(item);
                    } catch (IllegalArgumentException e) {
                        LOGGER.warning("Invalid material for shop item: " + itemId);
                    }
                }
            }
            
            categories.put(categoryId, category);
            LOGGER.info("Loaded shop category: " + name + " with " + category.getItems().size() + " items");
        }
    }
    
    /**
     * Get all categories
     */
    public Map<String, ShopCategory> getCategories() {
        return categories;
    }
    
    /**
     * Get a category by ID
     */
    public ShopCategory getCategory(String id) {
        return categories.get(id);
    }

    /**
     * Get the shop menu
     */
    public ShopMenu getShopMenu() {
        return shopMenu;
    }
    
    /**
     * Reload shop config
     */
    public void reload() {
        categories.clear();
        loadShopConfig();
    }
    
    public static ShopManager getInstance() {
        return instance;
    }
}
