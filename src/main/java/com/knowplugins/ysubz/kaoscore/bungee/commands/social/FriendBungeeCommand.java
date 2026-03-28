package com.knowplugins.ysubz.kaoscore.bungee;

import com.knowplugins.ysubz.kaoscore.integration.proxy.ProxySyncChannel;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

final class FriendBungeeCommand extends BaseBungeeCommand {

    FriendBungeeCommand(BungeeKaosCore plugin) {
        super(plugin, "amigo", "friend", "friends", "amigos", "amigas", "amigues");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        ProxiedPlayer player = playerOnly(sender);
        if (player == null) {
            return;
        }
        if (!require(sender, "kaoscore.member.friend", "§cVocê não tem permissão para usar esse comando.")) {
            return;
        }
        if (args.length == 0) {
            sendHelp(sender);
            return;
        }

        String sub = Texts.normalize(args[0]);
        if ("gui".equals(sub)) {
            openGui(player);
            return;
        }
        if ("adicionar".equals(sub) || "add".equals(sub)) {
            add(player, args);
            return;
        }
        if ("pedidos".equals(sub) || "requests".equals(sub)) {
            requests(player);
            return;
        }
        if ("aceitar".equals(sub) || "accept".equals(sub)) {
            accept(player, args);
            return;
        }
        if ("recusar".equals(sub) || "deny".equals(sub)) {
            deny(player, args);
            return;
        }
        if ("remover".equals(sub) || "remove".equals(sub)) {
            remove(player, args);
            return;
        }
        if ("listar".equals(sub) || "list".equals(sub)) {
            list(player, args);
            return;
        }
        if ("online".equals(sub)) {
            online(player);
            return;
        }

        if (args.length == 1) {
            add(player, new String[]{"adicionar", args[0]});
            return;
        }
        sendHelp(sender);
    }

    private void sendHelp(CommandSender sender) {
        reply(sender, "§eUso do §6/amigo§e:");
        reply(sender, " §e• §6/amigo gui");
        reply(sender, " §e• §6/amigo adicionar <jogador>");
        reply(sender, " §e• §6/amigo pedidos");
        reply(sender, " §e• §6/amigo aceitar <jogador>");
        reply(sender, " §e• §6/amigo recusar <jogador>");
        reply(sender, " §e• §6/amigo remover <jogador>");
        reply(sender, " §e• §6/amigo listar [pagina]");
        reply(sender, " §e• §6/amigo online");
    }

    private void openGui(ProxiedPlayer player) {
        if (BungeeSyncMessenger.send(player, ProxySyncChannel.FRIEND_GUI)) {
            return;
        }
        reply(player, "§cSistema de GUI indisponível no servidor atual.");
    }

    private void add(ProxiedPlayer player, String[] args) {
        if (args.length < 2) {
            reply(player, "§cUse: /amigo adicionar <jogador>");
            return;
        }

        TargetResult target = BungeeCommandUtil.resolveTarget(plugin, args[1]);
        if (target.getPlayer() == null) {
            reply(player, "§cJogador não está online.");
            return;
        }

        ProxiedPlayer targetPlayer = target.getPlayer();
        String visibleTarget = plugin.getDisguises().getVisibleName(targetPlayer.getUniqueId());
        BungeeFriendService.RequestResult result = plugin.getFriends().createRequest(player, targetPlayer);
        if (result == BungeeFriendService.RequestResult.SENT) {
            reply(player, "§aPedido de amizade enviado para §f" + visibleTarget);
            FriendRequest request = plugin.getFriends().getRequest(targetPlayer.getUniqueId(), player.getUniqueId());
            if (request != null) {
                sendRequestNotification(targetPlayer, request);
            }
            return;
        }
        if (result == BungeeFriendService.RequestResult.AUTO_ACCEPTED) {
            reply(player, "§aPedido mútuo detectado. Vocês agora são amigos.");
            reply(targetPlayer, "§aPedido mútuo detectado. Vocês agora são amigos.");
            return;
        }
        if (result == BungeeFriendService.RequestResult.ALREADY_FRIENDS) {
            reply(player, "§cVocê já é amigo deste jogador.");
            return;
        }
        if (result == BungeeFriendService.RequestResult.ALREADY_REQUESTED) {
            reply(player, "§cVocê já enviou um pedido de amizade para este jogador.");
            return;
        }
        if (result == BungeeFriendService.RequestResult.LIMIT_REACHED) {
            reply(player, "§cUma das contas atingiu o limite de amizades.");
            return;
        }
        if (result == BungeeFriendService.RequestResult.SELF) {
            reply(player, "§cVocê não pode enviar amizade para si mesmo.");
            return;
        }
        reply(player, "§cNão foi possível enviar o pedido de amizade.");
    }

    private void requests(ProxiedPlayer player) {
        List<FriendRequest> requests = plugin.getFriends().getRequests(player.getUniqueId());
        if (requests.isEmpty()) {
            reply(player, "§cVocê não possui pedidos de amizade pendentes.");
            return;
        }

        reply(player, "§6§m+-------------------§6[ §ePedidos de Amizade §6]§m-------------------+");
        for (FriendRequest request : requests) {
            player.sendMessage(buildRequestComponent(request));
        }
        reply(player, "§6§m----------------------------------------------------------------+");
    }

    private void accept(ProxiedPlayer player, String[] args) {
        if (args.length < 2) {
            reply(player, "§cUse: /amigo aceitar <jogador>");
            return;
        }
        FriendRequest request = plugin.getFriends().findRequest(player.getUniqueId(), Texts.join(args, 1));
        if (request == null) {
            reply(player, "§cVocê não possui pedido pendente deste jogador.");
            return;
        }
        if (!plugin.getFriends().acceptRequest(player.getUniqueId(), request.getRequesterId())) {
            reply(player, "§cNão foi possível aceitar o pedido de amizade.");
            return;
        }
        reply(player, "§aVocê agora é amigo de §f" + plugin.getFriends().getFriendDisplay(request.getRequesterId()) + "§a.");
        ProxiedPlayer requester = ProxyServer.getInstance().getPlayer(request.getRequesterId());
        if (requester != null) {
            reply(requester, "§a" + plugin.getDisguises().getVisibleName(player.getUniqueId()) + " aceitou seu pedido de amizade.");
        }
    }

    private void deny(ProxiedPlayer player, String[] args) {
        if (args.length < 2) {
            reply(player, "§cUse: /amigo recusar <jogador>");
            return;
        }
        FriendRequest request = plugin.getFriends().findRequest(player.getUniqueId(), Texts.join(args, 1));
        if (request == null) {
            reply(player, "§cVocê não possui pedido pendente deste jogador.");
            return;
        }
        plugin.getFriends().denyRequest(player.getUniqueId(), request.getRequesterId());
        reply(player, "§cPedido de amizade recusado.");
    }

    private void remove(ProxiedPlayer player, String[] args) {
        if (args.length < 2) {
            reply(player, "§cUse: /amigo remover <jogador>");
            return;
        }
        UUID friendId = plugin.getFriends().findFriend(player.getUniqueId(), Texts.join(args, 1));
        if (friendId == null) {
            reply(player, "§cEste jogador não está na sua lista de amizades.");
            return;
        }
        String friendName = plugin.getFriends().getFriendDisplay(friendId);
        plugin.getFriends().removeFriend(player.getUniqueId(), friendId);
        reply(player, "§cAmizade removida com §f" + friendName + "§c.");
        ProxiedPlayer friend = ProxyServer.getInstance().getPlayer(friendId);
        if (friend != null) {
            reply(friend, "§c" + plugin.getDisguises().getVisibleName(player.getUniqueId()) + " removeu a amizade com você.");
        }
    }

    private void list(ProxiedPlayer player, String[] args) {
        List<UUID> friends = new ArrayList<>(plugin.getFriends().getFriends(player.getUniqueId()));
        if (friends.isEmpty()) {
            reply(player, "§cVocê ainda não possui amizades.");
            return;
        }

        int page = 1;
        if (args.length >= 2) {
            try {
                page = Math.max(1, Integer.parseInt(args[1]));
            } catch (NumberFormatException ignored) {
            }
        }

        Collections.sort(friends, new Comparator<UUID>() {
            @Override
            public int compare(UUID first, UUID second) {
                return plugin.getFriends().getFriendDisplay(first).compareToIgnoreCase(plugin.getFriends().getFriendDisplay(second));
            }
        });

        int totalPages = Math.max(1, (friends.size() + 9) / 10);
        if (page > totalPages) {
            reply(player, "§cPágina inválida. Máximo: " + totalPages + ".");
            return;
        }

        int start = (page - 1) * 10;
        int end = Math.min(start + 10, friends.size());
        reply(player, "§eSuas amizades:");
        for (int index = start; index < end; index++) {
            UUID friendId = friends.get(index);
            boolean online = ProxyServer.getInstance().getPlayer(friendId) != null;
            reply(player, " §7" + (index + 1) + ". " + (online ? "§aON" : "§cOFF") + " §8- §f" + plugin.getFriends().getFriendDisplay(friendId));
        }
        reply(player, "§7Página §f" + page + " §7de §f" + totalPages + " §8| §7Online: §f" + plugin.getFriends().getOnlineFriends(player.getUniqueId()).size() + "§7/§f" + friends.size());
    }

    private void online(ProxiedPlayer player) {
        Collection<UUID> onlineFriends = plugin.getFriends().getOnlineFriends(player.getUniqueId());
        if (onlineFriends.isEmpty()) {
            reply(player, "§cNenhum amigo seu está online agora.");
            return;
        }
        reply(player, "§eAmigos online:");
        for (UUID friendId : onlineFriends) {
            reply(player, " §7- §f" + plugin.getFriends().getFriendDisplay(friendId));
        }
    }

    private void sendRequestNotification(ProxiedPlayer target, FriendRequest request) {
        if (target == null || request == null) {
            return;
        }

        Texts.send(target, "§8➥ " + request.getRequesterName() + " §6quer te adicionar como amigo(a).");
        target.sendMessage(buildRequestComponent(request));
    }

    private TextComponent buildRequestComponent(FriendRequest request) {
        TextComponent line = new TextComponent(" §7- §f" + request.getRequesterName() + " ");

        TextComponent accept = new TextComponent("§a§l[ACEITAR]");
        accept.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/amigo aceitar " + request.getRequesterName()));
        accept.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§aClique para aceitar").create()));

        TextComponent deny = new TextComponent("§c§l[RECUSAR]");
        deny.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/amigo recusar " + request.getRequesterName()));
        deny.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§cClique para recusar").create()));

        line.addExtra(accept);
        line.addExtra(new TextComponent(" "));
        line.addExtra(deny);
        return line;
    }
}
