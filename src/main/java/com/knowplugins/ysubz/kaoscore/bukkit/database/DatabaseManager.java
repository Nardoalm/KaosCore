package com.knowplugins.ysubz.kaoscore.bukkit.database;

import com.knowplugins.ysubz.kaoscore.bukkit.KaosCore;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bson.Document;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

    public enum DatabaseType {
        MYSQL,
        MONGODB
    }

    private final KaosCore plugin;
    private final String configuredType;
    private final DatabaseType databaseType;
    private final String host;
    private final String database;
    private final String username;
    private final String password;
    private final int port;
    private final String jdbcUrl;
    private final String mongoUri;
    private final String mongoDatabaseName;

    private volatile boolean connected;
    private volatile HikariDataSource dataSource;
    private volatile MongoClient mongoClient;
    private volatile MongoDatabase mongoDatabase;

    public DatabaseManager(KaosCore plugin) {
        this.plugin = plugin;
        this.configuredType = plugin.getConfig().getString("database.type", "mysql");
        this.databaseType = "mongodb".equalsIgnoreCase(configuredType) ? DatabaseType.MONGODB : DatabaseType.MYSQL;
        this.host = plugin.getConfig().getString("database.mysql.host");
        this.port = plugin.getConfig().getInt("database.mysql.port");
        this.database = plugin.getConfig().getString("database.mysql.database");
        this.username = plugin.getConfig().getString("database.mysql.user", plugin.getConfig().getString("database.mysql.username"));
        this.password = plugin.getConfig().getString("database.mysql.password");
        this.mongoUri = plugin.getConfig().getString("database.mongodb.uri", "mongodb://localhost:27017");
        this.mongoDatabaseName = plugin.getConfig().getString("database.mongodb.database", "kaoscore");
        this.jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + database
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=utf8&useUnicode=true&rewriteBatchedStatements=true";
    }

    public synchronized boolean connect() {
        if (databaseType == DatabaseType.MONGODB) {
            return connectMongo();
        }

        if (dataSource != null && !dataSource.isClosed()) {
            connected = true;
            return true;
        }

        try {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
            } catch (ClassNotFoundException ignored) {
            }

            HikariConfig config = new HikariConfig();
            config.setPoolName("KaosCore-Hikari");
            config.setJdbcUrl(jdbcUrl);
            config.setUsername(username != null ? username : "");
            config.setPassword(password != null ? password : "");
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
            config.setMaximumPoolSize(Math.max(2, plugin.getConfig().getInt("database.mysql.pool.maximum-pool-size", 10)));
            config.setMinimumIdle(Math.max(1, plugin.getConfig().getInt("database.mysql.pool.minimum-idle", 2)));
            config.setConnectionTimeout(Math.max(1500L, plugin.getConfig().getLong("database.mysql.pool.connection-timeout-ms", 3000L)));
            config.setIdleTimeout(Math.max(10000L, plugin.getConfig().getLong("database.mysql.pool.idle-timeout-ms", 300000L)));
            config.setMaxLifetime(Math.max(30000L, plugin.getConfig().getLong("database.mysql.pool.max-lifetime-ms", 900000L)));
            config.setKeepaliveTime(Math.max(15000L, plugin.getConfig().getLong("database.mysql.pool.keepalive-time-ms", 45000L)));
            config.setValidationTimeout(Math.max(1000L, plugin.getConfig().getLong("database.mysql.pool.validation-timeout-ms", 2000L)));
            config.setLeakDetectionThreshold(Math.max(0L, plugin.getConfig().getLong("database.mysql.pool.leak-detection-threshold-ms", 0L)));

            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            config.addDataSourceProperty("useServerPrepStmts", "true");
            config.addDataSourceProperty("maintainTimeStats", "false");

            HikariDataSource hikari = new HikariDataSource(config);
            try (Connection test = hikari.getConnection()) {
                connected = test != null && !test.isClosed();
            }

            if (!connected) {
                hikari.close();
                return false;
            }

            dataSource = hikari;
            connected = createTables();
            if (connected) {
                System.out.println("\u001B[32m[KaosCore] Connected to MySQL with HikariCP!\u001B[0m");
                return true;
            }
        } catch (Exception e) {
            connected = false;
            closeDataSource();
            System.out.println("\u001B[31m[KaosCore] Error connecting to MySQL: " + e + "\u001B[0m");
            e.printStackTrace();
        }

        return false;
    }

    private synchronized boolean connectMongo() {
        if (mongoClient != null && mongoDatabase != null) {
            connected = true;
            return true;
        }

        try {
            mongoClient = MongoClients.create(mongoUri);
            mongoDatabase = mongoClient.getDatabase(mongoDatabaseName);
            ensureMongoCollections();
            connected = true;
            plugin.getLogger().info("[KaosCore] Connected to MongoDB.");
            return true;
        } catch (Exception e) {
            connected = false;
            closeMongo();
            plugin.getLogger().severe("[KaosCore] Failed to connect to MongoDB: " + e.getMessage());
            return false;
        }
    }

    public synchronized Connection getConnection() throws SQLException {
        if (databaseType == DatabaseType.MONGODB) {
            throw new SQLException("MongoDB mode does not expose a SQL connection");
        }
        if (dataSource == null || dataSource.isClosed()) {
            connect();
        }
        if (dataSource == null || dataSource.isClosed()) {
            throw new SQLException("HikariDataSource not initialized");
        }
        return dataSource.getConnection();
    }

    public synchronized boolean isConnected() {
        if (databaseType == DatabaseType.MONGODB) {
            return connected && mongoClient != null && mongoDatabase != null;
        }
        return connected && dataSource != null && !dataSource.isClosed();
    }

    public synchronized void disconnect() {
        connected = false;
        closeMongo();
        closeDataSource();
        System.out.println("\u001B[33m[KaosCore] HikariCP shut down\u001B[0m");
    }

    public String getType() {
        return configuredType;
    }

    public DatabaseType getDatabaseType() {
        return databaseType;
    }

    public boolean isMongo() {
        return databaseType == DatabaseType.MONGODB;
    }

    public MongoDatabase getMongoDatabase() {
        return mongoDatabase;
    }

    public MongoCollection<Document> getMongoCollection(String name) {
        return mongoDatabase == null || name == null ? null : mongoDatabase.getCollection(name);
    }

    private synchronized void closeDataSource() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
        dataSource = null;
    }

    private synchronized void closeMongo() {
        if (mongoClient != null) {
            mongoClient.close();
        }
        mongoClient = null;
        mongoDatabase = null;
    }

    private boolean createTables() {
        if (databaseType == DatabaseType.MONGODB) {
            ensureMongoCollections();
            return true;
        }

        String authTable = "CREATE TABLE IF NOT EXISTS kaoscore_auth (" +
                "uuid CHAR(36) NOT NULL PRIMARY KEY," +
                "name VARCHAR(16) NOT NULL," +
                "password VARCHAR(60) NULL," +
                "is_premium TINYINT(1) NOT NULL DEFAULT 0," +
                "last_location TEXT NULL," +
                "registered_at BIGINT NOT NULL," +
                "last_login BIGINT NOT NULL" +
                ");";

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(authTable);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void ensureMongoCollections() {
        if (mongoDatabase == null) {
            return;
        }
        mongoDatabase.getCollection("kaoscore_auth");
    }
}