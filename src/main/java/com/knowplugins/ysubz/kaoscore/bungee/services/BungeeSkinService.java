package com.knowplugins.ysubz.kaoscore.bungee;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.UUID;

final class BungeeSkinService {

    private final BungeeKaosCore plugin;
    private final BungeeDatabase database;

    BungeeSkinService(BungeeKaosCore plugin, BungeeDatabase database) {
        this.plugin = plugin;
        this.database = database;
    }

    void savePending(UUID uuid, String skinName) {
        if (uuid == null || Texts.normalize(skinName).isEmpty()) {
            return;
        }

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement("REPLACE INTO skins (uuid, skin_name, skin_value, skin_signature, fetched_at) VALUES (?, ?, NULL, NULL, NULL)")) {
            statement.setString(1, uuid.toString());
            statement.setString(2, skinName.trim());
            statement.executeUpdate();
        } catch (Exception exception) {
            plugin.getLogger().warning("[KaosCoreBungee] Falha ao registrar skin pendente: " + exception.getMessage());
        }
    }

    void save(UUID uuid, String skinName, String value, String signature) {
        if (uuid == null || Texts.safe(value).trim().isEmpty()) {
            return;
        }

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement("REPLACE INTO skins (uuid, skin_name, skin_value, skin_signature, fetched_at) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)")) {
            statement.setString(1, uuid.toString());
            statement.setString(2, Texts.safe(skinName));
            statement.setString(3, value);
            statement.setString(4, signature);
            statement.executeUpdate();
        } catch (Exception exception) {
            plugin.getLogger().warning("[KaosCoreBungee] Falha ao salvar skin: " + exception.getMessage());
        }
    }

    void delete(UUID uuid) {
        if (uuid == null) {
            return;
        }

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM skins WHERE uuid = ?")) {
            statement.setString(1, uuid.toString());
            statement.executeUpdate();
        } catch (Exception exception) {
            plugin.getLogger().warning("[KaosCoreBungee] Falha ao remover skin: " + exception.getMessage());
        }
    }
}
