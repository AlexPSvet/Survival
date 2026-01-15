package com.alexpsvet.clan;

import org.bukkit.Location;

import java.util.*;

/**
 * Represents a clan
 */
public class Clan {
    private final String name;
    private final String tag;
    private UUID leader;
    private final Map<UUID, ClanRank> members;
    private final Set<String> allies;
    private final Set<String> enemies;
    private Location home;
    private final long createdAt;
    private String description;
    private boolean friendlyFire;
    
    public Clan(String name, String tag, UUID leader) {
        this.name = name;
        this.tag = tag;
        this.leader = leader;
        this.members = new HashMap<>();
        this.allies = new HashSet<>();
        this.enemies = new HashSet<>();
        this.createdAt = System.currentTimeMillis();
        this.description = "";
        this.friendlyFire = false;
        
        // Add leader as LEADER rank
        members.put(leader, ClanRank.LEADER);
    }
    
    public String getName() {
        return name;
    }
    
    public String getTag() {
        return tag;
    }
    
    public UUID getLeader() {
        return leader;
    }
    
    public void setLeader(UUID leader) {
        this.leader = leader;
    }
    
    public Map<UUID, ClanRank> getMembers() {
        return members;
    }
    
    public void addMember(UUID uuid, ClanRank rank) {
        members.put(uuid, rank);
    }
    
    public void removeMember(UUID uuid) {
        members.remove(uuid);
    }
    
    public boolean isMember(UUID uuid) {
        return members.containsKey(uuid);
    }
    
    public ClanRank getRank(UUID uuid) {
        return members.getOrDefault(uuid, ClanRank.MEMBER);
    }
    
    public void setRank(UUID uuid, ClanRank rank) {
        members.put(uuid, rank);
    }
    
    public Set<String> getAllies() {
        return allies;
    }
    
    public void addAlly(String clanName) {
        allies.add(clanName);
    }
    
    public void removeAlly(String clanName) {
        allies.remove(clanName);
    }
    
    public boolean isAlly(String clanName) {
        return allies.contains(clanName);
    }
    
    public Set<String> getEnemies() {
        return enemies;
    }
    
    public void addEnemy(String clanName) {
        enemies.add(clanName);
    }
    
    public void removeEnemy(String clanName) {
        enemies.remove(clanName);
    }
    
    public boolean isEnemy(String clanName) {
        return enemies.contains(clanName);
    }
    
    public Location getHome() {
        return home;
    }
    
    public void setHome(Location home) {
        this.home = home;
    }
    
    public long getCreatedAt() {
        return createdAt;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public boolean isFriendlyFire() {
        return friendlyFire;
    }
    
    public void setFriendlyFire(boolean friendlyFire) {
        this.friendlyFire = friendlyFire;
    }
    
    public int getMemberCount() {
        return members.size();
    }
    
    public List<UUID> getMembersByRank(ClanRank rank) {
        List<UUID> result = new ArrayList<>();
        for (Map.Entry<UUID, ClanRank> entry : members.entrySet()) {
            if (entry.getValue() == rank) {
                result.add(entry.getKey());
            }
        }
        return result;
    }
}
