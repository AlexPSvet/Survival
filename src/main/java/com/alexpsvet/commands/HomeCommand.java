package com.alexpsvet.commands;

import com.alexpsvet.Survival;
import com.alexpsvet.economy.EconomyManager;
import com.alexpsvet.home.Home;
import com.alexpsvet.home.HomeManager;
import com.alexpsvet.teleport.TeleportManager;
import com.alexpsvet.utils.MessageUtil;
import com.alexpsvet.utils.menu.Button;
import com.alexpsvet.utils.menu.Menu;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Home commands - /sethome, /home, /delhome
 */
public class HomeCommand implements CommandExecutor, TabCompleter {
    
    private final HomeManager homeManager;
    private final TeleportManager teleportManager;
    private final EconomyManager economyManager;
    
    public HomeCommand() {
        this.homeManager = Survival.getInstance().getHomeManager();
        this.teleportManager = Survival.getInstance().getTeleportManager();
        this.economyManager = Survival.getInstance().getEconomyManager();
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Cette commande ne peut être exécutée que par un joueur!");
            return true;
        }
        
        Player player = (Player) sender;
        String currency = Survival.getInstance().getConfig().getString("economy.currency-symbol", "⛁");
        
        // /sethome [name]
        if (label.equalsIgnoreCase("sethome")) {
            return handleSetHome(player, args);
        }
        
        // /delhome <name>
        if (label.equalsIgnoreCase("delhome")) {
            return handleDelHome(player, args);
        }
        
        // /home [name]
        if (label.equalsIgnoreCase("home")) {
            // No args - open menu
            if (args.length == 0) {
                openHomeMenu(player);
                return true;
            }
            
            // /home <name>
            String homeName = args[0];
            return handleTeleportHome(player, homeName);
        }
        
        return true;
    }
    
    private boolean handleSetHome(Player player, String[] args) {
        String homeName = args.length > 0 ? args[0] : "home";
        int maxHomes = Survival.getInstance().getConfig().getInt("home.max-homes", 3);
        double setCost = Survival.getInstance().getConfig().getDouble("home.set-cost", 1000.0);
        String currency = Survival.getInstance().getConfig().getString("economy.currency-symbol", "⛁");
        
        // Check if home already exists
        Home existing = homeManager.getHome(player.getUniqueId(), homeName);
        boolean isNew = existing == null;
        
        // Check home limit for new homes
        if (isNew && homeManager.getHomeCount(player.getUniqueId()) >= maxHomes) {
            MessageUtil.sendError(player, "Vous avez atteint le nombre maximum de homes (" + maxHomes + ")!");
            return true;
        }
        
        // Check balance for new homes
        if (isNew && economyManager.getBalance(player.getUniqueId()) < setCost) {
            MessageUtil.sendError(player, "Vous n'avez pas assez d'argent! Coût: " + setCost + " " + currency);
            return true;
        }
        
        // Deduct cost for new homes
        if (isNew) {
            economyManager.removeBalance(player.getUniqueId(), setCost);
        }
        
        // Create and save home
        Location loc = player.getLocation();
        Home home = new Home(
            player.getUniqueId(),
            homeName,
            loc.getWorld().getName(),
            loc.getX(),
            loc.getY(),
            loc.getZ(),
            loc.getYaw(),
            loc.getPitch()
        );
        
        if (homeManager.saveHome(home)) {
            if (isNew) {
                MessageUtil.sendSuccess(player, "Home &e" + homeName + " &adéfini! Coût: &e" + setCost + " " + currency);
                player.sendTitle("§a§lHome Défini", "§7" + homeName, 10, 40, 10);
            } else {
                MessageUtil.sendSuccess(player, "Home &e" + homeName + " &amis à jour!");
                player.sendTitle("§a§lHome Mis à Jour", "§7" + homeName, 10, 40, 10);
            }
            return true;
        } else {
            MessageUtil.sendError(player, "Erreur lors de la sauvegarde du home!");
            // Refund if new
            if (isNew) {
                economyManager.addBalance(player.getUniqueId(), setCost);
            }
            return true;
        }
    }
    
    private boolean handleDelHome(Player player, String[] args) {
        if (args.length == 0) {
            MessageUtil.sendError(player, "Usage: /delhome <nom>");
            return true;
        }
        
        String homeName = args[0];
        
        if (homeManager.deleteHome(player.getUniqueId(), homeName)) {
            MessageUtil.sendSuccess(player, "Home &e" + homeName + " &asupprimé!");
            return true;
        } else {
            MessageUtil.sendError(player, "Home &e" + homeName + " &cintrouvable!");
            return true;
        }
    }
    
    private boolean handleTeleportHome(Player player, String homeName) {
        String currency = Survival.getInstance().getConfig().getString("economy.currency-symbol", "⛁");
        double homeCost = Survival.getInstance().getConfig().getDouble("home.teleport-cost", 100.0);
        
        // Check if home exists
        Home home = homeManager.getHome(player.getUniqueId(), homeName);
        if (home == null) {
            MessageUtil.sendError(player, "Home &e" + homeName + " &cintrouvable!");
            return true;
        }
        
        // Check cooldown
        if (homeManager.isOnCooldown(player.getUniqueId())) {
            long remaining = homeManager.getCooldownRemaining(player.getUniqueId());
            MessageUtil.sendError(player, "Cooldown actif! Attendez " + remaining + " seconde(s).");
            return true;
        }
        
        // Check balance
        if (economyManager.getBalance(player.getUniqueId()) < homeCost) {
            MessageUtil.sendError(player, "Vous n'avez pas assez d'argent! Coût: " + homeCost + " " + currency);
            return true;
        }
        
        // Deduct cost
        economyManager.removeBalance(player.getUniqueId(), homeCost);
        
        // Teleport
        Location destination = home.getLocation();
        if (destination == null) {
            MessageUtil.sendError(player, "Le monde du home est introuvable!");
            economyManager.addBalance(player.getUniqueId(), homeCost); // Refund
            return true;
        }
        
        int delay = Survival.getInstance().getConfig().getInt("home.teleport-delay", 3);
        teleportManager.startTeleport(player, destination, delay);
        homeManager.setCooldown(player.getUniqueId());
        
        MessageUtil.sendSuccess(player, "Téléportation vers &e" + homeName + " &adans &e" + delay + "&as. Coût: &e" + homeCost + " " + currency);
        return true;
    }
    
    private void openHomeMenu(Player player) {
        Collection<Home> homes = homeManager.getHomes(player.getUniqueId());
        int maxHomes = Survival.getInstance().getConfig().getInt("home.max-homes", 3);
        double homeCost = Survival.getInstance().getConfig().getDouble("home.teleport-cost", 100.0);
        double setCost = Survival.getInstance().getConfig().getDouble("home.set-cost", 1000.0);
        int cooldown = Survival.getInstance().getConfig().getInt("home.cooldown-seconds", 300);
        String currency = Survival.getInstance().getConfig().getString("economy.currency-symbol", "⛁");
        
        Menu.Builder builder = new Menu.Builder()
            .title(MessageUtil.colorize("&6&lMes Homes"))
            .rows(5);
        
        // Info item
        ItemStack info = new ItemStack(Material.BOOK);
        List<String> infoLore = new ArrayList<>();
        infoLore.add(MessageUtil.colorize("&7Homes: &e" + homes.size() + "/" + maxHomes));
        infoLore.add("");
        infoLore.add(MessageUtil.colorize("&7Coût TP: &e" + homeCost + " " + currency));
        infoLore.add(MessageUtil.colorize("&7Cooldown: &e" + cooldown + "s"));
        infoLore.add(MessageUtil.colorize("&7Coût set home: &e" + setCost + " " + currency));
        
        if (homeManager.isOnCooldown(player.getUniqueId())) {
            long remaining = homeManager.getCooldownRemaining(player.getUniqueId());
            infoLore.add("");
            infoLore.add(MessageUtil.colorize("&cCooldown actif: " + remaining + "s"));
        }
        
        builder.button(new Button.Builder()
            .slot(4)
            .item(info)
            .name(MessageUtil.colorize("&6&lInformations"))
            .lore(infoLore)
            .build());
        
        // Add homes
        int slot = 10;
        for (Home home : homes) {
            if (slot >= 35) break;
            
            ItemStack homeItem = new ItemStack(Material.RED_BED);
            List<String> lore = new ArrayList<>();
            lore.add(MessageUtil.colorize("&7Monde: &e" + home.getWorldName()));
            lore.add(MessageUtil.colorize("&7Position: &e" + 
                (int)home.getX() + ", " + (int)home.getY() + ", " + (int)home.getZ()));
            lore.add("");
            lore.add(MessageUtil.colorize("&aClick gauche: &fTéléporter (&e" + homeCost + " " + currency + "&f)"));
            lore.add(MessageUtil.colorize("&cClick droit: &fSupprimer"));
            
            final Home finalHome = home;
            builder.button(new Button.Builder()
                .slot(slot++)
                .item(homeItem)
                .name(MessageUtil.colorize("&6&l" + home.getName()))
                .lore(lore)
                .onClick((p, clickType) -> {
                    p.closeInventory();
                    if (clickType.isRightClick()) {
                        // Delete
                        handleDelHome(p, new String[]{finalHome.getName()});
                    } else {
                        // Teleport
                        handleTeleportHome(p, finalHome.getName());
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
            .onClick((p, clickType) -> p.closeInventory())
            .build());
        
        builder.build().open(player);
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) return null;
        
        Player player = (Player) sender;
        
        if (command.getName().equalsIgnoreCase("home") || command.getName().equalsIgnoreCase("delhome")) {
            if (args.length == 1) {
                // Suggest home names
                Collection<Home> homes = homeManager.getHomes(player.getUniqueId());
                return homes.stream()
                    .map(Home::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
            }
        }
        
        return null;
    }
}
