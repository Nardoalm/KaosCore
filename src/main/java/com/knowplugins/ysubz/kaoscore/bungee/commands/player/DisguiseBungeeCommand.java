package com.knowplugins.ysubz.kaoscore.bungee.commands.player;

import com.knowplugins.ysubz.kaoscore.bungee.BaseBungeeCommand;
import com.knowplugins.ysubz.kaoscore.bungee.BungeeKaosCore;
import com.knowplugins.ysubz.kaoscore.bungee.Texts;
import com.knowplugins.ysubz.kaoscore.integration.proxy.ProxySyncChannel;
import com.knowplugins.ysubz.kaoscore.utils.disguise.SkinMatrix;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.List;
import java.util.UUID;

final class DisguiseBungeeCommand extends BaseBungeeCommand {

    DisguiseBungeeCommand(BungeeKaosCore plugin) {
        super(plugin, "disguise", "nick", "fake");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        ProxiedPlayer player = playerOnly(sender);
        if (player == null) {
            return;
        }

        if (args.length >= 1 && "list".equalsIgnoreCase(args[0])) {
            if (!require(sender, "kaoscore.staff.disguise", "§c§lDISGUISE ➔ §cVocê não possui permissão para executar este comando.")) {
                return;
            }
            sendDisguiseList(player);
            return;
        }

        boolean disguised = plugin.getDisguises().isDisguised(player.getUniqueId());
        if (args.length >= 1 && ("reset".equalsIgnoreCase(args[0]) || "off".equalsIgnoreCase(args[0]))) {
            if (!disguised) {
                reply(player, "§c§lDISGUISE ➔ §cVocê não está disfarçado.");
                return;
            }
            resetDisguise(player);
            return;
        }

        if (args.length == 0 && disguised) {
            resetDisguise(player);
            return;
        }

        if (!require(sender, "kaoscore.member.disguise", "§c§lDISGUISE ➔ §cVocê não possui permissão para executar este comando.")) {
            return;
        }

        if (args.length == 0) {
            applyRandomDisguise(player);
            return;
        }

        String nick = Texts.safe(args[0]).trim();
        if (nick.isEmpty() || nick.length() > 16) {
            reply(player, "§c§lDISGUISE ➔ §cO nick não pode ter mais de 16 caracteres.");
            return;
        }
        if (plugin.getDisguises().isNickInUse(nick)) {
            reply(player, "§c§lDISGUISE ➔ §cEsse nick já está em uso.");
            return;
        }
        if (plugin.getDisguises().hasJoinedBefore(nick)) {
            reply(player, "§c§lDISGUISE ➔ §cEsse nick pertence a um jogador que já entrou no servidor.");
            return;
        }

        UUID uuid = player.getUniqueId();
        plugin.getDisguises().saveDisguise(uuid, nick);

        if (plugin.getPermissions().hasPermission(player, "kaoscore.staff.disguise")) {
            plugin.getSkins().savePending(uuid, nick);
            if (!BungeeSyncMessenger.send(player, ProxySyncChannel.DISGUISE_SET_SKIN_NAME, nick, nick)) {
                reply(player, "§eBuscando skin para o nick §f" + nick + "§e. Ela será aplicada no próximo login se o servidor atual não suportar sync.");
            }
            return;
        }

        if (!BungeeSyncMessenger.send(player, ProxySyncChannel.DISGUISE_SET, nick)) {
            reply(player, "§a§lDISGUISE ➔ §aVocê agora é: §e" + nick + " §7(será aplicado ao entrar no servidor novamente)");
        }
    }

    private void applyRandomDisguise(ProxiedPlayer player) {
        String nick = plugin.getDisguises().getRandomNick();
        if (nick == null || nick.trim().isEmpty()) {
            reply(player, "§c§lDISGUISE ➔ §cNenhum nick disponível no momento.");
            return;
        }

        UUID uuid = player.getUniqueId();
        SkinMatrix.SkinData skin = SkinMatrix.getRandomSkin();
        plugin.getDisguises().saveDisguise(uuid, nick);
        plugin.getSkins().save(uuid, "random-disguise", skin.value, skin.signature);

        if (!BungeeSyncMessenger.send(player, ProxySyncChannel.DISGUISE_SET_TEXTURE, nick, "random-disguise", skin.value, skin.signature)) {
            reply(player, "§a§lDISGUISE ➔ §aDisfarce aleatório salvo: §e" + nick + " §7(será aplicado ao entrar no servidor novamente)");
        }
    }

    private void resetDisguise(ProxiedPlayer player) {
        UUID uuid = player.getUniqueId();
        plugin.getDisguises().clearDisguise(uuid);
        plugin.getSkins().delete(uuid);

        if (!BungeeSyncMessenger.send(player, ProxySyncChannel.DISGUISE_RESET)) {
            reply(player, "§a§lDISGUISE ➔ §aSeu disfarce foi removido.");
        }
    }

    private void sendDisguiseList(ProxiedPlayer viewer) {
        List<UUID> disguisedPlayers = plugin.getDisguises().getOnlineDisguisedPlayers();
        if (disguisedPlayers.isEmpty()) {
            reply(viewer, "§e§lFAKE ➔ §cNenhum jogador está disfarçado no momento.");
            return;
        }

        reply(viewer, "§e§lFAKE ➔ §fJogadores disfarçados online: §e" + disguisedPlayers.size());
        for (UUID uuid : disguisedPlayers) {
            String fakeNick = plugin.getDisguises().getVisibleName(uuid);
            String realName = plugin.getDisguises().getAccountName(uuid);
            reply(viewer, " §7- §f" + fakeNick + " §8-> §e" + realName);
        }
    }
}
