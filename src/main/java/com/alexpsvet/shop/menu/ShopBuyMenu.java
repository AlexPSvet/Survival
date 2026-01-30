package com.alexpsvet.shop.menu;

import com.alexpsvet.Survival;
import com.alexpsvet.economy.EconomyManager;
import com.alexpsvet.economy.TransactionType;
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
import java.util.List;

/**
 * Menu for buying a specific item with different quantities
 */
public class ShopBuyMenu {
    
    /**
     * Open the buy menu for a specific item
     */
    public static void open(Player player, ShopItem shopItem, String categoryId) {
        String currency = Survival.getInstance().getConfig().getString("economy.currency-symbol", "⛁");
        EconomyManager eco = Survival.getInstance().getEconomyManager();
        double balance = eco.getBalance(player.getUniqueId());
        
        Menu.Builder builder = new Menu.Builder()
            .title(MessageUtil.colorize("&6&lAcheter: " + shopItem.getDisplayName()))
            .rows(4);
        
        // Item display
        ItemStack displayItem = new ItemStack(shopItem.getMaterial(), shopItem.getAmount());
        ItemMeta meta = displayItem.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(MessageUtil.colorize(shopItem.getDisplayName()));
            List<String> lore = new ArrayList<>();
            for (String line : shopItem.getLore()) {
                lore.add(MessageUtil.colorize(line));
            }
            lore.add("");
            lore.add(MessageUtil.colorize("&7Prix unitaire: &e" + shopItem.getPrice() + " " + currency));
            lore.add("");
            lore.add(MessageUtil.colorize("&7Votre solde: &e" + balance + " " + currency));
            meta.setLore(lore);
            displayItem.setItemMeta(meta);
        }
        
        builder.button(new Button.Builder()
            .slot(4)
            .item(displayItem)
            .build());
        
        // Buy options: 1, 8, 32, 64
        int[] quantities = {1, 8, 32, 64};
        int[] slots = {20, 21, 22, 23};
        Material[] materials = {Material.GOLD_NUGGET, Material.GOLD_INGOT, Material.GOLD_BLOCK, Material.DIAMOND};
        
        for (int i = 0; i < quantities.length; i++) {
            final int quantity = quantities[i];
            double totalPrice = shopItem.getPrice() * quantity;
            
            ItemStack buyOption = new ItemStack(materials[i]);
            ItemMeta buyMeta = buyOption.getItemMeta();
            if (buyMeta != null) {
                buyMeta.setDisplayName(MessageUtil.colorize("&6&lAcheter x" + quantity));
                List<String> buyLore = new ArrayList<>();
                buyLore.add(MessageUtil.colorize("&7Quantité: &e" + quantity));
                buyLore.add(MessageUtil.colorize("&7Prix total: &e" + totalPrice + " " + currency));
                buyLore.add("");
                
                if (balance >= totalPrice) {
                    buyLore.add(MessageUtil.colorize("&aVous pouvez acheter!"));
                    buyLore.add("");
                    buyLore.add(MessageUtil.colorize("&eCliquez pour acheter!"));
                } else {
                    buyLore.add(MessageUtil.colorize("&cFonds insuffisants"));
                }
                
                buyMeta.setLore(buyLore);
                buyOption.setItemMeta(buyMeta);
            }
            
            builder.button(new Button.Builder()
                .slot(slots[i])
                .item(buyOption)
                .onClick((p, clickType) -> {
                    if (balance >= totalPrice) {
                        buyItem(p, shopItem, quantity, categoryId);
                    } else {
                        MessageUtil.sendError(p, "Vous n'avez pas assez d'argent! Il vous faut " + totalPrice + " " + currency);
                    }
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
            .slot(31)
            .item(back)
            .onClick((p, clickType) -> {
                ShopManager.getInstance().getShopMenu().openCategoryMenu(p, categoryId);
            })
            .build());
        
        builder.build().open(player);
    }
    
    /**
     * Buy an item from the shop
     */
    private static void buyItem(Player player, ShopItem shopItem, int quantity, String categoryId) {
        EconomyManager eco = Survival.getInstance().getEconomyManager();
        String currency = Survival.getInstance().getConfig().getString("economy.currency-symbol", "⛁");
        
        // Check permission
        if (shopItem.getPermission() != null && !player.hasPermission(shopItem.getPermission())) {
            MessageUtil.sendError(player, "Vous n'avez pas la permission d'acheter cet item!");
            return;
        }
        
        double totalPrice = shopItem.getPrice() * quantity;
        
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
        ItemStack item = shopItem.createItemStack(quantity);
        player.getInventory().addItem(item);
        
        // Record transaction
        eco.addTransaction(player.getUniqueId(), TransactionType.SHOP_PURCHASE, -totalPrice, 
            "Achat: " + shopItem.getDisplayName() + " x" + quantity);
        
        MessageUtil.sendSuccess(player, "Vous avez acheté &e" + shopItem.getDisplayName() + " x" + quantity + 
            " &apour &e" + totalPrice + " " + currency);
        
        // Reopen buy menu
        open(player, shopItem, categoryId);
    }
}
