package com.knowplugins.ysubz.kaoscore.listeners.player;

import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class SendTellListener implements Listener {
    public void onSendTell(PlayerCommandPreprocessEvent event){
        String command = event.getMessage().toLowerCase();
        if(command.startsWith("/tell") || command.startsWith("/msg") || command.startsWith("/w") || command.startsWith("/whisper")){
            event.setCancelled(true);
        }
    }
}
