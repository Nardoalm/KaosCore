package com.knowplugins.ysubz.kaoscore.listeners.world;

import com.knowplugins.ysubz.kaoscore.KaosCore;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.event.Listener;

public class SoundListener implements Listener {
    public void setupSoundBlocker() {
        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(KaosCore.getInstance(), ListenerPriority.NORMAL, PacketType.Play.Server.NAMED_SOUND_EFFECT) {
            @Override
            public void onPacketSending(PacketEvent event) {
                String soundName = event.getPacket().getStrings().read(0);

                if (soundName.contains("ambient.cave") || soundName.contains("cave")) {
                    event.setCancelled(true);
                }
            }
        });
    }
}
