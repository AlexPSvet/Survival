package com.alexpsvet.trade;

import com.alexpsvet.Survival;
import com.alexpsvet.economy.EconomyManager;
import com.alexpsvet.trade.menu.TradeMenu;
import com.alexpsvet.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an active trade session between two players
 */
public class TradeSession {
    private final Player player1;
    private final Player player2;
    
    private final List<ItemStack> player1Items;
    private final List<ItemStack> player2Items;
    
    private double player1Money;
    private double player2Money;
    
    private boolean player1Accepted;
    private boolean player2Accepted;
    
    private BukkitTask countdownTask;
    
    public TradeSession(Player player1, Player player2) {
        this.player1 = player1;
        this.player2 = player2;
        this.player1Items = new ArrayList<>();
        this.player2Items = new ArrayList<>();
        this.player1Money = 0;
        this.player2Money = 0;
        this.player1Accepted = false;
        this.player2Accepted = false;
    }
    
    public Player getPlayer1() { return player1; }
    public Player getPlayer2() { return player2; }
    
    public List<ItemStack> getPlayer1Items() { return player1Items; }
    public List<ItemStack> getPlayer2Items() { return player2Items; }
    
    public double getPlayer1Money() { return player1Money; }
    public double getPlayer2Money() { return player2Money; }
    
    public boolean isPlayer1Accepted() { return player1Accepted; }
    public boolean isPlayer2Accepted() { return player2Accepted; }
    
    /**
     * Add an item from a player
     */
    public void addItem(Player player, ItemStack item) {
        if (player.equals(player1)) {
            player1Items.add(item.clone());
        } else if (player.equals(player2)) {
            player2Items.add(item.clone());
        }
        
        // Reset acceptance when items change
        resetAcceptance();
        refreshMenus();
    }
    
    /**
     * Remove an item at index for a player
     */
    public void removeItem(Player player, int index) {
        if (player.equals(player1) && index >= 0 && index < player1Items.size()) {
            ItemStack removed = player1Items.remove(index);
            // Return item to player
            player.getInventory().addItem(removed);
        } else if (player.equals(player2) && index >= 0 && index < player2Items.size()) {
            ItemStack removed = player2Items.remove(index);
            // Return item to player
            player.getInventory().addItem(removed);
        }
        
        // Reset acceptance when items change
        resetAcceptance();
        refreshMenus();
    }
    
    /**
     * Set money for a player
     */
    public void setMoney(Player player, double amount) {
        EconomyManager eco = Survival.getInstance().getEconomyManager();
        
        // Validate amount
        if (amount < 0) amount = 0;
        if (amount > eco.getBalance(player.getUniqueId())) {
            amount = eco.getBalance(player.getUniqueId());
        }
        
        if (player.equals(player1)) {
            player1Money = amount;
        } else if (player.equals(player2)) {
            player2Money = amount;
        }
        
        // Reset acceptance when money changes
        resetAcceptance();
        refreshMenus();
    }
    
    /**
     * Toggle acceptance for a player
     */
    public void toggleAcceptance(Player player) {
        if (player.equals(player1)) {
            player1Accepted = !player1Accepted;
        } else if (player.equals(player2)) {
            player2Accepted = !player2Accepted;
        }
        
        // Check if both players accepted
        if (player1Accepted && player2Accepted) {
            startCountdown();
        } else {
            cancelCountdown();
        }
        
        refreshMenus();
    }
    
    /**
     * Reset acceptance status
     */
    private void resetAcceptance() {
        player1Accepted = false;
        player2Accepted = false;
        cancelCountdown();
    }
    
    /**
     * Start the 5-second countdown
     */
    private void startCountdown() {
        cancelCountdown(); // Cancel any existing countdown
        
        final int[] secondsLeft = {5};
        
        countdownTask = Bukkit.getScheduler().runTaskTimer(Survival.getInstance(), () -> {
            if (secondsLeft[0] <= 0) {
                // Complete trade
                cancelCountdown();
                TradeManager.getInstance().completeTrade(this);
                return;
            }
            
            // Notify players
            String message = MessageUtil.colorize("&e&lÉchange dans " + secondsLeft[0] + " seconde(s)...");
            player1.sendMessage(message);
            player2.sendMessage(message);
            
            player1.playSound(player1.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f + (0.1f * (6 - secondsLeft[0])));
            player2.playSound(player2.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f + (0.1f * (6 - secondsLeft[0])));
            
            secondsLeft[0]--;
        }, 0L, 20L);
    }
    
    /**
     * Cancel the countdown
     */
    public void cancelCountdown() {
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
    }
    
    /**
     * Return all items to players
     */
    public void returnItems() {
        for (ItemStack item : player1Items) {
            if (item != null) {
                player1.getInventory().addItem(item);
            }
        }
        
        for (ItemStack item : player2Items) {
            if (item != null) {
                player2.getInventory().addItem(item);
            }
        }
        
        player1Items.clear();
        player2Items.clear();
    }
    
    /**
     * Open trade menu for a player
     */
    public void openTradeMenu(Player player) {
        TradeMenu.open(player, this);
    }
    
    /**
     * Refresh menus for both players
     */
    public void refreshMenus() {
        openTradeMenu(player1);
        openTradeMenu(player2);
    }
    
    /**
     * Get the other player in the trade
     */
    public Player getOtherPlayer(Player player) {
        return player.equals(player1) ? player2 : player1;
    }
}
