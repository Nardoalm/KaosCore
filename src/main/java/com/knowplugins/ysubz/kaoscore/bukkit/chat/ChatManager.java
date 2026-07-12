package com.knowplugins.ysubz.kaoscore.bukkit.chat;

import com.knowplugins.ysubz.kaoscore.bukkit.KaosCore;
import com.knowplugins.ysubz.kaoscore.bukkit.util.disguise.api.DisguiseApiService;
import com.knowplugins.ysubz.kaoscore.bukkit.manager.MainManager;
import com.knowplugins.ysubz.kaoscore.bukkit.manager.player.PunishmentManager;
import com.knowplugins.ysubz.kaoscore.bukkit.manager.plugin.PunishmentMessageManager;
import com.knowplugins.ysubz.kaoscore.bukkit.util.medal.Medal;
import com.knowplugins.ysubz.kaoscore.bukkit.util.message.ActionBarUtil;
import com.knowplugins.ysubz.kaoscore.bukkit.util.placeholder.PlaceholderManager;
import com.knowplugins.ysubz.kaoscore.bukkit.util.punishment.PunishmentEntry;
import com.knowplugins.ysubz.kaoscore.bukkit.util.punishment.PunishmentTime;
import com.knowplugins.ysubz.kaoscore.bukkit.util.tag.Tag;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import live.pvphard.practice.HardPractice;
import live.pvphard.practice.common.text.LevelBadgeUtil;
import live.pvphard.practice.core.profile.Profile;
import live.pvphard.practice.core.profile.ProfileService;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatManager {

    public static final long CHAT_COOLDOWN_MS = 500L;
    public static final long SPAM_WINDOW_MS = 15000L;
    public static final int SPAM_REPEAT_THRESHOLD = 10;
    public static final String DEFAULT_CHAT_COLOR = "&7";
    public static final String DEFAULT_CHAT_FORMAT = "{level} {medal}{prefix}{player}: {message}";
    public static final String DEFAULT_VIP_CHAT_COLOR = "&f";
    public static final String DEFAULT_MENTION_FORMAT = "&b@{nick}";

    private final ConcurrentHashMap<UUID, Long> lastChatAt = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, String> lastNormalizedMessage = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Integer> repeatCount = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long> repeatWindowStart = new ConcurrentHashMap<>();

    public void processChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;

        String message = event.getMessage();
        if (message == null) return;

        if (isMuted(player)) return;

        long now = System.currentTimeMillis();
        UUID uuid = player.getUniqueId();

        if (isCooldownActive(player, uuid, now)) return;

        if (isSpamming(player, uuid, message, now)) return;

        lastChatAt.put(uuid, now);

        KaosCore plugin = KaosCore.getInstance();
        FileConfiguration config = plugin == null ? null : plugin.getConfig();

        boolean isVip = player.hasPermission("kaoscore.vip");
        if (plugin != null && plugin.getTagManager() != null) {
            Tag activeTag = plugin.getTagManager().getTag(player);
            if (activeTag != null && activeTag.getId().equalsIgnoreCase("member")) {
                isVip = false;
            }
        }
        String chatColor = resolveChatColor(config, isVip);

        message = isVip ? applyVipFormatting(message, chatColor) : message;

        MentionResult mentionResult = applyMentions(message, config);
        message = mentionResult.getMessage();

        String displayName = PlaceholderManager.getDisplayName(player);
        String formatTemplate = config != null ? config.getString("chat.format", DEFAULT_CHAT_FORMAT) : DEFAULT_CHAT_FORMAT;

        String level = resolveLevel(player);
        String tagPrefix = resolveTagPrefix(plugin, player);
        String medal = resolveMedal(plugin, player);

        if (DisguiseApiService.isDisguised(player)) {
            level = "&7[0✺]";
            medal = "";
        }

        String formatted = formatTemplate
                .replace("{level}", level)
                .replace("{medal}", medal)
                .replace("{prefix}", tagPrefix)
                .replace("{color}", chatColor)
                .replace("{player}", displayName)
                .replace("{message}", chatColor + message)
                .replace("{msg}", chatColor + message);

        formatted = PlaceholderManager.replacePlaceholders(formatted, player);
        formatted = PlaceholderAPI.setPlaceholders(player, formatted);
        formatted = colorize(formatted);

        for (Player online : Bukkit.getOnlinePlayers()) {
            online.sendMessage(formatted);
        }

        notifyMentions(plugin, mentionResult.getMentionedPlayers(), player.getUniqueId(), displayName, config);
    }

    public void cleanup(UUID uuid) {
        lastChatAt.remove(uuid);
        lastNormalizedMessage.remove(uuid);
        repeatCount.remove(uuid);
        repeatWindowStart.remove(uuid);
    }

    private boolean isMuted(Player player) {
        PunishmentManager punishmentManager = MainManager.getPunishmentManager();
        if (punishmentManager == null) return false;

        PunishmentEntry mute = punishmentManager.getActiveMute(player);
        if (mute == null) return false;

        KaosCore plugin = KaosCore.getInstance();
        PunishmentMessageManager messages = plugin == null ? null : plugin.getPunishmentMessageManager();

        if (messages != null) {
            for (String line : messages.buildMuteChatLines(mute)) {
                player.sendMessage(line);
            }
        } else {
            player.sendMessage("§cYou are muted.");
            player.sendMessage("§cReason: §7" + mute.getReason());
            player.sendMessage("§cDuration: §7" + PunishmentTime.formatRemaining(mute.getExpiresAt()));
        }
        return true;
    }

    private boolean isCooldownActive(Player player, UUID uuid, long now) {
        if (player.hasPermission("kaoscore.cooldown")) return false;

        Long last = lastChatAt.get(uuid);
        if (last != null && (now - last) < CHAT_COOLDOWN_MS) {
            player.sendMessage("§cPlease wait to send another message.");
            return true;
        }
        return false;
    }

    private boolean isSpamming(Player player, UUID uuid, String message, long now) {
        String normalized = normalizeForSpam(message);
        if (normalized.isEmpty()) return false;

        String prev = lastNormalizedMessage.get(uuid);
        long windowStart = repeatWindowStart.getOrDefault(uuid, now);
        int count;

        if (prev != null && prev.equals(normalized)) {
            if ((now - windowStart) > SPAM_WINDOW_MS) {
                windowStart = now;
                count = 1;
            } else {
                count = repeatCount.getOrDefault(uuid, 1) + 1;
            }
        } else {
            windowStart = now;
            count = 1;
        }

        lastNormalizedMessage.put(uuid, normalized);
        repeatWindowStart.put(uuid, windowStart);
        repeatCount.put(uuid, count);

        if (count >= SPAM_REPEAT_THRESHOLD) {
            KaosCore plugin = KaosCore.getInstance();
            if (plugin != null) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (player.isOnline()) player.kickPlayer("§cDo not spam.");
                });
            }
            return true;
        }
        return false;
    }

    private String resolveChatColor(FileConfiguration config, boolean isVip) {
        String defaultColor = isVip ? DEFAULT_VIP_CHAT_COLOR : DEFAULT_CHAT_COLOR;
        String path = isVip ? "chat.vip-color" : "chat.default-color";
        return config != null ? colorize(config.getString(path, defaultColor)) : colorize(defaultColor);
    }

    private String resolveLevel(Player player) {
        if (Bukkit.getPluginManager().isPluginEnabled("HardPractice")) {
            Profile profile = new Profile(player.getUniqueId(), player.getName());
            profile.load();
            ProfileService profileService = HardPractice.getInstance().getService(ProfileService.class);
            profileService.getProfile(player.getUniqueId());
            return LevelBadgeUtil.getDisplayBadge(player, profile.getProfileData().getExperience());
        }
        return "&7[0✺]";
    }

    private String resolveTagPrefix(KaosCore plugin, Player player) {
        if (plugin != null && plugin.getTagManager() != null) {
            Tag tag = plugin.getTagManager().getTag(player);
            if (tag != null) return tag.getPrefix();
        }
        return "";
    }

    private String resolveMedal(KaosCore plugin, Player player) {
        if (plugin != null && plugin.getMedalManager() != null) {
            Medal medal = plugin.getMedalManager().getMedal(player);
            if (medal != null && medal.getStaticChat() != null) {
                return medal.getStaticChat();
            }
        }
        return "";
    }

    private void notifyMentions(KaosCore plugin, Set<UUID> mentionedPlayers, UUID senderUuid, String senderName, FileConfiguration config) {
        if (plugin == null || mentionedPlayers == null || mentionedPlayers.isEmpty()) return;

        Set<UUID> toNotify = new HashSet<>(mentionedPlayers);
        toNotify.remove(senderUuid);
        if (toNotify.isEmpty()) return;

        Bukkit.getScheduler().runTask(plugin, () ->
                notifyMentions(toNotify, senderName, config));
    }

    public static String normalizeForSpam(String msg) {
        if (msg == null) return "";
        String s = msg.trim().toLowerCase(Locale.ROOT);
        if (s.isEmpty()) return "";
        s = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', s));
        if (s == null) return "";
        return s.replaceAll("\\s+", " ").trim();
    }

    public static String applyVipFormatting(String msg, String chatColor) {
        String formatted = ChatColor.translateAlternateColorCodes('&', msg);
        formatted = formatted.replaceAll("(?i)\\bgg\\b", "§3§lGG!" + chatColor);
        formatted = formatted.replaceAll("(?i)\\bez\\b", "§a§lEZ!" + chatColor);
        formatted = formatted.replaceAll("(?i)\\blol\\b", "§e§lLOL!" + chatColor);
        formatted = formatted.replaceAll("(?i)\\brip\\b", "§8§lRIP!" + chatColor);
        formatted = formatted.replaceAll("(?i)\\bnice\\b", "§a§lNICE!" + chatColor);
        formatted = formatted.replace("<3", "§c❤");
        formatted = formatted.replaceAll("(?i)\\bbruh\\b", "§7§lBRUH!" + chatColor);
        formatted = formatted.replaceAll("(?i)\\bnoob\\b", "§7§lNOOB!");
        formatted = formatted.replaceAll("(?i)\\bmid\\b", "§d§lMID!");
        formatted = formatted.replaceAll("(?i)\\beasy\\b", "§a§lEASY!");
        formatted = formatted.replace(":sword:", "⚔")
                .replace(":shield:", "⛨")
                .replace(":diamond:", "◆")
                .replace(":star:", "✦")
                .replace(":heart:", "❤")
                .replace(":skull:", "☠")
                .replace(":sun:", "☀")
                .replace(":moon:", "☾")
                .replaceAll("(?i)\\bwow\\b", "§6§lWOW");
        return formatted;
    }

    public static MentionResult applyMentions(String message, FileConfiguration config) {
        if (message == null || message.isEmpty()) return new MentionResult("");

        if (config != null && !config.getBoolean("chat.mentions.enabled", true)) {
            return new MentionResult(message);
        }

        String mentionFormat = colorize(config == null
                ? DEFAULT_MENTION_FORMAT
                : config.getString("chat.mentions.format", DEFAULT_MENTION_FORMAT));

        List<Player> candidates = new ArrayList<>(Bukkit.getOnlinePlayers());
        candidates.sort(Comparator.comparingInt((Player p) -> getMentionNames(p).stream()
                .filter(Objects::nonNull)
                .mapToInt(String::length)
                .max()
                .orElse(0)).reversed());

        String output = message;
        Set<UUID> mentioned = new LinkedHashSet<>();
        for (Player online : candidates) {
            for (String nick : getMentionNames(online)) {
                if (nick == null || nick.trim().isEmpty()) continue;
                String replaced = replaceMention(output, nick, mentionFormat.replace("{nick}", nick));
                if (!replaced.equals(output)) {
                    mentioned.add(online.getUniqueId());
                    output = replaced;
                }
            }
        }
        return new MentionResult(output, mentioned);
    }

    public static String colorize(String input) {
        return ChatColor.translateAlternateColorCodes('&', input == null ? "" : input);
    }

    public static void notifyMentions(Set<UUID> mentionedPlayers, String senderName, FileConfiguration config) {
        if (mentionedPlayers == null || mentionedPlayers.isEmpty()) return;

        String actionBar = colorize((config == null
                ? "&a{nick} mencionou você!"
                : config.getString("chat.mentions.actionbar", "&a{nick} mencionou você!"))
                .replace("{nick}", senderName));

        Sound sound = resolveSound(config == null ? "NOTE_PLING" : config.getString("chat.mentions.notify-sound", "NOTE_PLING"));
        float volume = (float) (config == null ? 1.0D : config.getDouble("chat.mentions.notify-volume", 1.0D));
        float pitch = (float) (config == null ? 1.0D : config.getDouble("chat.mentions.notify-pitch", 1.0D));

        for (UUID id : mentionedPlayers) {
            Player target = Bukkit.getPlayer(id);
            if (target == null || !target.isOnline()) continue;
            ActionBarUtil.sendActionBar(target, actionBar);
            if (sound != null) target.playSound(target.getLocation(), sound, volume, pitch);
        }
    }

    private static List<String> getMentionNames(Player player) {
        List<String> names = new ArrayList<>(2);
        String display = PlaceholderManager.getDisplayName(player);
        if (display != null && !display.trim().isEmpty()) names.add(display);
        String account = PlaceholderManager.getAccountName(player);
        if (account != null && !account.trim().isEmpty() && !account.equalsIgnoreCase(display)) {
            names.add(account);
        }
        return names;
    }

    private static String replaceMention(String message, String nick, String replacement) {
        String pattern = "(?i)(?<![A-Za-z0-9_])" + Pattern.quote(nick) + "(?![A-Za-z0-9_])";
        return Pattern.compile(pattern).matcher(message)
                .replaceAll(Matcher.quoteReplacement(replacement));
    }

    private static Sound resolveSound(String raw) {
        if (raw == null || raw.trim().isEmpty()) return null;
        try {
            return Sound.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ignored) {
            return null;
        }
    }

    public static class MentionResult {
        private final String message;
        private final Set<UUID> mentionedPlayers;

        public MentionResult(String message) {
            this(message, new LinkedHashSet<>());
        }

        public MentionResult(String message, Set<UUID> mentionedPlayers) {
            this.message = message == null ? "" : message;
            this.mentionedPlayers = mentionedPlayers == null ? new LinkedHashSet<>() : mentionedPlayers;
        }

        public String getMessage() { return message; }
        public Set<UUID> getMentionedPlayers() { return mentionedPlayers; }
    }
}