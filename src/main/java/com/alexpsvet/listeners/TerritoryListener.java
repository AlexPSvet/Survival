package com.alexpsvet.listeners;

import com.alexpsvet.Survival;
import com.alexpsvet.chat.ChatManager;
import com.alexpsvet.territory.Territory;
import com.alexpsvet.territory.TerritoryManager;
import com.alexpsvet.utils.MessageUtil;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Listener for territory protection
 */
public class TerritoryListener implements Listener {
    
    private static final Material PROTECTION_BLOCK = Material.SPONGE; // Configurable protection block
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        
        // Check if placing a protection block
        if (block.getType() == PROTECTION_BLOCK) {
            TerritoryManager territoryManager = Survival.getInstance().getTerritoryManager();
            
            // Check if already in a territory
            if (territoryManager.getTerritoryAt(block.getLocation()) != null) {
                ChatManager chatManager = ChatManager.getInstance();
                MessageUtil.sendMessage(player, chatManager.getMessage("territory.already-claimed"));
                event.setCancelled(true);
                return;
            }
            
            // Create territory with default radius
            int radius = Survival.getInstance().getConfig().getInt("territory.default-radius", 10);
            Territory territory = territoryManager.createTerritoryFromBlock(block, player.getUniqueId(), player.getName(), radius);
            
            if (territory != null) {
                ChatManager chatManager = ChatManager.getInstance();
                MessageUtil.sendMessage(player, chatManager.getMessage("territory.created",
                    "{size}", String.valueOf(territory.getSize())));
            }
            return;
        }
        
        // Check if can build in this territory
        TerritoryManager territoryManager = Survival.getInstance().getTerritoryManager();
        if (!territoryManager.canBuild(player.getUniqueId(), block.getLocation())) {
            ChatManager chatManager = ChatManager.getInstance();
            MessageUtil.sendMessage(player, chatManager.getMessage("territory.cannot-build"));
            event.setCancelled(true);
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        TerritoryManager territoryManager = Survival.getInstance().getTerritoryManager();
        
        // Check if breaking a protection block
        if (block.getType() == PROTECTION_BLOCK) {
            Territory territory = territoryManager.getTerritoryByBlock(block);
            if (territory != null) {
                if (!territory.hasPermission(player.getUniqueId())) {
                    ChatManager chatManager = ChatManager.getInstance();
                    MessageUtil.sendMessage(player, chatManager.getMessage("territory.no-permission"));
                    event.setCancelled(true);
                    return;
                }
                
                // Remove territory
                territoryManager.removeTerritory(territory.getId());
                ChatManager chatManager = ChatManager.getInstance();
                MessageUtil.sendMessage(player, chatManager.getMessage("territory.removed"));
                return;
            }
        }
        
        // Check if can break in this territory
        if (!territoryManager.canBuild(player.getUniqueId(), block.getLocation())) {
            ChatManager chatManager = ChatManager.getInstance();
            MessageUtil.sendMessage(player, chatManager.getMessage("territory.cannot-break"));
            event.setCancelled(true);
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;
        
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        TerritoryManager territoryManager = Survival.getInstance().getTerritoryManager();
        
        // Check if can interact in this territory
        if (!territoryManager.canBuild(player.getUniqueId(), block.getLocation())) {
            // Allow certain interactions
            Material type = block.getType();
            if (type.name().contains("DOOR") || 
                type.name().contains("BUTTON") || 
                type.name().contains("LEVER") ||
                type.name().contains("CHEST") ||
                type.name().contains("FURNACE")) {
                
                ChatManager chatManager = ChatManager.getInstance();
                MessageUtil.sendMessage(player, chatManager.getMessage("territory.cannot-interact"));
                event.setCancelled(true);
            }
        }
    }
}
