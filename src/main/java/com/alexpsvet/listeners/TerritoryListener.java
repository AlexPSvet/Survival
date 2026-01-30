package com.alexpsvet.listeners;

import com.alexpsvet.Survival;
import com.alexpsvet.chat.ChatManager;
import com.alexpsvet.territory.Territory;
import com.alexpsvet.territory.TerritoryManager;
import com.alexpsvet.utils.MessageUtil;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Listener for territory protection
 */
public class TerritoryListener implements Listener {
    
    private static final Material PROTECTION_BLOCK_SMALL = Material.SPONGE;
    private static final Material PROTECTION_BLOCK_MEDIUM = Material.GOLD_BLOCK;
    private static final Material PROTECTION_BLOCK_LARGE = Material.DIAMOND_BLOCK;
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        
        // Check if placing a protection block and determine radius
        int radius = -1;
        if (block.getType() == PROTECTION_BLOCK_SMALL) {
            radius = Survival.getInstance().getConfig().getInt("territory.protection-blocks.small.radius", 10);
        } else if (block.getType() == PROTECTION_BLOCK_MEDIUM) {
            radius = Survival.getInstance().getConfig().getInt("territory.protection-blocks.medium.radius", 25);
        } else if (block.getType() == PROTECTION_BLOCK_LARGE) {
            radius = Survival.getInstance().getConfig().getInt("territory.protection-blocks.large.radius", 50);
        }
        
        if (radius != -1) {
            TerritoryManager territoryManager = Survival.getInstance().getTerritoryManager();
            
            // Check if already in a territory
            if (territoryManager.getTerritoryAt(block.getLocation()) != null) {
                ChatManager chatManager = ChatManager.getInstance();
                MessageUtil.sendMessage(player, chatManager.getMessage("territory.already-claimed"));
                event.setCancelled(true);
                return;
            }
            
            // Check if would collide with existing territories
            if (territoryManager.wouldCollide(block.getLocation(), radius)) {
                ChatManager chatManager = ChatManager.getInstance();
                MessageUtil.sendMessage(player, chatManager.getMessage("territory.collision"));
                event.setCancelled(true);
                return;
            }
            
            // Create territory
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
        if (block.getType() == PROTECTION_BLOCK_SMALL || 
            block.getType() == PROTECTION_BLOCK_MEDIUM || 
            block.getType() == PROTECTION_BLOCK_LARGE) {
            Territory territory = territoryManager.getTerritoryByBlock(block);
            if (territory != null) {
                // Only owner can remove the protection block
                if (!territory.getOwner().equals(player.getUniqueId())) {
                    ChatManager chatManager = ChatManager.getInstance();
                    MessageUtil.sendMessage(player, chatManager.getMessage("territory.not-owner"));
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
    
    /**
     * Prevent explosions in protected territories
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityExplode(EntityExplodeEvent event) {
        TerritoryManager territoryManager = Survival.getInstance().getTerritoryManager();
        
        event.blockList().removeIf(block -> {
            Territory territory = territoryManager.getTerritoryAt(block.getLocation());
            // Remove blocks from explosion if in protected territory and explosions are disabled
            return territory != null && !territory.getFlags().isExplosions();
        });
    }
    
    /**
     * Prevent block explosions (TNT, respawn anchors, etc.)
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockExplode(BlockExplodeEvent event) {
        TerritoryManager territoryManager = Survival.getInstance().getTerritoryManager();
        
        event.blockList().removeIf(block -> {
            Territory territory = territoryManager.getTerritoryAt(block.getLocation());
            return territory != null && !territory.getFlags().isExplosions();
        });
    }
    
    /**
     * Prevent mob griefing (enderman, creepers changing blocks, etc.)
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        TerritoryManager territoryManager = Survival.getInstance().getTerritoryManager();
        Territory territory = territoryManager.getTerritoryAt(event.getBlock().getLocation());
        
        if (territory == null) return;
        
        // Prevent enderman from picking up blocks
        if (event.getEntity() instanceof Enderman && !territory.getFlags().isMobGriefing()) {
            event.setCancelled(true);
            return;
        }
        
        // Prevent other mob griefing
        if (!(event.getEntity() instanceof Player) && !territory.getFlags().isMobGriefing()) {
            event.setCancelled(true);
        }
    }
    
    /**
     * Prevent mob spawning in protected territories
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        // Allow natural spawning based on territory flag
        if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.NATURAL ||
            event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.CHUNK_GEN) {
            
            TerritoryManager territoryManager = Survival.getInstance().getTerritoryManager();
            Territory territory = territoryManager.getTerritoryAt(event.getLocation());
            
            if (territory != null && !territory.getFlags().isMobSpawning()) {
                event.setCancelled(true);
            }
        }
    }
    
    /**
     * Prevent fire spread in protected territories
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBurn(BlockBurnEvent event) {
        TerritoryManager territoryManager = Survival.getInstance().getTerritoryManager();
        Territory territory = territoryManager.getTerritoryAt(event.getBlock().getLocation());
        
        if (territory != null && !territory.getFlags().isFireSpread()) {
            event.setCancelled(true);
        }
    }
    
    /**
     * Prevent fire ignition in protected territories
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockIgnite(BlockIgniteEvent event) {
        // Allow players to ignite if they have permission
        if (event.getPlayer() != null) {
            TerritoryManager territoryManager = Survival.getInstance().getTerritoryManager();
            if (!territoryManager.canBuild(event.getPlayer().getUniqueId(), event.getBlock().getLocation())) {
                event.setCancelled(true);
                return;
            }
        }
        
        // Prevent natural fire spread
        if (event.getCause() == BlockIgniteEvent.IgniteCause.SPREAD ||
            event.getCause() == BlockIgniteEvent.IgniteCause.LAVA ||
            event.getCause() == BlockIgniteEvent.IgniteCause.LIGHTNING) {
            
            TerritoryManager territoryManager = Survival.getInstance().getTerritoryManager();
            Territory territory = territoryManager.getTerritoryAt(event.getBlock().getLocation());
            
            if (territory != null && !territory.getFlags().isFireSpread()) {
                event.setCancelled(true);
            }
        }
    }
    
    /**
     * Prevent block spread (like fire, vines, etc.) in protected territories
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockSpread(BlockSpreadEvent event) {
        TerritoryManager territoryManager = Survival.getInstance().getTerritoryManager();
        Territory territory = territoryManager.getTerritoryAt(event.getBlock().getLocation());
        
        if (territory != null && event.getSource().getType() == Material.FIRE && !territory.getFlags().isFireSpread()) {
            event.setCancelled(true);
        }
    }
    
    /**
     * Prevent liquid flow into protected territories
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockFromTo(BlockFromToEvent event) {
        TerritoryManager territoryManager = Survival.getInstance().getTerritoryManager();
        Territory fromTerritory = territoryManager.getTerritoryAt(event.getBlock().getLocation());
        Territory toTerritory = territoryManager.getTerritoryAt(event.getToBlock().getLocation());
        
        // Prevent flow from outside into protected territory or between different territories
        if (toTerritory != null && (fromTerritory == null || !fromTerritory.equals(toTerritory))) {
            event.setCancelled(true);
        }
    }
    
    /**
     * Prevent PvP in protected territories
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerPvP(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (!(event.getDamager() instanceof Player)) return;
        
        TerritoryManager territoryManager = Survival.getInstance().getTerritoryManager();
        Territory territory = territoryManager.getTerritoryAt(event.getEntity().getLocation());
        
        if (territory != null && !territory.getFlags().isPvp()) {
            event.setCancelled(true);
            ChatManager chatManager = ChatManager.getInstance();
            MessageUtil.sendMessage((Player) event.getDamager(), 
                chatManager.getMessage("territory.pvp-disabled"));
        }
    }
}
