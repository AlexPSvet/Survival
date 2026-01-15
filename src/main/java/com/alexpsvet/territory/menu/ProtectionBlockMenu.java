package com.alexpsvet.territory.menu;

import com.alexpsvet.Survival;
import com.alexpsvet.economy.EconomyManager;
import com.alexpsvet.economy.TransactionType;
import com.alexpsvet.utils.MessageUtil;
import com.alexpsvet.utils.menu.Button;
import com.alexpsvet.utils.menu.Menu;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Menu for purchasing protection blocks
 */
public class ProtectionBlockMenu {
    
    public static void open(Player player) {
        Menu.Builder builder = new Menu.Builder()
            .title(MessageUtil.colorize("&6&lAcheter Blocs de Protection"))
            .rows(3);
        
        EconomyManager economyManager = Survival.getInstance().getEconomyManager();
        String currency = Survival.getInstance().getConfig().getString("economy.currency-symbol", "⛁");
        
        // Small protection (radius 10)
        double smallCost = Survival.getInstance().getConfig().getDouble("territory.protection-blocks.small.cost", 2000.0);
        int smallRadius = Survival.getInstance().getConfig().getInt("territory.protection-blocks.small.radius", 10);
        ItemStack smallBlock = createProtectionBlockItem(
            Material.SPONGE,
            "&e&lPetite Protection",
            smallRadius,
            smallCost,
            currency
        );
        builder.button(new Button.Builder().slot(10).item(smallBlock)
            .onClick((p, clickType) -> {
                if (economyManager.removeBalance(p.getUniqueId(), smallCost)) {
                    ItemStack protectionBlock = new ItemStack(Material.SPONGE);
                    ItemMeta meta = protectionBlock.getItemMeta();
                    if (meta != null) {
                        meta.setDisplayName(MessageUtil.colorize("&e&lBloc de Protection"));
                        meta.setLore(Arrays.asList(
                            MessageUtil.colorize("&7Rayon: &e" + smallRadius + " blocs"),
                            MessageUtil.colorize("&7Placez ce bloc pour créer"),
                            MessageUtil.colorize("&7une zone protégée!")
                        ));
                        protectionBlock.setItemMeta(meta);
                    }
                    
                    p.getInventory().addItem(protectionBlock);
                    economyManager.addTransaction(p.getUniqueId(), TransactionType.PURCHASE, -smallCost, "Protection block (small)");
                    MessageUtil.sendSuccess(p, "Bloc de protection acheté!");
                    p.closeInventory();
                } else {
                    MessageUtil.sendError(p, "Fonds insuffisants! Vous avez besoin de " + smallCost + " " + currency);
                }
            })
            .build());
        
        // Medium protection (radius 25)
        double mediumCost = Survival.getInstance().getConfig().getDouble("territory.protection-blocks.medium.cost", 5000.0);
        int mediumRadius = Survival.getInstance().getConfig().getInt("territory.protection-blocks.medium.radius", 25);
        ItemStack mediumBlock = createProtectionBlockItem(
            Material.GOLD_BLOCK,
            "&6&lProtection Moyenne",
            mediumRadius,
            mediumCost,
            currency
        );
        builder.button(new Button.Builder().slot(13).item(mediumBlock)
            .onClick((p, clickType) -> {
                if (economyManager.removeBalance(p.getUniqueId(), mediumCost)) {
                    ItemStack protectionBlock = new ItemStack(Material.GOLD_BLOCK);
                    ItemMeta meta = protectionBlock.getItemMeta();
                    if (meta != null) {
                        meta.setDisplayName(MessageUtil.colorize("&6&lBloc de Protection Moyenne"));
                        meta.setLore(Arrays.asList(
                            MessageUtil.colorize("&7Rayon: &e" + mediumRadius + " blocs"),
                            MessageUtil.colorize("&7Placez ce bloc pour créer"),
                            MessageUtil.colorize("&7une zone protégée!")
                        ));
                        protectionBlock.setItemMeta(meta);
                    }
                    
                    p.getInventory().addItem(protectionBlock);
                    economyManager.addTransaction(p.getUniqueId(), TransactionType.PURCHASE, -mediumCost, "Protection block (medium)");
                    MessageUtil.sendSuccess(p, "Bloc de protection acheté!");
                    p.closeInventory();
                } else {
                    MessageUtil.sendError(p, "Fonds insuffisants! Vous avez besoin de " + mediumCost + " " + currency);
                }
            })
            .build());
        
        // Large protection (radius 50)
        double largeCost = Survival.getInstance().getConfig().getDouble("territory.protection-blocks.large.cost", 10000.0);
        int largeRadius = Survival.getInstance().getConfig().getInt("territory.protection-blocks.large.radius", 50);
        ItemStack largeBlock = createProtectionBlockItem(
            Material.DIAMOND_BLOCK,
            "&b&lGrande Protection",
            largeRadius,
            largeCost,
            currency
        );
        builder.button(new Button.Builder().slot(16).item(largeBlock)
            .onClick((p, clickType) -> {
                if (economyManager.removeBalance(p.getUniqueId(), largeCost)) {
                    ItemStack protectionBlock = new ItemStack(Material.DIAMOND_BLOCK);
                    ItemMeta meta = protectionBlock.getItemMeta();
                    if (meta != null) {
                        meta.setDisplayName(MessageUtil.colorize("&b&lBloc de Grande Protection"));
                        meta.setLore(Arrays.asList(
                            MessageUtil.colorize("&7Rayon: &e" + largeRadius + " blocs"),
                            MessageUtil.colorize("&7Placez ce bloc pour créer"),
                            MessageUtil.colorize("&7une zone protégée!")
                        ));
                        protectionBlock.setItemMeta(meta);
                    }
                    
                    p.getInventory().addItem(protectionBlock);
                    economyManager.addTransaction(p.getUniqueId(), TransactionType.PURCHASE, -largeCost, "Protection block (large)");
                    MessageUtil.sendSuccess(p, "Bloc de protection acheté!");
                    p.closeInventory();
                } else {
                    MessageUtil.sendError(p, "Fonds insuffisants! Vous avez besoin de " + largeCost + " " + currency);
                }
            })
            .build());
        
        // Info item
        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = info.getItemMeta();
        if (infoMeta != null) {
            infoMeta.setDisplayName(MessageUtil.colorize("&6&lInformations"));
            List<String> lore = new ArrayList<>();
            lore.add(MessageUtil.colorize("&7Les blocs de protection créent"));
            lore.add(MessageUtil.colorize("&7une zone sécurisée où seuls"));
            lore.add(MessageUtil.colorize("&7vous et vos alliés pouvez"));
            lore.add(MessageUtil.colorize("&7construire et interagir."));
            lore.add("");
            lore.add(MessageUtil.colorize("&7Placez le bloc pour activer"));
            lore.add(MessageUtil.colorize("&7la protection!"));
            infoMeta.setLore(lore);
            info.setItemMeta(infoMeta);
        }
        builder.button(new Button.Builder().slot(22).item(info).build());
        
        builder.build().open(player);
    }
    
    private static ItemStack createProtectionBlockItem(Material material, String name, int radius, double cost, String currency) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(MessageUtil.colorize(name));
            List<String> lore = new ArrayList<>();
            lore.add(MessageUtil.colorize("&7Rayon: &e" + radius + " blocs"));
            lore.add(MessageUtil.colorize("&7Prix: &a" + cost + " " + currency));
            lore.add("");
            lore.add(MessageUtil.colorize("&7Protégez votre territoire"));
            lore.add(MessageUtil.colorize("&7contre les intrus!"));
            lore.add("");
            lore.add(MessageUtil.colorize("&eCliquez pour acheter!"));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}
