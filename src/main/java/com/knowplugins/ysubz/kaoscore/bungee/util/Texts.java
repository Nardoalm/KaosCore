package com.knowplugins.ysubz.kaoscore.bungee;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;

public final class Texts {

    private Texts() {
    }

    public static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', safe(text));
    }

    public static String stripColors(String text) {
        return ChatColor.stripColor(safe(text));
    }

    public static String safe(String text) {
        return text == null ? "" : text;
    }

    public static String normalize(String value) {
        return safe(value).trim().toLowerCase(Locale.ROOT);
    }

    public static String join(String[] args, int start) {
        if (args == null || start < 0 || start >= args.length) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = start; i < args.length; i++) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(safe(args[i]));
        }
        return builder.toString().trim();
    }

    public static BaseComponent[] components(String message) {
        return TextComponent.fromLegacyText(color(message));
    }

    public static void send(CommandSender sender, String message) {
        if (sender == null || message == null) {
            return;
        }
        String[] parts = color(message).split("\n");
        for (String part : parts) {
            sender.sendMessage(TextComponent.fromLegacyText(part));
        }
    }

    public static void broadcast(String message) {
        if (message == null) {
            return;
        }
        String[] parts = color(message).split("\n");
        for (String part : parts) {
            ProxyServer.getInstance().broadcast(TextComponent.fromLegacyText(part));
        }
    }

    public static UUID offlineUuid(String name) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + safe(name)).getBytes(StandardCharsets.UTF_8));
    }
}


