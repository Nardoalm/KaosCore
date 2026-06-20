package com.knowplugins.ysubz.kaoscore.managers.world;

import com.knowplugins.ysubz.kaoscore.KaosCore;
import com.knowplugins.ysubz.kaoscore.integration.practice.PracticeModeBlocker;
import com.knowplugins.ysubz.kaoscore.managers.plugin.DatabaseManager;
import com.knowplugins.ysubz.kaoscore.managers.plugin.SkinCacheRepository;
import com.knowplugins.ysubz.kaoscore.managers.plugin.SkinData;
import com.knowplugins.ysubz.kaoscore.utils.disguise.SkinMatrix;
import com.knowplugins.ysubz.kaoscore.utils.menu.MenuUtils;
import com.knowplugins.ysubz.kaoscore.utils.skin.MojangSkinFetcher;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.server.v1_8_R3.EntityArmorStand;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import net.minecraft.server.v1_8_R3.MinecraftServer;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityDestroy;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityHeadRotation;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityMetadata;
import net.minecraft.server.v1_8_R3.PacketPlayOutNamedEntitySpawn;
import net.minecraft.server.v1_8_R3.PacketPlayOutPlayerInfo;
import net.minecraft.server.v1_8_R3.PacketPlayOutSpawnEntityLiving;
import net.minecraft.server.v1_8_R3.PlayerConnection;
import net.minecraft.server.v1_8_R3.PlayerInteractManager;
import net.minecraft.server.v1_8_R3.WorldServer;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.v1_8_R3.CraftServer;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class NPCManager {

    private static final long NPC_TABLIST_REMOVE_DELAY_TICKS = 60L;
    private static final double LINE_SPACING = 0.28;

    private final KaosCore plugin;
    private final File file;
    private final Object fileLock = new Object();
    private final SkinCacheRepository skinCacheRepository;
    private volatile boolean enabled;

    private final ConcurrentMap<String, NpcDefinition> npcsById = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Skin> skinByNick = new ConcurrentHashMap<>();
    private final Set<String> loadingSkins = ConcurrentHashMap.newKeySet();

    private final ConcurrentMap<UUID, ConcurrentMap<Integer, String>> npcByEntityIdByViewer = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, ConcurrentMap<String, Integer>> entityIdByNpcIdByViewer = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, ConcurrentMap<String, List<EntityArmorStand>>> textStandsByNpcByViewer = new ConcurrentHashMap<>();

    public NPCManager(KaosCore plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.file = plugin == null ? null : new File(plugin.getDataFolder(), "npcs.yml");
        this.skinCacheRepository = (plugin == null || databaseManager == null) ? null : new SkinCacheRepository(plugin, databaseManager);
        reload();
        startVisibilityHeartbeat();
    }

    private void startVisibilityHeartbeat() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            if (!enabled) return;
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online != null && online.isOnline()) {
                    checkProximityAndSpawn(online);
                    updateHologramTexts(online);
                }
            }
        }, 40L, 20L);
    }

    private void checkProximityAndSpawn(Player viewer) {
        Location viewerLoc = viewer.getLocation();
        UUID viewerId = viewer.getUniqueId();
        ConcurrentMap<String, Integer> npcToEntity = entityIdByNpcIdByViewer.get(viewerId);

        for (NpcDefinition npc : npcsById.values()) {
            if (npc == null || !npc.enabled) continue;

            Location loc = npc.location;
            if (!loc.getWorld().getName().equals(viewerLoc.getWorld().getName())) {
                if (npcToEntity != null && npcToEntity.containsKey(npc.id)) {
                    despawnSingleNpc(viewer, npc.id);
                }
                continue;
            }

            double distanceSquared = loc.distanceSquared(viewerLoc);
            boolean isSpawned = (npcToEntity != null && npcToEntity.containsKey(npc.id));

            if (distanceSquared <= 2304) {
                if (!isSpawned) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (viewer.isOnline()) spawnNpcFor(viewer, npc);
                    });
                }
            } else {
                if (isSpawned) {
                    despawnSingleNpc(viewer, npc.id);
                }
            }
        }
    }

    private void updateHologramTexts(Player viewer) {
        ConcurrentMap<String, List<EntityArmorStand>> viewerStands = textStandsByNpcByViewer.get(viewer.getUniqueId());
        if (viewerStands == null || viewerStands.isEmpty()) return;

        PlayerConnection connection = ((CraftPlayer) viewer).getHandle().playerConnection;

        for (Map.Entry<String, List<EntityArmorStand>> entry : viewerStands.entrySet()) {
            NpcDefinition npc = npcsById.get(entry.getKey());
            if (npc == null || npc.lines.isEmpty()) continue;

            List<EntityArmorStand> stands = entry.getValue();
            int limit = Math.min(npc.lines.size(), stands.size());

            for (int i = 0; i < limit; i++) {
                EntityArmorStand as = stands.get(i);
                String rawLine = npc.lines.get(i);
                String processed = ChatColor.translateAlternateColorCodes('&', PlaceholderAPI.setPlaceholders(viewer, rawLine));

                if (!processed.equals(as.getCustomName())) {
                    as.setCustomName(processed);
                    as.setCustomNameVisible(!processed.trim().isEmpty());
                    connection.sendPacket(new PacketPlayOutEntityMetadata(as.getId(), as.getDataWatcher(), true));
                }
            }
        }
    }

    private void despawnSingleNpc(Player viewer, String npcId) {
        UUID viewerId = viewer.getUniqueId();
        ConcurrentMap<String, Integer> npcToEntity = entityIdByNpcIdByViewer.get(viewerId);
        ConcurrentMap<Integer, String> entityToNpc = npcByEntityIdByViewer.get(viewerId);

        List<Integer> entitiesToDestroy = new ArrayList<>();

        if (npcToEntity != null && npcToEntity.containsKey(npcId)) {
            int entityId = npcToEntity.remove(npcId);
            entitiesToDestroy.add(entityId);
            if (entityToNpc != null) {
                entityToNpc.remove(entityId);
            }
        }

        ConcurrentMap<String, List<EntityArmorStand>> viewerStands = textStandsByNpcByViewer.get(viewerId);
        if (viewerStands != null) {
            List<EntityArmorStand> stands = viewerStands.remove(npcId);
            if (stands != null) {
                for (EntityArmorStand as : stands) {
                    entitiesToDestroy.add(as.getId());
                }
            }
        }

        if (!entitiesToDestroy.isEmpty()) {
            int[] ids = entitiesToDestroy.stream().mapToInt(Integer::intValue).toArray();
            PlayerConnection connection = ((CraftPlayer) viewer).getHandle().playerConnection;
            connection.sendPacket(new PacketPlayOutEntityDestroy(ids));
        }
    }

    public void reload() {
        npcsById.clear();
        enabled = false;

        if (plugin == null || file == null) {
            return;
        }

        if (!file.exists()) {
            try {
                plugin.saveResource("npcs.yml", false);
            } catch (Exception ignored) {
            }
        }

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        enabled = cfg.getBoolean("enabled", true);
        if (!enabled) {
            return;
        }
        ConfigurationSection root = cfg.getConfigurationSection("npcs");
        if (root == null) {
            return;
        }

        Map<String, NpcDefinition> loaded = new LinkedHashMap<>();
        for (String key : root.getKeys(false)) {
            if (key == null) {
                continue;
            }

            ConfigurationSection section = root.getConfigurationSection(key);
            if (section == null) {
                continue;
            }

            String id = normalize(key);
            if (id.isEmpty()) {
                continue;
            }

            boolean enabled = section.getBoolean("enabled", true);
            String worldName = section.getString("world", "");
            World world = worldName == null ? null : Bukkit.getWorld(worldName);
            if (world == null) {
                continue;
            }

            double x = section.getDouble("x");
            double y = section.getDouble("y");
            double z = section.getDouble("z");
            float yaw = (float) section.getDouble("yaw");
            float pitch = (float) section.getDouble("pitch");
            Location loc = new Location(world, x, y, z, yaw, pitch);

            List<String> lines = new ArrayList<>();
            if (section.contains("lines")) {
                lines.addAll(section.getStringList("lines"));
            } else {
                for (int i = 1; i <= 5; i++) {
                    if (section.contains("line" + i)) {
                        lines.add(section.getString("line" + i));
                    }
                }
            }

            String skinNick = cleanNick(section.getString("skin-nick", ""));
            List<String> commands = section.getStringList("commands");
            if (commands == null) {
                commands = Collections.emptyList();
            }

            loaded.put(id, new NpcDefinition(id, lines, enabled, loc, skinNick, commands));
        }

        npcsById.putAll(loaded);
        preloadSkinsAsync();
    }

    public Set<String> getNpcIds() {
        return new LinkedHashSet<>(npcsById.keySet());
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean upsertNpc(String id, Location loc, String skinNick) {
        String key = normalize(id);
        if (plugin == null || file == null || key.isEmpty() || loc == null || loc.getWorld() == null) {
            return false;
        }

        String cleanSkin = skinNick == null ? "" : cleanNick(skinNick);

        synchronized (fileLock) {
            FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
            String path = "npcs." + key;
            cfg.set(path + ".enabled", true);
            cfg.set(path + ".world", loc.getWorld().getName());
            cfg.set(path + ".x", loc.getX());
            cfg.set(path + ".y", loc.getY());
            cfg.set(path + ".z", loc.getZ());
            cfg.set(path + ".yaw", (double) loc.getYaw());
            cfg.set(path + ".pitch", (double) loc.getPitch());
            if (!cfg.contains(path + ".line1")) {
                cfg.set(path + ".line1", "&b&lALLEY");
            }
            if (!cfg.contains(path + ".line2")) {
                cfg.set(path + ".line2", "%kaos_playing-count%");
            }
            if (!cleanSkin.isEmpty()) {
                cfg.set(path + ".skin-nick", cleanSkin);
            } else if (!cfg.contains(path + ".skin-nick")) {
                cfg.set(path + ".skin-nick", "");
            }
            if (!cfg.contains(path + ".commands")) {
                cfg.set(path + ".commands", Collections.singletonList("menu"));
            }

            try {
                cfg.save(file);
            } catch (Exception e) {
                plugin.getLogger().warning("[KaosCore] Error saving npcs.yml: " + e.getMessage());
                return false;
            }
        }

        reload();
        refreshAllViewers();
        if (!cleanSkin.isEmpty()) {
            preloadSkinAsync(cleanSkin);
        }
        return true;
    }

    public boolean removeNpc(String id) {
        String key = normalize(id);
        if (plugin == null || file == null || key.isEmpty()) {
            return false;
        }

        synchronized (fileLock) {
            FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
            if (!cfg.contains("npcs." + key)) {
                return false;
            }
            cfg.set("npcs." + key, null);
            try {
                cfg.save(file);
            } catch (Exception e) {
                plugin.getLogger().warning("[KaosCore] Error saving npcs.yml: " + e.getMessage());
                return false;
            }
        }

        reload();
        refreshAllViewers();
        return true;
    }

    public void refreshAllViewers() {
        if (plugin == null || !enabled) {
            return;
        }

        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, this::refreshAllViewers);
            return;
        }

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online != null && online.isOnline()) {
                despawnAll(online);
                spawnAll(online);
            }
        }
    }

    public void spawnAll(Player viewer) {
        if (!enabled || viewer == null || plugin == null || !viewer.isOnline()) {
            return;
        }
        Location viewerLoc = viewer.getLocation();
        for (NpcDefinition npc : npcsById.values()) {
            if (npc == null || !npc.enabled || npc.location == null || !npc.location.getWorld().getName().equals(viewerLoc.getWorld().getName())) {
                continue;
            }
            if (npc.location.distanceSquared(viewerLoc) <= 2304) {
                spawnNpcFor(viewer, npc);
            }
        }
    }

    public void despawnAll(Player viewer) {
        if (viewer == null || !viewer.isOnline()) {
            return;
        }

        UUID viewerId = viewer.getUniqueId();
        Map<Integer, String> map = npcByEntityIdByViewer.remove(viewerId);
        entityIdByNpcIdByViewer.remove(viewerId);

        List<Integer> entitiesToDestroy = new ArrayList<>();

        if (map != null && !map.isEmpty()) {
            entitiesToDestroy.addAll(map.keySet());
        }

        ConcurrentMap<String, List<EntityArmorStand>> viewerStands = textStandsByNpcByViewer.remove(viewerId);
        if (viewerStands != null) {
            for (List<EntityArmorStand> stands : viewerStands.values()) {
                for (EntityArmorStand as : stands) {
                    entitiesToDestroy.add(as.getId());
                }
            }
            viewerStands.clear();
        }

        if (entitiesToDestroy.isEmpty()) {
            return;
        }

        int[] ids = entitiesToDestroy.stream().mapToInt(Integer::intValue).toArray();
        PlayerConnection connection = ((CraftPlayer) viewer).getHandle().playerConnection;
        connection.sendPacket(new PacketPlayOutEntityDestroy(ids));
    }

    public void cleanup(Player viewer) {
        if (viewer == null) {
            return;
        }
        UUID viewerId = viewer.getUniqueId();
        npcByEntityIdByViewer.remove(viewerId);
        entityIdByNpcIdByViewer.remove(viewerId);
        textStandsByNpcByViewer.remove(viewerId);
    }

    public void shutdown() {
        if (plugin != null && Bukkit.isPrimaryThread()) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online != null && online.isOnline()) {
                    despawnAll(online);
                }
            }
        }
        npcsById.clear();
        skinByNick.clear();
        loadingSkins.clear();
        npcByEntityIdByViewer.clear();
        entityIdByNpcIdByViewer.clear();
        textStandsByNpcByViewer.clear();
    }

    public boolean isNpcEntity(Player viewer, int entityId) {
        if (!enabled || viewer == null) {
            return false;
        }
        Map<Integer, String> map = npcByEntityIdByViewer.get(viewer.getUniqueId());
        return map != null && map.containsKey(entityId);
    }

    public boolean handleInteract(Player viewer, int entityId) {
        if (!enabled || viewer == null || !viewer.isOnline()) {
            return false;
        }

        if (PracticeModeBlocker.check(viewer, false)) {
            return true;
        }

        Map<Integer, String> map = npcByEntityIdByViewer.get(viewer.getUniqueId());
        if (map == null) {
            return false;
        }

        String npcId = map.get(entityId);
        if (npcId == null || npcId.isEmpty()) {
            return false;
        }

        NpcDefinition npc = npcsById.get(normalize(npcId));
        if (npc == null) {
            return false;
        }

        executeNpcCommands(viewer, npc);
        return true;
    }

    private void spawnNpcFor(Player viewer, NpcDefinition npc) {
        if (viewer == null || npc == null || npc.location == null || npc.location.getWorld() == null) {
            return;
        }

        if (!ensureSkinReady(npc)) {
            return;
        }

        UUID viewerId = viewer.getUniqueId();
        ConcurrentMap<String, Integer> npcToEntity = entityIdByNpcIdByViewer.computeIfAbsent(viewerId, k -> new ConcurrentHashMap<>());
        if (npcToEntity.containsKey(npc.id)) {
            return;
        }

        Location loc = npc.location;
        MinecraftServer server = ((CraftServer) Bukkit.getServer()).getServer();
        WorldServer world = ((CraftWorld) loc.getWorld()).getHandle();

        GameProfile profile = new GameProfile(buildProfileUuid(npc), "§8[NPC] " + npc.id);
        if (profile.getName().length() > 16) {
            profile = new GameProfile(buildProfileUuid(npc), profile.getName().substring(0, 16));
        }
        applySkin(profile, npc);

        EntityPlayer entity = new EntityPlayer(server, world, profile, new PlayerInteractManager(world));
        entity.setLocation(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());

        int entityId = entity.getId();
        npcByEntityIdByViewer
                .computeIfAbsent(viewerId, k -> new ConcurrentHashMap<>())
                .put(entityId, npc.id);
        npcToEntity.put(npc.id, entityId);

        PlayerConnection connection = ((CraftPlayer) viewer).getHandle().playerConnection;
        connection.sendPacket(new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, entity));
        connection.sendPacket(new PacketPlayOutNamedEntitySpawn(entity));
        connection.sendPacket(new PacketPlayOutEntityHeadRotation(entity, (byte) (loc.getYaw() * 256 / 360)));

        if (!npc.lines.isEmpty()) {
            List<EntityArmorStand> stands = new ArrayList<>();
            Location currentLoc = loc.clone().add(0, 1.85 + ((npc.lines.size() - 1) * LINE_SPACING), 0);

            for (String raw : npc.lines) {
                EntityArmorStand as = new EntityArmorStand(world);
                as.setLocation(currentLoc.getX(), currentLoc.getY(), currentLoc.getZ(), 0.0F, 0.0F);
                as.setGravity(false);
                as.setInvisible(true);
                as.setSmall(true);
                as.setBasePlate(false);
                as.setArms(false);

                String text = ChatColor.translateAlternateColorCodes('&', PlaceholderAPI.setPlaceholders(viewer, raw));
                as.setCustomName(text);
                as.setCustomNameVisible(!text.trim().isEmpty());

                connection.sendPacket(new PacketPlayOutSpawnEntityLiving(as));
                connection.sendPacket(new PacketPlayOutEntityMetadata(as.getId(), as.getDataWatcher(), true));

                stands.add(as);
                currentLoc.subtract(0, LINE_SPACING, 0);
            }
            textStandsByNpcByViewer.computeIfAbsent(viewerId, k -> new ConcurrentHashMap<>()).put(npc.id, stands);
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            try {
                if (viewer.isOnline()) {
                    connection.sendPacket(new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER, entity));
                }
            } catch (Exception ignored) {
            }
        }, NPC_TABLIST_REMOVE_DELAY_TICKS);
    }

    private void applySkin(GameProfile profile, NpcDefinition npc) {
        if (profile == null || npc == null) {
            return;
        }

        String nick = normalize(npc.skinNick);
        Skin skin = nick.isEmpty() ? null : skinByNick.get(nick);
        if (skin == null || skin.value == null || skin.value.trim().isEmpty()) {
            SkinMatrix.SkinData matrix = SkinMatrix.getDeterministicSkin(nick.isEmpty() ? npc.id : nick);
            if (matrix != null && matrix.value != null && !matrix.value.trim().isEmpty()) {
                skin = new Skin(matrix.value, matrix.signature);
            }
        }

        if (skin == null || skin.value == null || skin.value.trim().isEmpty()) {
            return;
        }

        profile.getProperties().removeAll("textures");
        if (skin.signature != null && !skin.signature.trim().isEmpty()) {
            profile.getProperties().put("textures", new Property("textures", skin.value, skin.signature));
        } else {
            profile.getProperties().put("textures", new Property("textures", skin.value));
        }
    }

    private void preloadSkinsAsync() {
        if (plugin == null) {
            return;
        }

        Set<String> unique = new LinkedHashSet<>();
        for (NpcDefinition npc : npcsById.values()) {
            if (npc == null) {
                continue;
            }
            String nick = normalize(npc.skinNick);
            if (!nick.isEmpty()) {
                unique.add(nick);
            }
        }

        if (unique.isEmpty()) {
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean changed = false;
            for (String nick : unique) {
                Skin resolved = resolveSkinBlocking(nick);
                if (resolved != null && resolved.value != null && !resolved.value.trim().isEmpty()) {
                    skinByNick.put(normalize(nick), resolved);
                    changed = true;
                }
            }

            if (changed) {
                Bukkit.getScheduler().runTask(plugin, this::refreshAllViewers);
            }
        });
    }

    private void preloadSkinAsync(String nick) {
        if (plugin == null) {
            return;
        }
        String clean = normalize(nick);
        if (clean.isEmpty()) {
            return;
        }
        if (skinByNick.containsKey(clean)) {
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                Skin resolved = resolveSkinBlocking(clean);
                if (resolved != null && resolved.value != null && !resolved.value.trim().isEmpty()) {
                    skinByNick.put(clean, resolved);
                    Bukkit.getScheduler().runTask(plugin, this::refreshAllViewers);
                }
            } finally {
                loadingSkins.remove(clean);
            }
        });
    }

    private boolean ensureSkinReady(NpcDefinition npc) {
        if (npc == null) {
            return false;
        }

        String nick = normalize(npc.skinNick);
        if (nick.isEmpty()) {
            return true;
        }

        Skin skin = skinByNick.get(nick);
        if (skin != null && skin.value != null && !skin.value.trim().isEmpty()) {
            return true;
        }

        if (loadingSkins.add(nick)) {
            preloadSkinAsync(nick);
        }
        return false;
    }

    private Skin resolveSkinBlocking(String nick) {
        String clean = cleanNick(nick);
        if (clean.isEmpty()) {
            return null;
        }

        try {
            Property textures = MojangSkinFetcher.fetchPropertyByName(clean);
            if (textures != null && textures.getValue() != null && !textures.getValue().trim().isEmpty()) {
                Skin skin = new Skin(textures.getValue(), textures.getSignature());
                SkinCacheRepository repo = skinCacheRepository;
                if (repo != null) {
                    repo.saveAsync(clean, skin.value, skin.signature);
                }
                return skin;
            }
        } catch (Exception ignored) {
        }

        SkinCacheRepository repo = skinCacheRepository;
        if (repo != null) {
            try {
                SkinData cached = repo.loadSync(clean);
                if (cached != null && cached.value != null && !cached.value.trim().isEmpty()) {
                    return new Skin(cached.value, cached.signature);
                }
            } catch (Exception ignored) {
            }
        }

        SkinMatrix.SkinData matrix = SkinMatrix.getDeterministicSkin(clean);
        if (matrix != null && matrix.value != null && !matrix.value.trim().isEmpty()) {
            return new Skin(matrix.value, matrix.signature);
        }

        return null;
    }

    private void executeNpcCommands(Player player, NpcDefinition npc) {
        if (player == null || npc == null) {
            return;
        }

        List<String> commands = npc.commands == null ? Collections.emptyList() : npc.commands;
        if (commands.isEmpty()) {
            MenuUtils.openMenu(player);
            return;
        }

        for (String raw : commands) {
            if (raw == null) {
                continue;
            }
            String line = raw.trim();
            if (line.isEmpty()) {
                continue;
            }

            String replaced = line.replace("{player}", player.getName()).replace("{npc}", npc.id);
            String lower = replaced.toLowerCase(Locale.ROOT);

            if (lower.equals("menu") || lower.equals("openmenu")) {
                MenuUtils.openMenu(player);
                continue;
            }

            if (lower.startsWith("message:") || lower.startsWith("msg:")) {
                int idx = replaced.indexOf(':');
                String msg = idx == -1 ? "" : replaced.substring(idx + 1);
                if (!msg.trim().isEmpty()) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', msg.trim()));
                }
                continue;
            }

            if (lower.startsWith("console:")) {
                String cmd = replaced.substring("console:".length()).trim();
                dispatch(Bukkit.getConsoleSender(), cmd);
                continue;
            }

            if (lower.startsWith("player:")) {
                String cmd = replaced.substring("player:".length()).trim();
                dispatch(player, cmd);
                continue;
            }

            dispatch(player, replaced);
        }
    }

    private void dispatch(org.bukkit.command.CommandSender sender, String command) {
        if (sender == null || command == null) {
            return;
        }
        String cmd = command.trim();
        if (cmd.isEmpty()) {
            return;
        }
        if (cmd.startsWith("/")) {
            cmd = cmd.substring(1);
        }
        Bukkit.dispatchCommand(sender, cmd);
    }

    private UUID buildProfileUuid(NpcDefinition npc) {
        String key = "kaoscore:npc:" + safeNpcPart(npc == null ? null : npc.id) + ":" + safeNpcPart(npc == null ? null : npc.skinNick);
        return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));
    }

    private String safeNpcPart(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String cleanNick(String nick) {
        if (nick == null) {
            return "";
        }
        String s = ChatColor.stripColor(nick).trim();
        if (s.isEmpty()) {
            return "";
        }
        if (s.length() > 16) {
            s = s.substring(0, 16);
        }
        return s;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private static final class NpcDefinition {
        public final String id;
        public final List<String> lines;
        public final boolean enabled;
        public final Location location;
        public final String skinNick;
        public final List<String> commands;

        private NpcDefinition(String id, List<String> lines, boolean enabled, Location location, String skinNick, List<String> commands) {
            this.id = id;
            this.lines = lines == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(lines));
            this.enabled = enabled;
            this.location = location;
            this.skinNick = skinNick == null ? "" : skinNick;
            this.commands = commands == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(commands));
        }
    }

    private static final class Skin {
        public final String value;
        public final String signature;

        private Skin(String value, String signature) {
            this.value = value;
            this.signature = signature;
        }
    }
}