package com.knowplugins.ysubz.kaoscore.bungee;

import com.knowplugins.ysubz.kaoscore.integration.proxy.ProxySyncChannel;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

public final class BungeeSyncMessenger {

    private BungeeSyncMessenger() {
    }

    public static boolean send(ProxiedPlayer player, String action, String... payload) {
        if (player == null || player.getUniqueId() == null) {
            return false;
        }

        Server server = player.getServer();
        if (server == null || server.getInfo() == null) {
            return false;
        }

        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream output = new DataOutputStream(bytes);
            output.writeUTF(action == null ? "" : action);
            output.writeUTF(player.getUniqueId().toString());
            if (payload != null) {
                for (String value : payload) {
                    output.writeUTF(value == null ? "" : value);
                }
            }
            output.flush();
            server.getInfo().sendData(ProxySyncChannel.CHANNEL, bytes.toByteArray());
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }
}
