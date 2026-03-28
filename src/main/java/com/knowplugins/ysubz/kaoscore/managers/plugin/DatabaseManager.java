package com.knowplugins.ysubz.kaoscore.managers.plugin;

import com.knowplugins.ysubz.kaoscore.KaosCore;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Bukkit;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

    private final KaosCore plugin;
    private final String host;
    private final String database;
    private final String username;
    private final String password;
    private final int port;
    private final String jdbcUrl;

    private volatile boolean connected;
    private volatile HikariDataSource dataSource;

    public DatabaseManager(KaosCore plugin) {
        this.plugin = plugin;
        this.host = plugin.getConfig().getString("database.mysql.host");
        this.port = plugin.getConfig().getInt("database.mysql.port");
        this.database = plugin.getConfig().getString("database.mysql.database");
        this.username = plugin.getConfig().getString("database.mysql.user", plugin.getConfig().getString("database.mysql.username"));
        this.password = plugin.getConfig().getString("database.mysql.password");
        this.jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + database
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=utf8&useUnicode=true&rewriteBatchedStatements=true";
    }

    public synchronized boolean connect() {
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
            config.setConnectionTimeout(Math.max(2500L, plugin.getConfig().getLong("database.mysql.pool.connection-timeout-ms", 10000L)));
            config.setIdleTimeout(Math.max(10000L, plugin.getConfig().getLong("database.mysql.pool.idle-timeout-ms", 600000L)));
            config.setMaxLifetime(Math.max(30000L, plugin.getConfig().getLong("database.mysql.pool.max-lifetime-ms", 1800000L)));
            config.setValidationTimeout(Math.max(1000L, plugin.getConfig().getLong("database.mysql.pool.validation-timeout-ms", 5000L)));
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
                System.out.println("\u001B[32m[KaosCore] Conectado ao MySQL com HikariCP!\u001B[0m");
                return true;
            }
        } catch (Exception e) {
            connected = false;
            closeDataSource();
            System.out.println("\u001B[31m[KaosCore] Erro ao conectar ao MySQL: " + e + "\u001B[0m");
            e.printStackTrace();
        }

        return false;
    }

    public synchronized Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            connect();
        }
        if (dataSource == null || dataSource.isClosed()) {
            throw new SQLException("HikariDataSource not initialized");
        }
        return dataSource.getConnection();
    }

    public synchronized boolean isConnected() {
        return connected && dataSource != null && !dataSource.isClosed();
    }

    public synchronized void disconnect() {
        connected = false;
        closeDataSource();
        System.out.println("\u001B[33m[KaosCore] HikariCP encerrado\u001B[0m");
    }

    private synchronized void closeDataSource() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
        dataSource = null;
    }

    private boolean createTables() {
        String clansTable = "CREATE TABLE IF NOT EXISTS clans (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "name VARCHAR(32) NOT NULL UNIQUE," +
                "tag VARCHAR(8) NOT NULL," +
                "tag_color VARCHAR(64) NOT NULL," +
                "leader VARCHAR(36) NOT NULL," +
                "created BIGINT NOT NULL," +
                "wins INT NOT NULL DEFAULT 0," +
                "losses INT NOT NULL DEFAULT 0," +
                "elo INT NOT NULL DEFAULT 1000" +
                ");";

        String clanMembersTable = "CREATE TABLE IF NOT EXISTS clan_members (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "clan_name VARCHAR(32) NOT NULL," +
                "player_uuid VARCHAR(36) NOT NULL UNIQUE," +
                "player_name VARCHAR(16) NOT NULL," +
                "role VARCHAR(32) NOT NULL," +
                "joined BIGINT NOT NULL" +
                ");";

        String clanInvitesTable = "CREATE TABLE IF NOT EXISTS clan_invites (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "clan_name VARCHAR(32) NOT NULL," +
                "inviter_uuid CHAR(36) NOT NULL," +
                "target_uuid CHAR(36) NOT NULL," +
                "created_at BIGINT NOT NULL," +
                "UNIQUE KEY unique_clan_invite (target_uuid, clan_name)," +
                "INDEX idx_clan_invites_target (target_uuid)" +
                ");";

        String friendsTable = "CREATE TABLE IF NOT EXISTS friends (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "player_uuid VARCHAR(36) NOT NULL," +
                "friend_uuid VARCHAR(36) NOT NULL," +
                "friend_name VARCHAR(16) NOT NULL," +
                "since BIGINT NOT NULL," +
                "UNIQUE KEY unique_friend (player_uuid, friend_uuid)" +
                ");";

        String friendRequestsTable = "CREATE TABLE IF NOT EXISTS friend_requests (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "requester_uuid CHAR(36) NOT NULL," +
                "requester_name VARCHAR(16) NOT NULL," +
                "target_uuid CHAR(36) NOT NULL," +
                "created_at BIGINT NOT NULL," +
                "UNIQUE KEY unique_friend_request (requester_uuid, target_uuid)," +
                "INDEX idx_friend_requests_target (target_uuid)" +
                ");";

        String vanishes = "CREATE TABLE IF NOT EXISTS vanishes (" +
                "uuid CHAR(36) NOT NULL PRIMARY KEY," +
                "is_vanished TINYINT(1) NOT NULL DEFAULT 0," +
                "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                ");";

        String disguises = "CREATE TABLE IF NOT EXISTS disguises (" +
                "uuid CHAR(36) NOT NULL PRIMARY KEY," +
                "is_disguised TINYINT(1) NOT NULL DEFAULT 0," +
                "disguise_type VARCHAR(64) NULL," +
                "disguise_meta TEXT NULL," +
                "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                ");";

        String skins = "CREATE TABLE IF NOT EXISTS skins (" +
                "uuid CHAR(36) NOT NULL PRIMARY KEY," +
                "skin_name VARCHAR(64) NULL," +
                "skin_value TEXT NULL," +
                "skin_signature TEXT NULL," +
                "fetched_at TIMESTAMP NULL" +
                ");";

        String playerTags = "CREATE TABLE IF NOT EXISTS player_tags (" +
                "uuid CHAR(36) NOT NULL PRIMARY KEY," +
                "tag_id VARCHAR(64) NOT NULL," +
                "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                ");";

        String playerMedals = "CREATE TABLE IF NOT EXISTS player_medals (" +
                "uuid CHAR(36) NOT NULL PRIMARY KEY," +
                "medal_id VARCHAR(64) NOT NULL," +
                "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                ");";

        String playerWelcomeMessages = "CREATE TABLE IF NOT EXISTS player_welcome_messages (" +
                "uuid CHAR(36) NOT NULL PRIMARY KEY," +
                "message_id VARCHAR(64) NOT NULL," +
                "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                ");";

        String punishments = "CREATE TABLE IF NOT EXISTS punishments (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "target_uuid CHAR(36) NULL," +
                "target_name VARCHAR(16) NOT NULL," +
                "punishment_type VARCHAR(16) NOT NULL," +
                "reason TEXT NOT NULL," +
                "created_at BIGINT NOT NULL," +
                "expires_at BIGINT NULL," +
                "punisher_uuid CHAR(36) NULL," +
                "punisher_name VARCHAR(16) NOT NULL," +
                "source VARCHAR(32) NULL," +
                "active TINYINT(1) NOT NULL DEFAULT 1," +
                "removed_at BIGINT NULL," +
                "removed_by_uuid CHAR(36) NULL," +
                "removed_by_name VARCHAR(16) NULL," +
                "removed_reason TEXT NULL," +
                "INDEX idx_punishments_target_uuid (target_uuid)," +
                "INDEX idx_punishments_target_name (target_name)," +
                "INDEX idx_punishments_type_active (punishment_type, active)" +
                ");";

        String playerAddresses = "CREATE TABLE IF NOT EXISTS player_addresses (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "player_uuid CHAR(36) NOT NULL," +
                "player_name VARCHAR(16) NOT NULL," +
                "ip VARCHAR(64) NOT NULL," +
                "first_seen BIGINT NOT NULL," +
                "last_seen BIGINT NOT NULL," +
                "UNIQUE KEY unique_player_ip (player_uuid, ip)," +
                "INDEX idx_player_addresses_name (player_name)," +
                "INDEX idx_player_addresses_ip (ip)" +
                ");";

        String permissionGroups = "CREATE TABLE IF NOT EXISTS kaoscore_groups (" +
                "name VARCHAR(64) NOT NULL PRIMARY KEY," +
                "priority INT NOT NULL DEFAULT 0," +
                "prefix TEXT NULL," +
                "suffix TEXT NULL," +
                "is_default TINYINT(1) NOT NULL DEFAULT 0," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                ");";

        String permissionGroupPermissions = "CREATE TABLE IF NOT EXISTS kaoscore_group_permissions (" +
                "group_name VARCHAR(64) NOT NULL," +
                "permission VARCHAR(128) NOT NULL," +
                "value TINYINT(1) NOT NULL DEFAULT 1," +
                "PRIMARY KEY (group_name, permission)," +
                "INDEX idx_kgp_group (group_name)" +
                ");";

        String permissionGroupInherits = "CREATE TABLE IF NOT EXISTS kaoscore_group_inherits (" +
                "group_name VARCHAR(64) NOT NULL," +
                "inherit_group VARCHAR(64) NOT NULL," +
                "PRIMARY KEY (group_name, inherit_group)," +
                "INDEX idx_kgi_group (group_name)" +
                ");";

        String permissionUserPermissions = "CREATE TABLE IF NOT EXISTS kaoscore_user_permissions (" +
                "uuid CHAR(36) NOT NULL," +
                "permission VARCHAR(128) NOT NULL," +
                "value TINYINT(1) NOT NULL DEFAULT 1," +
                "expires_at BIGINT NULL," +
                "PRIMARY KEY (uuid, permission)," +
                "INDEX idx_kup_uuid (uuid)" +
                ");";

        String permissionUserGrants = "CREATE TABLE IF NOT EXISTS kaoscore_user_grants (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "uuid CHAR(36) NOT NULL," +
                "player_name VARCHAR(16) NOT NULL," +
                "group_name VARCHAR(64) NOT NULL," +
                "given_by_uuid CHAR(36) NULL," +
                "given_by_name VARCHAR(16) NOT NULL," +
                "reason TEXT NULL," +
                "created_at BIGINT NOT NULL," +
                "expires_at BIGINT NULL," +
                "active TINYINT(1) NOT NULL DEFAULT 1," +
                "removed_at BIGINT NULL," +
                "removed_by_uuid CHAR(36) NULL," +
                "removed_by_name VARCHAR(16) NULL," +
                "removed_reason TEXT NULL," +
                "INDEX idx_kug_uuid_active (uuid, active)," +
                "INDEX idx_kug_group (group_name)" +
                ");";

        String skinCacheByName = "CREATE TABLE IF NOT EXISTS kaoscore_skin_cache (" +
                "nick VARCHAR(16) NOT NULL PRIMARY KEY," +
                "skin_value TEXT NOT NULL," +
                "skin_signature TEXT NULL," +
                "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                ");";

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(clansTable);
            stmt.executeUpdate(clanMembersTable);
            stmt.executeUpdate(clanInvitesTable);
            stmt.executeUpdate(friendsTable);
            stmt.executeUpdate(friendRequestsTable);
            stmt.executeUpdate(vanishes);
            stmt.executeUpdate(disguises);
            stmt.executeUpdate(skins);
            stmt.executeUpdate(playerTags);
            stmt.executeUpdate(playerMedals);
            stmt.executeUpdate(playerWelcomeMessages);
            stmt.executeUpdate(punishments);
            stmt.executeUpdate(playerAddresses);
            stmt.executeUpdate(permissionGroups);
            stmt.executeUpdate(permissionGroupPermissions);
            stmt.executeUpdate(permissionGroupInherits);
            stmt.executeUpdate(permissionUserPermissions);
            stmt.executeUpdate(permissionUserGrants);
            stmt.executeUpdate(skinCacheByName);

            ensureColumn(conn, "clans", "created", "BIGINT NOT NULL DEFAULT 0");
            ensureColumn(conn, "clans", "wins", "INT NOT NULL DEFAULT 0");
            ensureColumn(conn, "clans", "losses", "INT NOT NULL DEFAULT 0");
            ensureColumn(conn, "clans", "elo", "INT NOT NULL DEFAULT 1000");

            Bukkit.getConsoleSender().sendMessage("§a[KaosCore] §a§lSUCESSO -> §aTabelas criadas com sucesso!");
            return true;
        } catch (SQLException e) {
            Bukkit.getConsoleSender().sendMessage("§c[KaosCore] §c§lERRO -> §cErro ao criar as tabelas: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private void ensureColumn(Connection conn, String tableName, String columnName, String definition) throws SQLException {
        DatabaseMetaData metaData = conn.getMetaData();
        try (ResultSet rs = metaData.getColumns(conn.getCatalog(), null, tableName, columnName)) {
            if (rs.next()) {
                return;
            }
        }

        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + definition);
        }
    }
}
