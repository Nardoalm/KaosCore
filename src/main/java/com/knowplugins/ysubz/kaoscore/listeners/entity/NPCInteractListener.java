package com.knowplugins.ysubz.kaoscore.listeners.entity;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.knowplugins.ysubz.kaoscore.KaosCore;
import com.knowplugins.ysubz.kaoscore.managers.Managers;
import com.knowplugins.ysubz.kaoscore.utils.menu.MenuUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

public class NPCInteractListener implements Listener {

    public static void register(KaosCore instance) {
        ProtocolLibrary.getProtocolManager().addPacketListener(
                new PacketAdapter(instance, PacketType.Play.Client.USE_ENTITY) {
                    @Override
                    public void onPacketReceiving(PacketEvent event) {
                        Player player = event.getPlayer();
                        if (player == null) return;

                        if (Managers.getNpcManager() == null) return;

                        int entityId = event.getPacket().getIntegers().readSafely(0);

                        Integer npcId = Managers.getNpcManager().getNpcId(player);

                        if (npcId != null && entityId == npcId) {

                            EnumWrappers.EntityUseAction action = event.getPacket().getEntityUseActions().readSafely(0);

                            if (action != null && (action == EnumWrappers.EntityUseAction.ATTACK || action == EnumWrappers.EntityUseAction.INTERACT)) {
                                event.setCancelled(true);

                                Bukkit.getScheduler().runTask(plugin, () -> {
                                    MenuUtils.openMenu(player);
                                });
                            }
                        }
                    }
                }
        );
    }
}
