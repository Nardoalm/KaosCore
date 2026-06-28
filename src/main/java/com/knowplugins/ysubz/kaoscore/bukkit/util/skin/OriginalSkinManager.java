package com.knowplugins.ysubz.kaoscore.bukkit.util.skin;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.bukkit.entity.Player;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;

import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class OriginalSkinManager {

    private static final Map<UUID, Property> ORIGINAL_SKINS = new ConcurrentHashMap<>();

    private OriginalSkinManager() {
    }

    public static void cache(Player player) {
        if (player == null) {
            return;
        }
        cache(player, ((CraftPlayer) player).getProfile());
    }

    public static void cache(Player player, GameProfile profile) {
        if (player == null || profile == null) {
            return;
        }

        UUID id = player.getUniqueId();
        if (ORIGINAL_SKINS.containsKey(id)) {
            return;
        }

        Collection<Property> textures = profile.getProperties().get("textures");
        if (textures != null && !textures.isEmpty()) {
            ORIGINAL_SKINS.put(id, textures.iterator().next());
        }
    }

    public static Property get(UUID uuid) {
        return uuid == null ? null : ORIGINAL_SKINS.get(uuid);
    }

    public static Property fetchOriginal(UUID uuid) throws Exception {
        if (uuid == null) {
            return null;
        }

        URL url = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid.toString().replace("-", "") + "?unsigned=false");
        try (InputStreamReader reader = new InputStreamReader(url.openStream())) {
            JsonObject json = new JsonParser().parse(reader).getAsJsonObject();
            JsonArray properties = json.getAsJsonArray("properties");
            if (properties == null || properties.size() == 0) {
                return null;
            }

            for (int i = 0; i < properties.size(); i++) {
                JsonObject property = properties.get(i).getAsJsonObject();
                if (!property.has("name") || !"textures".equalsIgnoreCase(property.get("name").getAsString())) {
                    continue;
                }

                String value = property.has("value") && !property.get("value").isJsonNull() ? property.get("value").getAsString() : null;
                String signature = property.has("signature") && !property.get("signature").isJsonNull() ? property.get("signature").getAsString() : null;
                if (value != null && !value.trim().isEmpty()) {
                    Property texture = new Property("textures", value, signature);
                    ORIGINAL_SKINS.putIfAbsent(uuid, texture);
                    return texture;
                }
            }
        }

        return null;
    }
}
