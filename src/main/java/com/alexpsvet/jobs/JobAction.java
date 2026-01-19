package com.alexpsvet.jobs;

import org.bukkit.entity.EntityType;

/**
 * Enum representing different job actions that earn money
 */
public enum JobAction {
    // Mining actions
    MINE_STONE("Miner de la pierre", EntityType.UNKNOWN),
    MINE_COAL_ORE("Miner du charbon", EntityType.UNKNOWN),
    MINE_IRON_ORE("Miner du fer", EntityType.UNKNOWN),
    MINE_GOLD_ORE("Miner de l'or", EntityType.UNKNOWN),
    MINE_DIAMOND_ORE("Miner du diamant", EntityType.UNKNOWN),
    MINE_EMERALD_ORE("Miner de l'émeraude", EntityType.UNKNOWN),
    MINE_LAPIS_ORE("Miner du lapis", EntityType.UNKNOWN),
    MINE_REDSTONE_ORE("Miner de la redstone", EntityType.UNKNOWN),
    MINE_DEEPSLATE_COAL_ORE("Miner du charbon (deepslate)", EntityType.UNKNOWN),
    MINE_DEEPSLATE_IRON_ORE("Miner du fer (deepslate)", EntityType.UNKNOWN),
    MINE_DEEPSLATE_GOLD_ORE("Miner de l'or (deepslate)", EntityType.UNKNOWN),
    MINE_DEEPSLATE_DIAMOND_ORE("Miner du diamant (deepslate)", EntityType.UNKNOWN),
    
    // Woodcutting actions
    CUT_OAK_LOG("Couper du chêne", EntityType.UNKNOWN),
    CUT_SPRUCE_LOG("Couper du sapin", EntityType.UNKNOWN),
    CUT_BIRCH_LOG("Couper du bouleau", EntityType.UNKNOWN),
    CUT_JUNGLE_LOG("Couper de la jungle", EntityType.UNKNOWN),
    CUT_ACACIA_LOG("Couper de l'acacia", EntityType.UNKNOWN),
    CUT_DARK_OAK_LOG("Couper du chêne noir", EntityType.UNKNOWN),
    
    // Farming actions
    HARVEST_WHEAT("Récolter du blé", EntityType.UNKNOWN),
    HARVEST_CARROT("Récolter des carottes", EntityType.UNKNOWN),
    HARVEST_POTATO("Récolter des pommes de terre", EntityType.UNKNOWN),
    HARVEST_BEETROOT("Récolter des betteraves", EntityType.UNKNOWN),
    HARVEST_MELON("Récolter des melons", EntityType.UNKNOWN),
    HARVEST_PUMPKIN("Récolter des citrouilles", EntityType.UNKNOWN),
    HARVEST_SUGAR_CANE("Récolter de la canne à sucre", EntityType.UNKNOWN),
    
    // Fishing actions
    CATCH_FISH("Pêcher un poisson", EntityType.UNKNOWN),
    CATCH_TREASURE("Pêcher un trésor", EntityType.UNKNOWN),
    
    // Hunting actions (with entity types)
    KILL_ZOMBIE("Tuer un zombie", EntityType.ZOMBIE),
    KILL_SKELETON("Tuer un squelette", EntityType.SKELETON),
    KILL_CREEPER("Tuer un creeper", EntityType.CREEPER),
    KILL_SPIDER("Tuer une araignée", EntityType.SPIDER),
    KILL_ENDERMAN("Tuer un enderman", EntityType.ENDERMAN),
    KILL_WITCH("Tuer une sorcière", EntityType.WITCH),
    KILL_BLAZE("Tuer un blaze", EntityType.BLAZE),
    KILL_WITHER_SKELETON("Tuer un wither squelette", EntityType.WITHER_SKELETON),
    KILL_PIGLIN("Tuer un piglin", EntityType.PIGLIN),
    KILL_HOGLIN("Tuer un hoglin", EntityType.HOGLIN),
    
    // Building actions
    PLACE_BLOCK("Placer un bloc", EntityType.UNKNOWN),
    
    // Breeding actions
    BREED_COW("Élever une vache", EntityType.COW),
    BREED_SHEEP("Élever un mouton", EntityType.SHEEP),
    BREED_PIG("Élever un cochon", EntityType.PIG),
    BREED_CHICKEN("Élever une poule", EntityType.CHICKEN),
    
    // Crafting actions
    CRAFT_ITEM("Crafter un objet", EntityType.UNKNOWN),
    SMELT_ITEM("Fondre un objet", EntityType.UNKNOWN);
    
    private final String displayName;
    private final EntityType entityType;
    
    JobAction(String displayName, EntityType entityType) {
        this.displayName = displayName;
        this.entityType = entityType;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public EntityType getEntityType() {
        return entityType;
    }
    
    /**
     * Check if this action involves an entity type
     */
    public boolean hasEntityType() {
        return entityType != EntityType.UNKNOWN;
    }
}
