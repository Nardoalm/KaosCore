package com.knowplugins.ysubz.kaoscore.bukkit.listeners.entity;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.knowplugins.ysubz.kaoscore.KaosCore;
import com.knowplugins.ysubz.kaoscore.bukkit.managers.Managers;
import com.knowplugins.ysubz.kaoscore.bukkit.managers.world.NPCManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Captura clique/interacao no NPC fake (USE_ENTITY) via ProtocolLib.
 */
public final class NPCInteractListener {

    private NPCInteractListener() {
    }

    public static void register(KaosCore plugin) {
        if (plugin == null) {
            return;
        }

        ProtocolLibrary.getProtocolManager().addPacketListener(
                new PacketAdapter(plugin, PacketType.Play.Client.USE_ENTITY) {
                    @Override
                    public void onPacketReceiving(PacketEvent event) {
                        Player player = event.getPlayer();
                        if (player == null) {
                            return;
                        }

                        NPCManager manager = Managers.getNpcManager();
                        if (manager == null) {
                            return;
                        }

                        int entityId = event.getPacket().getIntegers().readSafely(0);
                        EnumWrappers.EntityUseAction action = event.getPacket().getEntityUseActions().readSafely(0);

                        if (action != null && (action == EnumWrappers.EntityUseAction.ATTACK || action == EnumWrappers.EntityUseAction.INTERACT)) {
                            if (!manager.isNpcEntity(player, entityId)) {
                                return;
                            }

                            event.setCancelled(true);
                            Bukkit.getScheduler().runTask(plugin, () -> manager.handleInteract(player, entityId));
                        }
                    }
                }
        );
    }
}
