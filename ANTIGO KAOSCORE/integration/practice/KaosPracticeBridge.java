package com.knowplugins.ysubz.kaoscore.integration.practice;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class KaosPracticeBridge {

    private static final String API_CLASS_NAME = "us.alleypvp.practice.api.AlleyPracticeAPI";
    private static final List<String> MATCH_COLOR_POOL = Arrays.asList(
            "§c", "§9", "§a", "§e", "§b", "§d", "§6", "§5", "§3", "§2", "§1", "§4"
    );

    public boolean isAvailable() {
        return resolveApi() != null;
    }

    public boolean isInLobby(Player player) {
        Object api = resolveApi();
        return api != null && invokeBoolean(api, "isInLobby", player);
    }

    public boolean isQueueing(Player player) {
        Object api = resolveApi();
        return api != null && invokeBoolean(api, "isQueueing", player);
    }

    public PracticeState getState(Player player) {
        if (player == null) {
            return PracticeState.empty();
        }

        Object api = resolveApi();
        if (api == null) {
            return PracticeState.empty();
        }

        boolean inMatch = invokeBoolean(api, "isInMatch", player);
        boolean spectating = invokeBoolean(api, "isSpectating", player);
        boolean inEvent = invokeBoolean(api, "isInEvent", player); // Adicionado para consistência
        boolean overrideDefaultNametag = invokeBoolean(api, "shouldOverrideDefaultNametag", player);
        String matchId = invokeString(api, "getMatchId", player);
        String teamId = invokeString(api, "getTeamId", player);
        String teamColor = normalizeColor(invokeString(api, "getTeamColor", player));
        Object context = invoke(api, "getNametagContext", player);

        String prefix = normalizeText(invokeContextString(context, "getPrefix"));
        String suffix = normalizeText(invokeContextString(context, "getSuffix"));
        String nameColor = normalizeColor(invokeContextString(context, "getNameColor"));
        int priority = invokeContextInt(context, "getPriority");

        if (spectating) {
            // CORREÇÃO: Adicionado o parâmetro 'false' para inEvent (Total 10 parâmetros)
            return new PracticeState(false, false, false, true, "", "§8", "", 9500, teamId, matchId);
        }

        if (overrideDefaultNametag || inMatch || inEvent) {
            String resolvedColor = !nameColor.isEmpty() ? nameColor : teamColor;
            if (resolvedColor.isEmpty()) {
                resolvedColor = fallbackMatchColor(teamId.isEmpty() ? player.getUniqueId().toString() : teamId);
            }
            int resolvedPriority = priority > 0 ? priority : 100;
            // CORREÇÃO: Adicionado o parâmetro inEvent (Total 10 parâmetros)
            return new PracticeState(true, inMatch, inEvent, false, prefix, resolvedColor, suffix, resolvedPriority, teamId, matchId);
        }

        return PracticeState.empty();
    }

    private Object resolveApi() {
        try {
            Class<?> apiClass = Class.forName(API_CLASS_NAME);
            RegisteredServiceProvider<?> registration = Bukkit.getServicesManager().getRegistration(apiClass);
            if (registration != null && registration.getProvider() != null) {
                return registration.getProvider();
            }
        } catch (Throwable ignored) {
        }

        Plugin plugin = Bukkit.getPluginManager().getPlugin("KaosPractice");
        if (plugin == null) {
            return null;
        }

        try {
            Method getApi = plugin.getClass().getMethod("getAPI");
            return getApi.invoke(plugin);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private Object invoke(Object target, String methodName, Player player) {
        if (target == null || methodName == null || player == null) {
            return null;
        }

        try {
            Method method = target.getClass().getMethod(methodName, Player.class);
            return method.invoke(target, player);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private boolean invokeBoolean(Object target, String methodName, Player player) {
        Object value = invoke(target, methodName, player);
        return value instanceof Boolean && (Boolean) value;
    }

    private String invokeString(Object target, String methodName, Player player) {
        Object value = invoke(target, methodName, player);
        return value == null ? "" : String.valueOf(value);
    }

    private String invokeContextString(Object context, String methodName) {
        if (context == null || methodName == null) {
            return "";
        }

        try {
            Method method = context.getClass().getMethod(methodName);
            Object value = method.invoke(context);
            return value == null ? "" : String.valueOf(value);
        } catch (Throwable ignored) {
            return "";
        }
    }

    private int invokeContextInt(Object context, String methodName) {
        if (context == null || methodName == null) {
            return 0;
        }

        try {
            Method method = context.getClass().getMethod(methodName);
            Object value = method.invoke(context);
            return value instanceof Number ? ((Number) value).intValue() : 0;
        } catch (Throwable ignored) {
            return 0;
        }
    }

    private String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', value);
    }

    private String normalizeColor(String value) {
        String normalized = normalizeText(value);
        if (normalized.length() < 2) {
            return "";
        }

        String color = normalized.substring(0, 2);
        String lowerColor = color.toLowerCase(Locale.ROOT);
        if (!lowerColor.matches("§[0-9a-f]")) {
            return "";
        }

        if ("§7".equals(lowerColor) || "§8".equals(lowerColor)) {
            return "";
        }

        return lowerColor;
    }

    private String fallbackMatchColor(String seed) {
        String safeSeed = seed == null ? "" : seed;
        int index = Math.abs(safeSeed.hashCode());
        return MATCH_COLOR_POOL.get(index % MATCH_COLOR_POOL.size());
    }

    public static final class PracticeState {
        private final boolean overrideDefaultNametag;
        private final boolean inMatch;
        private final boolean inEvent;
        private final boolean spectating;
        private final String prefix;
        private final String nameColor;
        private final String suffix;
        private final int priority;
        private final String teamId;
        private final String matchId;

        public PracticeState(boolean overrideDefaultNametag, boolean inMatch, boolean inEvent, boolean spectating, String prefix, String nameColor, String suffix, int priority, String teamId, String matchId) {
            this.overrideDefaultNametag = overrideDefaultNametag;
            this.inMatch = inMatch;
            this.inEvent = inEvent;
            this.spectating = spectating;
            this.prefix = prefix == null ? "" : prefix;
            this.nameColor = nameColor == null ? "" : nameColor;
            this.suffix = suffix == null ? "" : suffix;
            this.priority = priority;
            this.teamId = teamId == null ? "" : teamId;
            this.matchId = matchId == null ? "" : matchId;
        }

        public static PracticeState empty() {
            // CORREÇÃO: Adicionado o parâmetro false para inEvent (Total 10 parâmetros)
            return new PracticeState(false, false, false, false, "", "", "", 0, "", "");
        }

        public boolean isOverrideDefaultNametag() {
            return overrideDefaultNametag;
        }

        public boolean isInMatch() {
            return inMatch;
        }

        public boolean isInEvent() {
            return inEvent;
        }

        public boolean isSpectating() {
            return spectating;
        }

        public String getPrefix() {
            return prefix;
        }

        public String getNameColor() {
            return nameColor;
        }

        public String getSuffix() {
            return suffix;
        }

        public int getPriority() {
            return priority;
        }

        public String getTeamId() {
            return teamId;
        }

        public String getMatchId() {
            return matchId;
        }
    }
}