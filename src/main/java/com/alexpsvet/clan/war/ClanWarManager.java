package com.alexpsvet.clan.war;

import com.alexpsvet.Survival;
import com.alexpsvet.clan.Clan;
import com.alexpsvet.clan.ClanManager;
import com.alexpsvet.database.Database;
import com.alexpsvet.utils.MessageUtil;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manager for clan wars
 */
public class ClanWarManager {
    private static final Logger LOGGER = Logger.getLogger("survival");
    private static ClanWarManager instance;
    
    private final Database database;
    private final Map<Integer, ClanWar> activeWars;
    private final Map<String, UUID> warInvitations; // clan name -> requesting clan player UUID
    private final WarArena warArena;
    private int nextWarId = 1;
    
    // Config values
    private final int preparationMinutes;
    private final int battleMinutes;
    private final int borderShrinkStartMinutes;
    private final int borderShrinkRate;
    private final int minBorderSize;
    
    public ClanWarManager(Database database) {
        instance = this;
        this.database = database;
        this.activeWars = new HashMap<>();
        this.warInvitations = new HashMap<>();
        this.warArena = new WarArena();
        
        // Load config
        this.preparationMinutes = Survival.getInstance().getConfig().getInt("clan-wars.preparation-minutes", 20);
        this.battleMinutes = Survival.getInstance().getConfig().getInt("clan-wars.battle-minutes", 15);
        this.borderShrinkStartMinutes = Survival.getInstance().getConfig().getInt("clan-wars.border-shrink-start-minutes", 10);
        this.borderShrinkRate = Survival.getInstance().getConfig().getInt("clan-wars.border-shrink-rate", 5);
        this.minBorderSize = Survival.getInstance().getConfig().getInt("clan-wars.min-border-size", 50);
        
        createTables();
        loadActiveWars();
    }

    /**
     * Initialize/load the war arena world (admin helper)
     */
    public boolean loadArena() {
        return warArena.initializeArena();
    }

    /**
     * Teleport a player to the clan spawn inside the arena (1 or 2)
     */
    public boolean teleportToClanSpawn(int clanNumber, Player player) {
        if (warArena.getWarWorld() == null) {
            if (!warArena.initializeArena()) return false;
        }
        if (clanNumber == 1) {
            Location loc = warArena.getClan1Spawn();
            if (loc != null) {
                player.teleport(loc);
                return true;
            }
        } else if (clanNumber == 2) {
            Location loc = warArena.getClan2Spawn();
            if (loc != null) {
                player.teleport(loc);
                return true;
            }
        }
        return false;
    }

    /**
     * Start a test war with a single admin player as clan1 participant.
     * Useful to validate arena, phases and effects.
     */
    public void startTestWar(Player admin) {
        // Create a temporary war object without requiring real clans
        int warId = nextWarId++;
        ClanWar war = new ClanWar(warId, "__TEST_CLAN__", "__TEST_ENEMY__");
        war.setStatus(ClanWar.WarStatus.PREPARATION);
        war.setStartTime(System.currentTimeMillis());
        war.setPreparationEndTime(System.currentTimeMillis() + (preparationMinutes * 60 * 1000));
        war.setBattleEndTime(System.currentTimeMillis() + ((preparationMinutes + battleMinutes) * 60 * 1000));

        // Initialize arena
        if (!warArena.initializeArena()) {
            LOGGER.severe("Failed to initialize war arena for test war!");
            return;
        }

        war.setClan1SpawnLocation(warArena.getClan1Spawn());
        war.setClan2SpawnLocation(warArena.getClan2Spawn());

        // Add admin as the only participant
        war.addClan1Player(admin.getUniqueId());
        // Put into active wars map
        activeWars.put(warId, war);

        // Teleport admin to clan1 spawn
        if (war.getClan1SpawnLocation() != null) {
            admin.teleport(war.getClan1SpawnLocation());
            admin.sendMessage("[War Test] Teleporté au spawn de test (clan1)");
        }

        // Start preparation phase
        startPreparationPhase(war);
    }
    
    /**
     * Create war tables
     */
    private void createTables() {
        String warsTable = "CREATE TABLE IF NOT EXISTS clan_wars (" +
                "id INTEGER PRIMARY KEY " + (database.getType().name().equals("SQLITE") ? "AUTOINCREMENT" : "AUTO_INCREMENT") + "," +
                "clan1_name VARCHAR(16) NOT NULL," +
                "clan2_name VARCHAR(16) NOT NULL," +
                "status VARCHAR(20) NOT NULL," +
                "start_time BIGINT," +
                "end_time BIGINT," +
                "winner_clan VARCHAR(16)," +
                "created_at BIGINT NOT NULL" +
                ")";
        database.executeUpdate(warsTable);
        
        LOGGER.info("Clan war tables created/verified");
    }
    
    /**
     * Load active wars from database
     */
    private void loadActiveWars() {
        ResultSet rs = database.executeQuery("SELECT * FROM clan_wars WHERE status != 'FINISHED'");
        try {
            while (rs != null && rs.next()) {
                int id = rs.getInt("id");
                String clan1 = rs.getString("clan1_name");
                String clan2 = rs.getString("clan2_name");
                
                ClanWar war = new ClanWar(id, clan1, clan2);
                war.setStatus(ClanWar.WarStatus.valueOf(rs.getString("status")));
                
                long startTime = rs.getLong("start_time");
                if (startTime > 0) {
                    war.setStartTime(startTime);
                }
                
                activeWars.put(id, war);
                
                if (id >= nextWarId) {
                    nextWarId = id + 1;
                }
            }
            if (rs != null) rs.close();
            
            // Resume wars if server crashed
            if (!activeWars.isEmpty()) {
                LOGGER.warning("Found " + activeWars.size() + " unfinished wars. Cleaning up...");
                for (ClanWar war : new ArrayList<>(activeWars.values())) {
                    endWar(war.getId(), null, true);
                }
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to load clan wars", e);
        }
    }
    
    /**
     * Send war invitation
     */
    public boolean sendWarInvitation(String fromClan, String toClan, UUID senderUuid) {
        if (warInvitations.containsKey(toClan)) {
            return false;
        }
        
        warInvitations.put(toClan, senderUuid);
        
        // Notify target clan
        Clan targetClan = ClanManager.getInstance().getClan(toClan);
        if (targetClan != null) {
            String message = MessageUtil.colorize("&6&lGUERRE DE CLAN!\n&eLe clan &c" + fromClan + 
                " &evous a défié en guerre!\n&eUtilisez &a/clan war accept &eou &c/clan war deny &epour répondre.\n&7Vous avez 5 minutes pour répondre.");
            
            for (UUID memberUuid : targetClan.getMembers().keySet()) {
                Player member = Bukkit.getPlayer(memberUuid);
                if (member != null) {
                    member.sendMessage(message);
                    member.playSound(member.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);
                }
            }
        }
        
        // Auto-expire after 5 minutes
        new BukkitRunnable() {
            @Override
            public void run() {
                if (warInvitations.get(toClan) != null && warInvitations.get(toClan).equals(senderUuid)) {
                    warInvitations.remove(toClan);
                }
            }
        }.runTaskLater(Survival.getInstance(), 20 * 60 * 5);
        
        return true;
    }
    
    /**
     * Accept war invitation
     */
    public boolean acceptWarInvitation(String clanName) {
        if (!warInvitations.containsKey(clanName)) {
            return false;
        }
        
        UUID inviterUuid = warInvitations.remove(clanName);
        String inviterClan = ClanManager.getInstance().getPlayerClan(inviterUuid).getName();
        
        startWar(inviterClan, clanName);
        return true;
    }
    
    /**
     * Deny war invitation
     */
    public boolean denyWarInvitation(String clanName) {
        return warInvitations.remove(clanName) != null;
    }
    
    /**
     * Start a clan war
     */
    public void startWar(String clan1Name, String clan2Name) {
        // Create war record
        database.executeUpdate(
            "INSERT INTO clan_wars (clan1_name, clan2_name, status, created_at) VALUES (?, ?, ?, ?)",
            clan1Name, clan2Name, ClanWar.WarStatus.PREPARATION.name(), System.currentTimeMillis()
        );
        
        ResultSet rs = database.executeQuery("SELECT MAX(id) as max_id FROM clan_wars");
        try {
            if (rs != null && rs.next()) {
                int warId = rs.getInt("max_id");
                ClanWar war = new ClanWar(warId, clan1Name, clan2Name);
                war.setStatus(ClanWar.WarStatus.PREPARATION);
                war.setStartTime(System.currentTimeMillis());
                war.setPreparationEndTime(System.currentTimeMillis() + (preparationMinutes * 60 * 1000));
                war.setBattleEndTime(System.currentTimeMillis() + ((preparationMinutes + battleMinutes) * 60 * 1000));
                
                activeWars.put(warId, war);
                
                // Initialize arena
                if (!warArena.initializeArena()) {
                    LOGGER.severe("Failed to initialize war arena!");
                    endWar(warId, null, true);
                    return;
                }
                
                war.setClan1SpawnLocation(warArena.getClan1Spawn());
                war.setClan2SpawnLocation(warArena.getClan2Spawn());
                
                // Teleport players
                teleportClansToArena(war);
                
                // Start preparation timer
                startPreparationPhase(war);
                
                rs.close();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to create war", e);
        }
    }
    
    /**
     * Teleport clans to arena
     */
    private void teleportClansToArena(ClanWar war) {
        Clan clan1 = ClanManager.getInstance().getClan(war.getClan1Name());
        Clan clan2 = ClanManager.getInstance().getClan(war.getClan2Name());
        
        if (clan1 != null) {
            for (UUID memberUuid : clan1.getMembers().keySet()) {
                Player player = Bukkit.getPlayer(memberUuid);
                if (player != null) {
                    player.teleport(war.getClan1SpawnLocation());
                    war.addClan1Player(memberUuid);
                    player.sendMessage(MessageUtil.colorize("&6&lLA GUERRE COMMENCE!\n&eVous avez &c" + preparationMinutes + " minutes &epour vous préparer!"));
                }
            }
        }
        
        if (clan2 != null) {
            for (UUID memberUuid : clan2.getMembers().keySet()) {
                Player player = Bukkit.getPlayer(memberUuid);
                if (player != null) {
                    player.teleport(war.getClan2SpawnLocation());
                    war.addClan2Player(memberUuid);
                    player.sendMessage(MessageUtil.colorize("&6&lLA GUERRE COMMENCE!\n&eVous avez &c" + preparationMinutes + " minutes &epour vous préparer!"));
                }
            }
        }
    }
    
    /**
     * Start preparation phase
     */
    private void startPreparationPhase(ClanWar war) {
        new BukkitRunnable() {
            int timeLeft = preparationMinutes * 60;
            
            @Override
            public void run() {
                if (!activeWars.containsKey(war.getId())) {
                    cancel();
                    return;
                }
                
                if (timeLeft <= 0) {
                    startBattlePhase(war);
                    cancel();
                    return;
                }
                
                // Countdown messages
                if (timeLeft == 60 || timeLeft == 30 || timeLeft == 10 || timeLeft <= 5) {
                    broadcastToWar(war, "&eLa bataille commence dans &c" + timeLeft + " &esecondes!");
                    
                    for (UUID playerUuid : war.getClan1Players()) {
                        Player player = Bukkit.getPlayer(playerUuid);
                        if (player != null) {
                            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
                        }
                    }
                    for (UUID playerUuid : war.getClan2Players()) {
                        Player player = Bukkit.getPlayer(playerUuid);
                        if (player != null) {
                            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
                        }
                    }
                }
                
                timeLeft--;
            }
        }.runTaskTimer(Survival.getInstance(), 0, 20);
    }
    
    /**
     * Start battle phase
     */
    private void startBattlePhase(ClanWar war) {
        war.setStatus(ClanWar.WarStatus.BATTLE);
        updateWarStatus(war);
        
        broadcastToWar(war, "&c&lLA BATAILLE COMMENCE!\n&eÉliminez tous les membres du clan adverse!");
        
        for (UUID playerUuid : war.getClan1Players()) {
            Player player = Bukkit.getPlayer(playerUuid);
            if (player != null) {
                player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 1.0f);
            }
        }
        for (UUID playerUuid : war.getClan2Players()) {
            Player player = Bukkit.getPlayer(playerUuid);
            if (player != null) {
                player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 1.0f);
            }
        }
        
        // Start battle timer
        new BukkitRunnable() {
            int timeLeft = battleMinutes * 60;
            
            @Override
            public void run() {
                if (!activeWars.containsKey(war.getId())) {
                    cancel();
                    return;
                }
                
                // Check for winners
                checkWarEnd(war);
                
                if (war.getStatus() == ClanWar.WarStatus.FINISHED) {
                    cancel();
                    return;
                }
                
                if (timeLeft <= 0) {
                    startShrinkingPhase(war);
                    cancel();
                    return;
                }
                
                // Countdown messages
                if (timeLeft == 300 || timeLeft == 60 || timeLeft == 30 || timeLeft == 10 || timeLeft <= 5) {
                    broadcastToWar(war, "&eLa bordure commence à rétrécir dans &c" + timeLeft + " &esecondes!");
                }
                
                timeLeft--;
            }
        }.runTaskTimer(Survival.getInstance(), 0, 20);
    }
    
    /**
     * Start shrinking border phase
     */
    private void startShrinkingPhase(ClanWar war) {
        war.setStatus(ClanWar.WarStatus.SHRINKING);
        updateWarStatus(war);
        
        broadcastToWar(war, "&c&lLA BORDURE RÉTRÉCIT!\n&eDépêchez-vous ou vous serez éliminés!");
        
        World warWorld = warArena.getWarWorld();
        if (warWorld != null) {
            WorldBorder border = warWorld.getWorldBorder();
            border.setCenter(warArena.getCenterLocation());
            border.setSize(minBorderSize, borderShrinkRate * 60);
        }
        
        // Check for winners every second
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!activeWars.containsKey(war.getId())) {
                    cancel();
                    return;
                }
                
                checkWarEnd(war);
                
                if (war.getStatus() == ClanWar.WarStatus.FINISHED) {
                    cancel();
                }
            }
        }.runTaskTimer(Survival.getInstance(), 0, 20);
    }
    
    /**
     * Check if war should end
     */
    private void checkWarEnd(ClanWar war) {
        Set<UUID> alive1 = new HashSet<>();
        Set<UUID> alive2 = new HashSet<>();
        
        for (UUID playerUuid : war.getAlivePlayers()) {
            if (war.getClan1Players().contains(playerUuid)) {
                alive1.add(playerUuid);
            } else if (war.getClan2Players().contains(playerUuid)) {
                alive2.add(playerUuid);
            }
        }
        
        String winner = null;
        if (alive1.isEmpty() && !alive2.isEmpty()) {
            winner = war.getClan2Name();
        } else if (alive2.isEmpty() && !alive1.isEmpty()) {
            winner = war.getClan1Name();
        } else if (alive1.isEmpty() && alive2.isEmpty()) {
            winner = null; // Draw
        }
        
        if (winner != null || (alive1.isEmpty() && alive2.isEmpty())) {
            endWar(war.getId(), winner, false);
        }
    }
    
    /**
     * End a war
     */
    public void endWar(int warId, String winnerClan, boolean crashed) {
        ClanWar war = activeWars.remove(warId);
        if (war == null) return;
        
        war.setStatus(ClanWar.WarStatus.FINISHED);
        
        // Update database
        database.executeUpdate(
            "UPDATE clan_wars SET status = ?, end_time = ?, winner_clan = ? WHERE id = ?",
            ClanWar.WarStatus.FINISHED.name(), System.currentTimeMillis(), winnerClan, warId
        );
        
        if (!crashed && winnerClan != null) {
            broadcastToWar(war, "&6&l=========================");
            broadcastToWar(war, "&e&lFIN DE LA GUERRE!");
            broadcastToWar(war, "&a&lVICTOIRE: &6" + winnerClan);
            broadcastToWar(war, "&6&l=========================");
            
            // Fireworks for winners
            Clan winnerClanObj = ClanManager.getInstance().getClan(winnerClan);
            if (winnerClanObj != null) {
                for (UUID memberUuid : winnerClanObj.getMembers().keySet()) {
                    Player player = Bukkit.getPlayer(memberUuid);
                    if (player != null) {
                        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                    }
                }
            }
        }
        
        // Teleport players back
        World mainWorld = Bukkit.getWorlds().get(0);
        Location spawn = mainWorld.getSpawnLocation();
        
        for (UUID playerUuid : war.getClan1Players()) {
            Player player = Bukkit.getPlayer(playerUuid);
            if (player != null) {
                player.teleport(spawn);
            }
        }
        for (UUID playerUuid : war.getClan2Players()) {
            Player player = Bukkit.getPlayer(playerUuid);
            if (player != null) {
                player.teleport(spawn);
            }
        }
        
        // Restore arena
        warArena.restoreArena();
    }
    
    /**
     * Broadcast message to all war participants
     */
    private void broadcastToWar(ClanWar war, String message) {
        String formatted = MessageUtil.colorize(message);
        
        for (UUID playerUuid : war.getClan1Players()) {
            Player player = Bukkit.getPlayer(playerUuid);
            if (player != null) {
                player.sendMessage(formatted);
            }
        }
        for (UUID playerUuid : war.getClan2Players()) {
            Player player = Bukkit.getPlayer(playerUuid);
            if (player != null) {
                player.sendMessage(formatted);
            }
        }
    }
    
    /**
     * Update war status in database
     */
    private void updateWarStatus(ClanWar war) {
        database.executeUpdate(
            "UPDATE clan_wars SET status = ? WHERE id = ?",
            war.getStatus().name(), war.getId()
        );
    }
    
    /**
     * Handle player death in war
     */
    public void handlePlayerDeath(UUID player) {
        for (ClanWar war : activeWars.values()) {
            if (war.isParticipant(player)) {
                war.removeAlivePlayer(player);
                
                Player p = Bukkit.getPlayer(player);
                if (p != null) {
                    p.setGameMode(GameMode.SPECTATOR);
                    p.sendMessage(MessageUtil.colorize("&cVous avez été éliminé! Vous êtes en mode spectateur."));
                }
                
                checkWarEnd(war);
                break;
            }
        }
    }
    
    /**
     * Get active war for a player
     */
    public ClanWar getPlayerWar(UUID player) {
        for (ClanWar war : activeWars.values()) {
            if (war.isParticipant(player)) {
                return war;
            }
        }
        return null;
    }
    
    public boolean hasWarInvitation(String clanName) {
        return warInvitations.containsKey(clanName);
    }
    
    public static ClanWarManager getInstance() {
        return instance;
    }
}
