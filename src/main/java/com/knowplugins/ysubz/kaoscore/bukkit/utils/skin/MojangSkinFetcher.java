package com.knowplugins.ysubz.kaoscore.bukkit.utils.skin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.properties.Property;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Fetches skin textures from Mojang official endpoints (name -> uuid -> textures).
 * This avoids reliance on third-party APIs and works even when the server runs in offline mode.
 */
public final class MojangSkinFetcher {

    private static final int CONNECT_TIMEOUT_MS = 8000;
    private static final int READ_TIMEOUT_MS = 8000;
    private static final String USER_AGENT = "KaosCore/skins";

    private MojangSkinFetcher() {
    }

    public static Property fetchPropertyByName(String playerName) throws IOException {
        SkinTextures textures = fetchTexturesByName(playerName);
        if (textures == null || textures.value == null || textures.value.trim().isEmpty()) {
            return null;
        }
        return new Property("textures", textures.value, textures.signature);
    }

    public static Property fetchPropertyByUuid(UUID uuid) throws IOException {
        SkinTextures textures = fetchTexturesByUuid(uuid);
        if (textures == null || textures.value == null || textures.value.trim().isEmpty()) {
            return null;
        }
        return new Property("textures", textures.value, textures.signature);
    }

    public static SkinTextures fetchTexturesByName(String playerName) throws IOException {
        String clean = cleanName(playerName);
        if (clean == null) {
            return null;
        }

        String uuidNoDashes = fetchUuidNoDashes(clean);
        if (uuidNoDashes == null) {
            return null;
        }

        return fetchTexturesByUuidNoDashes(uuidNoDashes, clean);
    }

    public static SkinTextures fetchTexturesByUuid(UUID uuid) throws IOException {
        if (uuid == null) {
            return null;
        }
        return fetchTexturesByUuidNoDashes(uuid.toString().replace("-", ""), null);
    }

    private static String cleanName(String name) {
        if (name == null) {
            return null;
        }
        String s = name.trim();
        if (s.isEmpty()) {
            return null;
        }
        if (s.length() > 16) {
            s = s.substring(0, 16);
        }
        return s;
    }

    private static String fetchUuidNoDashes(String playerName) throws IOException {
        String encoded = URLEncoder.encode(playerName, "UTF-8");
        JsonObject json = getJson("https://api.mojang.com/users/profiles/minecraft/" + encoded);
        if (json == null) {
            return null;
        }

        String id = getString(json, "id");
        if (id == null) {
            return null;
        }

        String clean = id.replace("-", "").trim();
        return clean.length() == 32 ? clean : null;
    }

    private static SkinTextures fetchTexturesByUuidNoDashes(String uuidNoDashes, String nameHint) throws IOException {
        if (uuidNoDashes == null) {
            return null;
        }
        String cleanUuid = uuidNoDashes.replace("-", "").trim();
        if (cleanUuid.length() != 32) {
            return null;
        }

        JsonObject json = getJson("https://sessionserver.mojang.com/session/minecraft/profile/" + cleanUuid + "?unsigned=false");
        if (json == null) {
            return null;
        }

        JsonArray properties = json.getAsJsonArray("properties");
        if (properties == null || properties.size() == 0) {
            return null;
        }

        for (JsonElement element : properties) {
            if (element == null || !element.isJsonObject()) {
                continue;
            }
            JsonObject property = element.getAsJsonObject();
            String propName = getString(property, "name");
            if (propName == null || !"textures".equalsIgnoreCase(propName)) {
                continue;
            }

            String value = getString(property, "value");
            String signature = getString(property, "signature");
            if (value == null || value.trim().isEmpty()) {
                continue;
            }

            String resolvedName = nameHint != null ? nameHint : getString(json, "name");
            return new SkinTextures(resolvedName, cleanUuid, value, signature);
        }

        return null;
    }

    private static JsonObject getJson(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        connection.setUseCaches(true);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", USER_AGENT);
        connection.setRequestProperty("Accept", "application/json");

        int code = connection.getResponseCode();
        if (code == HttpURLConnection.HTTP_NO_CONTENT || code == HttpURLConnection.HTTP_NOT_FOUND) {
            connection.disconnect();
            return null;
        }

        InputStream stream = (code >= 200 && code < 300) ? connection.getInputStream() : connection.getErrorStream();
        if (stream == null) {
            connection.disconnect();
            if (code >= 200 && code < 300) {
                return null;
            }
            throw new IOException("HTTP " + code + " sem resposta");
        }

        try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            JsonElement parsed = new JsonParser().parse(reader);
            JsonObject json = parsed != null && parsed.isJsonObject() ? parsed.getAsJsonObject() : null;

            if (code >= 200 && code < 300) {
                return json;
            }

            String error = json == null ? null : getString(json, "errorMessage");
            throw new IOException("HTTP " + code + (error == null ? "" : (": " + error)));
        } finally {
            connection.disconnect();
        }
    }

    private static String getString(JsonObject json, String key) {
        if (json == null || key == null || !json.has(key) || json.get(key).isJsonNull()) {
            return null;
        }
        try {
            return json.get(key).getAsString();
        } catch (Exception ignored) {
            return null;
        }
    }

    public static final class SkinTextures {
        public final String name;
        public final String uuidNoDashes;
        public final String value;
        public final String signature;

        public SkinTextures(String name, String uuidNoDashes, String value, String signature) {
            this.name = name;
            this.uuidNoDashes = uuidNoDashes;
            this.value = value;
            this.signature = signature;
        }
    }
}

