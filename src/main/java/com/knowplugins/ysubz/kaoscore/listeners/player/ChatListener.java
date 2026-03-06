package com.knowplugins.ysubz.kaoscore.listeners.player;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;


public class ChatListener implements Listener {

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        event.setCancelled(true);

        Player p = event.getPlayer();
        String msg = event.getMessage();

        String level = PlaceholderAPI.setPlaceholders(
                p,
                "%kaos_level% "
        );

        String tag = PlaceholderAPI.setPlaceholders(
                p,
                "%kaoscore_tag_prefix%"
        );

        String format = level + tag + p.getName() + " §8» §7" + msg;

        if(event.getPlayer().hasPermission("nardocore.member.vip")) {
            format = level + tag + p.getName() + " §8» §f" + msg;
        }
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.sendMessage(format);
        }
    }
}

