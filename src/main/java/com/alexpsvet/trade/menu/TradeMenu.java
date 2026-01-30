package com.alexpsvet.trade.menu;

import com.alexpsvet.Survival;
import com.alexpsvet.economy.EconomyManager;
import com.alexpsvet.trade.TradeManager;
import com.alexpsvet.trade.TradeSession;
import com.alexpsvet.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.Map;

/**
 * Menu for trading between players
 */
public class TradeMenu implements Listener {
    
    private static TradeMenu instance;
    private static final Map<UUID, Inventory> tradeInventories = new HashMap<>();
    
    public TradeMenu() {
        instance = this;
        Bukkit.getPluginManager().registerEvents(this, Survival.getInstance());
    }
    
    public static TradeMenu getInstance() {
        if (instance == null) {
            instance = new TradeMenu();
        }
        return instance;
    }
    
    /**
     * Open trade menu for a player
     */
    public static void open(Player player, TradeSession session) {
        Player otherPlayer = session.getOtherPlayer(player);
        String currency = Survival.getInstance().getConfig().getString("economy.currency-symbol", "⛁");
        
        Inventory inv = Bukkit.createInventory(null, 54, MessageUtil.colorize("&6&lÉchange avec " + otherPlayer.getName()));
        
        // Glass pane separator
        ItemStack separator = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta separatorMeta = separator.getItemMeta();
        if (separatorMeta != null) {
            separatorMeta.setDisplayName(" ");
            separator.setItemMeta(separatorMeta);
        }
        
        for (int i = 4; i < 54; i += 9) {
            inv.setItem(i, separator);
        }
        
        // Player's head
        ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta playerSkullMeta = (SkullMeta) playerHead.getItemMeta();
        if (playerSkullMeta != null) {
            playerSkullMeta.setOwningPlayer(player);
            playerSkullMeta.setDisplayName(MessageUtil.colorize("&a&lVotre Offre"));
            List<String> playerLore = new ArrayList<>();
            playerLore.add(MessageUtil.colorize("&7Placez vos items ici"));
            double playerMoney = player.equals(session.getPlayer1()) ? session.getPlayer1Money() : session.getPlayer2Money();
            if (playerMoney > 0) {
                playerLore.add(MessageUtil.colorize("&7Argent: &e" + playerMoney + " " + currency));
            }
            playerSkullMeta.setLore(playerLore);
            playerHead.setItemMeta(playerSkullMeta);
        }
        inv.setItem(0, playerHead);
        
        // Other player's head
        ItemStack otherHead = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta otherSkullMeta = (SkullMeta) otherHead.getItemMeta();
        if (otherSkullMeta != null) {
            otherSkullMeta.setOwningPlayer(otherPlayer);
            otherSkullMeta.setDisplayName(MessageUtil.colorize("&c&lOffre de " + otherPlayer.getName()));
            List<String> otherLore = new ArrayList<>();
            double otherMoney = otherPlayer.equals(session.getPlayer1()) ? session.getPlayer1Money() : session.getPlayer2Money();
            if (otherMoney > 0) {
                otherLore.add(MessageUtil.colorize("&7Argent: &e" + otherMoney + " " + currency));
            }
            otherSkullMeta.setLore(otherLore);
            otherHead.setItemMeta(otherSkullMeta);
        }
        inv.setItem(8, otherHead);
        
        // Display items
        List<ItemStack> playerItems = player.equals(session.getPlayer1()) ? session.getPlayer1Items() : session.getPlayer2Items();
        List<ItemStack> otherItems = otherPlayer.equals(session.getPlayer1()) ? session.getPlayer1Items() : session.getPlayer2Items();
        
        // Player's items (slots 9-12, 18-21, 27-30, 36-39)
        int[] playerSlots = {9, 10, 11, 12, 18, 19, 20, 21, 27, 28, 29, 30, 36, 37, 38, 39};
        for (int i = 0; i < Math.min(playerItems.size(), playerSlots.length); i++) {
            inv.setItem(playerSlots[i], playerItems.get(i));
        }
        
        // Other player's items (slots 14-17, 23-26, 32-35, 41-44)
        int[] otherSlots = {14, 15, 16, 17, 23, 24, 25, 26, 32, 33, 34, 35, 41, 42, 43, 44};
        for (int i = 0; i < Math.min(otherItems.size(), otherSlots.length); i++) {
            ItemStack displayItem = otherItems.get(i).clone();
            ItemMeta meta = displayItem.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                lore.add("");
                lore.add(MessageUtil.colorize("&7(Offre de " + otherPlayer.getName() + ")"));
                meta.setLore(lore);
                displayItem.setItemMeta(meta);
            }
            inv.setItem(otherSlots[i], displayItem);
        }
        
        // Money button
        ItemStack moneyButton = new ItemStack(Material.GOLD_INGOT);
        ItemMeta moneyMeta = moneyButton.getItemMeta();
        if (moneyMeta != null) {
            moneyMeta.setDisplayName(MessageUtil.colorize("&6&lAjouter de l'Argent"));
            List<String> moneyLore = new ArrayList<>();
            double currentMoney = player.equals(session.getPlayer1()) ? session.getPlayer1Money() : session.getPlayer2Money();
            moneyLore.add(MessageUtil.colorize("&7Montant actuel: &e" + currentMoney + " " + currency));
            moneyLore.add("");
            moneyLore.add(MessageUtil.colorize("&7Click gauche: &a+100 " + currency));
            moneyLore.add(MessageUtil.colorize("&7Click droit: &a+1000 " + currency));
            moneyLore.add(MessageUtil.colorize("&7Shift + Click gauche: &c-100 " + currency));
            moneyLore.add(MessageUtil.colorize("&7Shift + Click droit: &c-1000 " + currency));
            moneyMeta.setLore(moneyLore);
            moneyButton.setItemMeta(moneyMeta);
        }
        inv.setItem(45, moneyButton);
        
        // Accept button
        boolean playerAccepted = player.equals(session.getPlayer1()) ? session.isPlayer1Accepted() : session.isPlayer2Accepted();
        boolean otherAccepted = otherPlayer.equals(session.getPlayer1()) ? session.isPlayer1Accepted() : session.isPlayer2Accepted();
        
        ItemStack acceptButton = new ItemStack(playerAccepted ? Material.LIME_CONCRETE : Material.RED_CONCRETE);
        ItemMeta acceptMeta = acceptButton.getItemMeta();
        if (acceptMeta != null) {
            acceptMeta.setDisplayName(MessageUtil.colorize(playerAccepted ? "&a&lVous avez accepté" : "&c&lAccepter l'échange"));
            List<String> acceptLore = new ArrayList<>();
            acceptLore.add("");
            acceptLore.add(MessageUtil.colorize("&7Votre statut: " + (playerAccepted ? "&aAccepté" : "&cEn attente")));
            acceptLore.add(MessageUtil.colorize("&7" + otherPlayer.getName() + ": " + (otherAccepted ? "&aAccepté" : "&cEn attente")));
            acceptLore.add("");
            if (playerAccepted && otherAccepted) {
                acceptLore.add(MessageUtil.colorize("&e&lÉchange dans 5 secondes!"));
            } else {
                acceptLore.add(MessageUtil.colorize("&eCliquez pour accepter!"));
            }
            acceptMeta.setLore(acceptLore);
            acceptButton.setItemMeta(acceptMeta);
        }
        inv.setItem(48, acceptButton);
        
        // Cancel button
        ItemStack cancelButton = new ItemStack(Material.BARRIER);
        ItemMeta cancelMeta = cancelButton.getItemMeta();
        if (cancelMeta != null) {
            cancelMeta.setDisplayName(MessageUtil.colorize("&c&lAnnuler l'échange"));
            cancelButton.setItemMeta(cancelMeta);
        }
        inv.setItem(50, cancelButton);
        
        tradeInventories.put(player.getUniqueId(), inv);
        player.openInventory(inv);
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        
        Inventory tradeInv = tradeInventories.get(player.getUniqueId());
        if (tradeInv == null || !event.getInventory().equals(tradeInv)) {
            return;
        }
        
        TradeSession session = TradeManager.getInstance().getTradeSession(player.getUniqueId());
        if (session == null) {
            player.closeInventory();
            return;
        }
        
        event.setCancelled(true);
        
        int slot = event.getRawSlot();
        
        // Money button
        if (slot == 45) {
            double currentMoney = player.equals(session.getPlayer1()) ? session.getPlayer1Money() : session.getPlayer2Money();
            double change = 0;
            
            if (event.isShiftClick()) {
                change = event.isLeftClick() ? -100 : -1000;
            } else {
                change = event.isLeftClick() ? 100 : 1000;
            }
            
            session.setMoney(player, currentMoney + change);
            // Refresh the menu
            open(player, session);
            Player otherPlayer = session.getOtherPlayer(player);
            if (otherPlayer.isOnline()) {
                open(otherPlayer, session);
            }
            return;
        }
        
        // Accept button
        if (slot == 48) {
            session.toggleAcceptance(player);
            // Refresh the menu
            open(player, session);
            Player otherPlayer = session.getOtherPlayer(player);
            if (otherPlayer.isOnline()) {
                open(otherPlayer, session);
            }
            return;
        }
        
        // Cancel button
        if (slot == 50) {
            TradeManager.getInstance().cancelTrade(player.getUniqueId(), player.getName() + " a annulé");
            return;
        }
        
        // Player item slots (allow removal)
        int[] playerSlots = {9, 10, 11, 12, 18, 19, 20, 21, 27, 28, 29, 30, 36, 37, 38, 39};
        for (int i = 0; i < playerSlots.length; i++) {
            if (slot == playerSlots[i]) {
                session.removeItem(player, i);
                // Refresh the menu
                open(player, session);
                Player otherPlayer = session.getOtherPlayer(player);
                if (otherPlayer.isOnline()) {
                    open(otherPlayer, session);
                }
                return;
            }
        }
        
        // Player inventory clicks - add item to trade
        if (slot >= 54) {
            ItemStack clicked = event.getCurrentItem();
            if (clicked != null && clicked.getType() != Material.AIR) {
                // Check if trade is full
                List<ItemStack> playerItems = player.equals(session.getPlayer1()) ? session.getPlayer1Items() : session.getPlayer2Items();
                if (playerItems.size() >= 16) {
                    MessageUtil.sendError(player, "Votre offre est pleine!");
                    return;
                }
                
                // Add item and remove from inventory
                session.addItem(player, clicked);
                player.getInventory().setItem(event.getSlot(), null);
                // Refresh the menu
                open(player, session);
                Player otherPlayer = session.getOtherPlayer(player);
                if (otherPlayer.isOnline()) {
                    open(otherPlayer, session);
                }
            }
        }
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();
        
        Inventory tradeInv = tradeInventories.remove(player.getUniqueId());
        if (tradeInv == null || !event.getInventory().equals(tradeInv)) {
            return;
        }
        
        // Don't cancel if trade is completing
        TradeSession session = TradeManager.getInstance().getTradeSession(player.getUniqueId());
        if (session != null) {
            // Reopen on next tick unless both accepted
            boolean bothAccepted = session.isPlayer1Accepted() && session.isPlayer2Accepted();
            if (!bothAccepted) {
                Bukkit.getScheduler().runTaskLater(Survival.getInstance(), () -> {
                    if (TradeManager.getInstance().getTradeSession(player.getUniqueId()) != null) {
                        open(player, session);
                    }
                }, 1L);
            }
        }
    }
}
