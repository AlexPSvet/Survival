package com.alexpsvet.auction;

import com.alexpsvet.Survival;
import com.alexpsvet.chat.ChatManager;
import com.alexpsvet.database.Database;
import com.alexpsvet.economy.EconomyManager;
import com.alexpsvet.economy.TransactionType;
import com.alexpsvet.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Manager for auction house
 */
public class AuctionManager {
    private static final Logger LOGGER = Logger.getLogger("survival");
    private static AuctionManager instance;
    
    private final Database database;
    private final Map<Integer, AuctionListing> listings;
    private int nextId = 1;
    
    public AuctionManager(Database database) {
        instance = this;
        this.database = database;
        this.listings = new HashMap<>();
        createTables();
        loadListings();
        startExpirationTask();
    }
    
    /**
     * Create auction tables
     */
    private void createTables() {
        String query = "CREATE TABLE IF NOT EXISTS auction_listings (" +
                "id INTEGER PRIMARY KEY " + (database.getType().name().equals("SQLITE") ? "AUTOINCREMENT" : "AUTO_INCREMENT") + "," +
                "seller_uuid VARCHAR(36) NOT NULL," +
                "seller_name VARCHAR(16) NOT NULL," +
                "item_data BLOB NOT NULL," +
                "price DOUBLE NOT NULL," +
                "listed_at BIGINT NOT NULL," +
                "expires_at BIGINT NOT NULL," +
                "sold BOOLEAN DEFAULT 0," +
                "expired BOOLEAN DEFAULT 0" +
                ")";
        database.executeUpdate(query);
        LOGGER.info("Auction tables created/verified");
    }
    
    /**
     * Load all active listings from database
     */
    private void loadListings() {
        ResultSet rs = database.executeQuery("SELECT * FROM auction_listings WHERE sold = 0 AND expired = 0");
        try {
            while (rs != null && rs.next()) {
                int id = rs.getInt("id");
                UUID seller = UUID.fromString(rs.getString("seller_uuid"));
                String sellerName = rs.getString("seller_name");
                ItemStack item = deserializeItem(rs.getBytes("item_data"));
                double price = rs.getDouble("price");
                long listedAt = rs.getLong("listed_at");
                long expiresAt = rs.getLong("expires_at");
                
                if (item != null) {
                    AuctionListing listing = new AuctionListing(id, seller, sellerName, item, price, listedAt, expiresAt);
                    listings.put(id, listing);
                    if (id >= nextId) {
                        nextId = id + 1;
                    }
                }
            }
            if (rs != null) rs.close();
            LOGGER.info("Loaded " + listings.size() + " auction listings");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to load auction listings", e);
        }
    }
    
    /**
     * Create a new auction listing
     */
    public boolean createListing(Player seller, ItemStack item, double price, long durationHours) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        
        ItemStack clonedItem = item.clone();
        long now = System.currentTimeMillis();
        long expiresAt = now + (durationHours * 3600000L);
        
        byte[] itemData = serializeItem(clonedItem);
        if (itemData == null) {
            return false;
        }
        
        database.executeUpdate(
            "INSERT INTO auction_listings (seller_uuid, seller_name, item_data, price, listed_at, expires_at) VALUES (?, ?, ?, ?, ?, ?)",
            seller.getUniqueId().toString(), seller.getName(), itemData, price, now, expiresAt
        );
        
        // Get the ID of the newly created listing
        ResultSet rs = database.executeQuery("SELECT MAX(id) as max_id FROM auction_listings");
        try {
            if (rs != null && rs.next()) {
                int id = rs.getInt("max_id");
                AuctionListing listing = new AuctionListing(id, seller.getUniqueId(), seller.getName(), clonedItem, price, now, expiresAt);
                listings.put(id, listing);
                rs.close();
                
                String currencySymbol = Survival.getInstance().getConfig().getString("economy.currency-symbol", "⛁");
                ChatManager chatManager = ChatManager.getInstance();
                MessageUtil.sendMessage(seller, chatManager.getMessage("auction.listed",
                    "{price}", String.format("%.2f", price),
                    "{currency}", currencySymbol));
                
                return true;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to get listing ID", e);
        }
        
        return false;
    }
    
    /**
     * Buy a listing
     */
    public boolean buyListing(Player buyer, int listingId) {
        AuctionListing listing = listings.get(listingId);
        if (listing == null || !listing.isActive()) {
            return false;
        }
        
        if (listing.getSeller().equals(buyer.getUniqueId())) {
            MessageUtil.sendError(buyer, "Vous ne pouvez pas acheter votre propre article!");
            return false;
        }
        
        EconomyManager economyManager = Survival.getInstance().getEconomyManager();
        
        // Check if buyer has enough money
        if (economyManager.getBalance(buyer.getUniqueId()) < listing.getPrice()) {
            ChatManager chatManager = ChatManager.getInstance();
            MessageUtil.sendMessage(buyer, chatManager.getMessage("auction.no-money"));
            return false;
        }
        
        // Check if buyer has inventory space
        if (buyer.getInventory().firstEmpty() == -1) {
            ChatManager chatManager = ChatManager.getInstance();
            MessageUtil.sendMessage(buyer, chatManager.getMessage("auction.full-inventory"));
            return false;
        }
        
        // Transfer money
        economyManager.removeBalance(buyer.getUniqueId(), listing.getPrice());
        economyManager.addBalance(listing.getSeller(), listing.getPrice());
        
        economyManager.addTransaction(buyer.getUniqueId(), TransactionType.PURCHASE, -listing.getPrice(), "Auction purchase");
        economyManager.addTransaction(listing.getSeller(), TransactionType.SALE, listing.getPrice(), "Auction sale");
        
        // Give item to buyer
        buyer.getInventory().addItem(listing.getItem());
        
        // Mark as sold
        listing.setSold(true);
        database.executeUpdate("UPDATE auction_listings SET sold = 1 WHERE id = ?", listingId);
        listings.remove(listingId);
        
        // Notify players
        String currencySymbol = Survival.getInstance().getConfig().getString("economy.currency-symbol", "⛁");
        ChatManager chatManager = ChatManager.getInstance();
        
        String itemName = listing.getItem().hasItemMeta() && listing.getItem().getItemMeta().hasDisplayName()
            ? listing.getItem().getItemMeta().getDisplayName()
            : listing.getItem().getType().name();
        
        MessageUtil.sendMessage(buyer, chatManager.getMessage("auction.bought",
            "{item}", itemName,
            "{price}", String.format("%.2f", listing.getPrice()),
            "{currency}", currencySymbol));
        
        Player seller = Bukkit.getPlayer(listing.getSeller());
        if (seller != null) {
            MessageUtil.sendMessage(seller, chatManager.getMessage("auction.sold",
                "{item}", itemName,
                "{price}", String.format("%.2f", listing.getPrice()),
                "{currency}", currencySymbol));
        }
        
        return true;
    }
    
    /**
     * Cancel a listing
     */
    public boolean cancelListing(Player player, int listingId) {
        AuctionListing listing = listings.get(listingId);
        if (listing == null || !listing.getSeller().equals(player.getUniqueId())) {
            return false;
        }
        
        // Return item to player
        if (player.getInventory().firstEmpty() != -1) {
            player.getInventory().addItem(listing.getItem());
        } else {
            player.getWorld().dropItem(player.getLocation(), listing.getItem());
        }
        
        // Remove listing
        database.executeUpdate("DELETE FROM auction_listings WHERE id = ?", listingId);
        listings.remove(listingId);
        
        ChatManager chatManager = ChatManager.getInstance();
        MessageUtil.sendMessage(player, chatManager.getMessage("auction.cancelled"));
        
        return true;
    }
    
    /**
     * Get all active listings
     */
    public List<AuctionListing> getActiveListings() {
        return listings.values().stream()
                .filter(AuctionListing::isActive)
                .sorted((a, b) -> Long.compare(b.getListedAt(), a.getListedAt()))
                .collect(Collectors.toList());
    }
    
    /**
     * Get listings by seller
     */
    public List<AuctionListing> getListingsBySeller(UUID seller) {
        return listings.values().stream()
                .filter(l -> l.getSeller().equals(seller) && l.isActive())
                .sorted((a, b) -> Long.compare(b.getListedAt(), a.getListedAt()))
                .collect(Collectors.toList());
    }
    
    /**
     * Start task to check for expired listings
     */
    private void startExpirationTask() {
        Bukkit.getScheduler().runTaskTimer(Survival.getInstance(), () -> {
            long now = System.currentTimeMillis();
            List<AuctionListing> expired = new ArrayList<>();
            
            for (AuctionListing listing : listings.values()) {
                if (listing.isExpired()) {
                    expired.add(listing);
                }
            }
            
            for (AuctionListing listing : expired) {
                expireListing(listing);
            }
        }, 20L * 60L, 20L * 60L); // Check every minute
    }
    
    /**
     * Expire a listing
     */
    private void expireListing(AuctionListing listing) {
        listing.setExpired(true);
        database.executeUpdate("UPDATE auction_listings SET expired = 1 WHERE id = ?", listing.getId());
        listings.remove(listing.getId());
        
        // Return item to seller
        Player seller = Bukkit.getPlayer(listing.getSeller());
        if (seller != null) {
            if (seller.getInventory().firstEmpty() != -1) {
                seller.getInventory().addItem(listing.getItem());
            } else {
                seller.getWorld().dropItem(seller.getLocation(), listing.getItem());
            }
            
            String itemName = listing.getItem().hasItemMeta() && listing.getItem().getItemMeta().hasDisplayName()
                ? listing.getItem().getItemMeta().getDisplayName()
                : listing.getItem().getType().name();
            
            ChatManager chatManager = ChatManager.getInstance();
            MessageUtil.sendMessage(seller, chatManager.getMessage("auction.expired", "{item}", itemName));
        }
    }
    
    /**
     * Serialize an ItemStack to byte array
     */
    private byte[] serializeItem(ItemStack item) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
            dataOutput.writeObject(item);
            dataOutput.close();
            return outputStream.toByteArray();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to serialize item", e);
            return null;
        }
    }
    
    /**
     * Deserialize an ItemStack from byte array
     */
    private ItemStack deserializeItem(byte[] data) {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            ItemStack item = (ItemStack) dataInput.readObject();
            dataInput.close();
            return item;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to deserialize item", e);
            return null;
        }
    }
    
    public static AuctionManager getInstance() {
        return instance;
    }
}
