package com.alexpsvet;

import java.util.logging.Logger;
import java.io.File;
import org.bukkit.plugin.java.JavaPlugin;

import com.alexpsvet.utils.MessageUtil;
import com.alexpsvet.utils.menu.MenuListener;
import com.alexpsvet.utils.menu.MenuManager;
import com.alexpsvet.database.Database;
import com.alexpsvet.economy.EconomyManager;
import com.alexpsvet.economy.SalaryTask;
import com.alexpsvet.clan.ClanManager;
import com.alexpsvet.clan.war.ClanWarManager;
import com.alexpsvet.chat.ChatManager;
import com.alexpsvet.auction.AuctionManager;
import com.alexpsvet.territory.TerritoryManager;
import com.alexpsvet.territory.TerritoryDisplayManager;
import com.alexpsvet.player.PlayerStatsManager;
import com.alexpsvet.teleport.TeleportManager;
import com.alexpsvet.shop.ShopManager;
import com.alexpsvet.bounty.BountyManager;
import com.alexpsvet.jobs.JobsManager;
import com.alexpsvet.home.HomeManager;
import com.alexpsvet.display.ScoreboardManager;
import com.alexpsvet.display.TabManager;
import com.alexpsvet.commands.EconomyCommand;
import com.alexpsvet.commands.EconomyAdminCommand;
import com.alexpsvet.commands.ClanCommand;
import com.alexpsvet.commands.AuctionCommand;
import com.alexpsvet.commands.TeleportCommand;
import com.alexpsvet.commands.TradeCommand;
import com.alexpsvet.commands.ShopCommand;
import com.alexpsvet.commands.ProtectionCommand;
import com.alexpsvet.commands.GuideCommand;
import com.alexpsvet.commands.ClanWarAdminCommand;
import com.alexpsvet.commands.BountyCommand;
import com.alexpsvet.commands.JobsCommand;
import com.alexpsvet.commands.GambleCommand;
import com.alexpsvet.commands.HomeCommand;
import com.alexpsvet.commands.TradeCommand;
import com.alexpsvet.commands.MessageCommand;
import com.alexpsvet.trade.TradeManager;
import com.alexpsvet.listeners.PlayerJoinListener;
import com.alexpsvet.listeners.ClanListener;
import com.alexpsvet.listeners.ClanWarListener;
import com.alexpsvet.listeners.ChatListener;
import com.alexpsvet.listeners.TerritoryListener;
import com.alexpsvet.listeners.StatsListener;
import com.alexpsvet.bounty.BountyListener;
import com.alexpsvet.jobs.JobsListener;
import com.alexpsvet.rpgmobs.RPGMobManager;
import com.alexpsvet.rpgmobs.RPGMobListener;

/*
 * survival java plugin
 */
public class Survival extends JavaPlugin {
  private static final Logger LOGGER = Logger.getLogger("survival");
  private static Survival instance;
  private Database database;
  private EconomyManager economyManager;
  private ClanManager clanManager;
  private ClanWarManager clanWarManager;
  private ChatManager chatManager;
  private AuctionManager auctionManager;
  private TerritoryManager territoryManager;
  private TerritoryDisplayManager territoryDisplayManager;
  private PlayerStatsManager statsManager;
  private TeleportManager teleportManager;
  private ShopManager shopManager;
  private BountyManager bountyManager;
  private JobsManager jobsManager;
  private HomeManager homeManager;
  private ScoreboardManager scoreboardManager;
  private TabManager tabManager;
  private SalaryTask salaryTask;
  private RPGMobManager rpgMobManager;

  @Override
  public void onEnable() {
    instance = this;
    // Ensure plugin data folder exists
    if (!getDataFolder().exists()) {
      getDataFolder().mkdirs();
    }

    // Save default config
    saveDefaultConfig();
    
    // Initialize database
    if (!initDatabase()) {
      LOGGER.severe("Failed to initialize database! Disabling plugin...");
      getServer().getPluginManager().disablePlugin(this);
      return;
    }
    
    // Initialize managers
    economyManager = new EconomyManager(database);
    shopManager = new ShopManager();
    clanManager = new ClanManager(database);
    clanWarManager = new ClanWarManager(database);
    chatManager = new ChatManager();
    auctionManager = new AuctionManager(database);
    territoryManager = new TerritoryManager(database);
    territoryDisplayManager = new TerritoryDisplayManager(this, territoryManager);
    statsManager = new PlayerStatsManager(database);
    teleportManager = new TeleportManager();
    bountyManager = new BountyManager(database);
    jobsManager = new JobsManager(database);
    homeManager = new HomeManager(database);
    scoreboardManager = new ScoreboardManager();
    tabManager = new TabManager();
    rpgMobManager = new RPGMobManager();
    
    // Initialize trade manager
    TradeManager.getInstance();
    
    // Initialize shop sell menu (registers listener)
    new com.alexpsvet.shop.menu.ShopSellMenu();
    
    // Initialize trade menu (registers listener)
    new com.alexpsvet.trade.menu.TradeMenu();
    
    // Register listeners
    getServer().getPluginManager().registerEvents(new MenuListener(), this);
    getServer().getPluginManager().registerEvents(new PlayerJoinListener(), this);
    getServer().getPluginManager().registerEvents(new ClanListener(), this);
    getServer().getPluginManager().registerEvents(new ClanWarListener(), this);
    getServer().getPluginManager().registerEvents(new ChatListener(), this);
    getServer().getPluginManager().registerEvents(new TerritoryListener(), this);
    getServer().getPluginManager().registerEvents(new StatsListener(), this);
    getServer().getPluginManager().registerEvents(new BountyListener(), this);
    getServer().getPluginManager().registerEvents(new JobsListener(), this);
    getServer().getPluginManager().registerEvents(new RPGMobListener(rpgMobManager), this);
    
    // Register commands
    EconomyCommand economyCommand = new EconomyCommand();
    getCommand("balance").setExecutor(economyCommand);
    getCommand("bal").setExecutor(economyCommand);
    getCommand("pay").setExecutor(economyCommand);
    getCommand("economy").setExecutor(economyCommand);
    getCommand("eco").setExecutor(economyCommand);
    // admin command uses BaseCommand with sub-commands
    EconomyAdminCommand ecoAdmin = new EconomyAdminCommand();
    getCommand("ecoadmin").setExecutor(ecoAdmin);
    getCommand("ecoadmin").setTabCompleter(ecoAdmin);
    
    ClanCommand clanCommand = new ClanCommand();
    getCommand("clan").setExecutor(clanCommand);
    
    AuctionCommand auctionCommand = new AuctionCommand();
    getCommand("ah").setExecutor(auctionCommand);
    getCommand("ah").setTabCompleter(auctionCommand);
    
    TeleportCommand teleportCommand = new TeleportCommand();
    getCommand("tpa").setExecutor(teleportCommand);
    getCommand("tpa").setTabCompleter(teleportCommand);
    getCommand("tpaccept").setExecutor(teleportCommand);
    getCommand("tpdeny").setExecutor(teleportCommand);
    
    ShopCommand shopCommand = new ShopCommand();
    getCommand("shop").setExecutor(shopCommand);
    
    ProtectionCommand protectionCommand = new ProtectionCommand();
    getCommand("protection").setExecutor(protectionCommand);
    getCommand("protection").setTabCompleter(protectionCommand);
    
    GuideCommand guideCommand = new GuideCommand();
    getCommand("guide").setExecutor(guideCommand);
    
    ClanWarAdminCommand warAdminCommand = new ClanWarAdminCommand();
    getCommand("waradmin").setExecutor(warAdminCommand);
    
    BountyCommand bountyCommand = new BountyCommand();
    getCommand("bounty").setExecutor(bountyCommand);
    
    JobsCommand jobsCommand = new JobsCommand();
    getCommand("jobs").setExecutor(jobsCommand);
    
    GambleCommand gambleCommand = new GambleCommand();
    getCommand("gamble").setExecutor(gambleCommand);
    
    HomeCommand homeCommand = new HomeCommand();
    getCommand("home").setExecutor(homeCommand);
    getCommand("home").setTabCompleter(homeCommand);
    getCommand("sethome").setExecutor(homeCommand);
    getCommand("sethome").setTabCompleter(homeCommand);
    getCommand("delhome").setExecutor(homeCommand);
    getCommand("delhome").setTabCompleter(homeCommand);
    
    TradeCommand tradeCommand = new TradeCommand();
    getCommand("trade").setExecutor(tradeCommand);
    getCommand("trade").setTabCompleter(tradeCommand);
    
    MessageCommand messageCommand = new MessageCommand();
    getCommand("msg").setExecutor(messageCommand);
    getCommand("msg").setTabCompleter(messageCommand);
    getCommand("r").setExecutor(messageCommand);
    
    // Set server motd from config
    String motd = getConfig().getString("server.motd", "Welcome to the Survival Server!");
    getServer().setMotd(MessageUtil.colorize(motd));

    // Start salary task if enabled
    if (getConfig().getBoolean("economy.salary.enabled", true)) {
      salaryTask = new SalaryTask(economyManager);
      // Run every minute to check for salary eligibility
      salaryTask.runTaskTimer(this, 20L * 60L, 20L * 60L);
    }
    
    LOGGER.info("Survival plugin enabled successfully!");
  }

  @Override
  public void onDisable() {
    // Save all player stats
    if (statsManager != null) {
      statsManager.saveAll();
    }
    
    // Shutdown territory display manager
    if (territoryDisplayManager != null) {
      territoryDisplayManager.shutdown();
    }
    
    // Clear all open menus
    MenuManager.getInstance().clearAll();
    
    // Cancel salary task
    if (salaryTask != null) {
      salaryTask.cancel();
    }
    
    // Disconnect database
    if (database != null) {
      database.disconnect();
    }
    
    LOGGER.info("Survival plugin disabled");
  }
  
  /**
   * Initialize the database connection
   * @return true if successful
   */
  private boolean initDatabase() {
    String dbType = getConfig().getString("database.type", "SQLITE").toUpperCase();
    
    if (dbType.equals("SQLITE")) {
      String sqliteName = getConfig().getString("database.sqlite.file", "data.db");
      String file = new File(getDataFolder(), sqliteName).getAbsolutePath();
      database = new Database(file);
    } else if (dbType.equals("MYSQL")) {
      String host = getConfig().getString("database.mysql.host", "localhost");
      int port = getConfig().getInt("database.mysql.port", 3306);
      String dbName = getConfig().getString("database.mysql.database", "survival");
      String username = getConfig().getString("database.mysql.username", "root");
      String password = getConfig().getString("database.mysql.password", "password");
      database = new Database(host, port, dbName, username, password);
    } else {
      LOGGER.severe("Invalid database type: " + dbType);
      return false;
    }
    
    return database.connect();
  }
  
  /**
   * Get the plugin instance
   * @return the plugin instance
   */
  public static Survival getInstance() {
    return instance;
  }
  
  /**
   * Get the database
   * @return the database
   */
  public Database getDatabase() {
    return database;
  }
  
  /**
   * Get the economy manager
   * @return the economy manager
   */
  public EconomyManager getEconomyManager() {
    return economyManager;
  }
  
  /**
   * Get the clan manager
   * @return the clan manager
   */
  public ClanManager getClanManager() {
    return clanManager;
  }

  /**
   * Get the clan war manager
   * @return the clan war manager
   */
  public ClanWarManager getClanWarManager() {
    return clanWarManager;
  }

  /**
   * Get the chat manager
   * @return the chat manager
   */
  public ChatManager getChatManager() {
    return chatManager;
  }

  /**
   * Get the auction manager
   * @return the auction manager
   */
  public AuctionManager getAuctionManager() {
    return auctionManager;
  }

  /**
   * Get the territory manager
   * @return the territory manager
   */
  public TerritoryManager getTerritoryManager() {
    return territoryManager;
  }

  /**
   * Get the player stats manager
   * @return the stats manager
   */
  public PlayerStatsManager getStatsManager() {
    return statsManager;
  }

  /**
   * Get the teleport manager
   * @return the teleport manager
   */
  public TeleportManager getTeleportManager() {
    return teleportManager;
  }

  /**
   * Get the shop manager
   * @return the shop manager
   */
  public ShopManager getShopManager() {
    return shopManager;
  }

  /**
   * Get the scoreboard manager
   * @return the scoreboard manager
   */
  public ScoreboardManager getScoreboardManager() {
    return scoreboardManager;
  }

  /**
   * Get the tab manager
   * @return the tab manager
   */
  public TabManager getTabManager() {
    return tabManager;
  }

  /**
   * Get the bounty manager
   * @return the bounty manager
   */
  public BountyManager getBountyManager() {
    return bountyManager;
  }

  /**
   * Get the jobs manager
   * @return the jobs manager
   */
  public JobsManager getJobsManager() {
    return jobsManager;
  }

  /**
   * Get the home manager
   * @return the home manager
   */
  public HomeManager getHomeManager() {
    return homeManager;
  }

  /**
   * Get the territory display manager
   * @return the territory display manager
   */
  public TerritoryDisplayManager getTerritoryDisplayManager() {
    return territoryDisplayManager;
  }
}
