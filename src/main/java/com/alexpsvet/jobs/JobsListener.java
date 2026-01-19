package com.alexpsvet.jobs;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Listener for job-related events
 */
public class JobsListener implements Listener {
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Material material = block.getType();
        
        JobAction action = getBlockBreakAction(material);
        if (action != null) {
            JobsManager.getInstance().performAction(player, action);
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        JobsManager.getInstance().performAction(player, JobAction.PLACE_BLOCK);
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) {
            return;
        }
        
        EntityType entityType = event.getEntityType();
        JobAction action = getEntityKillAction(entityType);
        if (action != null) {
            JobsManager.getInstance().performAction(killer, action);
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) {
            return;
        }
        
        Player player = event.getPlayer();
        ItemStack caught = event.getCaught() != null ? 
            ((org.bukkit.entity.Item) event.getCaught()).getItemStack() : null;
        
        if (caught != null) {
            if (isValuableItem(caught.getType())) {
                JobsManager.getInstance().performAction(player, JobAction.CATCH_TREASURE);
            } else {
                JobsManager.getInstance().performAction(player, JobAction.CATCH_FISH);
            }
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityBreed(EntityBreedEvent event) {
        if (!(event.getBreeder() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getBreeder();
        EntityType entityType = event.getEntityType();
        JobAction action = getBreedAction(entityType);
        if (action != null) {
            JobsManager.getInstance().performAction(player, action);
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraftItem(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        JobsManager.getInstance().performAction(player, JobAction.CRAFT_ITEM);
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFurnaceExtract(FurnaceExtractEvent event) {
        Player player = event.getPlayer();
        JobsManager.getInstance().performAction(player, JobAction.SMELT_ITEM);
    }
    
    /**
     * Get JobAction for block break
     */
    private JobAction getBlockBreakAction(Material material) {
        switch (material) {
            // Ores
            case COAL_ORE:
                return JobAction.MINE_COAL_ORE;
            case IRON_ORE:
                return JobAction.MINE_IRON_ORE;
            case GOLD_ORE:
                return JobAction.MINE_GOLD_ORE;
            case DIAMOND_ORE:
                return JobAction.MINE_DIAMOND_ORE;
            case EMERALD_ORE:
                return JobAction.MINE_EMERALD_ORE;
            case LAPIS_ORE:
                return JobAction.MINE_LAPIS_ORE;
            case REDSTONE_ORE:
                return JobAction.MINE_REDSTONE_ORE;
            case DEEPSLATE_COAL_ORE:
                return JobAction.MINE_DEEPSLATE_COAL_ORE;
            case DEEPSLATE_IRON_ORE:
                return JobAction.MINE_DEEPSLATE_IRON_ORE;
            case DEEPSLATE_GOLD_ORE:
                return JobAction.MINE_DEEPSLATE_GOLD_ORE;
            case DEEPSLATE_DIAMOND_ORE:
                return JobAction.MINE_DEEPSLATE_DIAMOND_ORE;
            case STONE:
                return JobAction.MINE_STONE;
            
            // Logs
            case OAK_LOG:
                return JobAction.CUT_OAK_LOG;
            case SPRUCE_LOG:
                return JobAction.CUT_SPRUCE_LOG;
            case BIRCH_LOG:
                return JobAction.CUT_BIRCH_LOG;
            case JUNGLE_LOG:
                return JobAction.CUT_JUNGLE_LOG;
            case ACACIA_LOG:
                return JobAction.CUT_ACACIA_LOG;
            case DARK_OAK_LOG:
                return JobAction.CUT_DARK_OAK_LOG;
            
            // Crops
            case WHEAT:
                return JobAction.HARVEST_WHEAT;
            case CARROTS:
                return JobAction.HARVEST_CARROT;
            case POTATOES:
                return JobAction.HARVEST_POTATO;
            case BEETROOTS:
                return JobAction.HARVEST_BEETROOT;
            case MELON:
                return JobAction.HARVEST_MELON;
            case PUMPKIN:
                return JobAction.HARVEST_PUMPKIN;
            case SUGAR_CANE:
                return JobAction.HARVEST_SUGAR_CANE;
            
            default:
                return null;
        }
    }
    
    /**
     * Get JobAction for entity kill
     */
    private JobAction getEntityKillAction(EntityType entityType) {
        switch (entityType) {
            case ZOMBIE:
                return JobAction.KILL_ZOMBIE;
            case SKELETON:
                return JobAction.KILL_SKELETON;
            case CREEPER:
                return JobAction.KILL_CREEPER;
            case SPIDER:
                return JobAction.KILL_SPIDER;
            case ENDERMAN:
                return JobAction.KILL_ENDERMAN;
            case WITCH:
                return JobAction.KILL_WITCH;
            case BLAZE:
                return JobAction.KILL_BLAZE;
            case WITHER_SKELETON:
                return JobAction.KILL_WITHER_SKELETON;
            case PIGLIN:
                return JobAction.KILL_PIGLIN;
            case HOGLIN:
                return JobAction.KILL_HOGLIN;
            default:
                return null;
        }
    }
    
    /**
     * Get JobAction for breeding
     */
    private JobAction getBreedAction(EntityType entityType) {
        switch (entityType) {
            case COW:
                return JobAction.BREED_COW;
            case SHEEP:
                return JobAction.BREED_SHEEP;
            case PIG:
                return JobAction.BREED_PIG;
            case CHICKEN:
                return JobAction.BREED_CHICKEN;
            default:
                return null;
        }
    }
    
    /**
     * Check if item is valuable (treasure)
     */
    private boolean isValuableItem(Material material) {
        return material == Material.BOW || 
               material == Material.ENCHANTED_BOOK ||
               material == Material.NAME_TAG ||
               material == Material.SADDLE ||
               material == Material.NAUTILUS_SHELL;
    }
}
