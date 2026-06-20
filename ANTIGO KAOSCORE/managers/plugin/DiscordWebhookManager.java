package com.knowplugins.ysubz.kaoscore.managers.plugin;

import com.knowplugins.ysubz.kaoscore.KaosCore;
import com.knowplugins.ysubz.kaoscore.managers.Managers;
import com.knowplugins.ysubz.kaoscore.managers.player.DisguiseManager;
import com.knowplugins.ysubz.kaoscore.utils.disguise.DisguiseApiService;
import com.knowplugins.ysubz.kaoscore.utils.punishment.PunishmentEntry;
import com.knowplugins.ysubz.kaoscore.utils.punishment.PunishmentTime;
import com.knowplugins.ysubz.kaoscore.utils.punishment.PunishmentType;
import com.knowplugins.ysubz.kaoscore.utils.discord.DiscordWebhookClient;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class DiscordWebhookManager {

    private static final Gson GSON = new Gson();
    private static final int DEFAULT_TIMEOUT_MS = 7000;

    private final KaosCore plugin;

    private Webhook staffWebhook;
    private Webhook punishmentsWebhook;

    private boolean staffLogCommands;
    private Set<String> staffCommands;

    private boolean punishmentsLogExpired;
    private boolean punishmentsLogReplaced;

    public DiscordWebhookManager(KaosCore plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        if (plugin == null) {
            return;
        }

        File file = new File(plugin.getDataFolder(), "webhooks.yml");
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        this.staffWebhook = readWebhook(cfg, "staff", "KaosCore Staff");
        this.punishmentsWebhook = readWebhook(cfg, "punishments", "KaosCore Punishments");

        this.staffLogCommands = cfg.getBoolean("staff.log-commands", true);
        this.staffCommands = new HashSet<>();
        List<String> commands = cfg.getStringList("staff.commands");
        if (commands != null) {
            for (String cmd : commands) {
                String normalized = normalizeCommand(cmd);
                if (!normalized.isEmpty()) {
                    staffCommands.add(normalized);
                }
            }
        }

        this.punishmentsLogExpired = cfg.getBoolean("punishments.log-expired", false);
        this.punishmentsLogReplaced = cfg.getBoolean("punishments.log-replaced", false);
    }

    public boolean shouldLogStaffCommand(String commandLabel) {
        if (!staffLogCommands || staffWebhook == null || !staffWebhook.isConfigured()) {
            return false;
        }

        String normalized = normalizeCommand(commandLabel);
        return !normalized.isEmpty() && staffCommands.contains(normalized);
    }

    public void logStaffCommand(CommandSender sender, String fullCommandLine) {
        if (staffWebhook == null || !staffWebhook.isConfigured()) {
            return;
        }

        String actorName = sender == null ? "UNKNOWN" : safe(sender.getName());
        String actorUuid = "";
        if (sender instanceof Player) {
            Player player = (Player) sender;
            UUID id = player.getUniqueId();
            actorUuid = id == null ? "" : id.toString();

            String original = DisguiseApiService.getOriginalNick(player);
            if (original != null && !original.trim().isEmpty()) {
                actorName = original;
            }
        }

        JsonObject embed = new JsonObject();
        embed.addProperty("title", "Staff Command");
        embed.addProperty("color", 0x3498db);
        embed.addProperty("timestamp", Instant.now().toString());

        JsonArray fields = new JsonArray();
        fields.add(field("Autor", actorName + (actorUuid.isEmpty() ? "" : " (" + actorUuid + ")"), false));
        fields.add(field("Comando", safe(fullCommandLine), false));
        embed.add("fields", fields);

        JsonObject payload = new JsonObject();
        staffWebhook.applyTo(payload);
        payload.add("embeds", array(embed));

        send(staffWebhook, payload);
    }

    public void logPunishmentApplied(PunishmentEntry entry) {
        if (punishmentsWebhook == null || !punishmentsWebhook.isConfigured() || entry == null) {
            return;
        }

        String cleanTargetName = resolveOriginalName(entry.getTargetUuid(), entry.getTargetName());

        String actionText = "⚠️ PUNIÇÃO";
        switch (entry.getType()) {
            case BAN: actionText = "🚫 BANIDO"; break;
            case MUTE: actionText = "🔇 MUTADO"; break;
            case WARN: actionText = "⚠️ AVISADO"; break;
        }

        JsonObject embed = new JsonObject();
        embed.addProperty("color", colorFor(entry.getType()));
        embed.addProperty("timestamp", Instant.now().toString());

        // Topo: "🚫 BANIDO"
        JsonObject author = new JsonObject();
        author.addProperty("name", actionText);
        embed.add("author", author);

        // Título Maior: Nick do Jogador
        embed.addProperty("title", cleanTargetName);

        // Descrição: Motivo da punição destacado
        embed.addProperty("description", "📢 **Motivo**\n" + stripMinecraftColors(entry.getReason()));

        // Thumbnail (Rosto no canto direito igual a imagem)
        JsonObject thumbnail = new JsonObject();
        thumbnail.addProperty("url", "https://mc-heads.net/avatar/" + cleanTargetName + "/128");
        embed.add("thumbnail", thumbnail);

        // Informações adicionais do Staff separadas de forma discreta
        JsonArray fields = new JsonArray();
        fields.add(field("Staff", formatIdentityOriginal(entry.getPunisherName(), entry.getPunisherUuid()), true));
        if (entry.getType() != PunishmentType.WARN) {
            fields.add(field("Duração", entry.isPermanent() ? "Permanente" : PunishmentTime.formatRemaining(entry.getExpiresAt()), true));
        }
        embed.add("fields", fields);

        JsonObject payload = new JsonObject();
        punishmentsWebhook.applyTo(payload);
        payload.add("embeds", array(embed));

        send(punishmentsWebhook, payload);
    }

    public void logPunishmentRevoked(PunishmentEntry entry, boolean replaced) {
        if (punishmentsWebhook == null || !punishmentsWebhook.isConfigured() || entry == null) {
            return;
        }

        if (replaced && !punishmentsLogReplaced) {
            return;
        }

        String cleanTargetName = resolveOriginalName(entry.getTargetUuid(), entry.getTargetName());

        String actionText = "✅ PUNIÇÃO REVOGADA";
        if (entry.getType() == PunishmentType.BAN) actionText = "✅ DESBANIDO";
        if (entry.getType() == PunishmentType.MUTE) actionText = "🔊 DESMUTADO";

        JsonObject embed = new JsonObject();
        embed.addProperty("color", 0x2ecc71); // Verde
        embed.addProperty("timestamp", Instant.now().toString());

        JsonObject author = new JsonObject();
        author.addProperty("name", actionText);
        embed.add("author", author);

        embed.addProperty("title", cleanTargetName);
        embed.addProperty("description", "📢 **Motivo da Remoção**\n" + stripMinecraftColors(entry.getRemovedReason()));

        JsonObject thumbnail = new JsonObject();
        thumbnail.addProperty("url", "https://mc-heads.net/avatar/" + cleanTargetName + "/128");
        embed.add("thumbnail", thumbnail);

        JsonArray fields = new JsonArray();
        fields.add(field("Motivo Original", stripMinecraftColors(entry.getReason()), true));
        embed.add("fields", fields);

        JsonObject payload = new JsonObject();
        punishmentsWebhook.applyTo(payload);
        payload.add("embeds", array(embed));

        send(punishmentsWebhook, payload);
    }

    public void logPunishmentExpired(PunishmentEntry entry) {
        if (punishmentsWebhook == null || !punishmentsWebhook.isConfigured() || entry == null || !punishmentsLogExpired) {
            return;
        }

        String cleanTargetName = resolveOriginalName(entry.getTargetUuid(), entry.getTargetName());

        JsonObject embed = new JsonObject();
        embed.addProperty("color", 0x95a5a6); // Cinza
        embed.addProperty("timestamp", Instant.now().toString());

        JsonObject author = new JsonObject();
        author.addProperty("name", "⏰ PUNIÇÃO EXPIRADA");
        embed.add("author", author);

        embed.addProperty("title", cleanTargetName);
        embed.addProperty("description", "📢 **Motivo Original**\n" + stripMinecraftColors(entry.getReason()));

        JsonObject thumbnail = new JsonObject();
        thumbnail.addProperty("url", "https://mc-heads.net/avatar/" + cleanTargetName + "/128");
        embed.add("thumbnail", thumbnail);

        JsonObject payload = new JsonObject();
        punishmentsWebhook.applyTo(payload);
        payload.add("embeds", array(embed));

        send(punishmentsWebhook, payload);
    }

    public void logKick(String targetName, UUID targetUuid, String staffName, UUID staffUuid, String reason) {
        if (punishmentsWebhook == null || !punishmentsWebhook.isConfigured()) {
            return;
        }

        String cleanTargetName = resolveOriginalName(targetUuid, targetName);

        JsonObject embed = new JsonObject();
        embed.addProperty("color", 0xe67e22); // Laranja
        embed.addProperty("timestamp", Instant.now().toString());

        JsonObject author = new JsonObject();
        author.addProperty("name", "🚪 KICKADO");
        embed.add("author", author);

        embed.addProperty("title", cleanTargetName);
        embed.addProperty("description", "📢 **Motivo**\n" + stripMinecraftColors(reason));

        JsonObject thumbnail = new JsonObject();
        thumbnail.addProperty("url", "https://mc-heads.net/avatar/" + cleanTargetName + "/128");
        embed.add("thumbnail", thumbnail);

        JsonArray fields = new JsonArray();
        embed.add("fields", fields);

        JsonObject payload = new JsonObject();
        punishmentsWebhook.applyTo(payload);
        payload.add("embeds", array(embed));

        send(punishmentsWebhook, payload);
    }

    private void send(Webhook webhook, JsonObject payload) {
        if (plugin == null || webhook == null || !webhook.isConfigured() || payload == null) {
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                DiscordWebhookClient.postJson(webhook.url, GSON.toJson(payload), DEFAULT_TIMEOUT_MS);
            } catch (Exception e) {
                plugin.getLogger().warning("[KaosCore] Falha ao enviar webhook (" + webhook.name + "): " + e.getMessage());
            }
        });
    }

    private Webhook readWebhook(FileConfiguration cfg, String path, String defaultUsername) {
        boolean enabled = cfg.getBoolean(path + ".enabled", false);
        String url = safe(cfg.getString(path + ".url"));
        String username = safe(cfg.getString(path + ".username"));
        if (username.trim().isEmpty()) {
            username = defaultUsername;
        }
        String avatarUrl = safe(cfg.getString(path + ".avatar-url"));
        return new Webhook(path, enabled, url, username, avatarUrl);
    }

    private static JsonObject field(String name, String value, boolean inline) {
        JsonObject field = new JsonObject();
        field.addProperty("name", safe(name));
        field.addProperty("value", safe(value));
        field.addProperty("inline", inline);
        return field;
    }

    private static JsonArray array(JsonObject... objects) {
        JsonArray array = new JsonArray();
        if (objects != null) {
            for (JsonObject obj : objects) {
                if (obj != null) {
                    array.add(obj);
                }
            }
        }
        return array;
    }

    private static int colorFor(PunishmentType type) {
        if (type == null) {
            return 0x9b59b6;
        }
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

    private static String typeName(PunishmentType type) {
        return type == null ? "UNKNOWN" : type.name();
    }

    private static String formatIdentity(String name, UUID uuid) {
        String n = safe(name);
        if (uuid == null) {
            return n.isEmpty() ? "UNKNOWN" : n;
        }
        if (n.isEmpty()) {
            return uuid.toString();
        }
        return n + " (" + uuid.toString() + ")";
    }

    private String formatIdentityOriginal(String fallbackName, UUID uuid) {
        String name = resolveOriginalName(uuid, fallbackName);
        return formatIdentity(name, uuid);
    }

    private String resolveOriginalName(UUID uuid, String fallbackName) {
        String fallback = safe(fallbackName);
        if (uuid == null) {
            return fallback;
        }

        try {
            Player online = Bukkit.getPlayer(uuid);
            if (online != null) {
                String original = DisguiseApiService.getOriginalNick(online);
                if (original != null && !original.trim().isEmpty()) {
                    return original;
                }
                return online.getName();
            }
        } catch (Exception ignored) {
        }

        try {
            DisguiseManager disguiseManager = Managers.getDisguiseManager();
            if (disguiseManager != null) {
                String original = disguiseManager.getOriginalName(uuid);
                if (original != null && !original.trim().isEmpty()) {
                    return original;
                }
            }
        } catch (Exception ignored) {
        }

        return fallback;
    }

    private static String normalizeCommand(String commandLabel) {
        if (commandLabel == null) {
            return "";
        }
        String normalized = commandLabel.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        int space = normalized.indexOf(' ');
        if (space >= 0) {
            normalized = normalized.substring(0, space);
        }
        return normalized.trim();
    }

    private static String stripMinecraftColors(String value) {
        String s = safe(value);
        return s.replaceAll("(?i)§[0-9A-FK-OR]", "");
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static final class Webhook {
        private final String name;
        private final boolean enabled;
        private final String url;
        private final String username;
        private final String avatarUrl;

        private Webhook(String name, boolean enabled, String url, String username, String avatarUrl) {
            this.name = name;
            this.enabled = enabled;
            this.url = url == null ? "" : url;
            this.username = username == null ? "" : username;
            this.avatarUrl = avatarUrl == null ? "" : avatarUrl;
        }

        private boolean isConfigured() {
            return enabled && url != null && !url.trim().isEmpty();
        }

        private void applyTo(JsonObject payload) {
            if (payload == null) {
                return;
            }

            if (!safe(username).trim().isEmpty()) {
                payload.addProperty("username", username);
            }

            if (!safe(avatarUrl).trim().isEmpty()) {
                payload.addProperty("avatar_url", avatarUrl);
            }
        }
    }
}