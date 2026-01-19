package com.alexpsvet.territory.menu;

import com.alexpsvet.Survival;
import com.alexpsvet.territory.Territory;
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
import java.util.List;
import java.util.UUID;

/**
 * Menu for managing trusted players in a territory
 */
public class TerritoryTrustedMenu {
    
    public static void open(Player player, Territory territory) {
        open(player, territory, 0);
    }
    
    public static void open(Player player, Territory territory, int page) {
        if (!territory.hasPermission(player.getUniqueId())) {
            MessageUtil.sendError(player, "Vous n'avez pas la permission de gérer ce territoire!");
            return;
        }
        
        Menu.Builder builder = new Menu.Builder()
            .title(MessageUtil.colorize("&b&lJoueurs de Confiance"))
            .rows(6);
        
        TerritoryManager territoryManager = Survival.getInstance().getTerritoryManager();
        List<UUID> trustedPlayers = new ArrayList<>(territory.getTrusted());
        
        int itemsPerPage = 45;
        int totalPages = (int) Math.ceil((double) trustedPlayers.size() / itemsPerPage);
        int startIndex = page * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, trustedPlayers.size());
        
        // Display trusted players
        for (int i = startIndex; i < endIndex; i++) {
            UUID trustedUUID = trustedPlayers.get(i);
            Player trustedPlayer = Bukkit.getPlayer(trustedUUID);
            String trustedName = trustedPlayer != null ? trustedPlayer.getName() : 
                                 Bukkit.getOfflinePlayer(trustedUUID).getName();
            
            int slot = i - startIndex;
            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) skull.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(MessageUtil.colorize("&e" + trustedName));
                List<String> lore = new ArrayList<>();
                lore.add(MessageUtil.colorize("&7Peut construire dans ce territoire"));
                lore.add("");
                lore.add(MessageUtil.colorize("&cClic gauche pour retirer"));
                meta.setLore(lore);
                
                if (trustedPlayer != null) {
                    meta.setOwningPlayer(trustedPlayer);
                }
                skull.setItemMeta(meta);
            }
            
            builder.button(new Button.Builder()
                .slot(slot)
                .item(skull)
                .onClick((p, clickType) -> {
                    territoryManager.removeTrusted(territory.getId(), trustedUUID);
                    MessageUtil.sendSuccess(p, trustedName + " retiré de la liste de confiance!");
                    open(p, territory, page);
                })
                .build());
        }
        
        // Add player button
        ItemStack addButton = new ItemStack(Material.LIME_DYE);
        ItemMeta addMeta = addButton.getItemMeta();
        if (addMeta != null) {
            addMeta.setDisplayName(MessageUtil.colorize("&a&lAjouter un Joueur"));
            List<String> lore = new ArrayList<>();
            lore.add(MessageUtil.colorize("&7Utilisez la commande:"));
            lore.add(MessageUtil.colorize("&e/protection trust <joueur>"));
            lore.add("");
            lore.add(MessageUtil.colorize("&7Le joueur doit être en ligne"));
            addMeta.setLore(lore);
            addButton.setItemMeta(addMeta);
        }
        builder.button(new Button.Builder().slot(48).item(addButton).build());
        
        // Back button
        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backButton.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName(MessageUtil.colorize("&6&lRetour"));
            backButton.setItemMeta(backMeta);
        }
        builder.button(new Button.Builder()
            .slot(49)
            .item(backButton)
            .onClick((p, clickType) -> {
                TerritoryConfigMenu.open(p, territory);
            })
            .build());
        
        // Navigation buttons if needed
        if (page > 0) {
            ItemStack prevButton = new ItemStack(Material.ARROW);
            ItemMeta prevMeta = prevButton.getItemMeta();
            if (prevMeta != null) {
                prevMeta.setDisplayName(MessageUtil.colorize("&e&lPage Précédente"));
                prevButton.setItemMeta(prevMeta);
            }
            builder.button(new Button.Builder()
                .slot(45)
                .item(prevButton)
                .onClick((p, clickType) -> open(p, territory, page - 1))
                .build());
        }
        
        if (page < totalPages - 1) {
            ItemStack nextButton = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = nextButton.getItemMeta();
            if (nextMeta != null) {
                nextMeta.setDisplayName(MessageUtil.colorize("&e&lPage Suivante"));
                nextButton.setItemMeta(nextMeta);
            }
            builder.button(new Button.Builder()
                .slot(53)
                .item(nextButton)
                .onClick((p, clickType) -> open(p, territory, page + 1))
                .build());
        }
        
        builder.build().open(player);
    }
}
