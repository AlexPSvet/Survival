package com.alexpsvet.shop.menu;

import com.alexpsvet.Survival;
import com.alexpsvet.shop.ShopCategory;
import com.alexpsvet.shop.ShopItem;
import com.alexpsvet.shop.ShopManager;
import com.alexpsvet.utils.MessageUtil;
import com.alexpsvet.utils.menu.Button;
import com.alexpsvet.utils.menu.Menu;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.List;

/**
 * Shop menu using the Menu system
 */
public class ShopMenu {

    private final Menu shopMenu;
    private final HashMap<String, Menu> categoryMenu;

    public ShopMenu(Collection<ShopCategory> categories) {
        this.shopMenu = createShopMenu();
        this.categoryMenu = createCategoryMenus(categories);
    }

    public Menu createShopMenu() {
        Menu.Builder builder = new Menu.Builder()
            .title(MessageUtil.colorize("&6&lBoutique Periquito"))
            .rows(6);

        // Add categories
        for (ShopCategory category : ShopManager.getInstance().getCategories().values()) {
            ItemStack icon = new ItemStack(category.getIcon());
            ItemMeta meta = icon.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(MessageUtil.colorize("&6&l" + category.getName()));
                List<String> lore = new ArrayList<>();
                lore.add(MessageUtil.colorize("&7Articles: &e" + category.getItems().size()));
                lore.add("");
                lore.add(MessageUtil.colorize("&eCliquez pour ouvrir!"));
                meta.setLore(lore);
                icon.setItemMeta(meta);
            }

            builder.button(new Button.Builder()
                .slot(category.getSlot())
                .item(icon)
                .onClick((p, clickType) -> openCategoryMenu(p, category.getId()))
                .build());
        }
        
        // Sell items button
        ItemStack sellButton = new ItemStack(Material.EMERALD);
        ItemMeta sellMeta = sellButton.getItemMeta();
        if (sellMeta != null) {
            sellMeta.setDisplayName(MessageUtil.colorize("&a&lVendre des Items"));
            List<String> sellLore = new ArrayList<>();
            sellLore.add(MessageUtil.colorize("&7Vendez vos items à la boutique"));
            sellLore.add(MessageUtil.colorize("&7et gagnez de l'argent!"));
            sellLore.add("");
            sellLore.add(MessageUtil.colorize("&eCliquez pour vendre!"));
            sellMeta.setLore(sellLore);
            sellButton.setItemMeta(sellMeta);
        }
        
        builder.button(new Button.Builder()
            .slot(48)
            .item(sellButton)
            .onClick((p, clickType) -> ShopSellMenu.openSellMenu(p))
            .build());

        // Close button
        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = close.getItemMeta();
        if (closeMeta != null) {
            closeMeta.setDisplayName(MessageUtil.colorize("&cFermer"));
            close.setItemMeta(closeMeta);
        }

        builder.button(new Button.Builder()
            .slot(49)
            .item(close)
            .onClick((p, clickType) -> p.closeInventory())
            .build());

        return builder.build();
    }

    public HashMap<String, Menu> createCategoryMenus(Collection<ShopCategory> categories) {
        HashMap<String, Menu> categoryMenus = new HashMap<>();

        for (ShopCategory category : categories) {
            Menu.Builder builder = new Menu.Builder()
                .title(MessageUtil.colorize("&6&l" + category.getName()))
                .rows(6);
            
            String currency = Survival.getInstance().getConfig().getString("economy.currency-symbol", "⛁");
            
            // Add items
            int slot = 0;
            for (ShopItem shopItem : category.getItems()) {
                if (slot >= 45) break;
                
                ItemStack item = new ItemStack(shopItem.getMaterial(), shopItem.getAmount());
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(MessageUtil.colorize(shopItem.getDisplayName()));
                    List<String> lore = new ArrayList<>();
                    for (String line : shopItem.getLore()) {
                        lore.add(MessageUtil.colorize(line));
                    }
                    lore.add("");
                    lore.add(MessageUtil.colorize("&7Prix unitaire: &e" + shopItem.getPrice() + " " + currency));
                    if (shopItem.canSell()) {
                        lore.add(MessageUtil.colorize("&7Prix de vente: &a" + shopItem.getSellPrice() + " " + currency));
                    }
                    lore.add("");
                    lore.add(MessageUtil.colorize("&eCliquez pour voir les options d'achat!"));
                    
                    if (shopItem.getPermission() != null) {
                        lore.add("");
                        lore.add(MessageUtil.colorize("&5Permission requise!"));
                    }
                    
                    meta.setLore(lore);
                    item.setItemMeta(meta);
                }
                
                final ShopItem finalItem = shopItem;
                final String categoryId = category.getId();
                builder.button(new Button.Builder()
                    .slot(slot++)
                    .item(item)
                    .onClick((p, clickType) -> {
                        ShopBuyMenu.open(p, finalItem, categoryId);
                    })
                    .build());
            }
            
            // Back button
            ItemStack back = new ItemStack(Material.ARROW);
            ItemMeta backMeta = back.getItemMeta();
            if (backMeta != null) {
                backMeta.setDisplayName(MessageUtil.colorize("&cRetour"));
                back.setItemMeta(backMeta);
            }
            
            builder.button(new Button.Builder()
                .slot(49)
                .item(back)
                .onClick((p, clickType) -> openMainMenu(p))
                .build());
            
            categoryMenus.put(category.getId(), builder.build());
        }

        return categoryMenus;
    }

    /**
     * Open main shop menu
     */
    public void openMainMenu(Player player) {
        this.shopMenu.open(player);
    }
    
    /**
     * Open category menu
     */
    public void openCategoryMenu(Player player, String id) {
        categoryMenu.get(id).open(player);
    }
}