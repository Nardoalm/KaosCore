package com.knowplugins.ysubz.kaoscore.listeners.proxy;

import com.knowplugins.ysubz.kaoscore.KaosCore;
import com.knowplugins.ysubz.kaoscore.cmd.commands.vip.SkinCommand;
import com.knowplugins.ysubz.kaoscore.integration.proxy.ProxySyncChannel;
import com.knowplugins.ysubz.kaoscore.utils.disguise.DisguiseApiService;
import com.knowplugins.ysubz.kaoscore.utils.menu.FriendMenu;
import com.knowplugins.ysubz.kaoscore.utils.menu.SkinMenu;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.UUID;

public final class ProxySyncListener implements PluginMessageListener {

    private final KaosCore plugin;

    public ProxySyncListener(KaosCore plugin) {
        this.plugin = plugin;
    }

    public void register() {
        Bukkit.getMessenger().registerIncomingPluginChannel(plugin, ProxySyncChannel.CHANNEL, this);
        Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, ProxySyncChannel.CHANNEL);
    }

    public void unregister() {
        Bukkit.getMessenger().unregisterIncomingPluginChannel(plugin, ProxySyncChannel.CHANNEL, this);
        Bukkit.getMessenger().unregisterOutgoingPluginChannel(plugin, ProxySyncChannel.CHANNEL);
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!ProxySyncChannel.CHANNEL.equals(channel) || message == null || message.length == 0) {
            return;
        }

        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(message))) {
            String action = input.readUTF();
            UUID targetId = UUID.fromString(input.readUTF());
            Player target = Bukkit.getPlayer(targetId);
            if (target == null || !target.isOnline()) {
                return;
            }

            if (ProxySyncChannel.DISGUISE_SET.equalsIgnoreCase(action)) {
                applyDisguise(target, input.readUTF());
                return;
            }

            if (ProxySyncChannel.DISGUISE_SET_SKIN_NAME.equalsIgnoreCase(action)) {
                applyDisguiseWithSkinName(target, input.readUTF(), input.readUTF());
                return;
            }

            if (ProxySyncChannel.DISGUISE_SET_TEXTURE.equalsIgnoreCase(action)) {
                applyDisguiseWithTextures(target, input.readUTF(), input.readUTF(), input.readUTF(), input.readUTF());
                return;
            }

            if (ProxySyncChannel.DISGUISE_RESET.equalsIgnoreCase(action)) {
                removeDisguise(target);
                return;
            }

            if (ProxySyncChannel.SKIN_SET.equalsIgnoreCase(action)) {
                SkinCommand.setSkin(target, input.readUTF());
                return;
            }

            if (ProxySyncChannel.SKIN_RESET.equalsIgnoreCase(action)) {
                SkinCommand.resetSkin(target);
                return;
            }

            if (ProxySyncChannel.SKIN_GUI.equalsIgnoreCase(action)) {
                SkinMenu.open(target);
                target.playSound(target.getLocation(), Sound.CLICK, 1.0f, 1.0f);
                return;
            }

            if (ProxySyncChannel.FRIEND_GUI.equalsIgnoreCase(action)) {
                FriendMenu.open(target);
            }
        } catch (Exception exception) {
            plugin.getLogger().warning("[KaosCore] Falha ao processar sincronizacao do proxy: " + exception.getMessage());
        }
    }

    private void applyDisguise(Player player, String nick) {
        if (!DisguiseApiService.applyDisguise(player, nick)) {
            player.sendMessage("§c§lDISGUISE ➔ §cErro ao aplicar disfarce.");
            player.playSound(player.getLocation(), Sound.CLICK, 1.0f, 1.0f);
            return;
        }

        player.sendMessage("§a§lDISGUISE ➔ §aVocê agora é: §e" + nick);
        player.playSound(player.getLocation(), Sound.LEVEL_UP, 1.0f, 1.0f);
    }

    private void applyDisguiseWithSkinName(Player player, String nick, String skinName) {
        if (!DisguiseApiService.applyDisguise(player, nick)) {
            player.sendMessage("§c§lDISGUISE ➔ §cErro ao aplicar disfarce.");
            player.playSound(player.getLocation(), Sound.CLICK, 1.0f, 1.0f);
            return;
        }

        player.sendMessage("§eBuscando skin para o nick §f" + skinName + "§e...");
        SkinCommand.setSkinForOthers(player, skinName);
    }

    private void applyDisguiseWithTextures(Player player, String nick, String skinName, String value, String signature) {
        if (!DisguiseApiService.applyDisguiseWithTextures(player, nick, skinName, value, signature)) {
            player.sendMessage("§c§lDISGUISE ➔ §cErro ao aplicar disfarce.");
            player.playSound(player.getLocation(), Sound.CLICK, 1.0f, 1.0f);
            return;
        }

        player.sendMessage("§a§lDISGUISE ➔ §aDisfarce aleatório aplicado: §e" + nick);
        player.playSound(player.getLocation(), Sound.LEVEL_UP, 1.0f, 1.0f);
    }

    private void removeDisguise(Player player) {
        DisguiseApiService.removeDisguise(player).whenComplete((success, throwable) ->
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (throwable != null || !Boolean.TRUE.equals(success)) {
                        player.sendMessage("§c§lDISGUISE ➔ §cOcorreu um erro ao remover o disfarce.");
                        player.playSound(player.getLocation(), Sound.CLICK, 1.0f, 1.0f);
                        return;
                    }

                    player.sendMessage("§a§lDISGUISE ➔ §aSeu disfarce foi removido.");
                    player.playSound(player.getLocation(), Sound.LEVEL_UP, 1.0f, 1.0f);
                }));
    }
}
