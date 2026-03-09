package com.knowplugins.ysubz.kaoscore.managers.plugin;


import com.knowplugins.ysubz.kaoscore.KaosCore;
import org.bukkit.Bukkit;

import java.sql.*;
import java.util.Properties;

public class DatabaseManager {

    private final KaosCore plugin;
    private Properties props;
    private String host, database, username, password;
    private int port;
    private boolean connected;
    private String jdbcUrl;

    public DatabaseManager(KaosCore plugin) {
        this.plugin = plugin;
        this.host = plugin.getConfig().getString("database.mysql.host");
        this.port = plugin.getConfig().getInt("database.mysql.port");
        this.database = plugin.getConfig().getString("database.mysql.database");
        this.username = plugin.getConfig().getString("database.mysql.user", plugin.getConfig().getString("database.mysql.username"));
        this.password = plugin.getConfig().getString("database.mysql.password");
        this.jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&autoReconnect=true&characterEncoding=utf8";
    }

    public synchronized boolean connect() {
        if (connected) return true;

        try {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
            } catch (ClassNotFoundException ignored) {
            }

            props = new Properties();
            props.setProperty("user", username != null ? username : "");
            props.setProperty("password", password != null ? password : "");
            props.setProperty("autoReconnect", "true");
            props.setProperty("characterEncoding", "utf8");
            props.setProperty("useUnicode", "true");

            try (Connection test = DriverManager.getConnection(jdbcUrl, props)) {
                if (test != null && !test.isClosed()) {
                    connected = true;
                }
            }

            if (connected) {
                connected = createTables();
            }

            if (connected) {
                System.out.println("\u001B[32m[KaosCore] Conectado ao MySQL!\u001B[0m");
                return true;
            }
        } catch (Exception e) {
            connected = false;
            System.out.println("\u001B[31m[KaosCore] Erro ao conectar ao MySQL: " + e + "\u001B[0m");
            e.printStackTrace();
        }

        return false;
    }

    public synchronized Connection getConnection() throws SQLException {
        if (props == null) {
            connect();
        }
        if (props == null) throw new SQLException("Database properties not initialized");
        return DriverManager.getConnection(jdbcUrl, props);
    }

    public synchronized boolean isConnected() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed() && conn.isValid(2);
        } catch (SQLException e) {
            return false;
        }
    }

    public void disconnect() {
        connected = false;
        System.out.println("\u001B[33m[KaosCore] Desconectado do MySQL\u001B[0m");
    }

    private boolean createTables() {
        String clansTable = "CREATE TABLE IF NOT EXISTS clans (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "name VARCHAR(32) NOT NULL UNIQUE," +
                "tag VARCHAR(6) NOT NULL," +
                "tag_color VARCHAR(64) NOT NULL," +
                "leader VARCHAR(36) NOT NULL," +
                "created BIGINT NOT NULL" +
                ");";

        String clanMembersTable = "CREATE TABLE IF NOT EXISTS clan_members (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "clan_name VARCHAR(32) NOT NULL," +
                "player_uuid VARCHAR(36) NOT NULL UNIQUE," +
                "player_name VARCHAR(16) NOT NULL," +
                "role VARCHAR(32) NOT NULL," +
                "joined BIGINT NOT NULL" +
                ");";

        String friendsTable = "CREATE TABLE IF NOT EXISTS friends (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "player_uuid VARCHAR(36) NOT NULL," +
                "friend_uuid VARCHAR(36) NOT NULL," +
                "friend_name VARCHAR(16) NOT NULL," +
                "since BIGINT NOT NULL," +
                "UNIQUE KEY unique_friend (player_uuid, friend_uuid)" +
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

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(clansTable);
            stmt.executeUpdate(clanMembersTable);
            stmt.executeUpdate(friendsTable);
            stmt.executeUpdate(vanishes);
            stmt.executeUpdate(disguises);
            stmt.executeUpdate(skins);
            stmt.executeUpdate(playerTags);
            stmt.executeUpdate(punishments);
            stmt.executeUpdate(playerAddresses);

            Bukkit.getConsoleSender().sendMessage("§a§lSUCESSO -> §aTabelas criadas com sucesso!");
            return true;
        } catch (SQLException e) {
            Bukkit.getConsoleSender().sendMessage("§c§lERRO -> §cErro ao criar as tabelas: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
