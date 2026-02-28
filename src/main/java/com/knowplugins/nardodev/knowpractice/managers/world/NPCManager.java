package com.knowplugins.nardodev.knowpractice.managers.world;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.knowplugins.nardodev.knowpractice.KnowPractice;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.server.v1_8_R3.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_8_R3.CraftServer;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NPCManager {

    private final KnowPractice plugin;
    private String cachedValue = "";
    private String cachedSignature = "";
    private final Map<UUID, Integer> npcIds = new HashMap<>();


    public NPCManager(KnowPractice plugin) {
        this.plugin = plugin;
    }

    public Integer getNpcId(Player player) {
        return this.npcIds.get(player.getUniqueId());
    }

    public void removeNpcId(Player player) {
        this.npcIds.remove(player.getUniqueId());
    }

    public void applySkin(GameProfile profile) {
        if (!cachedValue.isEmpty() && !cachedSignature.isEmpty()) {
            profile.getProperties().put("textures", new Property("textures", cachedValue, cachedSignature));
        }
    }

    public void loadSkinCache() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String skinNick = plugin.getConfig().getString("npc-spawn.skin-nick");
            if (skinNick == null || skinNick.isEmpty()) return;

            try {
                URL url = new URL("https://api.ashcon.app/mojang/v2/user/" + skinNick);
                InputStreamReader reader = new InputStreamReader(url.openStream());
                JsonObject json = new JsonParser().parse(reader).getAsJsonObject();
                JsonObject textureProperty = json.getAsJsonObject("textures").getAsJsonObject("raw");

                this.cachedValue = textureProperty.get("value").getAsString();
                this.cachedSignature = textureProperty.get("signature").getAsString();

                plugin.getLogger().info("Skin de " + skinNick + " carregada no cache com sucesso.");
            } catch (Exception e) {
                plugin.getLogger().warning("Erro ao carregar cache de skin para: " + skinNick);
            }
        });
    }

    public void spawnNPC(Player player, Location loc) {
        MinecraftServer server = ((CraftServer) Bukkit.getServer()).getServer();
        WorldServer world = ((CraftWorld) loc.getWorld()).getHandle();

        GameProfile profile = new GameProfile(UUID.randomUUID(), "§bPractice");

        applySkin(profile);

        EntityPlayer npc = new EntityPlayer(server, world, profile, new PlayerInteractManager(world));
        npc.setLocation(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());

        this.npcIds.put(player.getUniqueId(), npc.getId());

        PlayerConnection connection = ((CraftPlayer) player).getHandle().playerConnection;

        connection.sendPacket(new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, npc));
        connection.sendPacket(new PacketPlayOutNamedEntitySpawn(npc));
        connection.sendPacket(new PacketPlayOutEntityHeadRotation(npc, (byte) (loc.getYaw() * 256 / 360)));

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            connection.sendPacket(new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER, npc));
        }, 40L);
    }

    public Location getNPCLocation() {
        String worldName = plugin.getConfig().getString("npc-spawn.world");
        if (worldName == null || Bukkit.getWorld(worldName) == null) return null;

        double x = plugin.getConfig().getDouble("npc-spawn.x");
        double y = plugin.getConfig().getDouble("npc-spawn.y");
        double z = plugin.getConfig().getDouble("npc-spawn.z");
        float yaw = (float) plugin.getConfig().getDouble("npc-spawn.yaw");
        float pitch = (float) plugin.getConfig().getDouble("npc-spawn.pitch");

        return new Location(Bukkit.getWorld(worldName), x, y, z, yaw, pitch);
    }
}