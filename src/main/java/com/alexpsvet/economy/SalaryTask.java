package com.alexpsvet.economy;

import com.alexpsvet.Survival;
import com.alexpsvet.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

/**
 * Task to pay salaries to online players
 */
public class SalaryTask extends BukkitRunnable {
    private final EconomyManager economyManager;
    private final double salaryAmount;
    private final long intervalMillis;
    
    public SalaryTask(EconomyManager economyManager) {
        this.economyManager = economyManager;
        this.salaryAmount = Survival.getInstance().getConfig().getDouble("economy.salary.amount", 50.0);
        int intervalMinutes = Survival.getInstance().getConfig().getInt("economy.salary.interval-minutes", 30);
        this.intervalMillis = intervalMinutes * 60 * 1000L;
    }
    
    @Override
    public void run() {
        long now = System.currentTimeMillis();
        String currencySymbol = Survival.getInstance().getConfig().getString("economy.currency-symbol", "⛁");
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            long lastSalary = economyManager.getLastSalary(uuid);
            
            if (now - lastSalary >= intervalMillis) {
                economyManager.addBalance(uuid, salaryAmount);
                economyManager.updateLastSalary(uuid);
                economyManager.addTransaction(uuid, TransactionType.SALARY, salaryAmount, "Salary payment");
                
                MessageUtil.sendSuccess(player, 
                    MessageUtil.format("Vous avez reçu votre salaire: &e{amount} {currency}",
                        "{amount}", String.format("%.2f", salaryAmount),
                        "{currency}", currencySymbol));
            }
        }
    }
}
