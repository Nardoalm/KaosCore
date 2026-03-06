package com.knowplugins.ysubz.kaoscore.utils.message;

import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import net.minecraft.server.v1_8_R3.IChatBaseComponent;
import net.minecraft.server.v1_8_R3.PacketPlayOutChat;
import org.bukkit.entity.Player;

public class sendRawJson {
    public static void sendDiscordLink(Player player, String link) {
        String json = "{\"text\":\"§9§lDISCORD ➔ §eClique §6§lAQUI §epara entrar no nosso grupo!\",\"clickEvent\":{\"action\":\"open_url\",\"value\":\"" + link + "\"},\"hoverEvent\":{\"action\":\"show_text\",\"value\":\"§bLink: " + link + "\"}}";

        IChatBaseComponent comp = IChatBaseComponent.ChatSerializer.a(json);
        PacketPlayOutChat packet = new PacketPlayOutChat(comp);

        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);
    }
}