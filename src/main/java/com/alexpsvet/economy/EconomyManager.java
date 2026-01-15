package com.alexpsvet.economy;

import com.alexpsvet.Survival;
import com.alexpsvet.database.Database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Economy manager for player balances
 */
public class EconomyManager {
    private static final Logger LOGGER = Logger.getLogger("survival");
    private static EconomyManager instance;
    private final Database database;
    private final Map<UUID, Double> balanceCache;
    private final double startingBalance;
    
    public EconomyManager(Database database) {
        instance = this;
        this.database = database;
        this.balanceCache = new HashMap<>();
        this.startingBalance = Survival.getInstance().getConfig().getDouble("economy.starting-balance", 1000.0);
        createTables();
    }
    
    /**
     * Create the economy tables
     */
    private void createTables() {
        String query = "CREATE TABLE IF NOT EXISTS economy_players (" +
                "uuid VARCHAR(36) PRIMARY KEY," +
                "player_name VARCHAR(16) NOT NULL," +
                "balance DOUBLE NOT NULL DEFAULT 0," +
                "last_salary BIGINT NOT NULL DEFAULT 0," +
                "total_earned DOUBLE NOT NULL DEFAULT 0," +
                "total_spent DOUBLE NOT NULL DEFAULT 0," +
                "created_at BIGINT NOT NULL" +
                ")";
        database.executeUpdate(query);
        
        String transactionsQuery = "CREATE TABLE IF NOT EXISTS economy_transactions (" +
                "id INTEGER PRIMARY KEY " + (database.getType().name().equals("SQLITE") ? "AUTOINCREMENT" : "AUTO_INCREMENT") + "," +
                "uuid VARCHAR(36) NOT NULL," +
                "type VARCHAR(20) NOT NULL," +
                "amount DOUBLE NOT NULL," +
                "description TEXT," +
                "timestamp BIGINT NOT NULL" +
                ")";
        database.executeUpdate(transactionsQuery);
        
        LOGGER.info("Economy tables created/verified");
    }
    
    /**
     * Get a player's balance
     * @param uuid The player's UUID
     * @return The balance
     */
    public double getBalance(UUID uuid) {
        if (balanceCache.containsKey(uuid)) {
            return balanceCache.get(uuid);
        }
        
        ResultSet rs = database.executeQuery("SELECT balance FROM economy_players WHERE uuid = ?", uuid.toString());
        try {
            if (rs != null && rs.next()) {
                double balance = rs.getDouble("balance");
                balanceCache.put(uuid, balance);
                rs.close();
                return balance;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to get balance for " + uuid, e);
        }
        
        return 0.0;
    }
    
    /**
     * Set a player's balance
     * @param uuid The player's UUID
     * @param playerName The player's name
     * @param balance The new balance
     */
    public void setBalance(UUID uuid, String playerName, double balance) {
        balanceCache.put(uuid, balance);
        
        int result = database.executeUpdate(
            "UPDATE economy_players SET balance = ? WHERE uuid = ?",
            balance, uuid.toString()
        );
        
        if (result == 0) {
            // Player doesn't exist, create entry
            database.executeUpdate(
                "INSERT INTO economy_players (uuid, player_name, balance, created_at) VALUES (?, ?, ?, ?)",
                uuid.toString(), playerName, balance, System.currentTimeMillis()
            );
        }
    }
    
    /**
     * Create a player account with starting balance
     * @param uuid The player's UUID
     * @param playerName The player's name
     */
    public void createAccount(UUID uuid, String playerName) {
        if (!hasAccount(uuid)) {
            setBalance(uuid, playerName, startingBalance);
            addTransaction(uuid, TransactionType.STARTING_BALANCE, startingBalance, "Starting balance");
            LOGGER.info("Created economy account for " + playerName);
        }
    }
    
    /**
     * Check if a player has an account
     * @param uuid The player's UUID
     * @return true if the player has an account
     */
    public boolean hasAccount(UUID uuid) {
        ResultSet rs = database.executeQuery("SELECT uuid FROM economy_players WHERE uuid = ?", uuid.toString());
        try {
            if (rs != null && rs.next()) {
                rs.close();
                return true;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to check account for " + uuid, e);
        }
        return false;
    }
    
    /**
     * Add money to a player's balance
     * @param uuid The player's UUID
     * @param amount The amount to add
     */
    public void addBalance(UUID uuid, double amount) {
        double current = getBalance(uuid);
        double newBalance = current + amount;
        balanceCache.put(uuid, newBalance);
        
        database.executeUpdate(
            "UPDATE economy_players SET balance = balance + ?, total_earned = total_earned + ? WHERE uuid = ?",
            amount, amount, uuid.toString()
        );
    }
    
    /**
     * Remove money from a player's balance
     * @param uuid The player's UUID
     * @param amount The amount to remove
     * @return true if successful (player had enough money)
     */
    public boolean removeBalance(UUID uuid, double amount) {
        double current = getBalance(uuid);
        if (current < amount) {
            return false;
        }
        
        double newBalance = current - amount;
        balanceCache.put(uuid, newBalance);
        
        database.executeUpdate(
            "UPDATE economy_players SET balance = balance - ?, total_spent = total_spent + ? WHERE uuid = ?",
            amount, amount, uuid.toString()
        );
        
        return true;
    }
    
    /**
     * Transfer money from one player to another
     * @param from The sender's UUID
     * @param to The receiver's UUID
     * @param amount The amount to transfer
     * @return true if successful
     */
    public boolean transfer(UUID from, UUID to, double amount) {
        if (removeBalance(from, amount)) {
            addBalance(to, amount);
            addTransaction(from, TransactionType.TRANSFER_SEND, -amount, "Transfer to " + to);
            addTransaction(to, TransactionType.TRANSFER_RECEIVE, amount, "Transfer from " + from);
            return true;
        }
        return false;
    }
    
    /**
     * Add a transaction to the history
     * @param uuid The player's UUID
     * @param type The transaction type
     * @param amount The amount
     * @param description The description
     */
    public void addTransaction(UUID uuid, TransactionType type, double amount, String description) {
        database.executeUpdate(
            "INSERT INTO economy_transactions (uuid, type, amount, description, timestamp) VALUES (?, ?, ?, ?, ?)",
            uuid.toString(), type.name(), amount, description, System.currentTimeMillis()
        );
    }
    
    /**
     * Get the last salary timestamp for a player
     * @param uuid The player's UUID
     * @return The timestamp in milliseconds
     */
    public long getLastSalary(UUID uuid) {
        ResultSet rs = database.executeQuery("SELECT last_salary FROM economy_players WHERE uuid = ?", uuid.toString());
        try {
            if (rs != null && rs.next()) {
                long timestamp = rs.getLong("last_salary");
                rs.close();
                return timestamp;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to get last salary for " + uuid, e);
        }
        return 0;
    }
    
    /**
     * Update the last salary timestamp for a player
     * @param uuid The player's UUID
     */
    public void updateLastSalary(UUID uuid) {
        database.executeUpdate(
            "UPDATE economy_players SET last_salary = ? WHERE uuid = ?",
            System.currentTimeMillis(), uuid.toString()
        );
    }
    
    /**
     * Clear the balance cache
     */
    public void clearCache() {
        balanceCache.clear();
    }
    
    /**
     * Get the economy manager instance
     * @return the instance
     */
    public static EconomyManager getInstance() {
        return instance;
    }
}
