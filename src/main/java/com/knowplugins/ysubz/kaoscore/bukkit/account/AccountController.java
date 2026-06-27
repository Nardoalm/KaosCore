package com.knowplugins.ysubz.kaoscore.bukkit.account;

import com.knowplugins.ysubz.kaoscore.bukkit.KaosCore;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AccountController {

    private final KaosCore plugin;
    private final Map<UUID, Account> byId = new ConcurrentHashMap<>();
    private final Map<String, UUID> byName = new ConcurrentHashMap<>();

    public AccountController(KaosCore plugin) {
        this.plugin = plugin;
    }

    public Account getOrCreate(UUID id, String name) {
        if (id == null) {
            return null;
        }

        Account account = byId.computeIfAbsent(id, key -> new Account(key, name));
        if (name != null && !name.trim().isEmpty()) {
            account.setName(name);
            byName.put(normalize(name), id);
        }
        return account;
    }

    public Account get(UUID id) {
        if (id == null) {
            return null;
        }
        Account account = byId.get(id);
        if (account != null) {
            return account;
        }
        Player player = plugin == null ? null : plugin.getServer().getPlayer(id);
        return player == null ? null : getOrCreate(player.getUniqueId(), player.getName());
    }

    public Account get(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }

        UUID id = byName.get(normalize(name));
        if (id != null) {
            return get(id);
        }

        Player player = plugin == null ? null : plugin.getServer().getPlayerExact(name);
        return player == null ? null : getOrCreate(player.getUniqueId(), player.getName());
    }

    public Account get(Player player) {
        return player == null ? null : getOrCreate(player.getUniqueId(), player.getName());
    }

    public Account trackLogin(UUID id, String name, String ip) {
        Account account = getOrCreate(id, name);
        if (account != null && ip != null && !ip.trim().isEmpty()) {
            account.setLastKnownIp(ip);
        }
        return account;
    }

    public void markOffline(Player player) {
        if (player != null) {
            getOrCreate(player.getUniqueId(), player.getName());
        }
    }

    public Collection<Account> getAccounts() {
        return Collections.unmodifiableCollection(byId.values());
    }

    public void clear() {
        byId.clear();
        byName.clear();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
