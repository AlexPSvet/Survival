package com.alexpsvet.territory.menu;

import com.alexpsvet.Survival;
import com.alexpsvet.territory.Territory;
import com.alexpsvet.territory.TerritoryFlags;
import com.alexpsvet.territory.TerritoryManager;
import com.alexpsvet.utils.MessageUtil;
import com.alexpsvet.utils.menu.Button;
import com.alexpsvet.utils.menu.Menu;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Menu for configuring territory settings
 */
public class TerritoryConfigMenu {
    
    public static void open(Player player, Territory territory) {
        if (!territory.hasPermission(player.getUniqueId())) {
            MessageUtil.sendError(player, "Vous n'avez pas la permission de configurer ce territoire!");
            return;
        }
        
        Menu.Builder builder = new Menu.Builder()
            .title(MessageUtil.colorize("&6&lConfiguration - " + territory.getOwnerName()))
            .rows(5);
        
        TerritoryFlags flags = territory.getFlags();
        
        // PVP Toggle
        builder.button(new Button.Builder()
            .slot(10)
            .item(createFlagItem(
                flags.isPvp() ? Material.DIAMOND_SWORD : Material.WOODEN_SWORD,
                "&c&lPvP",
                flags.isPvp(),
                "Autoriser le combat joueur contre joueur"
            ))
            .onClick((p, clickType) -> {
                flags.setPvp(!flags.isPvp());
                Survival.getInstance().getTerritoryManager().updateFlags(territory.getId());
                open(p, territory);
            })
            .build());
        
        // Explosions Toggle
        builder.button(new Button.Builder()
            .slot(11)
            .item(createFlagItem(
                Material.TNT,
                "&e&lExplosions",
                flags.isExplosions(),
                "Autoriser les explosions"
            ))
            .onClick((p, clickType) -> {
                flags.setExplosions(!flags.isExplosions());
                Survival.getInstance().getTerritoryManager().updateFlags(territory.getId());
                open(p, territory);
            })
            .build());
        
        // Mob Spawning Toggle
        builder.button(new Button.Builder()
            .slot(12)
            .item(createFlagItem(
                Material.ZOMBIE_HEAD,
                "&2&lSpawn de Mobs",
                flags.isMobSpawning(),
                "Autoriser le spawn naturel de mobs"
            ))
            .onClick((p, clickType) -> {
                flags.setMobSpawning(!flags.isMobSpawning());
                Survival.getInstance().getTerritoryManager().updateFlags(territory.getId());
                open(p, territory);
            })
            .build());
        
        // Mob Griefing Toggle
        builder.button(new Button.Builder()
            .slot(13)
            .item(createFlagItem(
                Material.GRASS_BLOCK,
                "&5&lGriefing de Mobs",
                flags.isMobGriefing(),
                "Autoriser les mobs à modifier le terrain"
            ))
            .onClick((p, clickType) -> {
                flags.setMobGriefing(!flags.isMobGriefing());
                Survival.getInstance().getTerritoryManager().updateFlags(territory.getId());
                open(p, territory);
            })
            .build());
        
        // Fire Spread Toggle
        builder.button(new Button.Builder()
            .slot(14)
            .item(createFlagItem(
                Material.FLINT_AND_STEEL,
                "&6&lPropagation du Feu",
                flags.isFireSpread(),
                "Autoriser le feu à se propager"
            ))
            .onClick((p, clickType) -> {
                flags.setFireSpread(!flags.isFireSpread());
                Survival.getInstance().getTerritoryManager().updateFlags(territory.getId());
                open(p, territory);
            })
            .build());
        
        // Trusted Players Management
        builder.button(new Button.Builder()
            .slot(30)
            .item(createItem(
                Material.PLAYER_HEAD,
                "&b&lJoueurs de Confiance",
                Arrays.asList(
                    "&7Gérer les joueurs autorisés",
                    "&7à construire dans ce territoire",
                    "",
                    "&7Joueurs actuels: &e" + territory.getTrusted().size(),
                    "",
                    "&eCliquez pour gérer!"
                )
            ))
            .onClick((p, clickType) -> {
                TerritoryTrustedMenu.open(p, territory);
            })
            .build());
        
        // Territory Info
        ItemStack info = new ItemStack(Material.MAP);
        ItemMeta infoMeta = info.getItemMeta();
        if (infoMeta != null) {
            infoMeta.setDisplayName(MessageUtil.colorize("&6&lInformations"));
            List<String> lore = new ArrayList<>();
            lore.add(MessageUtil.colorize("&7Propriétaire: &e" + territory.getOwnerName()));
            lore.add(MessageUtil.colorize("&7Rayon: &e" + territory.getRadius() + " blocs"));
            lore.add(MessageUtil.colorize("&7Centre: &e" + 
                territory.getCenter().getBlockX() + ", " +
                territory.getCenter().getBlockY() + ", " +
                territory.getCenter().getBlockZ()));
            if (territory.getClanName() != null) {
                lore.add(MessageUtil.colorize("&7Clan: &b" + territory.getClanName()));
            }
            infoMeta.setLore(lore);
            info.setItemMeta(infoMeta);
        }
        builder.button(new Button.Builder().slot(32).item(info).build());
        
        builder.build().open(player);
    }
    
    private static ItemStack createFlagItem(Material material, String name, boolean enabled, String description) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(MessageUtil.colorize(name));
            List<String> lore = new ArrayList<>();
            lore.add(MessageUtil.colorize("&7" + description));
            lore.add("");
            lore.add(MessageUtil.colorize(enabled ? "&a&l✔ ACTIVÉ" : "&c&l✘ DÉSACTIVÉ"));
            lore.add("");
            lore.add(MessageUtil.colorize("&eCliquez pour basculer!"));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    private static ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(MessageUtil.colorize(name));
            List<String> coloredLore = new ArrayList<>();
            for (String line : lore) {
                coloredLore.add(MessageUtil.colorize(line));
            }
            meta.setLore(coloredLore);
            item.setItemMeta(meta);
        }
        return item;
    }
}
