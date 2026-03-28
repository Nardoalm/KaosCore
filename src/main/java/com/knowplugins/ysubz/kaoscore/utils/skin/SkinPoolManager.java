package com.knowplugins.ysubz.kaoscore.utils.skin;

import com.knowplugins.ysubz.kaoscore.KaosCore;
import com.knowplugins.ysubz.kaoscore.managers.plugin.DatabaseManager;
import com.knowplugins.ysubz.kaoscore.managers.plugin.SkinCacheRepository;
import com.knowplugins.ysubz.kaoscore.managers.plugin.SkinData;
import com.knowplugins.ysubz.kaoscore.utils.disguise.SkinMatrix;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class SkinPoolManager {

    private static final long NEGATIVE_NOT_FOUND_TTL_MS = 12L * 60L * 60L * 1000L;
    private static final long NEGATIVE_ERROR_TTL_MS = 2L * 60L * 1000L;

    private final KaosCore plugin;
    private final File file;
    private final SkinCacheRepository cacheRepository;

    private final ConcurrentMap<String, CachedSkin> runtimeCacheByNick;
    private final ConcurrentMap<String, Long> negativeCacheUntilByNick;
    private volatile List<CachedSkin> poolSkins;
    private volatile List<String> poolNicks;

    public SkinPoolManager(KaosCore plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.file = plugin == null ? null : new File(plugin.getDataFolder(), "skins.yml");
        this.cacheRepository = (plugin == null || databaseManager == null) ? null : new SkinCacheRepository(plugin, databaseManager);
        this.runtimeCacheByNick = new ConcurrentHashMap<>();
        this.negativeCacheUntilByNick = new ConcurrentHashMap<>();
        this.poolSkins = Collections.emptyList();
        this.poolNicks = Collections.emptyList();
        reloadPoolList();
    }

    public void reloadPoolList() {
        if (file == null) {
            return;
        }

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        List<String> nicks = cfg.getStringList("pool.nicks");

        List<String> cleaned = new ArrayList<>();
        if (nicks != null) {
            for (String nick : nicks) {
                String cleanNick = cleanNick(nick);
                if (!cleanNick.isEmpty()) {
                    cleaned.add(cleanNick);
                }
            }
        }

        this.poolNicks = cleaned.isEmpty() ? Collections.emptyList() : Collections.unmodifiableList(cleaned);
    }

    public void preloadPoolAsync() {
        if (plugin == null) {
            return;
        }

        reloadPoolList();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<String> nicks = poolNicks;
            List<CachedSkin> loaded = new ArrayList<>();

            if (nicks != null && !nicks.isEmpty()) {
                for (String nick : nicks) {
                    CachedSkin cached = loadCachedSkin(nick);
                    if (cached != null && cached.isValid()) {
                        loaded.add(cached);
                        continue;
                    }

                    FetchResult fetched = fetchByNick(nick);
                    if (fetched.type == FetchType.SUCCESS && fetched.skin != null && fetched.skin.isValid()) {
                        loaded.add(fetched.skin);
                    }
                }
            }

            // Fallback: if pool.nicks is empty/invalid or Mojang is down, reuse any cached premium skins from DB.
            if (loaded.isEmpty()) {
                loaded.addAll(loadAnyCachedPool(128));
            }

            poolSkins = loaded.isEmpty() ? Collections.emptyList() : Collections.unmodifiableList(loaded);
        });
    }

    public void applyJoinSkin(Player player) {
        if (player == null || plugin == null || !player.isOnline()) {
            return;
        }

        if (!(player instanceof CraftPlayer)) {
            return;
        }

        String nick = player.getName();
        String normalized = normalizeNick(nick);
        long now = System.currentTimeMillis();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            // Apply cached premium skin if we have it.
            CachedSkin cached = loadCachedSkin(nick);
            if (cached != null && cached.isValid()) {
                Bukkit.getScheduler().runTask(plugin, () -> applySkinIfOnline(player, cached));
                return;
            }

            Long negativeUntil = negativeCacheUntilByNick.get(normalized);
            if (negativeUntil != null && negativeUntil > now) {
                CachedSkin pool = pickForPirate(nick);
                if (pool != null && pool.isValid()) {
                    Bukkit.getScheduler().runTask(plugin, () -> applySkinIfOnline(player, pool));
                }
                return;
            }

            FetchResult fetched = fetchByNick(nick);
            if (fetched.type == FetchType.SUCCESS && fetched.skin != null && fetched.skin.isValid()) {
                Bukkit.getScheduler().runTask(plugin, () -> applySkinIfOnline(player, fetched.skin));
                return;
            }

            long ttl = fetched.type == FetchType.NOT_FOUND ? NEGATIVE_NOT_FOUND_TTL_MS : NEGATIVE_ERROR_TTL_MS;
            negativeCacheUntilByNick.put(normalized, now + ttl);

            CachedSkin pool = pickForPirate(nick);
            if (pool != null && pool.isValid()) {
                Bukkit.getScheduler().runTask(plugin, () -> applySkinIfOnline(player, pool));
            }
        });
    }

    private void applySkinIfOnline(Player player, CachedSkin skin) {
        if (player == null || skin == null || !skin.isValid() || !player.isOnline()) {
            return;
        }
        applySkin(player, skin);
    }

    private CachedSkin pickForPirate(String playerName) {
        List<CachedSkin> skins = poolSkins;
        if (skins == null || skins.isEmpty()) {
            List<CachedSkin> fallback = loadAnyCachedPool(64);
            if (fallback != null && !fallback.isEmpty()) {
                poolSkins = Collections.unmodifiableList(fallback);
                skins = poolSkins;
            } else {
                // Hard fallback (no Mojang, no DB cache): use built-in signed skins so pirates never stay Steve.
                SkinMatrix.SkinData matrix = SkinMatrix.getDeterministicSkin(playerName);
                if (matrix == null || matrix.value == null || matrix.value.trim().isEmpty()) {
                    return null;
                }
                return new CachedSkin("matrix", matrix.value, matrix.signature);
            }
        }

        String key = normalizeNick(playerName);
        int hash = key.hashCode();
        int index = (hash & 0x7fffffff) % skins.size();
        return skins.get(index);
    }

    private List<CachedSkin> loadAnyCachedPool(int limit) {
        SkinCacheRepository repo = cacheRepository;
        if (repo == null) {
            return Collections.emptyList();
        }

        List<SkinData> any = repo.loadAnyPoolSync(limit);
        if (any == null || any.isEmpty()) {
            return Collections.emptyList();
        }

        List<CachedSkin> out = new ArrayList<>();
        for (SkinData data : any) {
            if (data == null || data.value == null || data.value.trim().isEmpty()) {
                continue;
            }
            out.add(new CachedSkin(cleanNick(data.name), data.value, data.signature));
        }
        return out;
    }

    private CachedSkin loadCachedSkin(String nick) {
        String cleanNick = cleanNick(nick);
        if (cleanNick.isEmpty()) {
            return null;
        }

        String normalized = normalizeNick(cleanNick);
        CachedSkin runtime = runtimeCacheByNick.get(normalized);
        if (runtime != null && runtime.isValid()) {
            return runtime;
        }

        SkinCacheRepository repo = cacheRepository;
        if (repo == null) {
            return null;
        }

        SkinData stored = repo.loadSync(cleanNick);
        if (stored == null || stored.value == null || stored.value.trim().isEmpty()) {
            return null;
        }

        CachedSkin cached = new CachedSkin(cleanNick, stored.value, stored.signature);
        runtimeCacheByNick.put(normalized, cached);
        return cached;
    }

    private FetchResult fetchByNick(String nick) {
        String cleanNick = cleanNick(nick);
        if (cleanNick.isEmpty()) {
            return FetchResult.notFound();
        }

        try {
            MojangSkinFetcher.SkinTextures textures = MojangSkinFetcher.fetchTexturesByName(cleanNick);
            if (textures == null || textures.value == null || textures.value.trim().isEmpty()) {
                return FetchResult.notFound();
            }

            CachedSkin skin = new CachedSkin(cleanNick, textures.value, textures.signature);
            runtimeCacheByNick.put(normalizeNick(cleanNick), skin);

            SkinCacheRepository repo = cacheRepository;
            if (repo != null) {
                repo.saveAsync(cleanNick, textures.value, textures.signature);
            }

            return FetchResult.success(skin);
        } catch (IOException e) {
            if (plugin != null) {
                plugin.getLogger().warning("[KaosCore] Erro ao buscar skin '" + cleanNick + "': " + e.getMessage());
            }
            return FetchResult.error();
        } catch (Exception e) {
            if (plugin != null) {
                plugin.getLogger().warning("[KaosCore] Erro ao buscar skin '" + cleanNick + "': " + e.getMessage());
            }
            return FetchResult.error();
        }
    }

    private void applySkin(Player player, CachedSkin skin) {
        if (player == null || skin == null || !skin.isValid() || !(player instanceof CraftPlayer)) {
            return;
        }

        try {
            GameProfile profile = ((CraftPlayer) player).getProfile();
            OriginalSkinManager.cache(player, profile);
            profile.getProperties().removeAll("textures");
            profile.getProperties().put("textures", new Property("textures", skin.value, skin.signature));
            OriginalSkinManager.cache(player, profile);
            new SkinApplier(player).applySkinForSelf();
        } catch (Exception e) {
            if (plugin != null) {
                plugin.getLogger().warning("[KaosCore] Falha ao aplicar skin em " + player.getName() + ": " + e.getMessage());
            }
        }
    }

    private String cleanNick(String nick) {
        if (nick == null) {
            return "";
        }
        String s = nick.trim();
        if (s.isEmpty()) {
            return "";
        }
        if (s.length() > 16) {
            s = s.substring(0, 16);
        }
        return s;
    }

    private String normalizeNick(String nick) {
        if (nick == null) {
            return "";
        }
        return nick.trim().toLowerCase(Locale.ROOT);
    }

    public static final class CachedSkin {
        public final String nick;
        public final String value;
        public final String signature;

        private CachedSkin(String nick, String value, String signature) {
            this.nick = nick == null ? "" : nick;
            this.value = value;
            this.signature = signature;
        }

        public boolean isValid() {
            return value != null && !value.trim().isEmpty();
        }
    }

    private enum FetchType {
        SUCCESS,
        NOT_FOUND,
        ERROR
    }

    private static final class FetchResult {
        private final FetchType type;
        private final CachedSkin skin;

        private FetchResult(FetchType type, CachedSkin skin) {
            this.type = type;
            this.skin = skin;
        }

        private static FetchResult success(CachedSkin skin) {
            return new FetchResult(FetchType.SUCCESS, skin);
        }

        private static FetchResult notFound() {
            return new FetchResult(FetchType.NOT_FOUND, null);
        }

        private static FetchResult error() {
            return new FetchResult(FetchType.ERROR, null);
        }
    }
}
