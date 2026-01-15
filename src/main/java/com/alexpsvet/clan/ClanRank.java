package com.alexpsvet.clan;

/**
 * Clan member ranks
 */
public enum ClanRank {
    MEMBER(0, "Membre"),
    MODERATOR(1, "Modérateur"),
    ADMIN(2, "Admin"),
    LEADER(3, "Chef");
    
    private final int level;
    private final String displayName;
    
    ClanRank(int level, String displayName) {
        this.level = level;
        this.displayName = displayName;
    }
    
    public int getLevel() {
        return level;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public boolean isHigherThan(ClanRank other) {
        return this.level > other.level;
    }
    
    public boolean canManageRank(ClanRank targetRank) {
        return this.level > targetRank.level;
    }
}
