package com.alexpsvet.player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Represents a permission group (like LuckPerms)
 */
public class Group {
    private final String name;
    private final String prefix;
    private final String suffix;
    private final String chatColor;
    private final List<String> permissions;
    private final int priority; // Higher = more important
    
    public Group(String name, String prefix, String suffix, String chatColor, List<String> permissions, int priority) {
        this.name = name;
        this.prefix = prefix;
        this.suffix = suffix;
        this.chatColor = chatColor;
        this.permissions = permissions;
        this.priority = priority;
    }
    
    // Getters
    public String getName() { return name; }
    public String getPrefix() { return prefix; }
    public String getSuffix() { return suffix; }
    public String getChatColor() { return chatColor; }
    public List<String> getPermissions() { return permissions; }
    public int getPriority() { return priority; }
    
    /**
     * Check if this group has a specific permission
     */
    public boolean hasPermission(String permission) {
        // Check for wildcard permissions
        if (permissions.contains("*")) return true;
        if (permissions.contains(permission)) return true;
        
        // Check for wildcard sub-permissions (e.g., "survival.*" matches "survival.admin.give")
        for (String perm : permissions) {
            if (perm.endsWith(".*")) {
                String base = perm.substring(0, perm.length() - 2);
                if (permission.startsWith(base + ".")) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Default groups
     */
    public static class Defaults {
        public static final Group USER = new Group(
            "USER",
            "&7",
            "",
            "&7",
            Arrays.asList(
                "survival.chat",
                "survival.clan.create",
                "survival.clan.join",
                "survival.economy.balance",
                "survival.economy.pay",
                "survival.auction.use",
                "survival.tpa"
            ),
            0
        );
        
        public static final Group RANK1 = new Group(
            "RANK1",
            "&b[VIP] &b",
            "",
            "&b",
            Arrays.asList(
                "survival.*",
                "survival.premium.rank1",
                "survival.clan.maxmembers.25"
            ),
            10
        );
        
        public static final Group RANK2 = new Group(
            "RANK2",
            "&d[VIP+] &d",
            "",
            "&d",
            Arrays.asList(
                "survival.*",
                "survival.premium.rank2",
                "survival.clan.maxmembers.30",
                "survival.shop.discount.10"
            ),
            20
        );
        
        public static final Group RANK3 = new Group(
            "RANK3",
            "&6[MVP] &6",
            "",
            "&6",
            Arrays.asList(
                "survival.*",
                "survival.premium.rank3",
                "survival.clan.maxmembers.40",
                "survival.shop.discount.20",
                "survival.auction.fee.50"
            ),
            30
        );
        
        public static final Group ADMIN = new Group(
            "ADMIN",
            "&c[Admin] &c",
            "",
            "&c",
            Arrays.asList(
                "*",
                "survival.*",
                "survival.admin.*"
            ),
            90
        );
        
        public static final Group OWNER = new Group(
            "OWNER",
            "&4[Owner] &4",
            "",
            "&4",
            Arrays.asList("*"),
            100
        );
        
        public static List<Group> getAll() {
            return Arrays.asList(USER, RANK1, RANK2, RANK3, ADMIN, OWNER);
        }
        
        public static Group getByName(String name) {
            for (Group group : getAll()) {
                if (group.getName().equalsIgnoreCase(name)) {
                    return group;
                }
            }
            return USER;
        }
    }
}
