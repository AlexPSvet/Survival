package com.alexpsvet.gamble;

import com.alexpsvet.Survival;
import com.alexpsvet.economy.EconomyManager;
import com.alexpsvet.economy.TransactionType;
import com.alexpsvet.utils.MessageUtil;
import com.alexpsvet.utils.menu.Button;
import com.alexpsvet.utils.menu.Menu;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Coin flip gambling menu (Pile ou Face)
 */
public class GambleMenu {
    
    private static final Map<UUID, Double> activeBets = new HashMap<>();
    private static final Map<UUID, CoinSide> activeChoices = new HashMap<>();
    
    public enum CoinSide {
        PILE("Pile", "⛏"),
        FACE("Face", "😊");
        
        private final String displayName;
        private final String symbol;
        
        CoinSide(String displayName, String symbol) {
            this.displayName = displayName;
            this.symbol = symbol;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public String getSymbol() {
            return symbol;
        }
    }
    
    /**
     * Open main gamble menu
     */
    public static void openMainMenu(Player player) {
        Menu.Builder builder = new Menu.Builder()
            .title(MessageUtil.colorize("&6&lPile ou Face"))
            .rows(5);
        
        String currency = Survival.getInstance().getConfig().getString("economy.currency-symbol", "⛁");
        EconomyManager eco = Survival.getInstance().getEconomyManager();
        double balance = eco.getBalance(player.getUniqueId());
        
        // Info item
        ItemStack info = new ItemStack(Material.BOOK);
        List<String> infoLore = new ArrayList<>();
        infoLore.add(MessageUtil.colorize("&7Pariez sur Pile ou Face"));
        infoLore.add(MessageUtil.colorize("&7et doublez votre mise!"));
        infoLore.add("");
        infoLore.add(MessageUtil.colorize("&7Votre solde: &e" + balance + " " + currency));
        infoLore.add("");
        infoLore.add(MessageUtil.colorize("&e&lChoisissez un montant à parier"));
        
        builder.button(new Button.Builder()
            .slot(4)
            .item(info)
            .name(MessageUtil.colorize("&6&lRègles du Jeu"))
            .lore(infoLore)
            .build());
        
        // Bet amount options
        double[] betAmounts = {10, 50, 100, 500, 1000, 5000, 10000, 25000, 50000};
        int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20};
        
        for (int i = 0; i < betAmounts.length; i++) {
            if (i >= slots.length) break;
            
            final double amount = betAmounts[i];
            Material material = amount <= 100 ? Material.GOLD_NUGGET :
                               amount <= 1000 ? Material.GOLD_INGOT :
                               amount <= 10000 ? Material.GOLD_BLOCK :
                               Material.DIAMOND;
            
            ItemStack item = new ItemStack(material);
            List<String> lore = new ArrayList<>();
            lore.add(MessageUtil.colorize("&7Pari: &e" + amount + " " + currency));
            lore.add("");
            
            if (balance >= amount) {
                lore.add(MessageUtil.colorize("&aGain possible: &e" + (amount * 2) + " " + currency));
                lore.add("");
                lore.add(MessageUtil.colorize("&eCliquez pour parier!"));
            } else {
                lore.add(MessageUtil.colorize("&cFonds insuffisants"));
            }
            
            builder.button(new Button.Builder()
                .slot(slots[i])
                .item(item)
                .name(MessageUtil.colorize("&6&lParier " + amount + " " + currency))
                .lore(lore)
                .onClick((p, clickType) -> {
                    if (balance >= amount) {
                        activeBets.put(p.getUniqueId(), amount);
                        openChoiceMenu(p);
                    } else {
                        p.sendMessage(MessageUtil.colorize("&cVous n'avez pas assez d'argent!"));
                    }
                })
                .build());
        }
        
        // Close button
        ItemStack close = new ItemStack(Material.BARRIER);
        builder.button(new Button.Builder()
            .slot(40)
            .item(close)
            .name(MessageUtil.colorize("&cFermer"))
            .onClick((p, clickType) -> {
                activeBets.remove(p.getUniqueId());
                activeChoices.remove(p.getUniqueId());
                p.closeInventory();
            })
            .build());
        
        builder.build().open(player);
    }
    
    /**
     * Open choice menu (Pile or Face)
     */
    public static void openChoiceMenu(Player player) {
        Double betAmount = activeBets.get(player.getUniqueId());
        if (betAmount == null) {
            openMainMenu(player);
            return;
        }
        
        Menu.Builder builder = new Menu.Builder()
            .title(MessageUtil.colorize("&6&lChoisissez!"))
            .rows(3);
        
        String currency = Survival.getInstance().getConfig().getString("economy.currency-symbol", "⛁");
        
        // Pile choice
        ItemStack pile = new ItemStack(Material.IRON_BLOCK);
        List<String> pileLore = new ArrayList<>();
        pileLore.add(MessageUtil.colorize("&7Pari: &e" + betAmount + " " + currency));
        pileLore.add("");
        pileLore.add(MessageUtil.colorize("&eCliquez pour choisir PILE!"));
        
        builder.button(new Button.Builder()
            .slot(11)
            .item(pile)
            .name(MessageUtil.colorize("&7&l⛏ PILE"))
            .lore(pileLore)
            .onClick((p, clickType) -> {
                activeChoices.put(p.getUniqueId(), CoinSide.PILE);
                playGame(p);
            })
            .build());
        
        // Face choice
        ItemStack face = new ItemStack(Material.GOLD_BLOCK);
        List<String> faceLore = new ArrayList<>();
        faceLore.add(MessageUtil.colorize("&7Pari: &e" + betAmount + " " + currency));
        faceLore.add("");
        faceLore.add(MessageUtil.colorize("&eCliquez pour choisir FACE!"));
        
        builder.button(new Button.Builder()
            .slot(15)
            .item(face)
            .name(MessageUtil.colorize("&6&l😊 FACE"))
            .lore(faceLore)
            .onClick((p, clickType) -> {
                activeChoices.put(p.getUniqueId(), CoinSide.FACE);
                playGame(p);
            })
            .build());
        
        // Back button
        ItemStack back = new ItemStack(Material.ARROW);
        builder.button(new Button.Builder()
            .slot(22)
            .item(back)
            .name(MessageUtil.colorize("&cRetour"))
            .onClick((p, clickType) -> {
                activeBets.remove(p.getUniqueId());
                activeChoices.remove(p.getUniqueId());
                openMainMenu(p);
            })
            .build());
        
        builder.build().open(player);
    }
    
    /**
     * Play the game and show result
     */
    private static void playGame(Player player) {
        Double betAmount = activeBets.get(player.getUniqueId());
        CoinSide choice = activeChoices.get(player.getUniqueId());
        
        if (betAmount == null || choice == null) {
            openMainMenu(player);
            return;
        }
        
        EconomyManager eco = Survival.getInstance().getEconomyManager();
        
        // Check balance
        if (eco.getBalance(player.getUniqueId()) < betAmount) {
            player.sendMessage(MessageUtil.colorize("&cVous n'avez pas assez d'argent!"));
            activeBets.remove(player.getUniqueId());
            activeChoices.remove(player.getUniqueId());
            player.closeInventory();
            return;
        }
        
        // Deduct bet
        eco.removeBalance(player.getUniqueId(), betAmount);
        
        // Show animation menu
        showFlippingAnimation(player, betAmount, choice);
    }
    
    /**
     * Show coin flipping animation
     */
    private static void showFlippingAnimation(Player player, double betAmount, CoinSide choice) {
        String currency = Survival.getInstance().getConfig().getString("economy.currency-symbol", "⛁");
        
        // Close current menu
        player.closeInventory();
        
        // Animation messages
        player.sendMessage(MessageUtil.colorize("&e&l━━━━━━━━━━━━━━━━━━━━━━━━"));
        player.sendMessage(MessageUtil.colorize("&6&l  PILE OU FACE"));
        player.sendMessage(MessageUtil.colorize("&e&l━━━━━━━━━━━━━━━━━━━━━━━━"));
        player.sendMessage("");
        player.sendMessage(MessageUtil.colorize("&7Votre choix: &e" + choice.getDisplayName()));
        player.sendMessage(MessageUtil.colorize("&7Pari: &e" + betAmount + " " + currency));
        player.sendMessage("");
        player.sendMessage(MessageUtil.colorize("&e⟳ La pièce tourne..."));
        
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
        
        // Flip animation
        Bukkit.getScheduler().runTaskLater(Survival.getInstance(), () -> {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.2f);
        }, 10L);
        
        Bukkit.getScheduler().runTaskLater(Survival.getInstance(), () -> {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.4f);
        }, 20L);
        
        // Show result after 2 seconds
        Bukkit.getScheduler().runTaskLater(Survival.getInstance(), () -> {
            showResult(player, betAmount, choice);
        }, 40L);
    }
    
    /**
     * Show result of the coin flip
     */
    private static void showResult(Player player, double betAmount, CoinSide choice) {
        String currency = Survival.getInstance().getConfig().getString("economy.currency-symbol", "⛁");
        EconomyManager eco = Survival.getInstance().getEconomyManager();
        
        // Random result
        CoinSide result = new Random().nextBoolean() ? CoinSide.PILE : CoinSide.FACE;
        boolean won = result == choice;
        
        player.sendMessage("");
        player.sendMessage(MessageUtil.colorize("&e&l━━━━━━━━━━━━━━━━━━━━━━━━"));
        player.sendMessage(MessageUtil.colorize("&7Résultat: &6&l" + result.getDisplayName() + " " + result.getSymbol()));
        player.sendMessage("");
        
        if (won) {
            double winAmount = betAmount * 2;
            eco.addBalance(player.getUniqueId(), winAmount);
            
            player.sendMessage(MessageUtil.colorize("&a&l✓ GAGNÉ!"));
            player.sendMessage(MessageUtil.colorize("&7Gain: &a+" + winAmount + " " + currency));
            player.sendMessage(MessageUtil.colorize("&7Profit: &a+" + betAmount + " " + currency));
            
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            
            // Firework effect
            player.getWorld().spawnParticle(
                org.bukkit.Particle.FIREWORKS_SPARK,
                player.getLocation().add(0, 2, 0),
                50, 0.5, 0.5, 0.5, 0.1
            );
        } else {
            player.sendMessage(MessageUtil.colorize("&c&l✗ PERDU!"));
            player.sendMessage(MessageUtil.colorize("&7Perte: &c-" + betAmount + " " + currency));
            
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
        
        player.sendMessage("");
        player.sendMessage(MessageUtil.colorize("&7Nouveau solde: &e" + eco.getBalance(player.getUniqueId()) + " " + currency));
        player.sendMessage(MessageUtil.colorize("&e&l━━━━━━━━━━━━━━━━━━━━━━━━"));
        
        // Clean up
        activeBets.remove(player.getUniqueId());
        activeChoices.remove(player.getUniqueId());
        
        // Reopen menu after 3 seconds
        Bukkit.getScheduler().runTaskLater(Survival.getInstance(), () -> {
            if (player.isOnline()) {
                openMainMenu(player);
            }
        }, 60L);
    }
}
