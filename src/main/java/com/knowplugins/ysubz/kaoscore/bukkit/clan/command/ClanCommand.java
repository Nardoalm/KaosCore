package com.knowplugins.ysubz.kaoscore.bukkit.clan.command;

import com.knowplugins.ysubz.kaoscore.bukkit.KaosCore;
import com.knowplugins.ysubz.kaoscore.bukkit.clan.Clan;
import com.knowplugins.ysubz.kaoscore.bukkit.clan.ClanRole;
import com.knowplugins.ysubz.kaoscore.bukkit.cmd.Commands;
import com.knowplugins.ysubz.kaoscore.bukkit.manager.MainManager;
import com.knowplugins.ysubz.kaoscore.bukkit.disguise.DisguiseManager;
import me.clip.placeholderapi.PlaceholderAPI;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class ClanCommand extends Commands {

    public ClanCommand(KaosCore plugin) {
        super("clan");
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
            case "create":
                cmdCreate(player, args);
                break;
            case "delete":
                cmdDelete(player);
                break;
            case "leave":
                cmdLeave(player);
                break;
            case "invite":
                cmdInvite(player, args);
                break;
            case "invites":
                cmdInvites(player);
                break;
            case "accept":
                cmdAccept(player, args);
                break;
            case "deny":
                cmdDeny(player, args);
                break;
            case "kick":
                cmdKick(player, args);
                break;
            case "promote":
                cmdPromote(player, args);
                break;
            case "demote":
                cmdDemote(player, args);
                break;
            case "transfer":
                cmdTransfer(player, args);
                break;
            case "color":
                cmdColorTag(player, args);
                break;
            case "edit":
                cmdEditTag(player, args);
                break;
            case "info":
                cmdInfo(player, args);
                break;
            case "members":
                cmdMembers(player, args);
                break;
            case "list":
                cmdList(player, args);
                break;
            case "chat":
                cmdChat(player);
                break;
            default:
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
        if (args.length == 1) {
            List<String> commands = Arrays.asList(
                    "create", "delete", "leave", "invite", "invites", "accept",
                    "deny", "kick", "promote", "demote", "transfer", "color",
                    "edit", "info", "members", "list", "chat"
            );
            return commands.stream()
                    .filter(command -> command.startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        }

        if (args.length >= 2) {
            String subCommand = args[0].toLowerCase(Locale.ROOT);
            if (subCommand.equals("color")) {
                return getAvailableTagColors(player).stream()
                        .map(option -> option.token)
                        .filter(token -> token.startsWith(args[args.length - 1].toLowerCase(Locale.ROOT)))
                        .collect(Collectors.toList());
            }

            if (Arrays.asList("invite", "kick", "promote", "demote", "transfer").contains(subCommand)) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getDisplayName)
                        .filter(name -> ChatColor.stripColor(name).toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT)))
                        .collect(Collectors.toList());
            }

            if (Arrays.asList("accept", "deny", "info", "members").contains(subCommand)) {
                return MainManager.getClanManager().getAllClans().stream()
                        .map(Clan::getName)
                        .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT)))
                        .collect(Collectors.toList());
            }
        }

        return Collections.emptyList();
    }

    private void sendHelp(Player player) {
        player.sendMessage("§eUsage of §6/clan§e:");
        sendHelpLine(player, "create", " §e* §6/clan create <name> <tag>");
        sendHelpLine(player, "delete", " §e* §6/clan delete");
        sendHelpLine(player, "leave", " §e* §6/clan leave");
        sendHelpLine(player, "invite", " §e* §6/clan invite <player>");
        sendHelpLine(player, "invites", " §e* §6/clan invites");
        sendHelpLine(player, "accept", " §e* §6/clan accept <name|tag>");
        sendHelpLine(player, "deny", " §e* §6/clan deny <name|tag>");
        sendHelpLine(player, "kick", " §e* §6/clan kick <player>");
        sendHelpLine(player, "promote", " §e* §6/clan promote <player>");
        sendHelpLine(player, "demote", " §e* §6/clan demote <player>");
        sendHelpLine(player, "transfer", " §e* §6/clan transfer <player>");
        sendHelpLine(player, "color", " §e* §6/clan color <color>");
        sendHelpLine(player, "edit", " §e* §6/clan edit <new_tag>");
        sendHelpLine(player, "info", " §e* §6/clan info <name|tag>");
        sendHelpLine(player, "members", " §e* §6/clan members <name|tag>");
        sendHelpLine(player, "list", " §e* §6/clan list [page]");
        sendHelpLine(player, "chat", " §e* §6/clan chat");
        player.sendMessage("");
    }

    private void cmdCreate(Player player, String[] args) {
        if (!requirePermission(player, "create")) return;

        if (args.length < 3) {
            player.sendMessage("§cUsage: /clan create <name> <tag>");
            return;
        }

        String name = args[1];
        String tag = args[2].toUpperCase(Locale.ROOT);

        if (tag.length() > 8) {
            player.sendMessage("§cThe tag can be at most 8 characters long!");
            return;
        }

        if (MainManager.getClanManager().isInClan(player.getUniqueId())) {
            player.sendMessage("§cYou are already in a clan!");
            return;
        }

        if (MainManager.getClanManager().createClan(name, tag, player)) {
            player.sendMessage("§aClan §f" + name + " §a[" + tag + "] §acreated successfully!");
            return;
        }

        player.sendMessage("§cName or tag already in use, or invalid value!");
    }

    private void cmdDelete(Player player) {
        if (!requirePermission(player, "delete")) return;

        Clan clan = MainManager.getClanManager().getPlayerClan(player.getUniqueId());
        if (clan == null) {
            player.sendMessage("§cYou are not in a clan!");
            return;
        }

        if (!clan.isLeader(player.getUniqueId())) {
            player.sendMessage("§cOnly the leader can delete the clan!");
            return;
        }

        if (MainManager.getClanManager().deleteClan(clan.getName(), player)) {
            player.sendMessage("§cClan deleted successfully!");
        }
    }

    private void cmdLeave(Player player) {
        if (!requirePermission(player, "leave")) return;

        Clan clan = MainManager.getClanManager().getPlayerClan(player.getUniqueId());
        if (clan == null) {
            player.sendMessage("§cYou are not in a clan!");
            return;
        }

        if (clan.isLeader(player.getUniqueId())) {
            player.sendMessage("§cYou are the leader and cannot leave! Use /clan transfer first.");
            return;
        }

        MainManager.getClanManager().removeMember(clan, player.getUniqueId());
        player.sendMessage("§aYou left the clan!");
    }

    private void cmdInvite(Player player, String[] args) {
        if (!requirePermission(player, "invite")) return;

        if (args.length < 2) {
            player.sendMessage("§cUsage: /clan invite <player>");
            return;
        }

        Clan clan = MainManager.getClanManager().getPlayerClan(player.getUniqueId());
        if (clan == null) {
            player.sendMessage("§cYou are not in a clan!");
            return;
        }

        if (!clan.isMod(player.getUniqueId())) {
            player.sendMessage("§cYou do not have clan permission to invite!");
            return;
        }

        if (!clan.canInvite()) {
            player.sendMessage("§cYour clan has reached the member limit!");
            return;
        }

        Player target = resolvePlayer(args[1]);
        if (target == null) {
            player.sendMessage("§cPlayer not found!");
            return;
        }

        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage("§cYou cannot invite yourself!");
            return;
        }

        if (MainManager.getClanManager().isInClan(target.getUniqueId())) {
            player.sendMessage("§cThat player is already in a clan!");
            return;
        }

        MainManager.getClanManager().sendInvite(clan, player.getUniqueId(), target.getUniqueId());
        String targetColorPrefix = PlaceholderAPI.setPlaceholders(target, "%kaoscore_tag_color%");
        player.sendMessage("§aInvite sent to §f" + targetColorPrefix + target.getDisplayName());
        MainManager.getClanManager().sendInviteNotification(target, clan, player.getUniqueId());
    }

    private void cmdInvites(Player player) {
        if (!requirePermission(player, "invites")) return;
        MainManager.getClanManager().sendInviteList(player, true, false);
    }

    private void cmdAccept(Player player, String[] args) {
        if (!requirePermission(player, "accept")) return;

        if (args.length < 2) {
            player.sendMessage("§cUsage: /clan accept <name|tag>");
            return;
        }

        if (MainManager.getClanManager().isInClan(player.getUniqueId())) {
            player.sendMessage("§cYou are already in a clan!");
            return;
        }

        String clanIdentifier = joinArgs(args, 1);
        if (!MainManager.getClanManager().hasInvite(player.getUniqueId(), clanIdentifier)) {
            player.sendMessage("§cYou do not have an invite for this clan!");
            return;
        }

        Clan clan = MainManager.getClanManager().getClan(clanIdentifier);
        if (clan == null) {
            MainManager.getClanManager().removeInvite(player.getUniqueId(), clanIdentifier);
            player.sendMessage("§cThis clan no longer exists. The invite was removed.");
            return;
        }

        MainManager.getClanManager().addMember(clan, player.getUniqueId(), ClanRole.RECRUIT);
        player.sendMessage("§aYou joined the clan §f" + clan.getName() + " §a" + clan.getFormattedTag());

        for (UUID memberId : clan.getMembers().keySet()) {
            Player member = Bukkit.getPlayer(memberId);
            if (member != null && member.isOnline()) {
                member.sendMessage("§a" + player.getDisplayName() + " joined the clan!");
            }
        }
    }

    private void cmdDeny(Player player, String[] args) {
        if (!requirePermission(player, "deny")) return;

        if (args.length < 2) {
            player.sendMessage("§cUsage: /clan deny <name|tag>");
            return;
        }

        String clanIdentifier = joinArgs(args, 1);
        if (!MainManager.getClanManager().hasInvite(player.getUniqueId(), clanIdentifier)) {
            player.sendMessage("§cYou do not have an invite for this clan!");
            return;
        }

        MainManager.getClanManager().removeInvite(player.getUniqueId(), clanIdentifier);
        player.sendMessage("§cInvite declined.");
    }

    private void cmdKick(Player player, String[] args) {
        if (!requirePermission(player, "kick")) return;

        if (args.length < 2) {
            player.sendMessage("§cUsage: /clan kick <player>");
            return;
        }

        Clan clan = MainManager.getClanManager().getPlayerClan(player.getUniqueId());
        if (clan == null) {
            player.sendMessage("§cYou are not in a clan!");
            return;
        }

        if (!clan.isMod(player.getUniqueId())) {
            player.sendMessage("§cYou do not have clan permission to kick members!");
            return;
        }

        Player target = resolvePlayer(args[1]);
        if (target == null) {
            player.sendMessage("§cPlayer not found!");
            return;
        }

        if (!clan.isMember(target.getUniqueId())) {
            player.sendMessage("§cThat player is not in the clan!");
            return;
        }

        ClanRole targetRole = clan.getRole(target.getUniqueId());
        ClanRole actorRole = clan.getRole(player.getUniqueId());
        if (actorRole == null || !actorRole.canKick(targetRole)) {
            player.sendMessage("§cYou cannot kick this player!");
            return;
        }

        MainManager.getClanManager().removeMember(clan, target.getUniqueId());
        player.sendMessage("§c" + target.getDisplayName() + " was kicked from the clan!");
        target.sendMessage("§cYou were kicked from the clan §f" + clan.getName());
    }

    private void cmdPromote(Player player, String[] args) {
        if (!requirePermission(player, "promote")) return;

        if (args.length < 2) {
            player.sendMessage("§cUse: /clan promote <player>");
            return;
        }

        Clan clan = MainManager.getClanManager().getPlayerClan(player.getUniqueId());
        if (clan == null) {
            player.sendMessage("§cYou are not in a clan!");
            return;
        }

        if (!clan.isLeader(player.getUniqueId())) {
            player.sendMessage("§cOnly the leader can promote!");
            return;
        }

        Player target = resolvePlayer(args[1]);
        if (target == null) {
            player.sendMessage("§cPlayer not found!");
            return;
        }

        if (!clan.isMember(target.getUniqueId())) {
            player.sendMessage("§cPlayer is not in the clan!");
            return;
        }

        MainManager.getClanManager().promote(clan, target.getUniqueId());
        player.sendMessage("§a" + target.getDisplayName() + " has been promoted!");
        target.sendMessage("§aYou have been promoted in the clan!");
    }

    private void cmdDemote(Player player, String[] args) {
        if (!requirePermission(player, "demote")) return;

        if (args.length < 2) {
            player.sendMessage("§cUse: /clan demote <player>");
            return;
        }

        Clan clan = MainManager.getClanManager().getPlayerClan(player.getUniqueId());
        if (clan == null) {
            player.sendMessage("§cYou are not in a clan!");
            return;
        }

        if (!clan.isLeader(player.getUniqueId())) {
            player.sendMessage("§cOnly the leader can demote!");
            return;
        }

        Player target = resolvePlayer(args[1]);
        if (target == null) {
            player.sendMessage("§cPlayer not found!");
            return;
        }

        if (!clan.isMember(target.getUniqueId())) {
            player.sendMessage("§cPlayer is not in the clan!");
            return;
        }

        MainManager.getClanManager().demote(clan, target.getUniqueId());
        player.sendMessage("§c" + target.getDisplayName() + " has been demoted!");
        target.sendMessage("§cYou have been demoted in the clan!");
    }

    private void cmdTransfer(Player player, String[] args) {
        if (!requirePermission(player, "transfer")) return;

        if (args.length < 2) {
            player.sendMessage("§cUse: /clan transfer <player>");
            return;
        }

        Clan clan = MainManager.getClanManager().getPlayerClan(player.getUniqueId());
        if (clan == null) {
            player.sendMessage("§cYou are not in a clan!");
            return;
        }

        if (!clan.isLeader(player.getUniqueId())) {
            player.sendMessage("§cOnly the leader can transfer leadership!");
            return;
        }

        Player target = resolvePlayer(args[1]);
        if (target == null) {
            player.sendMessage("§cPlayer not found!");
            return;
        }

        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage("§cYou are already the leader!");
            return;
        }

        if (!clan.isMember(target.getUniqueId())) {
            player.sendMessage("§cPlayer is not in the clan!");
            return;
        }

        MainManager.getClanManager().transfer(clan, target.getUniqueId());
        player.sendMessage("§cLeadership transferred to §f" + target.getDisplayName());
        target.sendMessage("§aYou are now the clan leader!");
    }

    private void cmdColorTag(Player player, String[] args) {
        if (!requirePermission(player, "color")) return;

        Clan clan = MainManager.getClanManager().getPlayerClan(player.getUniqueId());
        if (clan == null) {
            player.sendMessage("§cYou are not in a clan!");
            return;
        }

        if (!clan.isLeader(player.getUniqueId())) {
            player.sendMessage("§cOnly the leader can change the tag color!");
            return;
        }

        List<TagColorOption> availableColors = getAvailableTagColors(player);
        if (availableColors.isEmpty()) {
            player.sendMessage("§cYou do not have permission for any tag colors.");
            return;
        }

        if (args.length < 2) {
            player.sendMessage("§aAvailable tag colors:");
            TextComponent line = new TextComponent("");

            for (int i = 0; i < availableColors.size(); i++) {
                TagColorOption option = availableColors.get(i);
                TextComponent colorComponent = new TextComponent(option.colorCode + option.name);
                colorComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/clan color " + option.token));
                colorComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§7Click to use color §f" + option.name).create()));
                line.addExtra(colorComponent);

                if (i < availableColors.size() - 1) line.addExtra(new TextComponent("§f, "));
            }

            player.spigot().sendMessage(line);
            return;
        }

        String colorName = normalizeColorName(joinArgs(args, 1));
        TagColorOption selected = null;
        for (TagColorOption option : availableColors) {
            if (option.matches(colorName)) {
                selected = option;
                break;
            }
        }

        if (selected == null) {
            player.sendMessage("§cColor not found or no permission!");
            return;
        }

        MainManager.getClanManager().setTagColor(clan, selected.colorCode);
        player.sendMessage("§aTag color changed to: " + selected.preview());
    }

    private void cmdEditTag(Player player, String[] args) {
        if (!requirePermission(player, "edit")) return;

        if (args.length < 2) {
            player.sendMessage("§cUse: /clan edit <new_tag>");
            return;
        }

        Clan clan = MainManager.getClanManager().getPlayerClan(player.getUniqueId());
        if (clan == null) {
            player.sendMessage("§cYou are not in a clan!");
            return;
        }

        if (!clan.isLeader(player.getUniqueId())) {
            player.sendMessage("§cOnly the leader can edit the tag!");
            return;
        }

        String newTag = args[1].toUpperCase(Locale.ROOT);
        if (newTag.length() > 6) {
            player.sendMessage("§cThe tag must be at most 6 characters long!");
            return;
        }

        if (MainManager.getClanManager().isTagTaken(newTag, clan.getName())) {
            player.sendMessage("§cThis tag is already being used by another clan!");
            return;
        }

        clan.setTag(newTag);
        MainManager.getClanManager().saveClan(clan);
        player.sendMessage("§aTag changed to " + clan.getFormattedTag() + "§a.");
    }

    private void cmdInfo(Player player, String[] args) {
        if (!requirePermission(player, "info")) return;

        if (args.length < 2) {
            player.sendMessage("§cUse: /clan info <name|tag>");
            return;
        }

        Clan clan = MainManager.getClanManager().getClan(joinArgs(args, 1));
        if (clan == null) {
            player.sendMessage("§cClan not found!");
            return;
        }

        SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        player.sendMessage("§6§m+-------------------§6[ §e" + clan.getName() + " §6]§m-------------------+");
        player.sendMessage(" §7Tag: " + clan.getFormattedTag());
        player.sendMessage(" §7Leader: §f" + Bukkit.getOfflinePlayer(clan.getLeader()).getName());
        player.sendMessage(" §7Members: §f" + clan.getMemberCount() + "/" + clan.getMaxMembers());
        player.sendMessage(" §7Created at: §f" + format.format(new Date(clan.getCreationDate())));
        player.sendMessage("§6§m----------------------------------------------------------+");
    }

    private void cmdMembers(Player player, String[] args) {
        if (!requirePermission(player, "members")) return;

        if (args.length < 2) {
            player.sendMessage("§cUse: /clan members <name|tag>");
            return;
        }

        Clan clan = MainManager.getClanManager().getClan(joinArgs(args, 1));
        if (clan == null) {
            player.sendMessage("§cClan not found!");
            return;
        }

        player.sendMessage("§6§m+-------------------§6[ §eMembers of " + clan.getName() + " §6]§m-------------------+");
        for (Map.Entry<UUID, ClanRole> entry : clan.getMembers().entrySet()) {
            String playerName = Bukkit.getOfflinePlayer(entry.getKey()).getName();
            player.sendMessage(" §7- " + entry.getValue().getDisplay() + " §f" + playerName);
        }
        player.sendMessage("§6§m----------------------------------------------------------+");
    }

    private void cmdList(Player player, String[] args) {
        if (!requirePermission(player, "list")) return;

        Collection<Clan> clans = MainManager.getClanManager().getAllClans();
        if (clans.isEmpty()) {
            player.sendMessage("§cNo clans found.");
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
        if (page < 1) page = 1;

        List<Clan> clanList = new ArrayList<>(clans);
        clanList.sort(Comparator.comparing(Clan::getName, String.CASE_INSENSITIVE_ORDER));

        int totalPages = Math.max(1, (clanList.size() + 9) / 10);
        if (page > totalPages) {
            player.sendMessage("§cInvalid page. Max: " + totalPages + ".");
            return;
        }

        int start = (page - 1) * 10;
        int end = Math.min(start + 10, clanList.size());

        player.sendMessage("§6§m+-------------------§6[ §eClans §6]§m-------------------+");
        for (int i = start; i < end; i++) {
            Clan clan = clanList.get(i);
            player.sendMessage(" §7" + (i + 1) + ". " + clan.getFormattedTag() + " §f- " + clan.getName());
        }
        player.sendMessage("§6§m----------------------------------------------------------+");
        player.sendMessage("§7Page §f" + page + " §7of §f" + totalPages);
    }

    private void cmdChat(Player player) {
        if (!requirePermission(player, "chat")) return;

        if (!MainManager.getClanManager().isInClan(player.getUniqueId())) {
            player.sendMessage("§cYou are not in a clan!");
            return;
        }

        player.sendMessage("§6Use §f/cc <message> §6to talk in the clan chat.");
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

    private Player resolvePlayer(String name) {
        if (name == null || name.trim().isEmpty()) return null;
        return DisguiseManager.getOnlinePlayerByName(name);
    }

    private String joinArgs(String[] args, int startIndex) {
        StringBuilder builder = new StringBuilder();
        for (int i = startIndex; i < args.length; i++) {
            if (builder.length() > 0) builder.append(' ');
            builder.append(args[i]);
        }
        return builder.toString().trim();
    }

    private List<TagColorOption> getAvailableTagColors(Player player) {
        List<TagColorOption> colors = new ArrayList<>();
        TagColorOption animated = TagColorOption.animated();
        if (hasPermission(player, animated.permission)) colors.add(animated);

        for (String raw : MainManager.getClanManager().getTagColors()) {
            TagColorOption option = parseTagColor(raw);
            if (option != null && hasPermission(player, option.permission)) colors.add(option);
        }
        colors.sort(Comparator.comparing(option -> option.normalizedName));
        return colors;
    }

    private TagColorOption parseTagColor(String raw) {
        if (raw == null) return null;

        String[] parts = raw.split("\\|", 2);
        String value = parts[0].trim().replace('&', '§');
        if (Clan.ANIMATED_TAG_COLOR.equalsIgnoreCase(value)) {
            String permission = parts.length > 1 && !parts[1].trim().isEmpty()
                    ? parts[1].trim()
                    : "kaoscore.clan.cortag.animated";
            return new TagColorOption(Clan.ANIMATED_TAG_COLOR, "Animated", "animated", "animated", permission);
        }

        if (value.length() < 3 || value.charAt(0) != '§') return null;

        String colorCode = value.substring(0, 2);
        String name = value.substring(2).trim();
        if (name.isEmpty()) return null;

        String normalizedName = normalizeColorName(name);
        String token = normalizedName.replace(' ', '_');
        String permission = parts.length > 1 && !parts[1].trim().isEmpty()
                ? parts[1].trim()
                : "kaoscore.clan.cortag." + token;

        return new TagColorOption(colorCode, name, normalizedName, token, permission);
    }

    private String normalizeColorName(String value) {
        String stripped = ChatColor.stripColor(value == null ? "" : value.replace('&', '§'));
        stripped = stripped.replace('_', ' ').replace('-', ' ').trim().toLowerCase(Locale.ROOT);
        return stripped.replaceAll("\\s+", " ");
    }

    private static final class TagColorOption {
        private final String colorCode;
        private final String name;
        private final String normalizedName;
        private final String token;
        private final String permission;

        private TagColorOption(String colorCode, String name, String normalizedName, String token, String permission) {
            this.colorCode = colorCode;
            this.name = name;
            this.normalizedName = normalizedName;
            this.token = token;
            this.permission = permission;
        }

        private boolean matches(String input) {
            return normalizedName.equalsIgnoreCase(input) || token.equalsIgnoreCase(input.replace(' ', '_'));
        }

        private String preview() {
            if (Clan.ANIMATED_TAG_COLOR.equalsIgnoreCase(colorCode)) return "§f[§cW§6A§eV§aE§f]";
            return colorCode + name;
        }

        private static TagColorOption animated() {
            return new TagColorOption(Clan.ANIMATED_TAG_COLOR, "Animated", "animated", "animated", "kaoscore.clan.cortag.animated");
        }
    }
}