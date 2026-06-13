package com.knowplugins.ysubz.kaoscore.bungee.services;

import com.knowplugins.ysubz.kaoscore.bungee.BungeeDatabase;
import com.knowplugins.ysubz.kaoscore.bungee.Texts;
import com.knowplugins.ysubz.kaoscore.bungee.bootstrap.BungeeKaosCore;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BungeeSkinService {

    private final BungeeKaosCore plugin;
    private final BungeeDatabase database;
    private final Map<UUID, String> pendingSkins = new ConcurrentHashMap<>();
    private final Map<UUID, SkinData> skins = new ConcurrentHashMap<>();

    public BungeeSkinService(BungeeKaosCore plugin, BungeeDatabase database) {
        this.plugin = plugin;
        this.database = database;
    }

    public void savePending(UUID uuid, String skinName) {
        if (uuid == null || Texts.normalize(skinName).isEmpty()) {
            return;
        }
        pendingSkins.put(uuid, skinName);
    }

    public void save(UUID uuid, String skinName, String value, String signature) {
        if (uuid == null) {
            return;
        }
        skins.put(uuid, new SkinData(skinName, value, signature));
        pendingSkins.remove(uuid);
    }

    public void delete(UUID uuid) {
        if (uuid == null) {
            return;
        }
        skins.remove(uuid);
        pendingSkins.remove(uuid);
    }

    public SkinData get(UUID uuid) {
        return uuid == null ? null : skins.get(uuid);
    }

    public String getPendingSkin(UUID uuid) {
        return uuid == null ? null : pendingSkins.get(uuid);
    }

    public static final class SkinData {
        public final String name;
        public final String value;
        public final String signature;

        private SkinData(String name, String value, String signature) {
            this.name = name;
            this.value = value;
            this.signature = signature;
        }
    }
}
