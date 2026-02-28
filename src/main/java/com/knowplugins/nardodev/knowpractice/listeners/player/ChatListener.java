package com.knowplugins.nardodev.knowpractice.listeners.player;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;


public class ChatListener implements Listener {

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        event.setCancelled(true); // mata o chat padrão

        Player p = event.getPlayer();
        String msg = event.getMessage();

        String tag = PlaceholderAPI.setPlaceholders(
                p,
                "%leaftags_tag_prefix%"
        );

        String format = tag + p.getName() + " §8» §7" + msg;

        if(event.getPlayer().hasPermission("nardocore.member.vip")) {
            format = tag + p.getName() + " §8» §f" + msg;
        }
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.sendMessage(format);
        }
    }
}

