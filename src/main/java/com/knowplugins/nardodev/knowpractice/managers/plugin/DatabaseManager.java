package com.knowplugins.nardodev.knowpractice.managers.plugin;


import com.knowplugins.nardodev.knowpractice.KnowPractice;
import org.bukkit.Bukkit;

import java.sql.*;

public class DatabaseManager {

    private final KnowPractice plugin;
    private Connection connection;
    private String host, database, username, password;
    private int port;
    private boolean connected;

    public DatabaseManager(KnowPractice plugin) {
        this.plugin = plugin;
        this.host = plugin.getConfig().getString("database.mysql.host");
        this.port = plugin.getConfig().getInt("database.mysql.port");
        this.database = plugin.getConfig().getString("database.mysql.database");
        this.username = plugin.getConfig().getString("database.mysql.username");
        this.password = plugin.getConfig().getString("database.mysql.password");
    }

    public void connect() {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            connection = DriverManager.getConnection(
                    "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false",
                    username, password
            );
            connected = true;
            createTables();
            System.out.println("\u001B[32m[SakiPractice] Conectado ao MySQL!\u001B[0m");
        } catch (Exception e) {
            System.out.println("\u001B[31m[SakiPractice] Erro ao conectar ao MySQL: " + e.getMessage() + "\u001B[0m");
        }
    }

    public void disconnect() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("\u001B[33m[SakiPractice] Desconectado do MySQL\u001B[0m");
            }
        } catch (SQLException e) {
            System.out.println("\u001B[31m[KnowPractice] Erro ao desconectar: " + e.getMessage() + "\u001B[0m");
        }
    }

    private void createTables() {
        try {
            Statement stmt = connection.createStatement();

            String clansTable = "CREATE TABLE IF NOT EXISTS clans (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "name VARCHAR(32) NOT NULL UNIQUE," +
                    "tag VARCHAR(6) NOT NULL," +
                    "tag_color VARCHAR(64) NOT NULL," +
                    "leader VARCHAR(36) NOT NULL," +
                    "created BIGINT NOT NULL" +
                    ");";
            stmt.executeUpdate(clansTable);

            Bukkit.getConsoleSender().sendMessage("§a§lSUCESSO -> §aTabelas criadas com sucesso!");
        } catch (SQLException e) {
            Bukkit.getConsoleSender().sendMessage("§c§lERRO -> §cErro ao criar as tabelas: " + e.getMessage());
        }
    }
}