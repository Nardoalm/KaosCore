package com.knowplugins.ysubz.kaoscore.bukkit.util.discord;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class DiscordWebhookClient {

    private DiscordWebhookClient() {
    }

    public static int postJson(String webhookUrl, String json, int timeoutMs) throws IOException {
        if (webhookUrl == null || webhookUrl.trim().isEmpty()) {
            return -1;
        }

        byte[] payload = (json == null ? "" : json).getBytes(StandardCharsets.UTF_8);

        HttpURLConnection connection = (HttpURLConnection) new URL(webhookUrl).openConnection();
        connection.setConnectTimeout(Math.max(1000, timeoutMs));
        connection.setReadTimeout(Math.max(1000, timeoutMs));
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        connection.setRequestProperty("User-Agent", "KaosCore");
        connection.setFixedLengthStreamingMode(payload.length);

        try (OutputStream out = connection.getOutputStream()) {
            out.write(payload);
            out.flush();
        }

        int code = connection.getResponseCode();
        InputStream in = null;
        try {
            in = code >= 200 && code < 300 ? connection.getInputStream() : connection.getErrorStream();
            drain(in);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignored) {
                }
            }
            connection.disconnect();
        }

        return code;
    }

    private static void drain(InputStream in) throws IOException {
        if (in == null) {
            return;
        }

        byte[] buffer = new byte[512];
        while (in.read(buffer) != -1) {
            // drain
        }
    }
}

