package com.knowplugins.ysubz.kaoscore.bukkit.util.punishment;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PunishmentTime {

    private static final Pattern PART_PATTERN = Pattern.compile("(\\d+)([smhdwy])", Pattern.CASE_INSENSITIVE);

    private PunishmentTime() {
    }

    public static long parseDuration(String raw) {
        if (raw == null) {
            return Long.MIN_VALUE;
        }

        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return Long.MIN_VALUE;
        }

        if (normalized.equals("perm") || normalized.equals("perma") || normalized.equals("permanente") || normalized.equals("permanent")) {
            return 0L;
        }

        Matcher matcher = PART_PATTERN.matcher(normalized);
        long total = 0L;
        int index = 0;

        while (matcher.find()) {
            if (matcher.start() != index) {
                return Long.MIN_VALUE;
            }

            long amount = Long.parseLong(matcher.group(1));
            char unit = Character.toLowerCase(matcher.group(2).charAt(0));
            total += toMillis(amount, unit);
            index = matcher.end();
        }

        if (index != normalized.length() || total <= 0L) {
            return Long.MIN_VALUE;
        }

        return total;
    }

    private static long toMillis(long amount, char unit) {
        switch (unit) {
            case 's':
                return amount * 1000L;
            case 'm':
                return amount * 60_000L;
            case 'h':
                return amount * 3_600_000L;
            case 'd':
                return amount * 86_400_000L;
            case 'w':
                return amount * 604_800_000L;
            case 'y':
                return amount * 31_536_000_000L;
            default:
                return 0L;
        }
    }

    public static String formatDate(long timestamp) {
        if (timestamp <= 0L) {
            return "Never";
        }
        return new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date(timestamp));
    }

    public static String formatRemaining(long expiresAt) {
        if (expiresAt <= 0L) {
            return "Permanent";
        }

        long remaining = expiresAt - System.currentTimeMillis();
        if (remaining <= 0L) {
            return "Expired";
        }

        long days = remaining / 86_400_000L;
        remaining %= 86_400_000L;
        long hours = remaining / 3_600_000L;
        remaining %= 3_600_000L;
        long minutes = remaining / 60_000L;
        remaining %= 60_000L;
        long seconds = remaining / 1000L;

        StringBuilder builder = new StringBuilder();
        append(builder, days, "d");
        append(builder, hours, "h");
        append(builder, minutes, "m");
        if (builder.length() == 0) {
            append(builder, seconds, "s");
        }
        return builder.toString().trim();
    }

    private static void append(StringBuilder builder, long value, String suffix) {
        if (value <= 0L) {
            return;
        }
        builder.append(value).append(suffix).append(' ');
    }
}