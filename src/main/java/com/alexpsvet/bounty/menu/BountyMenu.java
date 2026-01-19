package com.alexpsvet.bounty.menu;

import com.alexpsvet.Survival;
import com.alexpsvet.bounty.Bounty;
import com.alexpsvet.bounty.BountyManager;
import com.alexpsvet.economy.EconomyManager;
import com.alexpsvet.utils.MessageUtil;
import com.alexpsvet.utils.menu.Button;
import com.alexpsvet.utils.menu.Menu;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Bounty menu system
 */
public class BountyMenu {
    
    /**
     * Open main bounty menu
     */
    public static void openMainMenu(Player player) {
        Menu.Builder builder = new Menu.Builder()
            .title(MessageUtil.colorize("&c&lPrimes"))
            .rows(6);
        
        String currency = Survival.getInstance().getConfig().getString("economy.currency-symbol", "⛁");
        BountyManager bountyManager = BountyManager.getInstance();
        
        // Place bounty button
        ItemStack placeBounty = new ItemStack(Material.DIAMOND_SWORD);
        List<String> placeLore = new ArrayList<>();
        placeLore.add(MessageUtil.colorize("&7Placer une prime sur un joueur"));
        placeLore.add("");
        placeLore.add(MessageUtil.colorize("&7Montant minimum: &e" + bountyManager.getMinimumBounty() + " " + currency));
        placeLore.add(MessageUtil.colorize("&7Montant maximum: &e" + bountyManager.getMaximumBounty() + " " + currency));
        placeLore.add("");
        placeLore.add(MessageUtil.colorize("&eCliquez pour placer une prime!"));
        
        builder.button(new Button.Builder()
            .slot(11)
            .item(placeBounty)
            .name(MessageUtil.colorize("&c&lPlacer une Prime"))
            .lore(placeLore)
            .onClick((p, clickType) -> openPlaceBountyMenu(p))
            .build());
        
        // View all bounties button
        ItemStack viewAll = new ItemStack(Material.BOOK);
        List<String> viewLore = new ArrayList<>();
        viewLore.add(MessageUtil.colorize("&7Voir toutes les primes actives"));
        viewLore.add("");
        viewLore.add(MessageUtil.colorize("&eCliquez pour voir!"));
        
        builder.button(new Button.Builder()
            .slot(13)
            .item(viewAll)
            .name(MessageUtil.colorize("&6&lToutes les Primes"))
            .lore(viewLore)
            .onClick((p, clickType) -> openAllBountiesMenu(p))
            .build());
        
        // My bounties (bounties I placed)
        ItemStack myBounties = new ItemStack(Material.WRITABLE_BOOK);
        List<String> myLore = new ArrayList<>();
        myLore.add(MessageUtil.colorize("&7Voir les primes que vous avez placées"));
        myLore.add("");
        myLore.add(MessageUtil.colorize("&eCliquez pour voir!"));
        
        builder.button(new Button.Builder()
            .slot(15)
            .item(myBounties)
            .name(MessageUtil.colorize("&e&lMes Primes"))
            .lore(myLore)
            .onClick((p, clickType) -> openMyBountiesMenu(p))
            .build());
        
        // Close button
        ItemStack close = new ItemStack(Material.BARRIER);
        builder.button(new Button.Builder()
            .slot(49)
            .item(close)
            .name(MessageUtil.colorize("&cFermer"))
            .onClick((p, clickType) -> p.closeInventory())
            .build());
        
        builder.build().open(player);
    }
    
    /**
     * Open menu to place a bounty
     */
    public static void openPlaceBountyMenu(Player player) {
        Menu.Builder builder = new Menu.Builder()
            .title(MessageUtil.colorize("&c&lChoisir un Joueur"))
            .rows(6);
        
        List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
        onlinePlayers.remove(player); // Can't place bounty on yourself
        
        int slot = 0;
        for (Player target : onlinePlayers) {
            if (slot >= 45) break;
            
            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) skull.getItemMeta();
            if (meta != null) {
                meta.setOwningPlayer(target);
                meta.setDisplayName(MessageUtil.colorize("&6" + target.getName()));
                
                List<String> lore = new ArrayList<>();
                double currentBounty = BountyManager.getInstance().getTotalBounty(target.getUniqueId());
                if (currentBounty > 0) {
                    String currency = Survival.getInstance().getConfig().getString("economy.currency-symbol", "⛁");
                    lore.add(MessageUtil.colorize("&7Prime actuelle: &c" + currentBounty + " " + currency));
                }
                lore.add("");
                lore.add(MessageUtil.colorize("&eCliquez pour placer une prime!"));
                meta.setLore(lore);
                skull.setItemMeta(meta);
            }
            
            builder.button(new Button.Builder()
                .slot(slot++)
                .item(skull)
                .onClick((p, clickType) -> openAmountSelectionMenu(p, target))
                .build());
        }
        
        // Back button
        ItemStack back = new ItemStack(Material.ARROW);
        builder.button(new Button.Builder()
            .slot(49)
            .item(back)
            .name(MessageUtil.colorize("&cRetour"))
            .onClick((p, clickType) -> openMainMenu(p))
            .build());
        
        builder.build().open(player);
    }
    
    /**
     * Open menu to select bounty amount
     */
    public static void openAmountSelectionMenu(Player player, Player target) {
        Menu.Builder builder = new Menu.Builder()
            .title(MessageUtil.colorize("&c&lMontant de la Prime"))
            .rows(5);
        
        String currency = Survival.getInstance().getConfig().getString("economy.currency-symbol", "⛁");
        EconomyManager eco = Survival.getInstance().getEconomyManager();
        double balance = eco.getBalance(player.getUniqueId());
        
        // Preset amounts
        double[] amounts = {100, 500, 1000, 2500, 5000, 10000, 25000, 50000, 100000};
        int slot = 10;
        
        for (double amount : amounts) {
            if (slot == 17) slot = 19; // Skip to next row
            if (slot >= 35) break;
            
            Material material = amount <= 1000 ? Material.GOLD_NUGGET :
                               amount <= 5000 ? Material.GOLD_INGOT :
                               amount <= 25000 ? Material.GOLD_BLOCK :
                               Material.DIAMOND_BLOCK;
            
            ItemStack item = new ItemStack(material);
            List<String> lore = new ArrayList<>();
            lore.add(MessageUtil.colorize("&7Montant: &e" + amount + " " + currency));
            lore.add("");
            
            if (balance >= amount) {
                lore.add(MessageUtil.colorize("&aVous avez assez d'argent"));
                lore.add(MessageUtil.colorize("&eCliquez pour placer!"));
            } else {
                lore.add(MessageUtil.colorize("&cFonds insuffisants"));
            }
            
            builder.button(new Button.Builder()
                .slot(slot++)
                .item(item)
                .name(MessageUtil.colorize("&6&l" + amount + " " + currency))
                .lore(lore)
                .onClick((p, clickType) -> {
                    if (balance >= amount) {
                        if (BountyManager.getInstance().placeBounty(
                            p.getUniqueId(), p.getName(),
                            target.getUniqueId(), target.getName(),
                            amount)) {
                            p.sendMessage(MessageUtil.colorize("&aPrime de &e" + amount + " " + currency + " &aplacée sur &6" + target.getName() + "&a!"));
                            p.closeInventory();
                            
                            // Notify target
                            target.sendMessage(MessageUtil.colorize("&c&l⚠ Une prime de &e" + amount + " " + currency + " &c&la été placée sur votre tête!"));
                        } else {
                            p.sendMessage(MessageUtil.colorize("&cErreur lors du placement de la prime."));
                        }
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
            .onClick((p, clickType) -> openPlaceBountyMenu(p))
            .build());
        
        builder.build().open(player);
    }
    
    /**
     * Open menu showing all active bounties
     */
    public static void openAllBountiesMenu(Player player) {
        Menu.Builder builder = new Menu.Builder()
            .title(MessageUtil.colorize("&c&lToutes les Primes"))
            .rows(6);
        
        String currency = Survival.getInstance().getConfig().getString("economy.currency-symbol", "⛁");
        Map<UUID, List<Bounty>> allBounties = BountyManager.getInstance().getAllBounties();
        
        // Sort by total bounty amount
        List<Map.Entry<UUID, List<Bounty>>> sortedBounties = allBounties.entrySet().stream()
            .sorted((e1, e2) -> {
                double total1 = e1.getValue().stream().mapToDouble(Bounty::getAmount).sum();
                double total2 = e2.getValue().stream().mapToDouble(Bounty::getAmount).sum();
                return Double.compare(total2, total1);
            })
            .collect(Collectors.toList());
        
        int slot = 0;
        for (Map.Entry<UUID, List<Bounty>> entry : sortedBounties) {
            if (slot >= 45) break;
            
            UUID targetUuid = entry.getKey();
            List<Bounty> bounties = entry.getValue();
            if (bounties.isEmpty()) continue;
            
            String targetName = bounties.get(0).getTargetName();
            double totalAmount = bounties.stream().mapToDouble(Bounty::getAmount).sum();
            
            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) skull.getItemMeta();
            if (meta != null) {
                Player target = Bukkit.getPlayer(targetUuid);
                if (target != null) {
                    meta.setOwningPlayer(target);
                }
                meta.setDisplayName(MessageUtil.colorize("&c&l" + targetName));
                
                List<String> lore = new ArrayList<>();
                lore.add(MessageUtil.colorize("&7Prime totale: &e" + totalAmount + " " + currency));
                lore.add(MessageUtil.colorize("&7Nombre de primes: &e" + bounties.size()));
                lore.add("");
                lore.add(MessageUtil.colorize("&7Placées par:"));
                for (Bounty bounty : bounties) {
                    lore.add(MessageUtil.colorize("  &8• &6" + bounty.getIssuerName() + " &7- &e" + bounty.getAmount() + " " + currency));
                }
                meta.setLore(lore);
                skull.setItemMeta(meta);
            }
            
            builder.button(new Button.Builder()
                .slot(slot++)
                .item(skull)
                .build());
        }
        
        // Back button
        ItemStack back = new ItemStack(Material.ARROW);
        builder.button(new Button.Builder()
            .slot(49)
            .item(back)
            .name(MessageUtil.colorize("&cRetour"))
            .onClick((p, clickType) -> openMainMenu(p))
            .build());
        
        builder.build().open(player);
    }
    
    /**
     * Open menu showing bounties placed by the player
     */
    public static void openMyBountiesMenu(Player player) {
        Menu.Builder builder = new Menu.Builder()
            .title(MessageUtil.colorize("&e&lMes Primes"))
            .rows(6);
        
        String currency = Survival.getInstance().getConfig().getString("economy.currency-symbol", "⛁");
        Map<UUID, List<Bounty>> allBounties = BountyManager.getInstance().getAllBounties();
        
        // Filter bounties placed by this player
        List<Bounty> myBounties = new ArrayList<>();
        for (List<Bounty> bounties : allBounties.values()) {
            for (Bounty bounty : bounties) {
                if (bounty.getIssuerUuid().equals(player.getUniqueId())) {
                    myBounties.add(bounty);
                }
            }
        }
        
        int slot = 0;
        for (Bounty bounty : myBounties) {
            if (slot >= 45) break;
            
            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) skull.getItemMeta();
            if (meta != null) {
                Player target = Bukkit.getPlayer(bounty.getTargetUuid());
                if (target != null) {
                    meta.setOwningPlayer(target);
                }
                meta.setDisplayName(MessageUtil.colorize("&c" + bounty.getTargetName()));
                
                List<String> lore = new ArrayList<>();
                lore.add(MessageUtil.colorize("&7Montant: &e" + bounty.getAmount() + " " + currency));
                lore.add(MessageUtil.colorize("&7Placée il y a: &e" + getTimeAgo(bounty.getCreatedAt())));
                lore.add("");
                lore.add(MessageUtil.colorize("&eCliquez pour annuler et être remboursé"));
                meta.setLore(lore);
                skull.setItemMeta(meta);
            }
            
            final UUID bountyId = bounty.getId();
            builder.button(new Button.Builder()
                .slot(slot++)
                .item(skull)
                .onClick((p, clickType) -> {
                    if (BountyManager.getInstance().cancelBounty(bountyId, p.getUniqueId())) {
                        p.sendMessage(MessageUtil.colorize("&aPrime annulée! Vous avez été remboursé de &e" + bounty.getAmount() + " " + currency));
                        openMyBountiesMenu(p); // Refresh menu
                    } else {
                        p.sendMessage(MessageUtil.colorize("&cErreur lors de l'annulation de la prime."));
                    }
                })
                .build());
        }
        
        // Back button
        ItemStack back = new ItemStack(Material.ARROW);
        builder.button(new Button.Builder()
            .slot(49)
            .item(back)
            .name(MessageUtil.colorize("&cRetour"))
            .onClick((p, clickType) -> openMainMenu(p))
            .build());
        
        builder.build().open(player);
    }
    
    /**
     * Get time ago string
     */
    private static String getTimeAgo(long timestamp) {
        long diff = System.currentTimeMillis() - timestamp;
        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) return days + " jour(s)";
        if (hours > 0) return hours + " heure(s)";
        if (minutes > 0) return minutes + " minute(s)";
        return seconds + " seconde(s)";
    }
}
