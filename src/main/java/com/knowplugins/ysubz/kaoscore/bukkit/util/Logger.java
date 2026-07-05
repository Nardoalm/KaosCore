package com.knowplugins.ysubz.kaoscore.bukkit.util;

import org.bukkit.Bukkit;

public class Logger {
    public static void severe(String message) {
        Bukkit.getConsoleSender().sendMessage("§c[§4KaosCore§c]§7: " + message);
    }

    public static void warning(String message) {
        Bukkit.getConsoleSender().sendMessage( "§6[§eKaosCore§6]§7: " + message);
    }

    public static void info(String message) {
        Bukkit.getConsoleSender().sendMessage("§b[§3KaosCore§b]§7: " + message);
    }
}
