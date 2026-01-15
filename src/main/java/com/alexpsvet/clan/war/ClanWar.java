package com.alexpsvet.clan.war;

import org.bukkit.Location;
import java.util.*;

/**
 * Represents a clan war
 */
public class ClanWar {
    private final int id;
    private final String clan1Name;
    private final String clan2Name;
    private final Set<UUID> clan1Players;
    private final Set<UUID> clan2Players;
    private final Set<UUID> alivePlayers;
    private WarStatus status;
    private long startTime;
    private long preparationEndTime;
    private long battleEndTime;
    private Location clan1SpawnLocation;
    private Location clan2SpawnLocation;
    private int borderSize;
    private int shrinkingBorderSize;
    
    public ClanWar(int id, String clan1Name, String clan2Name) {
        this.id = id;
        this.clan1Name = clan1Name;
        this.clan2Name = clan2Name;
        this.clan1Players = new HashSet<>();
        this.clan2Players = new HashSet<>();
        this.alivePlayers = new HashSet<>();
        this.status = WarStatus.PENDING;
        this.borderSize = 500;
        this.shrinkingBorderSize = 500;
    }
    
    public int getId() {
        return id;
    }
    
    public String getClan1Name() {
        return clan1Name;
    }
    
    public String getClan2Name() {
        return clan2Name;
    }
    
    public Set<UUID> getClan1Players() {
        return clan1Players;
    }
    
    public Set<UUID> getClan2Players() {
        return clan2Players;
    }
    
    public void addClan1Player(UUID player) {
        clan1Players.add(player);
        alivePlayers.add(player);
    }
    
    public void addClan2Player(UUID player) {
        clan2Players.add(player);
        alivePlayers.add(player);
    }
    
    public Set<UUID> getAlivePlayers() {
        return alivePlayers;
    }
    
    public void removeAlivePlayer(UUID player) {
        alivePlayers.remove(player);
    }
    
    public WarStatus getStatus() {
        return status;
    }
    
    public void setStatus(WarStatus status) {
        this.status = status;
    }
    
    public long getStartTime() {
        return startTime;
    }
    
    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }
    
    public long getPreparationEndTime() {
        return preparationEndTime;
    }
    
    public void setPreparationEndTime(long preparationEndTime) {
        this.preparationEndTime = preparationEndTime;
    }
    
    public long getBattleEndTime() {
        return battleEndTime;
    }
    
    public void setBattleEndTime(long battleEndTime) {
        this.battleEndTime = battleEndTime;
    }
    
    public Location getClan1SpawnLocation() {
        return clan1SpawnLocation;
    }
    
    public void setClan1SpawnLocation(Location clan1SpawnLocation) {
        this.clan1SpawnLocation = clan1SpawnLocation;
    }
    
    public Location getClan2SpawnLocation() {
        return clan2SpawnLocation;
    }
    
    public void setClan2SpawnLocation(Location clan2SpawnLocation) {
        this.clan2SpawnLocation = clan2SpawnLocation;
    }
    
    public int getBorderSize() {
        return borderSize;
    }
    
    public void setBorderSize(int borderSize) {
        this.borderSize = borderSize;
    }
    
    public int getShrinkingBorderSize() {
        return shrinkingBorderSize;
    }
    
    public void setShrinkingBorderSize(int shrinkingBorderSize) {
        this.shrinkingBorderSize = shrinkingBorderSize;
    }
    
    public boolean isParticipant(UUID player) {
        return clan1Players.contains(player) || clan2Players.contains(player);
    }
    
    public String getWinnerClan() {
        if (alivePlayers.isEmpty()) {
            return null;
        }
        
        // Check which clan has alive players
        for (UUID player : alivePlayers) {
            if (clan1Players.contains(player)) {
                return clan1Name;
            } else if (clan2Players.contains(player)) {
                return clan2Name;
            }
        }
        
        return null;
    }
    
    public enum WarStatus {
        PENDING,
        PREPARATION,
        BATTLE,
        SHRINKING,
        FINISHED
    }
}
