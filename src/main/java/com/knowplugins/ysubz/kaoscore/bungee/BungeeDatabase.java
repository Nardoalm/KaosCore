package com.knowplugins.ysubz.kaoscore.bungee;

import com.knowplugins.ysubz.kaoscore.bukkit.util.Logger;
import com.knowplugins.ysubz.kaoscore.bungee.bootstrap.BungeeKaosCore;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.md_5.bungee.config.Configuration;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class BungeeDatabase {

    private final BungeeKaosCore plugin;
    private final String jdbcUrl;
    private final String username;
    private final String password;

    private volatile boolean connected;
    private volatile HikariDataSource dataSource;

    public BungeeDatabase(BungeeKaosCore plugin, Configuration config) {
        this.plugin = plugin;
        String host = config.getString("database.mysql.host", "localhost");
        int port = config.getInt("database.mysql.port", 3306);
        String database = config.getString("database.mysql.database", "");
        this.username = config.getString("database.mysql.user", config.getString("database.mysql.username", "root"));
        this.password = config.getString("database.mysql.password", "");
        this.jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + database
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=utf8&useUnicode=true&rewriteBatchedStatements=true";
    }

    public synchronized boolean connect(Configuration config) {
        if (dataSource != null && !dataSource.isClosed()) {
            connected = true;
            return true;
        }
        try {
            HikariConfig hikari = new HikariConfig();
            hikari.setPoolName("KaosCore-Bungee");
            hikari.setJdbcUrl(jdbcUrl);
            hikari.setUsername(username);
            hikari.setPassword(password);
            hikari.setDriverClassName("com.mysql.cj.jdbc.Driver");
            hikari.setMaximumPoolSize(Math.max(2, config.getInt("database.mysql.pool.maximum-pool-size", 10)));
            hikari.setMinimumIdle(Math.max(1, config.getInt("database.mysql.pool.minimum-idle", 2)));
            hikari.setConnectionTimeout(Math.max(2500L, config.getLong("database.mysql.pool.connection-timeout-ms", 10000L)));
            hikari.setIdleTimeout(Math.max(10000L, config.getLong("database.mysql.pool.idle-timeout-ms", 600000L)));
            hikari.setMaxLifetime(Math.max(30000L, config.getLong("database.mysql.pool.max-lifetime-ms", 1800000L)));
            hikari.setValidationTimeout(Math.max(1000L, config.getLong("database.mysql.pool.validation-timeout-ms", 5000L)));
            hikari.setLeakDetectionThreshold(Math.max(0L, config.getLong("database.mysql.pool.leak-detection-threshold-ms", 0L)));
            hikari.addDataSourceProperty("cachePrepStmts", "true");
            hikari.addDataSourceProperty("prepStmtCacheSize", "250");
            hikari.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            hikari.addDataSourceProperty("useServerPrepStmts", "true");

            HikariDataSource source = new HikariDataSource(hikari);
            try (Connection ignored = source.getConnection()) {
                connected = true;
            }
            dataSource = source;
            connected = createTables();
            return connected;
        } catch (Exception exception) {
            connected = false;
            disconnect();
            Logger.warning("[KaosCore] Falha ao conectar no MySQL: " + exception.getMessage());
            return false;
        }
    }

    public synchronized Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            throw new SQLException("DataSource indisponivel");
        }
        return dataSource.getConnection();
    }

    public synchronized boolean isConnected() {
        return connected && dataSource != null && !dataSource.isClosed();
    }

    public synchronized void disconnect() {
        connected = false;
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
        dataSource = null;
    }

    private boolean createTables() {
        String[] ddl = new String[]{
                "CREATE TABLE IF NOT EXISTS clans (id INT AUTO_INCREMENT PRIMARY KEY,name VARCHAR(32) NOT NULL UNIQUE,tag VARCHAR(8) NOT NULL,tag_color VARCHAR(64) NOT NULL,leader VARCHAR(36) NOT NULL,created BIGINT NOT NULL,wins INT NOT NULL DEFAULT 0,losses INT NOT NULL DEFAULT 0,elo INT NOT NULL DEFAULT 1000);",
                "CREATE TABLE IF NOT EXISTS clan_members (id INT AUTO_INCREMENT PRIMARY KEY,clan_name VARCHAR(32) NOT NULL,player_uuid VARCHAR(36) NOT NULL UNIQUE,player_name VARCHAR(16) NOT NULL,role VARCHAR(32) NOT NULL,joined BIGINT NOT NULL);",
                "CREATE TABLE IF NOT EXISTS clan_invites (id BIGINT AUTO_INCREMENT PRIMARY KEY,clan_name VARCHAR(32) NOT NULL,inviter_uuid CHAR(36) NOT NULL,target_uuid CHAR(36) NOT NULL,created_at BIGINT NOT NULL,UNIQUE KEY unique_clan_invite (target_uuid, clan_name),INDEX idx_clan_invites_target (target_uuid));",
                "CREATE TABLE IF NOT EXISTS friends (id INT AUTO_INCREMENT PRIMARY KEY,player_uuid VARCHAR(36) NOT NULL,friend_uuid VARCHAR(36) NOT NULL,friend_name VARCHAR(16) NOT NULL,since BIGINT NOT NULL,UNIQUE KEY unique_friend (player_uuid, friend_uuid));",
                "CREATE TABLE IF NOT EXISTS friend_requests (id BIGINT AUTO_INCREMENT PRIMARY KEY,requester_uuid CHAR(36) NOT NULL,requester_name VARCHAR(16) NOT NULL,target_uuid CHAR(36) NOT NULL,created_at BIGINT NOT NULL,UNIQUE KEY unique_friend_request (requester_uuid, target_uuid),INDEX idx_friend_requests_target (target_uuid));",
                "CREATE TABLE IF NOT EXISTS vanishes (uuid CHAR(36) NOT NULL PRIMARY KEY,is_vanished TINYINT(1) NOT NULL DEFAULT 0,updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP);",
                "CREATE TABLE IF NOT EXISTS disguises (uuid CHAR(36) NOT NULL PRIMARY KEY,is_disguised TINYINT(1) NOT NULL DEFAULT 0,disguise_type VARCHAR(64) NULL,disguise_meta TEXT NULL,updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP);",
                "CREATE TABLE IF NOT EXISTS skins (uuid CHAR(36) NOT NULL PRIMARY KEY,skin_name VARCHAR(64) NULL,skin_value TEXT NULL,skin_signature TEXT NULL,fetched_at TIMESTAMP NULL);",
                "CREATE TABLE IF NOT EXISTS player_tags (uuid CHAR(36) NOT NULL PRIMARY KEY,tag_id VARCHAR(64) NOT NULL,updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP);",
                "CREATE TABLE IF NOT EXISTS player_medals (uuid CHAR(36) NOT NULL PRIMARY KEY,medal_id VARCHAR(64) NOT NULL,updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP);",
                "CREATE TABLE IF NOT EXISTS player_welcome_messages (uuid CHAR(36) NOT NULL PRIMARY KEY,message_id VARCHAR(64) NOT NULL,updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP);",
                "CREATE TABLE IF NOT EXISTS punishments (id BIGINT AUTO_INCREMENT PRIMARY KEY,target_uuid CHAR(36) NULL,target_name VARCHAR(16) NOT NULL,punishment_type VARCHAR(16) NOT NULL,reason TEXT NOT NULL,created_at BIGINT NOT NULL,expires_at BIGINT NULL,punisher_uuid CHAR(36) NULL,punisher_name VARCHAR(16) NOT NULL,source VARCHAR(32) NULL,active TINYINT(1) NOT NULL DEFAULT 1,removed_at BIGINT NULL,removed_by_uuid CHAR(36) NULL,removed_by_name VARCHAR(16) NULL,removed_reason TEXT NULL,INDEX idx_punishments_target_uuid (target_uuid),INDEX idx_punishments_target_name (target_name),INDEX idx_punishments_type_active (punishment_type, active));",
                "CREATE TABLE IF NOT EXISTS player_addresses (id BIGINT AUTO_INCREMENT PRIMARY KEY,player_uuid CHAR(36) NOT NULL,player_name VARCHAR(16) NOT NULL,ip VARCHAR(64) NOT NULL,first_seen BIGINT NOT NULL,last_seen BIGINT NOT NULL,UNIQUE KEY unique_player_ip (player_uuid, ip),INDEX idx_player_addresses_name (player_name),INDEX idx_player_addresses_ip (ip));",
                "CREATE TABLE IF NOT EXISTS kaoscore_groups (name VARCHAR(64) NOT NULL PRIMARY KEY,priority INT NOT NULL DEFAULT 0,prefix TEXT NULL,suffix TEXT NULL,is_default TINYINT(1) NOT NULL DEFAULT 0,created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP);",
                "CREATE TABLE IF NOT EXISTS kaoscore_group_permissions (group_name VARCHAR(64) NOT NULL,permission VARCHAR(128) NOT NULL,value TINYINT(1) NOT NULL DEFAULT 1,PRIMARY KEY (group_name, permission),INDEX idx_kgp_group (group_name));",
                "CREATE TABLE IF NOT EXISTS kaoscore_group_inherits (group_name VARCHAR(64) NOT NULL,inherit_group VARCHAR(64) NOT NULL,PRIMARY KEY (group_name, inherit_group),INDEX idx_kgi_group (group_name));",
                "CREATE TABLE IF NOT EXISTS kaoscore_user_permissions (uuid CHAR(36) NOT NULL,permission VARCHAR(128) NOT NULL,value TINYINT(1) NOT NULL DEFAULT 1,expires_at BIGINT NULL,PRIMARY KEY (uuid, permission),INDEX idx_kup_uuid (uuid));",
                "CREATE TABLE IF NOT EXISTS kaoscore_user_grants (id BIGINT AUTO_INCREMENT PRIMARY KEY,uuid CHAR(36) NOT NULL,player_name VARCHAR(16) NOT NULL,group_name VARCHAR(64) NOT NULL,given_by_uuid CHAR(36) NULL,given_by_name VARCHAR(16) NOT NULL,reason TEXT NULL,created_at BIGINT NOT NULL,expires_at BIGINT NULL,active TINYINT(1) NOT NULL DEFAULT 1,removed_at BIGINT NULL,removed_by_uuid CHAR(36) NULL,removed_by_name VARCHAR(16) NULL,removed_reason TEXT NULL,INDEX idx_kug_uuid_active (uuid, active),INDEX idx_kug_group (group_name));",
                "CREATE TABLE IF NOT EXISTS kaoscore_skin_cache (nick VARCHAR(16) NOT NULL PRIMARY KEY,skin_value TEXT NOT NULL,skin_signature TEXT NULL,updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP);"
        };

        try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
            for (String query : ddl) {
                statement.executeUpdate(query);
            }
            ensureColumn(connection, "clans", "created", "BIGINT NOT NULL DEFAULT 0");
            ensureColumn(connection, "clans", "wins", "INT NOT NULL DEFAULT 0");
            ensureColumn(connection, "clans", "losses", "INT NOT NULL DEFAULT 0");
            ensureColumn(connection, "clans", "elo", "INT NOT NULL DEFAULT 1000");
            return true;
        } catch (Exception exception) {
            Logger.warning("[KaosCore] Falha ao criar tabelas: " + exception.getMessage());
            return false;
        }
    }

    private void ensureColumn(Connection connection, String tableName, String columnName, String definition) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet resultSet = metadata.getColumns(connection.getCatalog(), null, tableName, columnName)) {
            if (resultSet.next()) {
                return;
            }
        }
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + definition);
        }
    }
}
