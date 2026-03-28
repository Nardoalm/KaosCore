package com.knowplugins.ysubz.kaoscore.bungee;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class BungeeDisguiseService {

    private static final long RELOAD_INTERVAL_MS = 1500L;

    private static final List<String> DEFAULT_NICKS = Arrays.asList(
            "SrGusta", "ziSnaake", "yNandoXP_", "SrLogann", "iPowerPvP",
            "zFuzion_", "LuanPvP", "Darkness_z", "uSkyWars_", "MasterSkill"
    );

    private static final List<String> EXTRA_NICKS = Arrays.asList(
            "Vexiity", "Zoltrus", "KyroPvP", "ImNotHack", "SlowMotion",
            "Darkness_PT", "LethalGhost", "VortexNuker", "Swiftly", "FrostByte",
            "iZack_", "MysterioBR", "NightWalk", "ExoticPanda", "VelocityHits",
            "SilentSteps", "HyperCombo", "ArcticFox_", "LunarPvP", "ShadowBound",
            "GrizzlyBear", "IronWill", "SkyFaller", "Noxious", "ZenithPlayer"
    );

    private final BungeeKaosCore plugin;
    private final BungeeDatabase database;
    private final BungeePlayerAddressRepository addresses;
    private final Map<UUID, String> activeDisguises = new ConcurrentHashMap<>();
    private final List<String> nickPool = new ArrayList<>();
    private volatile long lastReloadAt;

    BungeeDisguiseService(BungeeKaosCore plugin, BungeeDatabase database, BungeePlayerAddressRepository addresses) {
        this.plugin = plugin;
        this.database = database;
        this.addresses = addresses;
        reload();
    }

    void reload() {
        loadNickPool();
        reloadActiveDisguises();
    }

    private void reloadActiveDisguises() {
        activeDisguises.clear();

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT uuid, disguise_meta FROM disguises WHERE is_disguised = 1")) {
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    try {
                        UUID uuid = UUID.fromString(resultSet.getString("uuid"));
                        String nick = Texts.safe(resultSet.getString("disguise_meta")).trim();
                        if (!nick.isEmpty()) {
                            activeDisguises.put(uuid, nick);
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        } catch (Exception exception) {
            plugin.getLogger().warning("[KaosCoreBungee] Falha ao carregar disguises: " + exception.getMessage());
        }
        lastReloadAt = System.currentTimeMillis();
    }

    boolean isDisguised(UUID uuid) {
        ensureFresh();
        return uuid != null && activeDisguises.containsKey(uuid);
    }

    String getDisguiseNick(UUID uuid) {
        ensureFresh();
        return uuid == null ? null : activeDisguises.get(uuid);
    }

    String getVisibleName(UUID uuid) {
        ensureFresh();
        if (uuid == null) {
            return "Desconhecido";
        }

        String nick = activeDisguises.get(uuid);
        if (nick != null && !nick.trim().isEmpty()) {
            return nick;
        }

        ProxiedPlayer online = ProxyServer.getInstance().getPlayer(uuid);
        if (online != null) {
            return online.getName();
        }

        return addresses.findLatestName(uuid);
    }

    String getAccountName(UUID uuid) {
        if (uuid == null) {
            return "Desconhecido";
        }

        ProxiedPlayer online = ProxyServer.getInstance().getPlayer(uuid);
        if (online != null) {
            return online.getName();
        }

        return addresses.findLatestName(uuid);
    }

    ProxiedPlayer findOnlineByVisibleName(String input) {
        ensureFresh();
        String normalized = Texts.normalize(input);
        if (normalized.isEmpty()) {
            return null;
        }

        ProxiedPlayer exact = ProxyServer.getInstance().getPlayer(input);
        if (exact != null) {
            return exact;
        }

        for (ProxiedPlayer player : ProxyServer.getInstance().getPlayers()) {
            if (player == null) {
                continue;
            }

            String visibleName = getVisibleName(player.getUniqueId());
            if (Texts.normalize(visibleName).equals(normalized)) {
                return player;
            }
        }

        return null;
    }

    boolean isNickInUse(String nick) {
        ensureFresh();
        String normalized = Texts.normalize(nick);
        if (normalized.isEmpty()) {
            return false;
        }

        for (String activeNick : activeDisguises.values()) {
            if (Texts.normalize(activeNick).equals(normalized)) {
                return true;
            }
        }

        for (ProxiedPlayer player : ProxyServer.getInstance().getPlayers()) {
            if (player != null && Texts.normalize(player.getName()).equals(normalized)) {
                return true;
            }
        }

        return false;
    }

    boolean hasJoinedBefore(String nick) {
        return addresses.hasSeenName(nick);
    }

    String getRandomNick() {
        List<String> shuffled = new ArrayList<>(nickPool);
        Collections.shuffle(shuffled);

        for (String nick : shuffled) {
            if (!isNickInUse(nick) && !hasJoinedBefore(nick)) {
                return nick;
            }
        }

        for (String nick : shuffled) {
            if (!isNickInUse(nick)) {
                return nick;
            }
        }

        return shuffled.isEmpty() ? "Steve" : shuffled.get(0);
    }

    void saveDisguise(UUID uuid, String nick) {
        if (uuid == null || Texts.normalize(nick).isEmpty()) {
            return;
        }

        activeDisguises.put(uuid, nick.trim());
        lastReloadAt = System.currentTimeMillis();
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement("REPLACE INTO disguises (uuid, is_disguised, disguise_type, disguise_meta) VALUES (?, 1, ?, ?)")) {
            statement.setString(1, uuid.toString());
            statement.setString(2, "nick");
            statement.setString(3, nick.trim());
            statement.executeUpdate();
        } catch (Exception exception) {
            plugin.getLogger().warning("[KaosCoreBungee] Falha ao salvar disguise: " + exception.getMessage());
        }
    }

    void clearDisguise(UUID uuid) {
        if (uuid == null) {
            return;
        }

        activeDisguises.remove(uuid);
        lastReloadAt = System.currentTimeMillis();
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement("REPLACE INTO disguises (uuid, is_disguised, disguise_type, disguise_meta) VALUES (?, 0, NULL, NULL)")) {
            statement.setString(1, uuid.toString());
            statement.executeUpdate();
        } catch (Exception exception) {
            plugin.getLogger().warning("[KaosCoreBungee] Falha ao limpar disguise: " + exception.getMessage());
        }
    }

    List<UUID> getOnlineDisguisedPlayers() {
        ensureFresh();
        List<UUID> result = new ArrayList<>();
        for (ProxiedPlayer player : ProxyServer.getInstance().getPlayers()) {
            if (player != null && isDisguised(player.getUniqueId())) {
                result.add(player.getUniqueId());
            }
        }
        result.sort(Comparator.comparing(this::getVisibleName, String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    private void loadNickPool() {
        nickPool.clear();
        nickPool.addAll(DEFAULT_NICKS);

        File file = new File(plugin.getDataFolder(), "nicks.yml");
        try {
            if (!file.exists()) {
                File parent = file.getParentFile();
                if (parent != null) {
                    parent.mkdirs();
                }
                Configuration created = new Configuration();
                created.set("nicks-extras", EXTRA_NICKS);
                ConfigurationProvider.getProvider(YamlConfiguration.class).save(created, file);
            }

            Configuration config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(file);
            List<String> extras = config.getStringList("nicks-extras");
            if (extras != null) {
                for (String value : extras) {
                    String nick = Texts.safe(value).trim();
                    if (!nick.isEmpty()) {
                        nickPool.add(nick);
                    }
                }
            }
        } catch (Exception exception) {
            plugin.getLogger().warning("[KaosCoreBungee] Falha ao carregar nicks do disguise: " + exception.getMessage());
        }
    }

    private void ensureFresh() {
        if ((System.currentTimeMillis() - lastReloadAt) > RELOAD_INTERVAL_MS) {
            reloadActiveDisguises();
        }
    }
}
