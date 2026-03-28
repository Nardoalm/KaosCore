package com.knowplugins.ysubz.kaoscore.bungee;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.config.Configuration;

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class BungeeDiscordWebhookManager {

    private static final Gson GSON = new Gson();
    private static final int DEFAULT_TIMEOUT_MS = 7000;

    private final BungeeKaosCore plugin;
    private Webhook staffWebhook;
    private Webhook punishmentsWebhook;
    private boolean staffLogCommands;
    private final List<String> staffCommands = new ArrayList<>();
    private boolean punishmentsLogExpired;
    private boolean punishmentsLogReplaced;

    public BungeeDiscordWebhookManager(BungeeKaosCore plugin, Configuration config) {
        this.plugin = plugin;
        reload(config);
    }

    public void reload(Configuration config) {
        this.staffWebhook = new Webhook(config.getBoolean("staff.enabled", false), config.getString("staff.url", ""), config.getString("staff.username", "KaosCore Staff"), config.getString("staff.avatar-url", ""));
        this.punishmentsWebhook = new Webhook(config.getBoolean("punishments.enabled", false), config.getString("punishments.url", ""), config.getString("punishments.username", "KaosCore Punishments"), config.getString("punishments.avatar-url", ""));
        this.staffLogCommands = config.getBoolean("staff.log-commands", true);
        this.staffCommands.clear();
        for (String command : config.getStringList("staff.commands")) {
            String normalized = normalizeCommand(command);
            if (!normalized.isEmpty()) {
                this.staffCommands.add(normalized);
            }
        }
        this.punishmentsLogExpired = config.getBoolean("punishments.log-expired", false);
        this.punishmentsLogReplaced = config.getBoolean("punishments.log-replaced", false);
    }

    public boolean shouldLogStaffCommand(String commandLine) {
        return staffLogCommands && staffWebhook.isConfigured() && staffCommands.contains(normalizeCommand(commandLine));
    }

    public void logStaffCommand(CommandSender sender, String fullCommandLine) {
        if (!staffWebhook.isConfigured()) {
            return;
        }
        JsonObject embed = new JsonObject();
        embed.addProperty("title", "Staff Command");
        embed.addProperty("color", 0x3498db);
        embed.addProperty("timestamp", Instant.now().toString());
        JsonArray fields = new JsonArray();
        fields.add(field("Autor", sender == null ? "UNKNOWN" : sender.getName(), false));
        fields.add(field("Comando", fullCommandLine, false));
        embed.add("fields", fields);
        JsonObject payload = new JsonObject();
        staffWebhook.apply(payload);
        payload.add("embeds", array(embed));
        send(staffWebhook, payload);
    }

    public void logPunishmentApplied(PunishmentEntry entry) {
        if (punishmentsWebhook.isConfigured() && entry != null) {
            send(punishmentsWebhook, punishmentPayload(entry, colorFor(entry.getType()), titleFor(entry.getType()), entry.getReason()));
        }
    }

    public void logPunishmentRevoked(PunishmentEntry entry, boolean replaced) {
        if (punishmentsWebhook.isConfigured() && entry != null && (!replaced || punishmentsLogReplaced)) {
            send(punishmentsWebhook, punishmentPayload(entry, 0x2ecc71, "Punicao revogada", entry.getRemovedReason()));
        }
    }

    public void logPunishmentExpired(PunishmentEntry entry) {
        if (punishmentsWebhook.isConfigured() && entry != null && punishmentsLogExpired) {
            send(punishmentsWebhook, punishmentPayload(entry, 0x95a5a6, "Punicao expirada", entry.getReason()));
        }
    }

    public void logKick(String targetName, UUID targetUuid, String staffName, UUID staffUuid, String reason) {
        if (!punishmentsWebhook.isConfigured()) {
            return;
        }
        PunishmentEntry entry = new PunishmentEntry(0L, targetUuid, targetName, PunishmentType.BAN, reason, System.currentTimeMillis(), 0L, staffUuid, staffName, "KICK", false, 0L, null, null, null);
        send(punishmentsWebhook, punishmentPayload(entry, 0xe67e22, "Kick", reason));
    }

    private JsonObject punishmentPayload(PunishmentEntry entry, int color, String authorName, String description) {
        JsonObject embed = new JsonObject();
        embed.addProperty("color", color);
        embed.addProperty("timestamp", Instant.now().toString());
        JsonObject author = new JsonObject();
        author.addProperty("name", authorName);
        embed.add("author", author);
        embed.addProperty("title", Texts.safe(entry.getTargetName()));
        embed.addProperty("description", Texts.safe(description));
        JsonArray fields = new JsonArray();
        fields.add(field("Staff", Texts.safe(entry.getPunisherName()), true));
        if (entry.getType() != PunishmentType.WARN) {
            fields.add(field("Duracao", entry.isPermanent() ? "Permanente" : PunishmentTime.formatRemaining(entry.getExpiresAt()), true));
        }
        embed.add("fields", fields);
        JsonObject payload = new JsonObject();
        punishmentsWebhook.apply(payload);
        payload.add("embeds", array(embed));
        return payload;
    }

    private void send(Webhook webhook, JsonObject payload) {
        ProxyServer.getInstance().getScheduler().runAsync(plugin, () -> {
            try {
                postJson(webhook.url, GSON.toJson(payload));
            } catch (Exception exception) {
                plugin.getLogger().warning("[KaosCoreBungee] Falha ao enviar webhook: " + exception.getMessage());
            }
        });
    }

    private void postJson(String urlValue, String payload) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(urlValue).openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setConnectTimeout(DEFAULT_TIMEOUT_MS);
        connection.setReadTimeout(DEFAULT_TIMEOUT_MS);
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);
        connection.getOutputStream().write(bytes);
        connection.getInputStream().close();
        connection.disconnect();
    }

    private static String titleFor(PunishmentType type) {
        switch (type) {
            case BAN:
                return "Banido";
            case MUTE:
                return "Mutado";
            case WARN:
                return "Avisado";
            default:
                return "Punicao";
        }
    }

    private static int colorFor(PunishmentType type) {
        switch (type) {
            case BAN:
                return 0xe74c3c;
            case MUTE:
                return 0xf39c12;
            case WARN:
                return 0xf1c40f;
            default:
                return 0x9b59b6;
        }
    }

    private static String normalizeCommand(String input) {
        String normalized = Texts.normalize(input);
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        int space = normalized.indexOf(' ');
        return space >= 0 ? normalized.substring(0, space) : normalized;
    }

    private static JsonObject field(String name, String value, boolean inline) {
        JsonObject field = new JsonObject();
        field.addProperty("name", Texts.safe(name));
        field.addProperty("value", Texts.safe(value));
        field.addProperty("inline", inline);
        return field;
    }

    private static JsonArray array(JsonObject embed) {
        JsonArray array = new JsonArray();
        array.add(embed);
        return array;
    }

    private static final class Webhook {
        private final boolean enabled;
        private final String url;
        private final String username;
        private final String avatarUrl;

        private Webhook(boolean enabled, String url, String username, String avatarUrl) {
            this.enabled = enabled;
            this.url = url == null ? "" : url;
            this.username = username == null ? "" : username;
            this.avatarUrl = avatarUrl == null ? "" : avatarUrl;
        }

        private boolean isConfigured() {
            return enabled && !url.trim().isEmpty();
        }

        private void apply(JsonObject payload) {
            if (!username.trim().isEmpty()) {
                payload.addProperty("username", username);
            }
            if (!avatarUrl.trim().isEmpty()) {
                payload.addProperty("avatar_url", avatarUrl);
            }
        }
    }
}


