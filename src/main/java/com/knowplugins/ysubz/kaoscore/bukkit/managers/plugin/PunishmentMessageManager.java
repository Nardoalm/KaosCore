package com.knowplugins.ysubz.kaoscore.bukkit.managers.plugin;

import com.knowplugins.ysubz.kaoscore.bukkit.KaosCore;
import com.knowplugins.ysubz.kaoscore.bukkit.utils.punishment.PunishmentEntry;
import com.knowplugins.ysubz.kaoscore.bukkit.utils.punishment.PunishmentTime;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PunishmentMessageManager {

    private final KaosCore plugin;
    private final File file;
    private volatile FileConfiguration config;

    public PunishmentMessageManager(KaosCore plugin) {
        this.plugin = plugin;
        this.file = plugin == null ? null : new File(plugin.getDataFolder(), "punish.yml");
        reload();
    }

    public void reload() {
        if (plugin == null || file == null) {
            return;
        }

        if (!file.exists()) {
            try {
                plugin.saveResource("punish.yml", false);
            } catch (Exception ignored) {
            }
        }

        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public String buildBanScreen(PunishmentEntry entry) {
        boolean permanent = entry == null || entry.isPermanent();
        String reason = entry == null ? "No reason provided" : safe(entry.getReason(), "No reason provided");
        String expires = permanent ? "Permanent" : PunishmentTime.formatDate(entry.getExpiresAt());
        return buildBanScreen(permanent, reason, expires);
    }

    public String buildBanScreen(boolean permanent, String reason, String expires) {
        String path = permanent ? "ban.permanent" : "ban.temporary";
        List<String> lines = getLines(path, Collections.singletonList("&cYou have been banned."));
        return joinAndColor(apply(lines, reason, expires, "", ""));
    }

    public String buildKickScreen(String reason, String staffName) {
        List<String> lines = getLines("kick.screen", Collections.singletonList("&cYou have been kicked."));
        return joinAndColor(apply(lines, safe(reason, "No reason provided"), "Permanent", "", safe(staffName, "CONSOLE")));
    }

    public String buildMuteNotice(PunishmentEntry entry) {
        List<String> lines = getLines("mute.notice", Collections.singletonList("&cYou have been muted."));
        String reason = entry == null ? "No reason provided" : safe(entry.getReason(), "No reason provided");
        String duration = entry == null ? "Unknown" : PunishmentTime.formatRemaining(entry.getExpiresAt());
        return joinAndColor(apply(lines, reason, "Permanent", duration, ""));
    }

    public List<String> buildMuteChatLines(PunishmentEntry entry) {
        List<String> lines = getLines("mute.chat", Collections.singletonList("&cYou are muted."));
        String reason = entry == null ? "No reason provided" : safe(entry.getReason(), "No reason provided");
        String duration = entry == null ? "Unknown" : PunishmentTime.formatRemaining(entry.getExpiresAt());
        List<String> out = apply(lines, reason, "Permanent", duration, "");
        List<String> colored = new ArrayList<>();
        for (String raw : out) {
            colored.add(color(raw));
        }
        return colored;
    }

    private List<String> apply(List<String> lines, String reason, String expires, String duration, String staff) {
        if (lines == null || lines.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> out = new ArrayList<>(lines.size());
        for (String raw : lines) {
            String line = safe(raw, "");
            line = line.replace("{reason}", safe(reason, ""));
            line = line.replace("{expires}", safe(expires, ""));
            line = line.replace("{duration}", safe(duration, ""));
            line = line.replace("{staff}", safe(staff, ""));
            out.add(line);
        }
        return out;
    }

    private List<String> getLines(String path, List<String> fallback) {
        FileConfiguration cfg = this.config;
        if (cfg == null || path == null || path.trim().isEmpty()) {
            return fallback == null ? Collections.emptyList() : fallback;
        }

        List<String> lines = cfg.getStringList(path);
        if (lines != null && !lines.isEmpty()) {
            return lines;
        }

        String single = cfg.getString(path);
        if (single != null && !single.trim().isEmpty()) {
            return Collections.singletonList(single);
        }

        return fallback == null ? Collections.emptyList() : fallback;
    }

    private String joinAndColor(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                sb.append('\n');
            }
            sb.append(color(lines.get(i)));
        }
        return sb.toString();
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }

    private String safe(String value, String fallback) {
        if (value == null) {
            return fallback == null ? "" : fallback;
        }
        String v = value.trim();
        return v.isEmpty() ? (fallback == null ? "" : fallback) : v;
    }
}