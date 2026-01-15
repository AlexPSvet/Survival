package com.alexpsvet.auction.menu;

import com.alexpsvet.Survival;
import com.alexpsvet.auction.AuctionListing;
import com.alexpsvet.auction.AuctionManager;
import com.alexpsvet.utils.MessageUtil;
import com.alexpsvet.utils.menu.Button;
import com.alexpsvet.utils.menu.Menu;
import com.alexpsvet.utils.menu.MenuItems;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Auction House menu
 */
public class AuctionMenu {
    
    private static final int ITEMS_PER_PAGE = 45;
    
    /**
     * Open the main auction house menu
     */
    public static void open(Player player) {
        open(player, 0);
    }
    
    /**
     * Open the auction house menu at a specific page
     */
    public static void open(Player player, int page) {
        AuctionManager auctionManager = AuctionManager.getInstance();
        List<AuctionListing> listings = auctionManager.getActiveListings();
        
        int totalPages = (int) Math.ceil((double) listings.size() / ITEMS_PER_PAGE);
        if (totalPages == 0) totalPages = 1;
        if (page >= totalPages) page = totalPages - 1;
        if (page < 0) page = 0;
        
        int startIndex = page * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, listings.size());
        
        Menu.Builder menuBuilder = new Menu.Builder()
                .title(MessageUtil.colorize("&6&lAuction House &7(Page " + (page + 1) + "/" + totalPages + ")"))
                .rows(6);
        
        // Add listings
        int slot = 0;
        for (int i = startIndex; i < endIndex; i++) {
            AuctionListing listing = listings.get(i);
            menuBuilder.button(createListingButton(listing, slot++));
        }
        
        // Navigation and controls
        if (page > 0) {
            int finalPage = page;
            menuBuilder.button(new Button.Builder()
                    .slot(48)
                    .item(MenuItems.createItem(Material.ARROW, "&ePage Précédente"))
                    .onClick((p, clickType) -> open(p, finalPage - 1))
                    .build());
        }
        
        if (page < totalPages - 1) {
            int finalPage = page;
            menuBuilder.button(new Button.Builder()
                    .slot(50)
                    .item(MenuItems.createItem(Material.ARROW, "&ePage Suivante"))
                    .onClick((p, clickType) -> open(p, finalPage + 1))
                    .build());
        }
        
        // My listings button
        menuBuilder.button(new Button.Builder()
                .slot(49)
                .item(MenuItems.createItem(Material.CHEST, "&6Mes Ventes", "&7Cliquez pour voir vos ventes"))
                .onClick((p, clickType) -> openMyListings(p))
                .build());
        
        // Sell item button
        menuBuilder.button(new Button.Builder()
                .slot(53)
                .item(MenuItems.createItem(Material.GOLD_INGOT, "&aVendre un Article", 
                    "&7Utilisez: &e/ah sell <prix>",
                    "&7avec l'article en main"))
                .build());
        
        menuBuilder.build().open(player);
    }
    
    /**
     * Create a button for an auction listing
     */
    private static Button createListingButton(AuctionListing listing, int slot) {
        ItemStack displayItem = listing.getItem().clone();
        ItemMeta meta = displayItem.getItemMeta();
        
        if (meta != null) {
            List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            String currencySymbol = Survival.getInstance().getConfig().getString("economy.currency-symbol", "⛁");
            
            lore.add("");
            lore.add(MessageUtil.colorize("&7Vendeur: &e" + listing.getSellerName()));
            lore.add(MessageUtil.colorize("&7Prix: &e" + String.format("%.2f", listing.getPrice()) + " " + currencySymbol));
            
            long timeLeft = listing.getExpiresAt() - System.currentTimeMillis();
            long hoursLeft = timeLeft / 3600000L;
            lore.add(MessageUtil.colorize("&7Expire dans: &e" + hoursLeft + "h"));
            lore.add("");
            lore.add(MessageUtil.colorize("&aCliquez pour acheter!"));
            
            meta.setLore(lore);
            displayItem.setItemMeta(meta);
        }
        
        return new Button.Builder()
                .slot(slot)
                .item(displayItem)
                .onClick((player, clickType) -> {
                    if (AuctionManager.getInstance().buyListing(player, listing.getId())) {
                        player.closeInventory();
                        // Reopen menu after purchase
                        Bukkit.getScheduler().runTaskLater(Survival.getInstance(), () -> open(player, 0), 2L);
                    }
                })
                .build();
    }
    
    /**
     * Open menu showing player's listings
     */
    public static void openMyListings(Player player) {
        AuctionManager auctionManager = AuctionManager.getInstance();
        List<AuctionListing> listings = auctionManager.getListingsBySeller(player.getUniqueId());
        
        Menu.Builder menuBuilder = new Menu.Builder()
                .title(MessageUtil.colorize("&6&lMes Ventes"))
                .rows(6);
        
        int slot = 0;
        for (AuctionListing listing : listings) {
            if (slot >= 45) break;
            
            ItemStack displayItem = listing.getItem().clone();
            ItemMeta meta = displayItem.getItemMeta();
            
            if (meta != null) {
                List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                String currencySymbol = Survival.getInstance().getConfig().getString("economy.currency-symbol", "⛁");
                
                lore.add("");
                lore.add(MessageUtil.colorize("&7Prix: &e" + String.format("%.2f", listing.getPrice()) + " " + currencySymbol));
                
                long timeLeft = listing.getExpiresAt() - System.currentTimeMillis();
                long hoursLeft = timeLeft / 3600000L;
                lore.add(MessageUtil.colorize("&7Expire dans: &e" + hoursLeft + "h"));
                lore.add("");
                lore.add(MessageUtil.colorize("&cCliquez pour annuler"));
                
                meta.setLore(lore);
                displayItem.setItemMeta(meta);
            }
            
            int listingId = listing.getId();
            menuBuilder.button(new Button.Builder()
                    .slot(slot++)
                    .item(displayItem)
                    .onClick((p, clickType) -> {
                        if (auctionManager.cancelListing(p, listingId)) {
                            p.closeInventory();
                            Bukkit.getScheduler().runTaskLater(Survival.getInstance(), () -> openMyListings(p), 2L);
                        }
                    })
                    .build());
        }
        
        // Back button
        menuBuilder.button(new Button.Builder()
                .slot(49)
                .item(MenuItems.backButton())
                .onClick((p, clickType) -> open(p, 0))
                .build());
        
        menuBuilder.build().open(player);
    }
}
