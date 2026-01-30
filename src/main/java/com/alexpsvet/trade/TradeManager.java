package com.alexpsvet.trade;

import com.alexpsvet.Survival;
import com.alexpsvet.economy.EconomyManager;
import com.alexpsvet.economy.TransactionType;
import com.alexpsvet.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * Manages player-to-player trading
 */
public class TradeManager {
    private static TradeManager instance;
    
    private final Map<UUID, TradeSession> activeTrades; // Player UUID -> Their trade session
    private final Map<UUID, UUID> tradeRequests; // Sender UUID -> Receiver UUID
    private final Map<UUID, BukkitTask> requestTimeouts; // Sender UUID -> Timeout task
    
    public TradeManager() {
        instance = this;
        this.activeTrades = new HashMap<>();
        this.tradeRequests = new HashMap<>();
        this.requestTimeouts = new HashMap<>();
    }
    
    public static TradeManager getInstance() {
        if (instance == null) {
            instance = new TradeManager();
        }
        return instance;
    }
    
    /**
     * Send a trade request from one player to another
     */
    public boolean sendTradeRequest(Player sender, Player receiver) {
        UUID senderId = sender.getUniqueId();
        UUID receiverId = receiver.getUniqueId();
        
        // Check if either player is already in a trade
        if (activeTrades.containsKey(senderId)) {
            MessageUtil.sendError(sender, "Vous êtes déjà en train de trader!");
            return false;
        }
        
        if (activeTrades.containsKey(receiverId)) {
            MessageUtil.sendError(sender, receiver.getName() + " est déjà en train de trader!");
            return false;
        }
        
        // Check if there's already a pending request
        if (tradeRequests.containsKey(senderId)) {
            MessageUtil.sendError(sender, "Vous avez déjà une demande d'échange en attente!");
            return false;
        }
        
        // Check if receiver has sent a request to sender (accept automatically)
        if (tradeRequests.containsKey(receiverId) && tradeRequests.get(receiverId).equals(senderId)) {
            // Auto-accept
            acceptTradeRequest(receiver, sender);
            return true;
        }
        
        // Send request
        tradeRequests.put(senderId, receiverId);
        
        MessageUtil.sendSuccess(sender, "Demande d'échange envoyée à &e" + receiver.getName());
        receiver.sendMessage("");
        receiver.sendMessage(MessageUtil.colorize("&6&l━━━━━━━━━━━━━━━━━━━━━━━━"));
        receiver.sendMessage(MessageUtil.colorize("&e&lDemande d'Échange"));
        receiver.sendMessage("");
        receiver.sendMessage(MessageUtil.colorize("  &7" + sender.getName() + " &fvous propose un échange!"));
        receiver.sendMessage("");
        receiver.sendMessage(MessageUtil.colorize("  &a/trade accept " + sender.getName() + " &7- Accepter"));
        receiver.sendMessage(MessageUtil.colorize("  &c/trade deny " + sender.getName() + " &7- Refuser"));
        receiver.sendMessage("");
        receiver.sendMessage(MessageUtil.colorize("  &7Expire dans 60 secondes"));
        receiver.sendMessage(MessageUtil.colorize("&6&l━━━━━━━━━━━━━━━━━━━━━━━━"));
        receiver.sendMessage("");
        receiver.playSound(receiver.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
        
        // Set timeout
        BukkitTask timeout = Bukkit.getScheduler().runTaskLater(Survival.getInstance(), () -> {
            if (tradeRequests.containsKey(senderId)) {
                tradeRequests.remove(senderId);
                requestTimeouts.remove(senderId);
                MessageUtil.sendError(sender, "Demande d'échange à " + receiver.getName() + " expirée");
                MessageUtil.sendMessage(receiver, MessageUtil.colorize("&7Demande d'échange de " + sender.getName() + " expirée"));
            }
        }, 1200L); // 60 seconds
        
        requestTimeouts.put(senderId, timeout);
        
        return true;
    }
    
    /**
     * Accept a trade request
     */
    public boolean acceptTradeRequest(Player acceptor, Player requester) {
        UUID acceptorId = acceptor.getUniqueId();
        UUID requesterId = requester.getUniqueId();
        
        // Check if request exists
        if (!tradeRequests.containsKey(requesterId) || !tradeRequests.get(requesterId).equals(acceptorId)) {
            MessageUtil.sendError(acceptor, "Aucune demande d'échange de " + requester.getName());
            return false;
        }
        
        // Remove request and timeout
        tradeRequests.remove(requesterId);
        BukkitTask timeout = requestTimeouts.remove(requesterId);
        if (timeout != null) {
            timeout.cancel();
        }
        
        // Create trade session
        TradeSession session = new TradeSession(requester, acceptor);
        activeTrades.put(requesterId, session);
        activeTrades.put(acceptorId, session);
        
        // Open trade GUI for both players
        session.openTradeMenu(requester);
        session.openTradeMenu(acceptor);
        
        MessageUtil.sendSuccess(requester, "Échange accepté! Menu ouvert.");
        MessageUtil.sendSuccess(acceptor, "Échange accepté! Menu ouvert.");
        
        return true;
    }
    
    /**
     * Deny a trade request
     */
    public boolean denyTradeRequest(Player denier, Player requester) {
        UUID denierId = denier.getUniqueId();
        UUID requesterId = requester.getUniqueId();
        
        // Check if request exists
        if (!tradeRequests.containsKey(requesterId) || !tradeRequests.get(requesterId).equals(denierId)) {
            MessageUtil.sendError(denier, "Aucune demande d'échange de " + requester.getName());
            return false;
        }
        
        // Remove request and timeout
        tradeRequests.remove(requesterId);
        BukkitTask timeout = requestTimeouts.remove(requesterId);
        if (timeout != null) {
            timeout.cancel();
        }
        
        MessageUtil.sendMessage(denier, MessageUtil.colorize("&cDemande d'échange refusée"));
        MessageUtil.sendError(requester, denier.getName() + " a refusé votre demande d'échange");
        
        return true;
    }
    
    /**
     * Get a player's active trade session
     */
    public TradeSession getTradeSession(UUID playerId) {
        return activeTrades.get(playerId);
    }
    
    /**
     * Cancel a trade session
     */
    public void cancelTrade(UUID playerId, String reason) {
        TradeSession session = activeTrades.remove(playerId);
        if (session == null) return;
        
        // Remove both players
        activeTrades.remove(session.getPlayer1().getUniqueId());
        activeTrades.remove(session.getPlayer2().getUniqueId());
        
        // Cancel any countdown
        session.cancelCountdown();
        
        // Return items to both players
        session.returnItems();
        
        // Close inventories
        session.getPlayer1().closeInventory();
        session.getPlayer2().closeInventory();
        
        // Notify players
        MessageUtil.sendError(session.getPlayer1(), "Échange annulé: " + reason);
        MessageUtil.sendError(session.getPlayer2(), "Échange annulé: " + reason);
        
        session.getPlayer1().playSound(session.getPlayer1().getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        session.getPlayer2().playSound(session.getPlayer2().getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
    }
    
    /**
     * Complete a trade
     */
    public void completeTrade(TradeSession session) {
        Player player1 = session.getPlayer1();
        Player player2 = session.getPlayer2();
        
        // Remove from active trades
        activeTrades.remove(player1.getUniqueId());
        activeTrades.remove(player2.getUniqueId());
        
        // Transfer items
        for (ItemStack item : session.getPlayer1Items()) {
            if (item != null && item.getType() != Material.AIR) {
                player2.getInventory().addItem(item);
            }
        }
        
        for (ItemStack item : session.getPlayer2Items()) {
            if (item != null && item.getType() != Material.AIR) {
                player1.getInventory().addItem(item);
            }
        }
        
        // Transfer money
        EconomyManager eco = Survival.getInstance().getEconomyManager();
        double money1 = session.getPlayer1Money();
        double money2 = session.getPlayer2Money();
        String currency = Survival.getInstance().getConfig().getString("economy.currency-symbol", "⛁");
        
        if (money1 > 0) {
            eco.removeBalance(player1.getUniqueId(), money1);
            eco.addBalance(player2.getUniqueId(), money1);
            eco.addTransaction(player1.getUniqueId(), TransactionType.TRANSFER_SEND, -money1, 
                "Échange avec " + player2.getName());
            eco.addTransaction(player2.getUniqueId(), TransactionType.TRANSFER_RECEIVE, money1, 
                "Échange avec " + player1.getName());
        }
        
        if (money2 > 0) {
            eco.removeBalance(player2.getUniqueId(), money2);
            eco.addBalance(player1.getUniqueId(), money2);
            eco.addTransaction(player2.getUniqueId(), TransactionType.TRANSFER_SEND, -money2, 
                "Échange avec " + player1.getName());
            eco.addTransaction(player1.getUniqueId(), TransactionType.TRANSFER_RECEIVE, money2, 
                "Échange avec " + player2.getName());
        }
        
        // Close inventories
        player1.closeInventory();
        player2.closeInventory();
        
        // Notify players
        player1.sendMessage("");
        player1.sendMessage(MessageUtil.colorize("&a&l━━━━━━━━━━━━━━━━━━━━━━━━"));
        player1.sendMessage(MessageUtil.colorize("&a&l  ÉCHANGE RÉUSSI"));
        player1.sendMessage(MessageUtil.colorize("&a&l━━━━━━━━━━━━━━━━━━━━━━━━"));
        player1.sendMessage(MessageUtil.colorize("  &7Échangé avec: &e" + player2.getName()));
        if (money2 > 0) {
            player1.sendMessage(MessageUtil.colorize("  &7Argent reçu: &a+" + money2 + " " + currency));
        }
        if (money1 > 0) {
            player1.sendMessage(MessageUtil.colorize("  &7Argent donné: &c-" + money1 + " " + currency));
        }
        player1.sendMessage(MessageUtil.colorize("&a&l━━━━━━━━━━━━━━━━━━━━━━━━"));
        player1.sendMessage("");
        
        player2.sendMessage("");
        player2.sendMessage(MessageUtil.colorize("&a&l━━━━━━━━━━━━━━━━━━━━━━━━"));
        player2.sendMessage(MessageUtil.colorize("&a&l  ÉCHANGE RÉUSSI"));
        player2.sendMessage(MessageUtil.colorize("&a&l━━━━━━━━━━━━━━━━━━━━━━━━"));
        player2.sendMessage(MessageUtil.colorize("  &7Échangé avec: &e" + player1.getName()));
        if (money1 > 0) {
            player2.sendMessage(MessageUtil.colorize("  &7Argent reçu: &a+" + money1 + " " + currency));
        }
        if (money2 > 0) {
            player2.sendMessage(MessageUtil.colorize("  &7Argent donné: &c-" + money2 + " " + currency));
        }
        player2.sendMessage(MessageUtil.colorize("&a&l━━━━━━━━━━━━━━━━━━━━━━━━"));
        player2.sendMessage("");
        
        player1.playSound(player1.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        player2.playSound(player2.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
    }
    
    /**
     * Clean up when a player quits
     */
    public void onPlayerQuit(Player player) {
        UUID playerId = player.getUniqueId();
        
        // Cancel any pending requests
        tradeRequests.remove(playerId);
        BukkitTask timeout = requestTimeouts.remove(playerId);
        if (timeout != null) {
            timeout.cancel();
        }
        
        // Cancel active trade
        if (activeTrades.containsKey(playerId)) {
            cancelTrade(playerId, "Un joueur s'est déconnecté");
        }
    }
}
