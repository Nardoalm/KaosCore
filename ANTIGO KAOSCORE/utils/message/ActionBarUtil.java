package com.knowplugins.ysubz.kaoscore.bukkit.utils.message;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public final class ActionBarUtil {

    private static final String VERSION;

    static {
        String version;
        try {
            version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
        } catch (Exception ignored) {
            version = "v1_8_R3";
        }
        VERSION = version;
    }

    private ActionBarUtil() {
    }

    public static void sendActionBar(Player player, String message) {
        if (player == null || message == null) {
            return;
        }

        try {
            Class<?> craftPlayerClass = Class.forName("org.bukkit.craftbukkit." + VERSION + ".entity.CraftPlayer");
            Class<?> packetPlayOutChatClass = Class.forName("net.minecraft.server." + VERSION + ".PacketPlayOutChat");
            Class<?> iChatBaseComponentClass = Class.forName("net.minecraft.server." + VERSION + ".IChatBaseComponent");
            Class<?> chatComponentTextClass = Class.forName("net.minecraft.server." + VERSION + ".ChatComponentText");

            Object craftPlayer = craftPlayerClass.cast(player);
            Object handle = craftPlayerClass.getMethod("getHandle").invoke(craftPlayer);
            Object playerConnection = handle.getClass().getField("playerConnection").get(handle);

            Object chatComponent = chatComponentTextClass.getConstructor(String.class).newInstance(message);
            Constructor<?> packetConstructor = packetPlayOutChatClass.getConstructor(iChatBaseComponentClass, byte.class);
            Object packet = packetConstructor.newInstance(chatComponent, (byte) 2);

            Method sendPacket = playerConnection.getClass().getMethod("sendPacket", Class.forName("net.minecraft.server." + VERSION + ".Packet"));
            sendPacket.invoke(playerConnection, packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
