package com.alexpsvet.shop.menu;

import com.alexpsvet.Survival;
import com.alexpsvet.economy.EconomyManager;
import com.alexpsvet.economy.TransactionType;
import com.alexpsvet.shop.ShopCategory;
import com.alexpsvet.shop.ShopItem;
import com.alexpsvet.shop.ShopManager;
import com.alexpsvet.utils.MessageUtil;
import com.alexpsvet.utils.menu.Button;
import com.alexpsvet.utils.menu.Menu;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
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
                    lore.add(MessageUtil.colorize("&7Prix: &e" + shopItem.getPrice() + " " + currency));
                    lore.add(MessageUtil.colorize("&7Quantité: &ex" + shopItem.getAmount()));
                    lore.add("");
                    lore.add(MessageUtil.colorize("&aClick gauche: &fAcheter x64"));
                    lore.add(MessageUtil.colorize("&aClick droit: &fAcheter x128"));
                    meta.setLore(lore);
                    item.setItemMeta(meta);
                }
                
                final ShopItem finalItem = shopItem;
                builder.button(new Button.Builder()
                    .slot(slot++)
                    .item(item)
                    .onClick((p, clickType) -> {
                        int amountStacks = clickType == ClickType.RIGHT ? 2 : 1;
                        buyItem(p, finalItem, amountStacks);
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
    
    /**
     * Buy an item from the shop
     */
    private void buyItem(Player player, ShopItem shopItem, int amountStacks) {
        EconomyManager eco = Survival.getInstance().getEconomyManager();
        String currency = Survival.getInstance().getConfig().getString("economy.currency-symbol", "⛁");
        
        double totalPrice = shopItem.getPrice() * amountStacks;
        
        // Check balance
        if (eco.getBalance(player.getUniqueId()) < totalPrice) {
            MessageUtil.sendError(player, "Vous n'avez pas assez d'argent! Il vous faut " + totalPrice + " " + currency);
            return;
        }
        
        // Check inventory space
        if (player.getInventory().firstEmpty() == -1) {
            MessageUtil.sendError(player, "Votre inventaire est plein!");
            return;
        }
        
        // Remove money
        if (!eco.removeBalance(player.getUniqueId(), totalPrice)) {
            MessageUtil.sendError(player, "Erreur lors de la transaction!");
            return;
        }
        
        // Give items
        ItemStack item = shopItem.createItemStack();
        item.setAmount(amountStacks * 64);
        player.getInventory().addItem(item);
        
        // Record transaction
        eco.addTransaction(player.getUniqueId(), TransactionType.SHOP_PURCHASE, -totalPrice, 
            "Achat: " + shopItem.getDisplayName() + " x" + (amountStacks * 64));
        
        MessageUtil.sendSuccess(player, "Vous avez acheté &e" + shopItem.getDisplayName() + " x" + (amountStacks * 64) + 
            " &apour &e" + totalPrice + " " + currency);
    }
}
