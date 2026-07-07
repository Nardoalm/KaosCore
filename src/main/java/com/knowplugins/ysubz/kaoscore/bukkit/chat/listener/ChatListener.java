package com.knowplugins.ysubz.kaoscore.bukkit.chat.listener;

import com.knowplugins.ysubz.kaoscore.bukkit.chat.ChatManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class ChatListener implements Listener {

    private final ChatManager chatManager = new ChatManager();

    @EventHandler(ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        event.setCancelled(true);
        chatManager.processChat(event);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (event.getPlayer() != null) {
            chatManager.cleanup(event.getPlayer().getUniqueId());
        }
    }
}