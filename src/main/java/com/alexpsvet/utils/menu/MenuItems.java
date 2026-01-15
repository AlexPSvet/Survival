package com.alexpsvet.utils.menu;

import com.alexpsvet.utils.MessageUtil;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

/**
 * Utility class for creating menu items
 */
public class MenuItems {
    
    /**
     * Create an item with name and lore
     */
    public static ItemStack createItem(Material material, String name, String... lore) {
        return createItem(material, name, Arrays.asList(lore));
    }
    
    /**
     * Create an item with name and lore
     */
    public static ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(MessageUtil.colorize(name));
            meta.setLore(MessageUtil.colorize(lore));
            item.setItemMeta(meta);
        }
        return item;
    }
    
    /**
     * Create an item with name, lore, and amount
     */
    public static ItemStack createItem(Material material, int amount, String name, String... lore) {
        ItemStack item = createItem(material, name, lore);
        item.setAmount(amount);
        return item;
    }
    
    /**
     * Create a back button
     */
    public static ItemStack backButton() {
        return createItem(Material.ARROW, "&cRetour", "&7Retour au menu précédent");
    }
    
    /**
     * Create a close button
     */
    public static ItemStack closeButton() {
        return createItem(Material.BARRIER, "&cFermer", "&7Fermer ce menu");
    }
    
    /**
     * Create a filler item (glass pane)
     */
    public static ItemStack filler(Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            item.setItemMeta(meta);
        }
        return item;
    }
    
    /**
     * Create a black glass filler
     */
    public static ItemStack blackFiller() {
        return filler(Material.BLACK_STAINED_GLASS_PANE);
    }
}
