package com.knowplugins.ysubz.kaoscore.bungee;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;

import java.util.List;
import java.util.UUID;

public final class Texts {

    private Texts() {
    }

    public static String color(String text) {
        return com.knowplugins.ysubz.kaoscore.bungee.util.Texts.color(text);
    }

    public static String stripColors(String text) {
        return com.knowplugins.ysubz.kaoscore.bungee.util.Texts.stripColors(text);
    }

    public static String safe(String text) {
        return com.knowplugins.ysubz.kaoscore.bungee.util.Texts.safe(text);
    }

    public static String normalize(String value) {
        return com.knowplugins.ysubz.kaoscore.bungee.util.Texts.normalize(value);
    }

    public static String join(String[] args, int start) {
        return com.knowplugins.ysubz.kaoscore.bungee.util.Texts.join(args, start);
    }

    public static BaseComponent[] components(String message) {
        return com.knowplugins.ysubz.kaoscore.bungee.util.Texts.components(message);
    }

    public static void send(CommandSender sender, String message) {
        com.knowplugins.ysubz.kaoscore.bungee.util.Texts.send(sender, message);
    }

    public static void broadcast(String message) {
        com.knowplugins.ysubz.kaoscore.bungee.util.Texts.broadcast(message);
    }

    public static UUID offlineUuid(String name) {
        return com.knowplugins.ysubz.kaoscore.bungee.util.Texts.offlineUuid(name);
    }
}
