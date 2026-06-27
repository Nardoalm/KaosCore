package com.knowplugins.ysubz.kaoscore.bukkit.cmd.commands;

import com.knowplugins.ysubz.kaoscore.bukkit.KaosCore;
import com.knowplugins.ysubz.kaoscore.bukkit.cmd.Commands;
import com.knowplugins.ysubz.kaoscore.bukkit.friend.FriendRequest;
import com.knowplugins.ysubz.kaoscore.bukkit.managers.player.FriendManager;
import com.knowplugins.ysubz.kaoscore.bukkit.managers.Managers;
import com.knowplugins.ysubz.kaoscore.bukkit.disguise.DisguiseManager;
import com.knowplugins.ysubz.kaoscore.bukkit.managers.player.VanishManager;
import com.knowplugins.ysubz.kaoscore.bukkit.disguise.api.DisguiseApiService;
import com.knowplugins.ysubz.kaoscore.bukkit.utils.menu.FriendMenu;
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

    public FriendCommand() {
        super("amigo", "amiga", "amigue", "amigos", "amigas", "amigues", "friend", "friends");
    }

    @Override
    public void perform(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return;
        }

        Player player = (Player) sender;
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
            case "requests":
                cmdRequests(player);
                break;
            case "aceitar":
            case "accept":
                cmdAccept(player, args);
                break;
            case "recusar":
            case "deny":
                cmdDeny(player, args);
                break;
            case "remover":
            case "remove":
                cmdRemove(player, args);
                break;
            case "listar":
            case "list":
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
        FriendManager friendManager = Managers.getFriendManager();
        if (friendManager == null) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            String start = args[0].toLowerCase(Locale.ROOT);

            List<String> out = new ArrayList<>();
            List<String> commands = Arrays.asList("gui", "add", "requests", "accept", "deny", "remove", "list", "online");
            out.addAll(commands.stream()
                    .filter(command -> hasPermission(player, permissionFor(command)))
                    .filter(command -> command.startsWith(start))
                    .collect(Collectors.toList()));

            if (hasPermission(player, permissionFor("add"))) {
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

            if (subCommand.equals("remove") || subCommand.equals("remover")) {
                return friendManager.getFriends(player.getUniqueId()).stream()
                        .map(friendManager::getFriendDisplay)
                        .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT)))
                        .collect(Collectors.toList());
            }
        }

        return Collections.emptyList();
    }

    private void sendHelp(Player player) {
        player.sendMessage("§eUsage of §6/friend§e:");
        sendHelpLine(player, "gui", " §e* §6/friend gui");
        sendHelpLine(player, "adicionar", " §e* §6/friend add <player>");
        sendHelpLine(player, "pedidos", " §e* §6/friend requests");
        sendHelpLine(player, "aceitar", " §e* §6/friend accept <player>");
        sendHelpLine(player, "recusar", " §e* §6/friend deny <player>");
        sendHelpLine(player, "remover", " §e* §6/friend remove <player>");
        sendHelpLine(player, "listar", " §e* §6/friend list [page]");
        sendHelpLine(player, "online", " §e* §6/friend online");
        player.sendMessage("");
    }

    private void cmdAdd(Player player, String[] args) {
        if (!requirePermission(player, "adicionar")) {
            return;
        }

        if (args.length < 2) {
            player.sendMessage("§cUsage: /friend add <player>");
            return;
        }

        FriendManager friendManager = Managers.getFriendManager();
        if (friendManager == null) {
            player.sendMessage("§cFriend system unavailable.");
            return;
        }
        Player target = resolvePlayer(player, args[1]);
        if (target == null) {
            player.sendMessage("§cPlayer not found!");
            return;
        }

        FriendManager.RequestResult result = friendManager.createRequest(player, target);
        String targetColorPrefix = KaosCore.getInstance().getTagManager().getTag(target.getPlayer()).getColor();
        switch (result) {
            case SENT:
                player.sendMessage("§aFriend request sent to §f" + targetColorPrefix + target.getDisplayName());
                FriendRequest request = friendManager.getRequest(target.getUniqueId(), player.getUniqueId());
                if (request != null) {
                    friendManager.sendRequestNotification(target, request);
                }
                return;
            case AUTO_ACCEPTED:
                player.sendMessage("§aMutual request detected. You are now friends!");
                target.sendMessage("§aMutual request detected. You are now friends!");
                return;
            case ALREADY_FRIENDS:
                player.sendMessage("§cYou are already friends with this player.");
                return;
            case ALREADY_REQUESTED:
                player.sendMessage("§cYou have already sent a friend request to this player.");
                return;
            case LIMIT_REACHED:
                player.sendMessage("§cOne of the accounts has reached the friend limit.");
                return;
            case SELF:
                player.sendMessage("§cYou cannot friend yourself.");
                return;
            default:
                player.sendMessage("§cUnable to send the friend request.");
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
            player.sendMessage("§cUsage: /amigo aceitar <player>");
            return;
        }

        FriendManager friendManager = Managers.getFriendManager();
        FriendRequest request = friendManager == null ? null : friendManager.findRequest(player.getUniqueId(), joinArgs(args, 1));
        if (request == null) {
            player.sendMessage("§cYou do not have a pending request from this player.");
            return;
        }

        if (!friendManager.acceptRequest(player.getUniqueId(), request.getRequesterId())) {
            player.sendMessage("§cUnable to accept the friend request.");
            return;
        }

        player.sendMessage("§aYou are now friends with §f" + friendManager.getFriendAccountName(request.getRequesterId()) + "§a.");
        Player requester = Bukkit.getPlayer(request.getRequesterId());
        if (requester != null && requester.isOnline()) {
            requester.sendMessage("§a" + player.getDisplayName() + " accepted your friend request!");
        }
    }

    private void cmdDeny(Player player, String[] args) {
        if (!requirePermission(player, "recusar")) {
            return;
        }

        if (args.length < 2) {
            player.sendMessage("§cUsage: /amigo recusar <player>");
            return;
        }

        FriendManager friendManager = Managers.getFriendManager();
        FriendRequest request = friendManager == null ? null : friendManager.findRequest(player.getUniqueId(), joinArgs(args, 1));
        if (request == null) {
            player.sendMessage("§cYou do not have a pending request from this player.");
            return;
        }

        friendManager.denyRequest(player.getUniqueId(), request.getRequesterId());
        player.sendMessage("§cFriend request declined.");
    }

    private void cmdRemove(Player player, String[] args) {
        if (!requirePermission(player, "remover")) {
            return;
        }

        if (args.length < 2) {
            player.sendMessage("§cUsage: /amigo remover <player>");
            return;
        }

        FriendManager friendManager = Managers.getFriendManager();
        UUID friendId = friendManager == null ? null : friendManager.findFriend(player.getUniqueId(), joinArgs(args, 1));
        if (friendId == null) {
            player.sendMessage("§cThis player is not in your friends list.");
            return;
        }

        String friendName = friendManager.getFriendDisplay(friendId);
        friendManager.removeFriend(player.getUniqueId(), friendId);
        player.sendMessage("§cRemoved friendship with §f" + friendName + "§c.");

        Player onlineFriend = Bukkit.getPlayer(friendId);
        if (onlineFriend != null && onlineFriend.isOnline()) {
            onlineFriend.sendMessage("§c" + player.getDisplayName() + " removed you from their friends list.");
        }
    }

    private void cmdList(Player player, String[] args) {
        if (!requirePermission(player, "listar")) {
            return;
        }

        FriendManager friendManager = Managers.getFriendManager();
        if (friendManager == null) {
            player.sendMessage("§cFriend system unavailable.");
            return;
        }

        List<UUID> friends = new ArrayList<>(friendManager.getFriends(player.getUniqueId()));
        if (friends.isEmpty()) {
            player.sendMessage("§cYou do not have any friends yet.");
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
            player.sendMessage("§cInvalid page. Maximum: " + totalPages + ".");
            return;
        }

        int start = (page - 1) * 10;
        int end = Math.min(start + 10, friends.size());

        player.sendMessage("§eYour Friends: ");
        for (int i = start; i < end; i++) {
            UUID friendId = friends.get(i);
            boolean online = friendManager.isFriendOnlineFor(player.getUniqueId(), friendId);
            String status = online ? "§aON" : "§cOFF";
            String color = friendManager.getFriendTagColor(friendId);
            String name = friendManager.getFriendDisplay(friendId);
            player.sendMessage(" §7" + (i + 1) + ". " + status + " §8- " + color + name);
        }
        player.sendMessage("§7Page §f" + page + " §7of §f" + totalPages + " §8| §7Online: §f" + friendManager.getOnlineFriendCount(player.getUniqueId()) + "§7/§f" + friendManager.getFriendCount(player.getUniqueId()));
    }

    private void cmdOnline(Player player) {
        if (!requirePermission(player, "online")) {
            return;
        }

        FriendManager friendManager = Managers.getFriendManager();
        if (friendManager == null) {
            player.sendMessage("§cFriend system unavailable.");
            return;
        }

        Collection<UUID> onlineFriends = friendManager.getOnlineFriends(player.getUniqueId());
        if (onlineFriends.isEmpty()) {
            player.sendMessage("§cNone of your friends are online right now.");
            return;
        }

        player.sendMessage("§eOnline Friends: ");
        for (UUID friendId : onlineFriends) {
            String color = friendManager.getFriendTagColor(friendId);
            String name = friendManager.getFriendDisplay(friendId);
            player.sendMessage(" §7- " + color + name);
        }
    }

    private void sendHelpLine(Player player, String subCommand, String text) {
        player.sendMessage(text);
    }

    private boolean requirePermission(Player player, String subCommand) {
        return true;
    }

    private boolean hasPermission(Player player, String permission) {
        return permission == null || permission.isEmpty() || player.hasPermission(permission);
    }

    private String permissionFor(String subCommand) {
        return "";
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
