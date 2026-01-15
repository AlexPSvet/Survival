package com.alexpsvet.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Database manager for MySQL or SQLite
 */
public class Database {
    private static final Logger LOGGER = Logger.getLogger("survival");
    private Connection connection;
    private final DatabaseType type;
    
    // SQLite
    private final String sqliteFile;
    
    // MySQL
    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;
    
    /**
     * Create a SQLite database
     * @param sqliteFile The SQLite file path
     */
    public Database(String sqliteFile) {
        this.type = DatabaseType.SQLITE;
        this.sqliteFile = sqliteFile;
        this.host = null;
        this.port = 0;
        this.database = null;
        this.username = null;
        this.password = null;
        createSqliteFile();
    }
    
    /**
     * Create a MySQL database connection
     * @param host MySQL host
     * @param port MySQL port
     * @param database Database name
     * @param username Username
     * @param password Password
     */
    public Database(String host, int port, String database, String username, String password) {
        this.type = DatabaseType.MYSQL;
        this.sqliteFile = null;
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
    }

    public boolean createSqliteFile() {
        if (type != DatabaseType.SQLITE) {
            LOGGER.severe("Cannot create SQLite file for non-SQLite database");
            return false;
        }
        File dbFile = new File(sqliteFile);
        if (dbFile.exists()) {
            LOGGER.info("SQLite database file already exists: " + sqliteFile);
            return true;
        }
        // Ensure parent directories exist (in case a subdirectory is specified)
        File parent = dbFile.getParentFile();
        if (parent != null && !parent.exists()) {
            if (!parent.mkdirs()) {
                LOGGER.severe("Failed to create parent directories for SQLite file: " + parent.getAbsolutePath());
                return false;
            }
        }
        try {
            if (dbFile.createNewFile()) {
                LOGGER.info("Created new SQLite database file: " + sqliteFile);
                return true;
            } else {
                LOGGER.severe("Failed to create SQLite database file: " + sqliteFile);
                return false;
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "IOException while creating SQLite database file: " + sqliteFile, e);
            return false;
        }
    }
    
    /**
     * Connect to the database
     * @return true if connection successful
     */
    public boolean connect() {
        try {
            if (type == DatabaseType.SQLITE) {
                Class.forName("org.sqlite.JDBC");
                connection = DriverManager.getConnection("jdbc:sqlite:" + sqliteFile);
                LOGGER.info("Connected to SQLite database: " + sqliteFile);
            } else {
                Class.forName("com.mysql.jdbc.Driver");
                String url = "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&autoReconnect=true";
                connection = DriverManager.getConnection(url, username, password);
                LOGGER.info("Connected to MySQL database: " + database);
            }
            return true;
        } catch (ClassNotFoundException | SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to connect to database", e);
            return false;
        }
    }
    
    /**
     * Disconnect from the database
     */
    public void disconnect() {
        if (connection != null) {
            try {
                connection.close();
                LOGGER.info("Disconnected from database");
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Failed to disconnect from database", e);
            }
        }
    }
    
    /**
     * Get the database connection
     * @return the connection
     */
    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connect();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking connection status", e);
        }
        return connection;
    }
    
    /**
     * Execute an update query (INSERT, UPDATE, DELETE, CREATE TABLE, etc.)
     * @param query The SQL query
     * @return the number of affected rows, or -1 if error
     */
    public int executeUpdate(String query) {
        try (Statement stmt = getConnection().createStatement()) {
            return stmt.executeUpdate(query);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to execute update: " + query, e);
            return -1;
        }
    }
    
    /**
     * Execute an update query with prepared statement
     * @param query The SQL query with placeholders
     * @param params The parameters to replace placeholders
     * @return the number of affected rows, or -1 if error
     */
    public int executeUpdate(String query, Object... params) {
        try (PreparedStatement pstmt = getConnection().prepareStatement(query)) {
            for (int i = 0; i < params.length; i++) {
                pstmt.setObject(i + 1, params[i]);
            }
            return pstmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to execute prepared update: " + query, e);
            return -1;
        }
    }
    
    /**
     * Execute a query (SELECT)
     * @param query The SQL query
     * @return the result set, or null if error
     */
    public ResultSet executeQuery(String query) {
        try {
            Statement stmt = getConnection().createStatement();
            return stmt.executeQuery(query);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to execute query: " + query, e);
            return null;
        }
    }
    
    /**
     * Execute a query with prepared statement
     * @param query The SQL query with placeholders
     * @param params The parameters to replace placeholders
     * @return the result set, or null if error
     */
    public ResultSet executeQuery(String query, Object... params) {
        try {
            PreparedStatement pstmt = getConnection().prepareStatement(query);
            for (int i = 0; i < params.length; i++) {
                pstmt.setObject(i + 1, params[i]);
            }
            return pstmt.executeQuery();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to execute prepared query: " + query, e);
            return null;
        }
    }
    
    /**
     * Check if the connection is valid
     * @return true if valid
     */
    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
    
    /**
     * Get the database type
     * @return the database type
     */
    public DatabaseType getType() {
        return type;
    }
}
