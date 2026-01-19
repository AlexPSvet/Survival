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
    private final int leftClickAmount;
    private final int rightClickAmount;

    public ShopMenu(Collection<ShopCategory> categories) {
        this.leftClickAmount = Survival.getInstance().getConfig().getInt("shop.left-click-amount", 64);
        this.rightClickAmount = Survival.getInstance().getConfig().getInt("shop.right-click-amount", 128);
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
                    lore.add(MessageUtil.colorize("&7Prix unitaire: &e" + shopItem.getPrice() + " " + currency));
                    lore.add("");
                    lore.add(MessageUtil.colorize("&aClick gauche: &fAcheter x" + leftClickAmount + " &7(&e" + 
                        (shopItem.getPriceFor(leftClickAmount)) + " " + currency + "&7)"));
                    lore.add(MessageUtil.colorize("&aClick droit: &fAcheter x" + rightClickAmount + " &7(&e" + 
                        (shopItem.getPriceFor(rightClickAmount)) + " " + currency + "&7)"));
                    
                    if (shopItem.getPermission() != null) {
                        lore.add("");
                        lore.add(MessageUtil.colorize("&5Permission requise!"));
                    }
                    
                    meta.setLore(lore);
                    item.setItemMeta(meta);
                }
                
                final ShopItem finalItem = shopItem;
                builder.button(new Button.Builder()
                    .slot(slot++)
                    .item(item)
                    .onClick((p, clickType) -> {
                        int amount = clickType == ClickType.RIGHT ? rightClickAmount : leftClickAmount;
                        buyItem(p, finalItem, amount);
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
    private void buyItem(Player player, ShopItem shopItem, int amount) {
        EconomyManager eco = Survival.getInstance().getEconomyManager();
        String currency = Survival.getInstance().getConfig().getString("economy.currency-symbol", "⛁");
        
        // Check permission
        if (shopItem.getPermission() != null && !player.hasPermission(shopItem.getPermission())) {
            MessageUtil.sendError(player, "Vous n'avez pas la permission d'acheter cet item!");
            return;
        }
        
        double totalPrice = shopItem.getPriceFor(amount);
        
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
        ItemStack item = shopItem.createItemStack(amount);
        player.getInventory().addItem(item);
        
        // Record transaction
        eco.addTransaction(player.getUniqueId(), TransactionType.SHOP_PURCHASE, -totalPrice, 
            "Achat: " + shopItem.getDisplayName() + " x" + amount);
        
        MessageUtil.sendSuccess(player, "Vous avez acheté &e" + shopItem.getDisplayName() + " x" + amount + 
            " &apour &e" + totalPrice + " " + currency);
    }
}