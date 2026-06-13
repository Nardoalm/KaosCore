package com.knowplugins.ysubz.kaoscore.bungee.services;

import com.knowplugins.ysubz.kaoscore.bungee.BungeeDatabase;
import com.knowplugins.ysubz.kaoscore.bungee.Texts;
import com.knowplugins.ysubz.kaoscore.bungee.bootstrap.BungeeKaosCore;
import com.knowplugins.ysubz.kaoscore.bungee.repository.BungeePlayerAddressRepository;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BungeeDisguiseService {

    private final BungeeKaosCore plugin;
    private final BungeeDatabase database;
    private final BungeePlayerAddressRepository playerAddresses;
    private final Map<UUID, String> disguisedNames = new ConcurrentHashMap<>();
    private final Map<UUID, String> realNames = new ConcurrentHashMap<>();

    public BungeeDisguiseService(BungeeKaosCore plugin, BungeeDatabase database, BungeePlayerAddressRepository playerAddresses) {
        this.plugin = plugin;
        this.database = database;
        this.playerAddresses = playerAddresses;
    }

    public boolean isDisguised(UUID uuid) {
        return uuid != null && disguisedNames.containsKey(uuid);
    }

    public boolean isNickInUse(String nick) {
        String normalized = Texts.normalize(nick);
        if (normalized.isEmpty()) {
            return false;
        }
        for (String value : disguisedNames.values()) {
            if (normalized.equals(Texts.normalize(value))) {
                return true;
            }
        }
        for (ProxiedPlayer player : ProxyServer.getInstance().getPlayers()) {
            if (normalized.equals(Texts.normalize(player.getName()))) {
                return true;
            }
        }
        return playerAddresses.hasSeenName(nick);
    }

    public boolean hasJoinedBefore(String nick) {
        return playerAddresses.hasSeenName(nick);
    }

    public void saveDisguise(UUID uuid, String nick) {
        if (uuid == null || Texts.normalize(nick).isEmpty()) {
            return;
        }
        disguisedNames.put(uuid, nick);
        ProxiedPlayer online = ProxyServer.getInstance().getPlayer(uuid);
        realNames.put(uuid, online == null ? playerAddresses.findLatestName(uuid) : online.getName());
    }

    public void clearDisguise(UUID uuid) {
        if (uuid == null) {
            return;
        }
        disguisedNames.remove(uuid);
        realNames.remove(uuid);
    }

    public String getRandomNick() {
        for (int attempt = 0; attempt < 128; attempt++) {
            String candidate = "Nick" + Integer.toHexString((int) (System.nanoTime() & 0xffff)).toUpperCase();
            if (!isNickInUse(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    public List<UUID> getOnlineDisguisedPlayers() {
        List<UUID> result = new ArrayList<>();
        for (UUID uuid : disguisedNames.keySet()) {
            if (ProxyServer.getInstance().getPlayer(uuid) != null) {
                result.add(uuid);
            }
        }
        return result;
    }

    public String getVisibleName(UUID uuid) {
        if (uuid == null) {
            return "Desconhecido";
        }
        String disguised = disguisedNames.get(uuid);
        if (disguised != null && !disguised.trim().isEmpty()) {
            return disguised;
        }
        ProxiedPlayer online = ProxyServer.getInstance().getPlayer(uuid);
        if (online != null) {
            return online.getName();
        }
        return playerAddresses.findLatestName(uuid);
    }

    public String getAccountName(UUID uuid) {
        if (uuid == null) {
            return "Desconhecido";
        }
        String real = realNames.get(uuid);
        if (real != null && !real.trim().isEmpty()) {
            return real;
        }
        ProxiedPlayer online = ProxyServer.getInstance().getPlayer(uuid);
        return online != null ? online.getName() : playerAddresses.findLatestName(uuid);
    }

    public ProxiedPlayer findOnlineByVisibleName(String input) {
        String normalized = Texts.normalize(input);
        if (normalized.isEmpty()) {
            return null;
        }
        for (ProxiedPlayer player : ProxyServer.getInstance().getPlayers()) {
            if (normalized.equals(Texts.normalize(getVisibleName(player.getUniqueId()))) || normalized.equals(Texts.normalize(player.getName()))) {
                return player;
            }
        }
        return null;
    }

    public List<String> getAllVisibleNames() {
        List<String> result = new ArrayList<>();
        for (UUID uuid : disguisedNames.keySet()) {
            result.add(getVisibleName(uuid));
        }
        return Collections.unmodifiableList(result);
    }
}
