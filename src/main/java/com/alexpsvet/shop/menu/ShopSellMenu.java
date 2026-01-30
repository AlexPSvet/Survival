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
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * Menu for selling items back to the shop
 */
public class ShopSellMenu implements Listener {
    
    private static final Map<UUID, Inventory> sellInventories = new HashMap<>();
    private static ShopSellMenu instance;
    
    public ShopSellMenu() {
        instance = this;
        Bukkit.getPluginManager().registerEvents(this, Survival.getInstance());
    }
    
    public static ShopSellMenu getInstance() {
        if (instance == null) {
            instance = new ShopSellMenu();
        }
        return instance;
    }
    
    /**
     * Open the sell selection menu
     */
    public static void openSellMenu(Player player) {
        String currency = Survival.getInstance().getConfig().getString("economy.currency-symbol", "⛁");
        
        Menu.Builder builder = new Menu.Builder()
            .title(MessageUtil.colorize("&6&lVendre des Items"))
            .rows(4);
        
        // Info item
        ItemStack info = new ItemStack(Material.EMERALD);
        ItemMeta infoMeta = info.getItemMeta();
        if (infoMeta != null) {
            infoMeta.setDisplayName(MessageUtil.colorize("&a&lVendre vos Items"));
            List<String> lore = new ArrayList<>();
            lore.add(MessageUtil.colorize("&7Ouvrez votre inventaire de vente"));
            lore.add(MessageUtil.colorize("&7et placez les items à vendre."));
            lore.add("");
            lore.add(MessageUtil.colorize("&7Lorsque vous fermez le menu,"));
            lore.add(MessageUtil.colorize("&7vous recevrez l'argent automatiquement!"));
            lore.add("");
            lore.add(MessageUtil.colorize("&eCliquez pour ouvrir!"));
            infoMeta.setLore(lore);
            info.setItemMeta(infoMeta);
        }
        
        builder.button(new Button.Builder()
            .slot(13)
            .item(info)
            .onClick((p, clickType) -> openSellInventory(p))
            .build());
        
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
            .onClick((p, clickType) -> ShopManager.getInstance().getShopMenu().openMainMenu(p))
            .build());
        
        builder.build().open(player);
    }
    
    /**
     * Open a chest inventory for players to place items to sell
     */
    private static void openSellInventory(Player player) {
        Inventory sellInv = Bukkit.createInventory(null, 54, MessageUtil.colorize("&6&lPlacez vos items ici"));
        sellInventories.put(player.getUniqueId(), sellInv);
        player.openInventory(sellInv);
    }
    
    /**
     * Handle inventory close - process selling
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        
        Player player = (Player) event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        Inventory sellInv = sellInventories.remove(playerId);
        if (sellInv == null || !event.getInventory().equals(sellInv)) {
            return;
        }
        
        // Process all items in the sell inventory
        processSell(player, sellInv);
    }
    
    /**
     * Process selling of items
     */
    private void processSell(Player player, Inventory sellInv) {
        EconomyManager eco = Survival.getInstance().getEconomyManager();
        String currency = Survival.getInstance().getConfig().getString("economy.currency-symbol", "⛁");
        
        double totalValue = 0;
        Map<String, Integer> soldItems = new HashMap<>();
        List<ItemStack> unsellableItems = new ArrayList<>();
        
        // Build a map of materials to shop items for quick lookup
        Map<Material, ShopItem> sellableItems = new HashMap<>();
        for (ShopCategory category : ShopManager.getInstance().getCategories().values()) {
            for (ShopItem item : category.getItems()) {
                if (item.canSell()) {
                    sellableItems.put(item.getMaterial(), item);
                }
            }
        }
        
        // Process each item in the inventory
        for (ItemStack item : sellInv.getContents()) {
            if (item == null || item.getType() == Material.AIR) continue;
            
            ShopItem shopItem = sellableItems.get(item.getType());
            if (shopItem == null || !shopItem.canSell()) {
                // Item cannot be sold - return to player
                unsellableItems.add(item);
                continue;
            }
            
            // Calculate value
            int amount = item.getAmount();
            double itemValue = shopItem.getSellPrice() * amount;
            totalValue += itemValue;
            
            // Track sold items for message
            soldItems.put(shopItem.getDisplayName(), soldItems.getOrDefault(shopItem.getDisplayName(), 0) + amount);
        }
        
        // Return unsellable items to player
        for (ItemStack item : unsellableItems) {
            HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item);
            // Drop items that don't fit
            for (ItemStack drop : leftover.values()) {
                player.getWorld().dropItem(player.getLocation(), drop);
            }
        }
        
        // Give money to player
        if (totalValue > 0) {
            eco.addBalance(player.getUniqueId(), totalValue);
            eco.addTransaction(player.getUniqueId(), TransactionType.SHOP_SELL, totalValue, 
                "Vente d'items");
            
            player.sendMessage("");
            player.sendMessage(MessageUtil.colorize("&a&l━━━━━━━━━━━━━━━━━━━━━━━━"));
            player.sendMessage(MessageUtil.colorize("&a&l  VENTE RÉUSSIE"));
            player.sendMessage(MessageUtil.colorize("&a&l━━━━━━━━━━━━━━━━━━━━━━━━"));
            player.sendMessage("");
            
            for (Map.Entry<String, Integer> entry : soldItems.entrySet()) {
                player.sendMessage(MessageUtil.colorize("  &7" + entry.getKey() + " &fx" + entry.getValue()));
            }
            
            player.sendMessage("");
            player.sendMessage(MessageUtil.colorize("&7Total reçu: &a+" + totalValue + " " + currency));
            player.sendMessage(MessageUtil.colorize("&7Nouveau solde: &e" + eco.getBalance(player.getUniqueId()) + " " + currency));
            player.sendMessage(MessageUtil.colorize("&a&l━━━━━━━━━━━━━━━━━━━━━━━━"));
            player.sendMessage("");
            
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        }
        
        if (!unsellableItems.isEmpty()) {
            MessageUtil.sendMessage(player, MessageUtil.colorize("&cCertains items ne peuvent pas être vendus et vous ont été retournés."));
        }
        
        if (totalValue == 0 && unsellableItems.isEmpty()) {
            MessageUtil.sendMessage(player, MessageUtil.colorize("&7Vous n'avez vendu aucun item."));
        }
    }
}
