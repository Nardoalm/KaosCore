package com.knowplugins.ysubz.kaoscore.bukkit.manager.player;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;

public class TabListManager {

    /**
     * Aplica o Header e Footer conforme seu código original.
     */
    public static void setTabList(Player player, String header, String footer) {
        if (header == null) header = "";
        if (footer == null) footer = "";

        header = ChatColor.translateAlternateColorCodes('&', header);
        footer = ChatColor.translateAlternateColorCodes('&', footer);

        try {
            String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
            Class<?> packetClass = Class.forName("net.minecraft.server." + version + ".PacketPlayOutPlayerListHeaderFooter");
            Object packet = packetClass.getConstructor().newInstance();

            Class<?> chatSerializerClass = Class.forName("net.minecraft.server." + version + ".IChatBaseComponent$ChatSerializer");
            Object tabHeader = chatSerializerClass.getMethod("a", String.class).invoke(null, "{\"text\":\"" + escapeJson(header) + "\"}");
            Object tabFooter = chatSerializerClass.getMethod("a", String.class).invoke(null, "{\"text\":\"" + escapeJson(footer) + "\"}");

            Field a = packetClass.getDeclaredField("a");
            a.setAccessible(true);
            a.set(packet, tabHeader);

            Field b = packetClass.getDeclaredField("b");
            b.setAccessible(true);
            b.set(packet, tabFooter);

            Object craftPlayer = Class.forName("org.bukkit.craftbukkit." + version + ".entity.CraftPlayer").cast(player);
            Object handle = craftPlayer.getClass().getMethod("getHandle").invoke(craftPlayer);
            Object playerConnection = handle.getClass().getField("playerConnection").get(handle);

            playerConnection.getClass().getMethod("sendPacket", Class.forName("net.minecraft.server." + version + ".Packet")).invoke(playerConnection, packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\r", "").replace("\n", "\\n");
    }
}