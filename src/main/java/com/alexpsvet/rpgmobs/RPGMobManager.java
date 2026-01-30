package com.alexpsvet.rpgmobs;

import com.alexpsvet.Survival;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.util.*;
import java.util.logging.Logger;

/**
 * Manages RPG mobs system
 */
public class RPGMobManager {
    private static final Logger LOGGER = Logger.getLogger("survival");
    private static RPGMobManager instance;

    private final Map<EntityType, RPGMobConfig> mobConfigs;
    private final Map<UUID, RPGMob> activeMobs;
    private final Random random;

    // Global settings
    private boolean enabled;
    private String nameFormat;
    private boolean showHealthBar;
    private String healthBarFormat;
    private double expMultiplier;
    private double moneyMultiplier;

    public RPGMobManager() {
        instance = this;
        this.mobConfigs = new HashMap<>();
        this.activeMobs = new HashMap<>();
        this.random = new Random();
        loadConfig();
    }

    /**
     * Load configuration from rpgmobs.yml
     */
    private void loadConfig() {
        File configFile = new File(Survival.getInstance().getDataFolder(), "rpgmobs.yml");
        if (!configFile.exists()) {
            Survival.getInstance().saveResource("rpgmobs.yml", false);
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        // Load global settings
        ConfigurationSection settings = config.getConfigurationSection("settings");
        if (settings != null) {
            enabled = settings.getBoolean("enabled", true);
            nameFormat = ChatColor.translateAlternateColorCodes('&', 
                settings.getString("name-format", "&c[Lvl {level}] &e{name}"));
            showHealthBar = settings.getBoolean("show-health-bar", true);
            healthBarFormat = ChatColor.translateAlternateColorCodes('&',
                settings.getString("health-bar-format", "&a❤ &c{health}&7/&c{max-health}"));
            expMultiplier = settings.getDouble("exp-multiplier", 1.5);
            moneyMultiplier = settings.getDouble("money-multiplier", 2.0);
        }

        // Load individual mob configs
        ConfigurationSection mobsSection = config.getConfigurationSection("mobs");
        if (mobsSection == null) {
            LOGGER.warning("No mobs configured in rpgmobs.yml!");
            return;
        }

        for (String mobType : mobsSection.getKeys(false)) {
            try {
                EntityType entityType = EntityType.valueOf(mobType);
                ConfigurationSection mobSection = mobsSection.getConfigurationSection(mobType);
                if (mobSection == null) continue;

                RPGMobConfig mobConfig = loadMobConfig(entityType, mobSection);
                mobConfigs.put(entityType, mobConfig);
            } catch (IllegalArgumentException e) {
                LOGGER.warning("Invalid entity type: " + mobType);
            }
        }

        LOGGER.info("Loaded " + mobConfigs.size() + " RPG mob configurations");
    }

    /**
     * Load individual mob configuration
     */
    private RPGMobConfig loadMobConfig(EntityType entityType, ConfigurationSection section) {
        RPGMobConfig config = new RPGMobConfig(entityType);

        config.setEnabled(section.getBoolean("enabled", true));
        config.setSpawnChance(section.getDouble("spawn-chance", 1.0));
        config.setHealthMultiplier(section.getDouble("health-multiplier", 1.0));
        config.setDamageMultiplier(section.getDouble("damage-multiplier", 1.0));
        config.setSpeedMultiplier(section.getDouble("speed-multiplier", 1.0));

        // Level
        ConfigurationSection levelSection = section.getConfigurationSection("level");
        if (levelSection != null) {
            config.setMinLevel(levelSection.getInt("min", 1));
            config.setMaxLevel(levelSection.getInt("max", 5));
        }

        config.setCustomName(section.getString("custom-name", ""));
        
        // Explosion power (for creepers)
        if (section.contains("explosion-power")) {
            config.setExplosionPower(section.getDouble("explosion-power"));
        }

        // Equipment
        ConfigurationSection equipmentSection = section.getConfigurationSection("equipment");
        if (equipmentSection != null) {
            for (String slotName : equipmentSection.getKeys(false)) {
                try {
                    RPGMobConfig.EquipmentSlot slot = RPGMobConfig.EquipmentSlot.valueOf(slotName);
                    ConfigurationSection equipSection = equipmentSection.getConfigurationSection(slotName);
                    if (equipSection != null) {
                        Material material = Material.valueOf(equipSection.getString("material", "AIR"));
                        double chance = equipSection.getDouble("chance", 1.0);
                        config.addEquipment(slot, new RPGMobConfig.Equipment(material, chance));
                    }
                } catch (IllegalArgumentException e) {
                    LOGGER.warning("Invalid equipment slot or material for " + entityType + ": " + slotName);
                }
            }
        }

        // Abilities
        List<?> abilitiesList = section.getList("abilities");
        if (abilitiesList != null) {
            for (Object obj : abilitiesList) {
                if (obj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> abilityMap = (Map<String, Object>) obj;
                    try {
                        PotionEffectType type = PotionEffectType.getByName((String) abilityMap.get("type"));
                        if (type != null) {
                            int duration = ((Number) abilityMap.getOrDefault("duration", 100)).intValue();
                            int amplifier = ((Number) abilityMap.getOrDefault("amplifier", 0)).intValue();
                            double chance = ((Number) abilityMap.getOrDefault("chance", 1.0)).doubleValue();
                            config.addAbility(new RPGMobConfig.Ability(type, duration, amplifier, chance));
                        }
                    } catch (Exception e) {
                        LOGGER.warning("Error loading ability for " + entityType + ": " + e.getMessage());
                    }
                }
            }
        }

        // Attack effects
        List<?> attackEffectsList = section.getList("attack-effects");
        if (attackEffectsList != null) {
            for (Object obj : attackEffectsList) {
                if (obj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> effectMap = (Map<String, Object>) obj;
                    try {
                        PotionEffectType type = PotionEffectType.getByName((String) effectMap.get("type"));
                        if (type != null) {
                            int duration = ((Number) effectMap.getOrDefault("duration", 100)).intValue();
                            int amplifier = ((Number) effectMap.getOrDefault("amplifier", 0)).intValue();
                            config.addAttackEffect(new PotionEffect(type, duration, amplifier));
                        }
                    } catch (Exception e) {
                        LOGGER.warning("Error loading attack effect for " + entityType + ": " + e.getMessage());
                    }
                }
            }
        }

        // Drops
        List<?> dropsList = section.getList("drops");
        if (dropsList != null) {
            for (Object obj : dropsList) {
                if (obj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> dropMap = (Map<String, Object>) obj;
                    try {
                        Material material = Material.valueOf((String) dropMap.get("material"));
                        int minAmount = ((Number) dropMap.getOrDefault("min-amount", 1)).intValue();
                        int maxAmount = ((Number) dropMap.getOrDefault("max-amount", 1)).intValue();
                        double chance = ((Number) dropMap.getOrDefault("chance", 1.0)).doubleValue();
                        config.addDrop(new RPGMobConfig.Drop(material, minAmount, maxAmount, chance));
                    } catch (Exception e) {
                        LOGGER.warning("Error loading drop for " + entityType + ": " + e.getMessage());
                    }
                }
            }
        }

        // Powers
        List<?> powersList = section.getList("powers");
        if (powersList != null) {
            for (Object obj : powersList) {
                if (obj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> powerMap = (Map<String, Object>) obj;
                    try {
                        RPGMobPower.PowerType type = RPGMobPower.PowerType.valueOf((String) powerMap.get("type"));
                        double chance = ((Number) powerMap.getOrDefault("chance", 0.1)).doubleValue();
                        int cooldown = ((Number) powerMap.getOrDefault("cooldown", 100)).intValue();
                        config.addPower(new RPGMobPower(type, chance, cooldown));
                    } catch (Exception e) {
                        LOGGER.warning("Error loading power for " + entityType + ": " + e.getMessage());
                    }
                }
            }
        }

        // World multipliers
        ConfigurationSection worldMultipliersSection = section.getConfigurationSection("world-multipliers");
        if (worldMultipliersSection != null) {
            for (String worldName : worldMultipliersSection.getKeys(false)) {
                ConfigurationSection worldSection = worldMultipliersSection.getConfigurationSection(worldName);
                if (worldSection != null) {
                    double healthMult = worldSection.getDouble("health-multiplier", 1.0);
                    double damageMult = worldSection.getDouble("damage-multiplier", 1.0);
                    double speedMult = worldSection.getDouble("speed-multiplier", 1.0);
                    config.addWorldMultiplier(worldName, 
                        new RPGMobConfig.WorldMultiplier(healthMult, damageMult, speedMult));
                }
            }
        }

        // Height multipliers
        List<?> heightMultipliersList = section.getList("height-multipliers");
        if (heightMultipliersList != null) {
            for (Object obj : heightMultipliersList) {
                if (obj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> heightMap = (Map<String, Object>) obj;
                    try {
                        int minHeight = ((Number) heightMap.get("min-height")).intValue();
                        int maxHeight = ((Number) heightMap.get("max-height")).intValue();
                        double healthMult = ((Number) heightMap.getOrDefault("health-multiplier", 1.0)).doubleValue();
                        double damageMult = ((Number) heightMap.getOrDefault("damage-multiplier", 1.0)).doubleValue();
                        double speedMult = ((Number) heightMap.getOrDefault("speed-multiplier", 1.0)).doubleValue();
                        config.addHeightMultiplier(
                            new RPGMobConfig.HeightMultiplier(minHeight, maxHeight, healthMult, damageMult, speedMult));
                    } catch (Exception e) {
                        LOGGER.warning("Error loading height multiplier for " + entityType + ": " + e.getMessage());
                    }
                }
            }
        }

        return config;
    }

    /**
     * Apply RPG customization to a mob
     */
    public RPGMob customizeMob(LivingEntity entity) {
        if (!enabled) return null;

        EntityType type = entity.getType();
        RPGMobConfig config = mobConfigs.get(type);

        if (config == null || !config.isEnabled()) {
            return null;
        }

        // Check spawn chance
        if (random.nextDouble() > config.getSpawnChance()) {
            return null; // This mob won't become an RPG mob
        }

        // Generate random level
        int level = random.nextInt(config.getMaxLevel() - config.getMinLevel() + 1) + config.getMinLevel();

        // Calculate world and height multipliers
        double worldHealthMult = 1.0;
        double worldDamageMult = 1.0;
        double worldSpeedMult = 1.0;
        double heightHealthMult = 1.0;
        double heightDamageMult = 1.0;
        double heightSpeedMult = 1.0;

        // Apply world-specific multipliers
        String worldName = entity.getWorld().getName().toLowerCase();
        RPGMobConfig.WorldMultiplier worldMult = config.getWorldMultipliers().get(worldName);
        if (worldMult != null) {
            worldHealthMult = worldMult.getHealthMultiplier();
            worldDamageMult = worldMult.getDamageMultiplier();
            worldSpeedMult = worldMult.getSpeedMultiplier();
        }

        // Apply height-specific multipliers
        int entityY = entity.getLocation().getBlockY();
        for (RPGMobConfig.HeightMultiplier heightMult : config.getHeightMultipliers()) {
            if (heightMult.isInRange(entityY)) {
                heightHealthMult = heightMult.getHealthMultiplier();
                heightDamageMult = heightMult.getDamageMultiplier();
                heightSpeedMult = heightMult.getSpeedMultiplier();
                break; // Use first matching range
            }
        }

        // Apply health with all multipliers
        double baseHealth = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue();
        double newHealth = baseHealth * config.getHealthMultiplier() * worldHealthMult * heightHealthMult * (1 + (level - 1) * 0.1);
        entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(newHealth);
        entity.setHealth(newHealth);

        // Apply speed with all multipliers
        if (entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED) != null) {
            double baseSpeed = entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getBaseValue();
            entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(
                baseSpeed * config.getSpeedMultiplier() * worldSpeedMult * heightSpeedMult);
        }

        // Apply damage with all multipliers (for attack damage attribute)
        if (entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE) != null) {
            double baseDamage = entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).getBaseValue();
            entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(
                baseDamage * config.getDamageMultiplier() * worldDamageMult * heightDamageMult * (1 + (level - 1) * 0.05));
        }

        // Apply explosion power (creepers)
        if (entity instanceof Creeper && config.getExplosionPower() != null) {
            Creeper creeper = (Creeper) entity;
            creeper.setExplosionRadius((int) Math.round(config.getExplosionPower()));
        }

        // Apply equipment
        EntityEquipment equipment = entity.getEquipment();
        if (equipment != null) {
            for (Map.Entry<RPGMobConfig.EquipmentSlot, RPGMobConfig.Equipment> entry : 
                    config.getEquipment().entrySet()) {
                if (random.nextDouble() <= entry.getValue().getChance()) {
                    ItemStack item = new ItemStack(entry.getValue().getMaterial());
                    switch (entry.getKey()) {
                        case HELMET:
                            equipment.setHelmet(item);
                            equipment.setHelmetDropChance(0.0f);
                            break;
                        case CHESTPLATE:
                            equipment.setChestplate(item);
                            equipment.setChestplateDropChance(0.0f);
                            break;
                        case LEGGINGS:
                            equipment.setLeggings(item);
                            equipment.setLeggingsDropChance(0.0f);
                            break;
                        case BOOTS:
                            equipment.setBoots(item);
                            equipment.setBootsDropChance(0.0f);
                            break;
                        case MAINHAND:
                            equipment.setItemInMainHand(item);
                            equipment.setItemInMainHandDropChance(0.0f);
                            break;
                        case OFFHAND:
                            equipment.setItemInOffHand(item);
                            equipment.setItemInOffHandDropChance(0.0f);
                            break;
                    }
                }
            }
        }

        // Apply abilities (potion effects)
        for (RPGMobConfig.Ability ability : config.getAbilities()) {
            if (random.nextDouble() <= ability.getChance()) {
                entity.addPotionEffect(new PotionEffect(
                    ability.getType(), 
                    ability.getDuration(), 
                    ability.getAmplifier(), 
                    false, 
                    false
                ));
            }
        }

        // Set custom name
        String mobName = config.getCustomName().isEmpty() ? 
            formatEntityName(type) : config.getCustomName();
        String displayName = nameFormat
            .replace("{level}", String.valueOf(level))
            .replace("{name}", mobName);
        
        entity.setCustomName(displayName);
        entity.setCustomNameVisible(true);

        // Create RPG mob instance
        RPGMob rpgMob = new RPGMob(entity, config, level, displayName);
        activeMobs.put(entity.getUniqueId(), rpgMob);
        
        // Initialize health display
        if (showHealthBar) {
            updateHealthDisplay(rpgMob);
        }

        return rpgMob;
    }

    /**
     * Format entity type name for display
     */
    private String formatEntityName(EntityType type) {
        String name = type.name().replace("_", " ").toLowerCase();
        String[] words = name.split(" ");
        StringBuilder formatted = new StringBuilder();
        for (String word : words) {
            if (formatted.length() > 0) formatted.append(" ");
            formatted.append(word.substring(0, 1).toUpperCase()).append(word.substring(1));
        }
        return formatted.toString();
    }

    /**
     * Get RPG mob by entity UUID
     */
    public RPGMob getRPGMob(UUID uuid) {
        return activeMobs.get(uuid);
    }

    /**
     * Remove RPG mob from tracking
     */
    public void removeMob(UUID uuid) {
        activeMobs.remove(uuid);
    }

    /**
     * Get custom drops for a mob
     */
    public List<ItemStack> getCustomDrops(RPGMob rpgMob) {
        List<ItemStack> drops = new ArrayList<>();
        RPGMobConfig config = rpgMob.getConfig();

        for (RPGMobConfig.Drop drop : config.getDrops()) {
            if (random.nextDouble() <= drop.getChance()) {
                int amount = random.nextInt(drop.getMaxAmount() - drop.getMinAmount() + 1) 
                    + drop.getMinAmount();
                ItemStack item = new ItemStack(drop.getMaterial(), amount);
                drops.add(item);
            }
        }

        return drops;
    }

    /**
     * Update mob health display
     */
    public void updateHealthDisplay(RPGMob rpgMob) {
        if (!showHealthBar || !rpgMob.isValid()) return;

        LivingEntity entity = rpgMob.getEntity();
        double health = Math.round(entity.getHealth() * 10.0) / 10.0;
        double maxHealth = Math.round(rpgMob.getMaxHealth() * 10.0) / 10.0;

        String healthBar = healthBarFormat
            .replace("{health}", String.valueOf(health))
            .replace("{max-health}", String.valueOf(maxHealth));

        // Always reconstruct the full name with updated health
        String fullName = rpgMob.getBaseName() + " " + healthBar;
        entity.setCustomName(fullName);
    }

    // Getters
    public static RPGMobManager getInstance() {
        return instance;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public double getExpMultiplier() {
        return expMultiplier;
    }

    public double getMoneyMultiplier() {
        return moneyMultiplier;
    }

    public Map<UUID, RPGMob> getActiveMobs() {
        return activeMobs;
    }
}
