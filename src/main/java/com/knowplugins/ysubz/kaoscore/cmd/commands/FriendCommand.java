package com.knowplugins.ysubz.kaoscore.cmd.commands;

import com.knowplugins.ysubz.kaoscore.cmd.Commands;
import com.knowplugins.ysubz.kaoscore.friend.FriendRequest;
import com.knowplugins.ysubz.kaoscore.managers.player.FriendManager;
import com.knowplugins.ysubz.kaoscore.managers.Managers;
import com.knowplugins.ysubz.kaoscore.managers.player.DisguiseManager;
import com.knowplugins.ysubz.kaoscore.managers.player.VanishManager;
import com.knowplugins.ysubz.kaoscore.utils.disguise.DisguiseApiService;
import com.knowplugins.ysubz.kaoscore.utils.menu.FriendMenu;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

public class FriendCommand extends Commands {

    private static final String BASE_PERMISSION = "kaoscore.amigo.use";

    public FriendCommand() {
        super("amigo", "amiga", "amigue", "amigos", "amigas", "amigues", "friend", "friends");
    }

    @Override
    public void perform(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cApenas jogadores podem usar este comando!");
            return;
        }

        Player player = (Player) sender;
        if (!hasPermission(player, BASE_PERMISSION)) {
            player.sendMessage("§cVocê não tem permissão para usar o sistema de amizade.");
            return;
        }

        if (args.length == 0) {
            sendHelp(player);
            return;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "gui":
                FriendMenu.open(player);
                break;
            case "adicionar":
            case "add":
                cmdAdd(player, args);
                break;
            case "pedidos":
                cmdRequests(player);
                break;
            case "aceitar":
                cmdAccept(player, args);
                break;
            case "recusar":
                cmdDeny(player, args);
                break;
            case "remover":
                cmdRemove(player, args);
                break;
            case "listar":
                cmdList(player, args);
                break;
            case "online":
                cmdOnline(player);
                break;
            default:
                if (args.length == 1) {
                    cmdAdd(player, new String[]{"adicionar", joinArgs(args, 0)});
                    return;
                }
                sendHelp(player);
                break;
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        if (!(sender instanceof Player)) {
            return Collections.emptyList();
        }

        Player player = (Player) sender;
        if (!hasPermission(player, BASE_PERMISSION)) {
            return Collections.emptyList();
        }

        FriendManager friendManager = Managers.getFriendManager();
        if (friendManager == null) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            String start = args[0].toLowerCase(Locale.ROOT);

            List<String> out = new ArrayList<>();
            List<String> commands = Arrays.asList("gui", "adicionar", "pedidos", "aceitar", "recusar", "remover", "listar", "online");
            out.addAll(commands.stream()
                    .filter(command -> hasPermission(player, permissionFor(command)))
                    .filter(command -> command.startsWith(start))
                    .collect(Collectors.toList()));

            if (hasPermission(player, permissionFor("adicionar"))) {
                out.addAll(listAddTargets(player, start));
            }

            // distinct mantendo ordem
            return out.stream().distinct().collect(Collectors.toList());
        }

        if (args.length == 2) {
            String subCommand = args[0].toLowerCase(Locale.ROOT);
            if (subCommand.equals("adicionar") || subCommand.equals("add")) {
                String start = args[1].toLowerCase(Locale.ROOT);
                return listAddTargets(player, start);
            }

            if (subCommand.equals("aceitar") || subCommand.equals("recusar")) {
                return friendManager.getRequests(player.getUniqueId()).stream()
                        .map(FriendRequest::getRequesterName)
                        .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT)))
                        .collect(Collectors.toList());
            }

            if (subCommand.equals("remover")) {
                return friendManager.getFriends(player.getUniqueId()).stream()
                        .map(friendManager::getFriendDisplay)
                        .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT)))
                        .collect(Collectors.toList());
            }
        }

        return Collections.emptyList();
    }

    private void sendHelp(Player player) {
        player.sendMessage("§eUso do §6/amigo§e:");
        sendHelpLine(player, "gui", " §e• §6/amigo gui");
        sendHelpLine(player, "adicionar", " §e• §6/amigo adicionar <jogador>");
        sendHelpLine(player, "pedidos", " §e• §6/amigo pedidos");
        sendHelpLine(player, "aceitar", " §e• §6/amigo aceitar <jogador>");
        sendHelpLine(player, "recusar", " §e• §6/amigo recusar <jogador>");
        sendHelpLine(player, "remover", " §e• §6/amigo remover <jogador>");
        sendHelpLine(player, "listar", " §e• §6/amigo listar [pagina]");
        sendHelpLine(player, "online", " §e• §6/amigo online");
        player.sendMessage("");
    }

    private void cmdAdd(Player player, String[] args) {
        if (!requirePermission(player, "adicionar")) {
            return;
        }

        if (args.length < 2) {
            player.sendMessage("§cUse: /amigo adicionar <jogador>");
            return;
        }

        FriendManager friendManager = Managers.getFriendManager();
        if (friendManager == null) {
            player.sendMessage("§cSistema de amizade indisponível.");
            return;
        }
        Player target = resolvePlayer(player, args[1]);
        if (target == null) {
            player.sendMessage("§cJogador não encontrado!");
            return;
        }

        FriendManager.RequestResult result = friendManager.createRequest(player, target);
        String targetColorPrefix = PlaceholderAPI.setPlaceholders(
                target,
                "%kaoscore_tag_color%"
        );
        switch (result) {
            case SENT:
                player.sendMessage("§aPedido de amizade enviado para §f" + targetColorPrefix + target.getDisplayName());
                FriendRequest request = friendManager.getRequest(target.getUniqueId(), player.getUniqueId());
                if (request != null) {
                    friendManager.sendRequestNotification(target, request);
                }
                return;
            case AUTO_ACCEPTED:
                player.sendMessage("§aPedido mútuo detectado. Vocês agora são amigos!");
                target.sendMessage("§aPedido mútuo detectado. Vocês agora são amigos!");
                return;
            case ALREADY_FRIENDS:
                player.sendMessage("§cVocê já é amigo deste jogador.");
                return;
            case ALREADY_REQUESTED:
                player.sendMessage("§cVocê já enviou um pedido de amizade para este jogador.");
                return;
            case LIMIT_REACHED:
                player.sendMessage("§cUma das contas atingiu o limite de amizades.");
                return;
            case SELF:
                player.sendMessage("§cVocê não pode enviar amizade para si mesmo.");
                return;
            default:
                player.sendMessage("§cNão foi possível enviar o pedido de amizade.");
        }
    }

    private void cmdRequests(Player player) {
        if (!requirePermission(player, "pedidos")) {
            return;
        }

        FriendManager friendManager = Managers.getFriendManager();
        if (friendManager != null) {
            friendManager.sendRequestList(player, true, false);
        }
    }

    private void cmdAccept(Player player, String[] args) {
        if (!requirePermission(player, "aceitar")) {
            return;
        }

        if (args.length < 2) {
            player.sendMessage("§cUse: /amigo aceitar <jogador>");
            return;
        }

        FriendManager friendManager = Managers.getFriendManager();
        FriendRequest request = friendManager == null ? null : friendManager.findRequest(player.getUniqueId(), joinArgs(args, 1));
        if (request == null) {
            player.sendMessage("§cVocê não possui pedido pendente deste jogador.");
            return;
        }

        if (!friendManager.acceptRequest(player.getUniqueId(), request.getRequesterId())) {
            player.sendMessage("§cNão foi possível aceitar o pedido de amizade.");
            return;
        }

        player.sendMessage("§aVocê agora é amigo de §f" + friendManager.getFriendAccountName(request.getRequesterId()) + "§a.");
        Player requester = Bukkit.getPlayer(request.getRequesterId());
        if (requester != null && requester.isOnline()) {
            requester.sendMessage("§a" + player.getDisplayName() + " aceitou seu pedido de amizade!");
        }
    }

    private void cmdDeny(Player player, String[] args) {
        if (!requirePermission(player, "recusar")) {
            return;
        }

        if (args.length < 2) {
            player.sendMessage("§cUse: /amigo recusar <jogador>");
            return;
        }

        FriendManager friendManager = Managers.getFriendManager();
        FriendRequest request = friendManager == null ? null : friendManager.findRequest(player.getUniqueId(), joinArgs(args, 1));
        if (request == null) {
            player.sendMessage("§cVocê não possui pedido pendente deste jogador.");
            return;
        }

        friendManager.denyRequest(player.getUniqueId(), request.getRequesterId());
        player.sendMessage("§cPedido de amizade recusado.");
    }

    private void cmdRemove(Player player, String[] args) {
        if (!requirePermission(player, "remover")) {
            return;
        }

        if (args.length < 2) {
            player.sendMessage("§cUse: /amigo remover <jogador>");
            return;
        }

        FriendManager friendManager = Managers.getFriendManager();
        UUID friendId = friendManager == null ? null : friendManager.findFriend(player.getUniqueId(), joinArgs(args, 1));
        if (friendId == null) {
            player.sendMessage("§cEste jogador não está na sua lista de amizades.");
            return;
        }

        String friendName = friendManager.getFriendDisplay(friendId);
        friendManager.removeFriend(player.getUniqueId(), friendId);
        player.sendMessage("§cAmizade removida com §f" + friendName + "§c.");

        Player onlineFriend = Bukkit.getPlayer(friendId);
        if (onlineFriend != null && onlineFriend.isOnline()) {
            onlineFriend.sendMessage("§c" + player.getDisplayName() + " removeu a amizade com você.");
        }
    }

    private void cmdList(Player player, String[] args) {
        if (!requirePermission(player, "listar")) {
            return;
        }

        FriendManager friendManager = Managers.getFriendManager();
        if (friendManager == null) {
            player.sendMessage("§cSistema de amizade indisponível.");
            return;
        }

        List<UUID> friends = new ArrayList<>(friendManager.getFriends(player.getUniqueId()));
        if (friends.isEmpty()) {
            player.sendMessage("§cVocê ainda não possui amizades.");
            return;
        }

        int page = 1;
        if (args.length > 1) {
            try {
                page = Integer.parseInt(args[1]);
            } catch (NumberFormatException ignored) {
                page = 1;
            }
        }
        if (page < 1) {
            page = 1;
        }

        friends.sort(Comparator.comparing(friendManager::getFriendDisplay, String.CASE_INSENSITIVE_ORDER));
        int totalPages = Math.max(1, (friends.size() + 9) / 10);
        if (page > totalPages) {
            player.sendMessage("§cPágina inválida. Máximo: " + totalPages + ".");
            return;
        }

        int start = (page - 1) * 10;
        int end = Math.min(start + 10, friends.size());

        player.sendMessage("§eSuas Amizades: ");
        for (int i = start; i < end; i++) {
            UUID friendId = friends.get(i);
            boolean online = friendManager.isFriendOnlineFor(player.getUniqueId(), friendId);
            String status = online ? "§aON" : "§cOFF";
            String color = friendManager.getFriendTagColor(friendId);
            String name = friendManager.getFriendDisplay(friendId);
            player.sendMessage(" §7" + (i + 1) + ". " + status + " §8- " + color + name);
        }
        player.sendMessage("§7Página §f" + page + " §7de §f" + totalPages + " §8| §7Online: §f" + friendManager.getOnlineFriendCount(player.getUniqueId()) + "§7/§f" + friendManager.getFriendCount(player.getUniqueId()));
    }

    private void cmdOnline(Player player) {
        if (!requirePermission(player, "online")) {
            return;
        }

        FriendManager friendManager = Managers.getFriendManager();
        if (friendManager == null) {
            player.sendMessage("§cSistema de amizade indisponível.");
            return;
        }

        Collection<UUID> onlineFriends = friendManager.getOnlineFriends(player.getUniqueId());
        if (onlineFriends.isEmpty()) {
            player.sendMessage("§cNenhum amigo seu está online agora.");
            return;
        }

        player.sendMessage("§eAmigos Online: ");
        for (UUID friendId : onlineFriends) {
            String color = friendManager.getFriendTagColor(friendId);
            String name = friendManager.getFriendDisplay(friendId);
            player.sendMessage(" §7- " + color + name);
        }
    }

    private void sendHelpLine(Player player, String subCommand, String text) {
        if (hasPermission(player, permissionFor(subCommand))) {
            player.sendMessage(text);
        }
    }

    private boolean requirePermission(Player player, String subCommand) {
        if (hasPermission(player, permissionFor(subCommand))) {
            return true;
        }
        player.sendMessage("§cVocê não tem permissão para usar /amigo " + subCommand + ".");
        return false;
    }

    private boolean hasPermission(Player player, String permission) {
        return permission == null || permission.isEmpty() || player.hasPermission(permission);
    }

    private String permissionFor(String subCommand) {
        String normalized = subCommand.toLowerCase(Locale.ROOT);
        if (normalized.equals("gui")) {
            return BASE_PERMISSION; // GUI nao precisa de permissao extra alem da base.
        }
        if (normalized.equals("add")) {
            normalized = "adicionar";
        }
        return BASE_PERMISSION + "." + normalized;
    }

    private List<String> listAddTargets(Player viewer, String startLower) {
        if (viewer == null) {
            return Collections.emptyList();
        }
        String start = startLower == null ? "" : startLower.toLowerCase(Locale.ROOT);

        return Bukkit.getOnlinePlayers().stream()
                .filter(target -> target != null && target.isOnline())
                .filter(target -> !target.getUniqueId().equals(viewer.getUniqueId()))
                .filter(target -> !VanishManager.isVanished(target) || viewer.hasPermission("kaoscore.staff.vanish"))
                .map(target -> {
                    String nick = null;
                    try {
                        nick = DisguiseApiService.getDisguiseNick(target);
                    } catch (Exception ignored) {
                    }
                    String name = (nick != null && !nick.trim().isEmpty()) ? nick.trim() : target.getName();
                    return ChatColor.stripColor(name);
                })
                .filter(name -> name != null && !name.trim().isEmpty())
                .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(start))
                .collect(Collectors.toList());
    }

    private Player resolvePlayer(Player viewer, String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        Player target = DisguiseManager.getOnlinePlayerByName(name);
        if (target != null && VanishManager.isVanished(target) && (viewer == null || !viewer.hasPermission("kaoscore.staff.vanish"))) {
            return null;
        }
        return target;
    }

    private String joinArgs(String[] args, int startIndex) {
        StringBuilder builder = new StringBuilder();
        for (int i = startIndex; i < args.length; i++) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(args[i]);
        }
        return builder.toString().trim();
    }
}

