package com.knowplugins.ysubz.kaoscore.bungee;

import net.md_5.bungee.config.Configuration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BungeePunishmentMessageManager {

    private Configuration config;

    public BungeePunishmentMessageManager(Configuration config) {
        this.config = config;
    }

    public void reload(Configuration config) {
        this.config = config;
    }

    public String buildBanScreen(PunishmentEntry entry) {
        boolean permanent = entry == null || entry.isPermanent();
        String reason = entry == null ? "Sem motivo informado" : Texts.safe(entry.getReason());
        String expires = permanent ? "Permanente" : PunishmentTime.formatDate(entry.getExpiresAt());
        return buildBanScreen(permanent, reason, expires);
    }

    public String buildBanScreen(boolean permanent, String reason, String expires) {
        return join(apply(getLines(permanent ? "ban.permanent" : "ban.temporary"), reason, expires, "", ""));
    }

    public String buildKickScreen(String reason, String staffName) {
        return join(apply(getLines("kick.screen"), reason, "Permanente", "", staffName));
    }

    public String buildMuteNotice(PunishmentEntry entry) {
        String reason = entry == null ? "Sem motivo informado" : Texts.safe(entry.getReason());
        String duration = entry == null ? "Desconhecida" : PunishmentTime.formatRemaining(entry.getExpiresAt());
        return join(apply(getLines("mute.notice"), reason, "Permanente", duration, ""));
    }

    public List<String> buildMuteChatLines(PunishmentEntry entry) {
        String reason = entry == null ? "Sem motivo informado" : Texts.safe(entry.getReason());
        String duration = entry == null ? "Desconhecida" : PunishmentTime.formatRemaining(entry.getExpiresAt());
        return apply(getLines("mute.chat"), reason, "Permanente", duration, "");
    }

    private List<String> getLines(String path) {
        List<String> lines = config.getStringList(path);
        if (lines == null || lines.isEmpty()) {
            String single = config.getString(path, "");
            return single.trim().isEmpty() ? Collections.singletonList("&cMensagem indisponivel.") : Collections.singletonList(single);
        }
        return lines;
    }

    private List<String> apply(List<String> lines, String reason, String expires, String duration, String staff) {
        List<String> result = new ArrayList<>();
        for (String raw : lines) {
            result.add(Texts.color(Texts.safe(raw)
                    .replace("{reason}", Texts.safe(reason))
                    .replace("{expires}", Texts.safe(expires))
                    .replace("{duration}", Texts.safe(duration))
                    .replace("{staff}", Texts.safe(staff))));
        }
        return result;
    }

    private String join(List<String> lines) {
        return String.join("\n", lines);
    }
}


