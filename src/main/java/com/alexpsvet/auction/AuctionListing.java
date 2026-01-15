package com.alexpsvet.auction;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Represents an auction listing
 */
public class AuctionListing {
    private final int id;
    private final UUID seller;
    private final String sellerName;
    private final ItemStack item;
    private final double price;
    private final long listedAt;
    private final long expiresAt;
    private boolean sold;
    private boolean expired;
    
    public AuctionListing(int id, UUID seller, String sellerName, ItemStack item, double price, long listedAt, long expiresAt) {
        this.id = id;
        this.seller = seller;
        this.sellerName = sellerName;
        this.item = item;
        this.price = price;
        this.listedAt = listedAt;
        this.expiresAt = expiresAt;
        this.sold = false;
        this.expired = false;
    }
    
    public int getId() {
        return id;
    }
    
    public UUID getSeller() {
        return seller;
    }
    
    public String getSellerName() {
        return sellerName;
    }
    
    public ItemStack getItem() {
        return item;
    }
    
    public double getPrice() {
        return price;
    }
    
    public long getListedAt() {
        return listedAt;
    }
    
    public long getExpiresAt() {
        return expiresAt;
    }
    
    public boolean isSold() {
        return sold;
    }
    
    public void setSold(boolean sold) {
        this.sold = sold;
    }
    
    public boolean isExpired() {
        return expired || System.currentTimeMillis() > expiresAt;
    }
    
    public void setExpired(boolean expired) {
        this.expired = expired;
    }
    
    public boolean isActive() {
        return !sold && !isExpired();
    }
}
