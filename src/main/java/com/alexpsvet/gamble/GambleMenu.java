package com.alexpsvet.gamble;

import com.alexpsvet.Survival;
import com.alexpsvet.economy.EconomyManager;
import com.alexpsvet.economy.TransactionType;
import com.alexpsvet.gamble.blackjack.BlackjackGame;
import com.alexpsvet.gamble.blackjack.Card;
import com.alexpsvet.gamble.blackjack.Hand;
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
 * Gambling menus (Coin flip and Blackjack)
 */
public class GambleMenu {
    
    private static final Map<UUID, Double> activeBets = new HashMap<>();
    private static final Map<UUID, CoinSide> activeChoices = new HashMap<>();
    private static final Map<UUID, BlackjackGame> activeBlackjackGames = new HashMap<>();
    private static final Set<UUID> playersInActiveGame = new HashSet<>(); // Track players actively playing
    
    /**
     * Check if a player is in an active game (prevents menu closing)
     */
    public static boolean isPlayerInActiveGame(UUID playerId) {
        return playersInActiveGame.contains(playerId);
    }
    
    /**
     * Check if a player is in an active game
     */
    public static boolean isPlayerInActiveGame(Player player) {
        return isPlayerInActiveGame(player.getUniqueId());
    }
    
    /**
     * Handle player leaving game (disconnect or force close)
     * Charges them as if they lost
     */
    public static void handlePlayerLeaveGame(Player player) {
        UUID playerId = player.getUniqueId();
        
        // Check if player has active blackjack game
        BlackjackGame game = activeBlackjackGames.remove(playerId);
        if (game != null && !game.isGameOver()) {
            // Player left during blackjack game - they lose their bet
            EconomyManager eco = Survival.getInstance().getEconomyManager();
            eco.addTransaction(playerId, TransactionType.GAMBLE_LOSS, -game.getBet(), 
                "Blackjack Loss (Left Game)");
            
            if (player.isOnline()) {
                String currency = Survival.getInstance().getConfig().getString("economy.currency-symbol", "⛁");
                MessageUtil.sendError(player, "Vous avez quitté la partie! Mise perdue: " + 
                    game.getBet() + " " + currency);
            }
        }
        
        // Clean up all game data
        activeBets.remove(playerId);
        activeChoices.remove(playerId);
        playersInActiveGame.remove(playerId);
    }
    
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
     * Open main gamble menu (game selection)
     */
    public static void openMainMenu(Player player) {
        Menu.Builder builder = new Menu.Builder()
            .title(MessageUtil.colorize("&6&lCasino Periquito"))
            .rows(4);
        
        String currency = Survival.getInstance().getConfig().getString("economy.currency-symbol", "⛁");
        EconomyManager eco = Survival.getInstance().getEconomyManager();
        double balance = eco.getBalance(player.getUniqueId());
        
        // Info item
        ItemStack info = new ItemStack(Material.NETHER_STAR);
        List<String> infoLore = new ArrayList<>();
        infoLore.add(MessageUtil.colorize("&7Bienvenue au casino!"));
        infoLore.add(MessageUtil.colorize("&7Choisissez votre jeu"));
        infoLore.add("");
        infoLore.add(MessageUtil.colorize("&7Votre solde: &e" + balance + " " + currency));
        infoLore.add("");
        infoLore.add(MessageUtil.colorize("&e&lBonne chance!"));
        
        builder.button(new Button.Builder()
            .slot(4)
            .item(info)
            .name(MessageUtil.colorize("&6&lCasino"))
            .lore(infoLore)
            .build());
        
        // Coin Flip
        ItemStack coinFlip = new ItemStack(Material.GOLD_INGOT);
        List<String> coinLore = new ArrayList<>();
        coinLore.add(MessageUtil.colorize("&7Pariez sur Pile ou Face"));
        coinLore.add(MessageUtil.colorize("&7et doublez votre mise!"));
        coinLore.add("");
        coinLore.add(MessageUtil.colorize("&aGain: &e2x votre mise"));
        coinLore.add("");
        coinLore.add(MessageUtil.colorize("&eCliquez pour jouer!"));
        
        builder.button(new Button.Builder()
            .slot(11)
            .item(coinFlip)
            .name(MessageUtil.colorize("&6&lPile ou Face"))
            .lore(coinLore)
            .onClick((p, clickType) -> openCoinFlipMenu(p))
            .build());
        
        // Blackjack
        ItemStack blackjack = new ItemStack(Material.BOOK);
        List<String> bjLore = new ArrayList<>();
        bjLore.add(MessageUtil.colorize("&7Battez le dealer au Blackjack!"));
        bjLore.add(MessageUtil.colorize("&7Obtenez 21 ou proche"));
        bjLore.add("");
        bjLore.add(MessageUtil.colorize("&aBlackjack: &e2.5x votre mise"));
        bjLore.add(MessageUtil.colorize("&aVictoire: &e2x votre mise"));
        bjLore.add("");
        bjLore.add(MessageUtil.colorize("&eCliquez pour jouer!"));
        
        builder.button(new Button.Builder()
            .slot(15)
            .item(blackjack)
            .name(MessageUtil.colorize("&0&lBlackjack"))
            .lore(bjLore)
            .onClick((p, clickType) -> openBlackjackBetMenu(p))
            .build());
        
        // Close button
        ItemStack close = new ItemStack(Material.BARRIER);
        builder.button(new Button.Builder()
            .slot(31)
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
     * Open coin flip bet menu
     */
    public static void openCoinFlipMenu(Player player) {
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
        
        // Back button
        ItemStack back = new ItemStack(Material.ARROW);
        builder.button(new Button.Builder()
            .slot(40)
            .item(back)
            .name(MessageUtil.colorize("&cRetour"))
            .onClick((p, clickType) -> openMainMenu(p))
            .build());
        
        builder.build().open(player);
    }
    
    /**
     * Open choice menu (Pile or Face)
     */
    public static void openChoiceMenu(Player player) {
        Double betAmount = activeBets.get(player.getUniqueId());
        if (betAmount == null) {
            openCoinFlipMenu(player);
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
                openCoinFlipMenu(p);
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
            openCoinFlipMenu(player);
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
            
            eco.addTransaction(player.getUniqueId(), TransactionType.GAMBLE_WIN, betAmount, "Coinflip Win");
        } else {
            player.sendMessage(MessageUtil.colorize("&c&l✗ PERDU!"));
            player.sendMessage(MessageUtil.colorize("&7Perte: &c-" + betAmount + " " + currency));
            
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            
            eco.addTransaction(player.getUniqueId(), TransactionType.GAMBLE_LOSS, -betAmount, "Coinflip Loss");
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
    
    // ==================== BLACKJACK ====================
    
    /**
     * Open blackjack bet selection menu
     */
    public static void openBlackjackBetMenu(Player player) {
        Menu.Builder builder = new Menu.Builder()
            .title(MessageUtil.colorize("&c&lBlackjack - Mise"))
            .rows(5);
        
        String currency = Survival.getInstance().getConfig().getString("economy.currency-symbol", "⛁");
        EconomyManager eco = Survival.getInstance().getEconomyManager();
        double balance = eco.getBalance(player.getUniqueId());
        
        // Info item
        ItemStack info = new ItemStack(Material.BOOK);
        List<String> infoLore = new ArrayList<>();
        infoLore.add(MessageUtil.colorize("&7Votre solde: &e" + balance + " " + currency));
        infoLore.add("");
        infoLore.add(MessageUtil.colorize("&7Blackjack: &ax2.5"));
        infoLore.add(MessageUtil.colorize("&7Victoire: &ax2"));
        infoLore.add("");
        infoLore.add(MessageUtil.colorize("&e&lChoisissez votre mise"));
        
        builder.button(new Button.Builder()
            .slot(4)
            .item(info)
            .name(MessageUtil.colorize("&c&lBlackjack"))
            .lore(infoLore)
            .build());
        
        // Bet amounts
        double[] betAmounts = {100, 500, 1000, 5000, 10000, 25000, 50000};
        int[] slots = {10, 11, 12, 13, 14, 15, 16};
        
        for (int i = 0; i < betAmounts.length && i < slots.length; i++) {
            final double amount = betAmounts[i];
            Material material = amount <= 1000 ? Material.GOLD_NUGGET :
                               amount <= 10000 ? Material.GOLD_INGOT :
                               Material.DIAMOND;
            
            ItemStack item = new ItemStack(material);
            List<String> lore = new ArrayList<>();
            lore.add(MessageUtil.colorize("&7Mise: &e" + amount + " " + currency));
            lore.add("");
            
            if (balance >= amount) {
                lore.add(MessageUtil.colorize("&aGain possible: &e" + (amount * 2) + " " + currency));
                lore.add(MessageUtil.colorize("&aBlackjack: &e" + (amount * 2.5) + " " + currency));
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
                        startBlackjackGame(p, amount);
                    } else {
                        p.sendMessage(MessageUtil.colorize("&cVous n'avez pas assez d'argent!"));
                    }
                })
                .build());
        }
        
        // Back button
        ItemStack back = new ItemStack(Material.ARROW);
        builder.button(new Button.Builder()
            .slot(40)
            .item(back)
            .name(MessageUtil.colorize("&cRetour"))
            .onClick((p, clickType) -> openMainMenu(p))
            .build());
        
        builder.build().open(player);
    }
    
    /**
     * Start a new blackjack game
     */
    private static void startBlackjackGame(Player player, double bet) {
        EconomyManager eco = Survival.getInstance().getEconomyManager();
        
        // Check balance
        if (eco.getBalance(player.getUniqueId()) < bet) {
            player.sendMessage(MessageUtil.colorize("&cVous n'avez pas assez d'argent!"));
            return;
        }
        
        // Deduct bet
        eco.removeBalance(player.getUniqueId(), bet);
        eco.addTransaction(player.getUniqueId(), TransactionType.GAMBLE_BET, -bet, "Blackjack Bet");
        
        // Create game
        BlackjackGame game = new BlackjackGame(bet);
        activeBlackjackGames.put(player.getUniqueId(), game);
        
        // Check for immediate blackjack
        if (game.getState() == BlackjackGame.GameState.PLAYER_BLACKJACK) {
            showBlackjackResult(player, game);
        } else {
            openBlackjackGameMenu(player);
        }

        playersInActiveGame.add(player.getUniqueId()); // Mark player as in active game
    }
    
    /**
     * Open blackjack game menu
     */
    private static void openBlackjackGameMenu(Player player) {
        BlackjackGame game = activeBlackjackGames.get(player.getUniqueId());
        if (game == null) {
            openMainMenu(player);
            return;
        }
        
        String currency = Survival.getInstance().getConfig().getString("economy.currency-symbol", "⛁");
        Hand playerHand = game.getPlayerHand();
        Hand dealerHand = game.getDealerHand();
        
        Menu.Builder builder = new Menu.Builder()
            .title(MessageUtil.colorize("&c&lBlackjack - Jeu"))
            .rows(5);
        
        // Player hand display
        ItemStack playerHandItem = new ItemStack(Material.PLAYER_HEAD);
        List<String> playerLore = new ArrayList<>();
        playerLore.add(MessageUtil.colorize("&7Cartes:"));
        for (Card card : playerHand.getCards()) {
            playerLore.add(MessageUtil.colorize("&f  " + card.toString() + " &7- &e" + card.getDisplayName()));
        }
        playerLore.add("");
        playerLore.add(MessageUtil.colorize("&7Total: &a" + playerHand.getValue()));
        
        builder.button(new Button.Builder()
            .slot(11)
            .item(playerHandItem)
            .name(MessageUtil.colorize("&a&lVotre Main"))
            .lore(playerLore)
            .build());
        
        // Dealer hand display (hide second card if game ongoing)
        ItemStack dealerHandItem = new ItemStack(Material.SKELETON_SKULL);
        List<String> dealerLore = new ArrayList<>();
        dealerLore.add(MessageUtil.colorize("&7Cartes:"));
        List<Card> dealerCards = dealerHand.getCards();
        if (game.isGameOver()) {
            // Show all cards
            for (Card card : dealerCards) {
                dealerLore.add(MessageUtil.colorize("&f  " + card.toString() + " &7- &e" + card.getDisplayName()));
            }
            dealerLore.add("");
            dealerLore.add(MessageUtil.colorize("&7Total: &c" + dealerHand.getValue()));
        } else {
            // Hide second card
            dealerLore.add(MessageUtil.colorize("&f  " + dealerCards.get(0).toString() + " &7- &e" + dealerCards.get(0).getDisplayName()));
            dealerLore.add(MessageUtil.colorize("&f  ?? &7- &cCachée"));
            dealerLore.add("");
            dealerLore.add(MessageUtil.colorize("&7Total: &c??"));
        }
        
        builder.button(new Button.Builder()
            .slot(15)
            .item(dealerHandItem)
            .name(MessageUtil.colorize("&c&lMain du Dealer"))
            .lore(dealerLore)
            .build());
        
        // Info/Bet display
        ItemStack info = new ItemStack(Material.GOLD_INGOT);
        List<String> infoLore = new ArrayList<>();
        infoLore.add(MessageUtil.colorize("&7Mise: &e" + game.getBet() + " " + currency));
        infoLore.add("");
        infoLore.add(MessageUtil.colorize("&7État: &e" + getStateDisplay(game.getState())));
        
        builder.button(new Button.Builder()
            .slot(4)
            .item(info)
            .name(MessageUtil.colorize("&6&lInformation"))
            .lore(infoLore)
            .build());
        
        if (!game.isGameOver()) {
            // Hit button
            ItemStack hit = new ItemStack(Material.LIME_WOOL);
            List<String> hitLore = new ArrayList<>();
            hitLore.add(MessageUtil.colorize("&7Tirer une carte"));
            hitLore.add("");
            hitLore.add(MessageUtil.colorize("&eCliquez pour Hit!"));
            
            builder.button(new Button.Builder()
                .slot(30)
                .item(hit)
                .name(MessageUtil.colorize("&a&lHit"))
                .lore(hitLore)
                .onClick((p, clickType) -> {
                    game.hit();
                    p.playSound(p.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f);
                    if (game.isGameOver()) {
                        showBlackjackResult(p, game);
                    } else {
                        openBlackjackGameMenu(p);
                    }
                })
                .build());
            
            // Stand button
            ItemStack stand = new ItemStack(Material.RED_WOOL);
            List<String> standLore = new ArrayList<>();
            standLore.add(MessageUtil.colorize("&7Garder votre main"));
            standLore.add(MessageUtil.colorize("&7Le dealer jouera"));
            standLore.add("");
            standLore.add(MessageUtil.colorize("&eCliquez pour Stand!"));
            
            builder.button(new Button.Builder()
                .slot(32)
                .item(stand)
                .name(MessageUtil.colorize("&c&lStand"))
                .lore(standLore)
                .onClick((p, clickType) -> {
                    game.stand();
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.2f);
                    showBlackjackResult(p, game);
                })
                .build());
        }
        
        builder.build().open(player);
    }
    
    /**
     * Show blackjack result
     */
    private static void showBlackjackResult(Player player, BlackjackGame game) {
        String currency = Survival.getInstance().getConfig().getString("economy.currency-symbol", "⛁");
        EconomyManager eco = Survival.getInstance().getEconomyManager();
        
        player.closeInventory();
        
        // Display cards
        player.sendMessage("");
        player.sendMessage(MessageUtil.colorize("&c&l━━━━━━━━━━━━━━━━━━━━━━━━"));
        player.sendMessage(MessageUtil.colorize("&c&l  BLACKJACK - RÉSULTAT"));
        player.sendMessage(MessageUtil.colorize("&c&l━━━━━━━━━━━━━━━━━━━━━━━━"));
        player.sendMessage("");
        
        Hand playerHand = game.getPlayerHand();
        Hand dealerHand = game.getDealerHand();
        
        player.sendMessage(MessageUtil.colorize("&a&lVotre Main: &f" + playerHand.toString()));
        player.sendMessage(MessageUtil.colorize("&c&lDealer: &f" + dealerHand.toString()));
        player.sendMessage("");
        
        double winnings = game.getWinnings();
        double profit = winnings - game.getBet();
        
        switch (game.getState()) {
            case PLAYER_BLACKJACK:
                player.sendMessage(MessageUtil.colorize("&6&l★ BLACKJACK! ★"));
                player.sendMessage(MessageUtil.colorize("&7Gain: &a+" + winnings + " " + currency));
                player.sendMessage(MessageUtil.colorize("&7Profit: &a+" + profit + " " + currency));
                eco.addBalance(player.getUniqueId(), winnings);
                eco.addTransaction(player.getUniqueId(), TransactionType.GAMBLE_WIN, profit, "Blackjack Win (Blackjack)");
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                break;
                
            case PLAYER_WIN:
            case DEALER_BUSTED:
                player.sendMessage(MessageUtil.colorize("&a&l✓ VICTOIRE!"));
                player.sendMessage(MessageUtil.colorize("&7Gain: &a+" + winnings + " " + currency));
                player.sendMessage(MessageUtil.colorize("&7Profit: &a+" + profit + " " + currency));
                eco.addBalance(player.getUniqueId(), winnings);
                eco.addTransaction(player.getUniqueId(), TransactionType.GAMBLE_WIN, profit, "Blackjack Win");
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                break;
                
            case PUSH:
                player.sendMessage(MessageUtil.colorize("&e&l= ÉGALITÉ!"));
                player.sendMessage(MessageUtil.colorize("&7Mise rendue: &e" + game.getBet() + " " + currency));
                eco.addBalance(player.getUniqueId(), winnings);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
                break;
                
            case PLAYER_BUSTED:
                player.sendMessage(MessageUtil.colorize("&c&l✗ BUST! (Dépassé 21)"));
                player.sendMessage(MessageUtil.colorize("&7Perte: &c-" + game.getBet() + " " + currency));
                eco.addTransaction(player.getUniqueId(), TransactionType.GAMBLE_LOSS, -game.getBet(), "Blackjack Loss (Bust)");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 0.8f);
                break;
                
            case DEALER_WIN:
                player.sendMessage(MessageUtil.colorize("&c&l✗ DÉFAITE!"));
                player.sendMessage(MessageUtil.colorize("&7Perte: &c-" + game.getBet() + " " + currency));
                eco.addTransaction(player.getUniqueId(), TransactionType.GAMBLE_LOSS, -game.getBet(), "Blackjack Loss");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                break;
        }
        
        player.sendMessage("");
        player.sendMessage(MessageUtil.colorize("&7Nouveau solde: &e" + eco.getBalance(player.getUniqueId()) + " " + currency));
        player.sendMessage(MessageUtil.colorize("&c&l━━━━━━━━━━━━━━━━━━━━━━━━"));
        
        // Clean up
        activeBlackjackGames.remove(player.getUniqueId());
        playersInActiveGame.remove(player.getUniqueId()); // Remove from active game tracking
        
        // Reopen menu after 4 seconds
        Bukkit.getScheduler().runTaskLater(Survival.getInstance(), () -> {
            if (player.isOnline()) {
                openMainMenu(player);
            }
        }, 80L);
    }
    
    /**
     * Get display text for game state
     */
    private static String getStateDisplay(BlackjackGame.GameState state) {
        switch (state) {
            case PLAYING: return "En cours";
            case DEALER_TURN: return "Tour du dealer";
            case PLAYER_BLACKJACK: return "Blackjack!";
            case PLAYER_BUSTED: return "Bust!";
            case DEALER_BUSTED: return "Dealer Bust";
            case PLAYER_WIN: return "Victoire!";
            case DEALER_WIN: return "Défaite";
            case PUSH: return "Égalité";
            default: return "Inconnu";
        }
    }
}
