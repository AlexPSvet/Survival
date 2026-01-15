package com.alexpsvet.territory;

/**
 * Flags for territory permissions
 */
public class TerritoryFlags {
    private boolean pvp = false;
    private boolean explosions = false;
    private boolean mobSpawning = true;
    private boolean mobGriefing = false;
    private boolean fireSpread = false;
    
    public boolean isPvp() {
        return pvp;
    }
    
    public void setPvp(boolean pvp) {
        this.pvp = pvp;
    }
    
    public boolean isExplosions() {
        return explosions;
    }
    
    public void setExplosions(boolean explosions) {
        this.explosions = explosions;
    }
    
    public boolean isMobSpawning() {
        return mobSpawning;
    }
    
    public void setMobSpawning(boolean mobSpawning) {
        this.mobSpawning = mobSpawning;
    }
    
    public boolean isMobGriefing() {
        return mobGriefing;
    }
    
    public void setMobGriefing(boolean mobGriefing) {
        this.mobGriefing = mobGriefing;
    }
    
    public boolean isFireSpread() {
        return fireSpread;
    }
    
    public void setFireSpread(boolean fireSpread) {
        this.fireSpread = fireSpread;
    }
}
