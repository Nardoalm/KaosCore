package com.knowplugins.ysubz.kaoscore.cmd.commands.staff;

import com.knowplugins.ysubz.kaoscore.KaosCore;
import com.knowplugins.ysubz.kaoscore.managers.plugin.PunishmentMessageManager;
import com.knowplugins.ysubz.kaoscore.managers.player.DisguiseManager;
import com.knowplugins.ysubz.kaoscore.utils.punishment.PunishmentEntry;
import com.knowplugins.ysubz.kaoscore.utils.punishment.PunishmentTime;
import com.knowplugins.ysubz.kaoscore.utils.disguise.DisguiseApiService;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public final class ModerationCommandUtil {

    private ModerationCommandUtil() {
    }

    public static Target resolveTarget(String input) {
        if (input == null || input.trim().isEmpty()) {
            return new Target(null, "", null);
        }

        Player online = Bukkit.getPlayer(input);
        if (online == null) {
            online = DisguiseManager.getOnlinePlayerByName(input);
        }

        if (online != null) {
            String originalName = DisguiseApiService.getOriginalNick(online);
            if (originalName == null || originalName.trim().isEmpty()) {
                originalName = online.getName();
            }
            return new Target(online.getUniqueId(), originalName, online);
        }

        OfflinePlayer offline = Bukkit.getOfflinePlayer(input);
        if (offline != null && (offline.hasPlayedBefore() || offline.isOnline())) {
            String name = offline.getName() == null || offline.getName().trim().isEmpty() ? input : offline.getName();
            return new Target(offline.getUniqueId(), name, offline.isOnline() ? offline.getPlayer() : null);
        }

        return new Target(null, input, null);
    }

    static UUID getSenderUuid(CommandSender sender) {
        if (sender instanceof Player) {
            return ((Player) sender).getUniqueId();
        }
        return null;
    }

    static String getSenderName(CommandSender sender) {
        return sender == null ? "CONSOLE" : sender.getName();
    }

    static String buildReason(String[] args, int startIndex, String fallback) {
        if (args == null || args.length <= startIndex) {
            return fallback;
        }

        StringBuilder builder = new StringBuilder();
        for (int i = startIndex; i < args.length; i++) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(args[i]);
        }
        String reason = builder.toString().trim();
        return reason.isEmpty() ? fallback : reason;
    }

    public static void play(CommandSender sender, Sound sound) {
        if (!(sender instanceof Player) || sound == null) {
            return;
        }
        Player player = (Player) sender;
        player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
    }

    static String buildBanScreen(PunishmentEntry entry) {
        KaosCore plugin = KaosCore.getInstance();
        PunishmentMessageManager messages = plugin == null ? null : plugin.getPunishmentMessageManager();
        if (messages != null) {
            return messages.buildBanScreen(entry);
        }

        String expires = entry == null || entry.isPermanent() ? "Permanent" : PunishmentTime.formatDate(entry.getExpiresAt());
        String title = entry != null && entry.isPermanent() ? "You have been permanently banned." : "You have been temporarily banned.";
        String reason = entry == null || entry.getReason() == null ? "No reason provided" : entry.getReason();

        return "§c" + title + "\n\n" +
                "§cReason: §7" + reason + "\n" +
                "§cExpires in: §7" + expires + "\n" +
                "§eBuy your unban at: §bwww.freepmc.com.br\n" +
                "§cBanned unfairly? Contact us via\n" +
                "§9discord.gg/freepmc";
    }

    static String buildKickScreen(String reason, String punisherName) {
        KaosCore plugin = KaosCore.getInstance();
        PunishmentMessageManager messages = plugin == null ? null : plugin.getPunishmentMessageManager();
        if (messages != null) {
            return messages.buildKickScreen(reason, punisherName);
        }

        return "§cYou have been kicked from the server.\n\n" +
                "§cReason: §7" + safe(reason) + "\n" +
                "§cStaff: §7" + safe(punisherName);
    }

    static String buildMuteNotice(PunishmentEntry entry) {
        KaosCore plugin = KaosCore.getInstance();
        PunishmentMessageManager messages = plugin == null ? null : plugin.getPunishmentMessageManager();
        if (messages != null) {
            return messages.buildMuteNotice(entry);
        }

        return "§c§lMUTE ➔ §cYou have been muted.\n" +
                "§cReason: §7" + (entry == null ? "No reason provided" : safe(entry.getReason())) + "\n" +
                "§cDuration: §7" + (entry == null ? "Unknown" : PunishmentTime.formatRemaining(entry.getExpiresAt()));
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    public static final class Target {
        private final UUID uuid;
        private final String name;
        private Player player;

        private Target(UUID uuid, String name, Player player) {
            this.uuid = uuid;
            this.name = name == null ? "" : name;
            this.player = player;
        }

        UUID getUuid() {
            return uuid;
        }

        String getName() {
            return name;
        }

        Player getPlayer() {
            return player;
        }

        void setPlayer(Player player){
            this.player = player;
        }
    }
}
