package com.knowplugins.ysubz.kaoscore.bukkit.auth.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.knowplugins.ysubz.kaoscore.bukkit.auth.http.HttpClient;

import java.util.UUID;

public class MojangAPI {

    public static boolean isPremium(String name) {
        try {
            String response = HttpClient.DEFAULT.get("https://api.mojang.com/users/profiles/minecraft/" + name);
            if (response != null && !response.trim().isEmpty()) {
                JsonObject json = new JsonParser().parse(response).getAsJsonObject();
                return json.has("id");
            }
        } catch (Exception ignored) {}
        return false;
    }

    public static UUID hasJoined(String username, String serverHash) {
        try {
            String response = HttpClient.DEFAULT.get("https://sessionserver.mojang.com/session/minecraft/hasJoined?username=" + username + "&serverId=" + serverHash);
            if (response != null && !response.trim().isEmpty()) {
                JsonObject json = new JsonParser().parse(response).getAsJsonObject();
                if (json.has("id")) {
                    String rawId = json.get("id").getAsString();
                    String formatted = rawId.replaceFirst("(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)", "$1-$2-$3-$4-$5");
                    return UUID.fromString(formatted);
                }
            }
        } catch (Exception ignored) {}
        return null;
    }
}