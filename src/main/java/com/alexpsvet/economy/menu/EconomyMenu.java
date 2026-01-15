package com.alexpsvet.economy.menu;

import com.alexpsvet.Survival;
import com.alexpsvet.economy.EconomyManager;
import com.alexpsvet.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

/**
 * Menu for economy/balance information
 */
public class EconomyMenu {
    
    /**
     * Open the economy menu for a player
     */
    public static void open(Player player) {
        EconomyManager economyManager = EconomyManager.getInstance();
        double balance = economyManager.getBalance(player.getUniqueId());
        String currency = Survival.getInstance().getConfig().getString("economy.currency-name", "Coins");
        String symbol = Survival.getInstance().getConfig().getString("economy.currency-symbol", "⛁");
        
        Inventory inv = Bukkit.createInventory(null, 27, MessageUtil.colorize("&6&lÉconomie"));
        
        // Balance display
        ItemStack balanceItem = createItem(Material.GOLD_INGOT, "&e&lVotre Solde",
            "&7Solde actuel: &e" + String.format("%.2f", balance) + " " + symbol,
            "",
            "&7Monnaie: &e" + currency
        );
        inv.setItem(13, balanceItem);
        
        // Salary info
        boolean salaryEnabled = Survival.getInstance().getConfig().getBoolean("economy.salary.enabled");
        if (salaryEnabled) {
            double salaryAmount = Survival.getInstance().getConfig().getDouble("economy.salary.amount");
            int salaryInterval = Survival.getInstance().getConfig().getInt("economy.salary.interval-minutes");
            long lastSalary = economyManager.getLastSalary(player.getUniqueId());
            long nextSalary = lastSalary + (salaryInterval * 60 * 1000L);
            long timeUntilNext = Math.max(0, nextSalary - System.currentTimeMillis());
            int minutesUntil = (int) (timeUntilNext / 60000);
            
            ItemStack salaryItem = createItem(Material.CLOCK, "&a&lSalaire",
                "&7Montant: &e" + String.format("%.2f", salaryAmount) + " " + symbol,
                "&7Fréquence: &eToutes les " + salaryInterval + " minutes",
                "",
                "&7Prochain salaire dans: &e" + minutesUntil + " minutes"
            );
            inv.setItem(11, salaryItem);
        }
        
        // Pay another player
        ItemStack payItem = createItem(Material.EMERALD, "&a&lEnvoyer de l'argent",
            "&7Transférer de l'argent à",
            "&7un autre joueur",
            "",
            "&eUtilisez: /pay <joueur> <montant>"
        );
        inv.setItem(15, payItem);
        
        // Top balances
        ItemStack topItem = createItem(Material.DIAMOND, "&b&lClassement",
            "&7Voir les joueurs les plus riches",
            "",
            "&eUtilisez: /baltop"
        );
        inv.setItem(22, topItem);
        
        player.openInventory(inv);
    }
    
    /**
     * Create an item with name and lore
     */
    private static ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(MessageUtil.colorize(name));
            meta.setLore(MessageUtil.colorize(Arrays.asList(lore)));
            item.setItemMeta(meta);
        }
        return item;
    }
}
