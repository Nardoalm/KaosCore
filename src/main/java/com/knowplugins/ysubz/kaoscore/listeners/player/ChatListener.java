package com.knowplugins.ysubz.kaoscore.listeners.player;

import com.knowplugins.ysubz.kaoscore.KaosCore;
import com.knowplugins.ysubz.kaoscore.managers.Managers;
import com.knowplugins.ysubz.kaoscore.managers.player.PunishmentManager;
import com.knowplugins.ysubz.kaoscore.managers.plugin.PunishmentMessageManager;
import com.knowplugins.ysubz.kaoscore.utils.message.ActionBarUtil;
import com.knowplugins.ysubz.kaoscore.utils.punishment.PunishmentEntry;
import com.knowplugins.ysubz.kaoscore.utils.punishment.PunishmentTime;
import com.knowplugins.ysubz.kaoscore.utils.placeholder.PlaceholderManager;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatListener implements Listener {

    private static final long CHAT_COOLDOWN_MS = 2000L;
    private static final long SPAM_WINDOW_MS = 15000L;
    private static final int SPAM_REPEAT_THRESHOLD = 10;
    private static final String DEFAULT_CHAT_FORMAT = "%kaos_level% %kaoscore_medal_chat_static% %kaoscore_tag_prefix%{player} &8» {message}";
    private static final String DEFAULT_CHAT_COLOR = "&7";
    private static final String DEFAULT_VIP_CHAT_COLOR = "&f";
    private static final String DEFAULT_MENTION_FORMAT = "&a@{nick}";

    private final ConcurrentHashMap<UUID, Long> lastChatAt = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, String> lastNormalizedMessage = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Integer> repeatCount = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long> repeatWindowStart = new ConcurrentHashMap<>();

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        event.setCancelled(true);

        Player p = event.getPlayer();
        if (p == null) {
            return;
        }

        String msg = event.getMessage();
        if (msg == null) {
            return;
        }

        PunishmentManager punishmentManager = Managers.getPunishmentManager();
        if (punishmentManager != null) {
            PunishmentEntry mute = punishmentManager.getActiveMute(p);
            if (mute != null) {
                KaosCore plugin = KaosCore.getInstance();
                PunishmentMessageManager messages = plugin == null ? null : plugin.getPunishmentMessageManager();
                if (messages != null) {
                    for (String line : messages.buildMuteChatLines(mute)) {
                        p.sendMessage(line);
                    }
                } else {
                    p.sendMessage("§c§lMUTE ➔ §cVocê está mutado.");
                    p.sendMessage("§cMotivo: §7" + mute.getReason());
                    p.sendMessage("§cDuração: §7" + PunishmentTime.formatRemaining(mute.getExpiresAt()));
                }
                return;
            }
        }

        long now = System.currentTimeMillis();
        UUID uuid = p.getUniqueId();

        if (!p.hasPermission("kaoscore.vip.cooldown")) {
            Long last = lastChatAt.get(uuid);
            if (last != null && (now - last) < CHAT_COOLDOWN_MS) {
                long remaining = CHAT_COOLDOWN_MS - (now - last);
                long seconds = Math.max(1L, (long) Math.ceil(remaining / 1000.0));
                p.sendMessage("§cAguarde " + seconds + "s para enviar outra mensagem.");
                return;
            }
        }

        String normalized = normalizeForSpam(msg);
        if (!normalized.isEmpty()) {
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
                        if (p.isOnline()) {
                            p.kickPlayer("§cVocê foi expulso por §fSpam§c.");
                        }
                    });
                }
                return;
            }
        }

        lastChatAt.put(uuid, now);

        KaosCore plugin = KaosCore.getInstance();
        FileConfiguration config = plugin == null ? null : plugin.getConfig();
        boolean isVip = p.hasPermission("kaoscore.member.vip");
        String chatColor = colorize(config == null
                ? (isVip ? DEFAULT_VIP_CHAT_COLOR : DEFAULT_CHAT_COLOR)
                : config.getString(isVip ? "chat.vip-color" : "chat.default-color", isVip ? DEFAULT_VIP_CHAT_COLOR : DEFAULT_CHAT_COLOR));

        msg = isVip ? applyVipFormatting(msg, chatColor) : msg;
        MentionResult mentionResult = applyMentions(msg, config);
        msg = mentionResult.message;

        String displayName = PlaceholderManager.getDisplayName(p);
        String formatTemplate = config == null ? DEFAULT_CHAT_FORMAT : config.getString("chat.format", DEFAULT_CHAT_FORMAT);
        String format = formatTemplate
                .replace("{player}", displayName)
                .replace("{message}", chatColor + msg)
                .replace("{msg}", chatColor + msg);
        format = PlaceholderManager.replacePlaceholders(format, p);
        format = PlaceholderAPI.setPlaceholders(p, format);
        format = colorize(format);

        for (Player online : Bukkit.getOnlinePlayers()) {
            online.sendMessage(format);
        }

        if (plugin != null && !mentionResult.mentionedPlayers.isEmpty()) {
            mentionResult.mentionedPlayers.remove(p.getUniqueId());
            if (!mentionResult.mentionedPlayers.isEmpty()) {
                String senderName = displayName;
                Bukkit.getScheduler().runTask(plugin, () -> notifyMentions(mentionResult.mentionedPlayers, senderName, config));
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player p = event.getPlayer();
        if (p == null) {
            return;
        }
        UUID uuid = p.getUniqueId();
        lastChatAt.remove(uuid);
        lastNormalizedMessage.remove(uuid);
        repeatCount.remove(uuid);
        repeatWindowStart.remove(uuid);
    }

    private String normalizeForSpam(String msg) {
        if (msg == null) {
            return "";
        }
        String s = msg.trim().toLowerCase(Locale.ROOT);
        if (s.isEmpty()) {
            return "";
        }
        s = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', s));
        if (s == null) {
            return "";
        }
        s = s.replaceAll("\\s+", " ").trim();
        return s;
    }

    private String applyVipFormatting(String msg, String chatColor) {
        String formatted = ChatColor.translateAlternateColorCodes('&', msg);
        formatted = formatted.replaceAll("(?i)\\bgg\\b", "§3§lGG" + chatColor);
        formatted = formatted.replaceAll("(?i)\\bez\\b", "§a§lEZ!" + chatColor);
        formatted = formatted.replaceAll("(?i)\\blol\\b", "§e§lLOL" + chatColor);
        formatted = formatted.replaceAll("(?i)\\brip\\b", "§8§lRIP" + chatColor);
        formatted = formatted.replaceAll("(?i)\\bnice\\b", "§a§lNICE" + chatColor);
        formatted = formatted.replace("<3", "§c❤");
        formatted = formatted.replaceAll("(?i)\\bbruh\\b", "§7§lBRUH" + chatColor);
        formatted = formatted.replaceAll("(?i)\\bnoob\\b", "§7§lNOOB");
        formatted = formatted.replaceAll("(?i)\\bmid\\b", "§d§lMID");
        formatted = formatted.replaceAll("(?i)\\beasy\\b", "§a§lEASY");
        formatted = formatted.replace(":sword:", "⚔");
        formatted = formatted.replace(":shield:", "⛨");
        formatted = formatted.replace(":diamond:", "◆");
        formatted = formatted.replace(":star:", "✦");
        formatted = formatted.replace(":heart:", "❤");
        formatted = formatted.replace(":skull:", "☠");
        formatted = formatted.replace(":sun:", "☀");
        formatted = formatted.replace(":moon:", "☾");
        formatted = formatted.replaceAll("(?i)\\bwow\\b", "§6§lWOW");
        return formatted;
    }

    private MentionResult applyMentions(String message, FileConfiguration config) {
        if (message == null || message.isEmpty()) {
            return new MentionResult("");
        }

        if (config != null && !config.getBoolean("chat.mentions.enabled", true)) {
            return new MentionResult(message);
        }

        String mentionFormat = colorize(config == null
                ? DEFAULT_MENTION_FORMAT
                : config.getString("chat.mentions.format", DEFAULT_MENTION_FORMAT));

        ArrayList<Player> candidates = new ArrayList<>(Bukkit.getOnlinePlayers());
        candidates.sort(Comparator.comparingInt((Player online) -> getMentionNames(online).stream()
                .filter(Objects::nonNull)
                .mapToInt(String::length)
                .max()
                .orElse(0)).reversed());

        String output = message;
        Set<UUID> mentionedPlayers = new LinkedHashSet<>();
        for (Player online : candidates) {
            for (String nick : getMentionNames(online)) {
                if (nick == null || nick.trim().isEmpty()) {
                    continue;
                }
                String replaced = replaceMention(output, nick, mentionFormat.replace("{nick}", nick));
                if (!replaced.equals(output)) {
                    mentionedPlayers.add(online.getUniqueId());
                    output = replaced;
                }
            }
        }
        return new MentionResult(output, mentionedPlayers);
    }

    private List<String> getMentionNames(Player player) {
        ArrayList<String> names = new ArrayList<>(2);
        String display = PlaceholderManager.getDisplayName(player);
        if (display != null && !display.trim().isEmpty()) {
            names.add(display);
        }
        String account = PlaceholderManager.getAccountName(player);
        if (account != null && !account.trim().isEmpty() && !account.equalsIgnoreCase(display)) {
            names.add(account);
        }
        return names;
    }

    private String replaceMention(String message, String nick, String replacement) {
        String pattern = "(?i)(?<![A-Za-z0-9_])" + Pattern.quote(nick) + "(?![A-Za-z0-9_])";
        Matcher matcher = Pattern.compile(pattern).matcher(message);
        return matcher.replaceAll(Matcher.quoteReplacement(replacement));
    }

    private String colorize(String input) {
        return ChatColor.translateAlternateColorCodes('&', input == null ? "" : input);
    }

    private void notifyMentions(Set<UUID> mentionedPlayers, String senderName, FileConfiguration config) {
        if (mentionedPlayers == null || mentionedPlayers.isEmpty()) {
            return;
        }

        String actionBar = colorize((config == null
                ? "&a{nick} mencionou você!"
                : config.getString("chat.mentions.actionbar", "&a{nick} mencionou você!"))
                .replace("{nick}", senderName));
        Sound sound = resolveSound(config == null ? "NOTE_PLING" : config.getString("chat.mentions.notify-sound", "NOTE_PLING"));
        float volume = (float) (config == null ? 1.0D : config.getDouble("chat.mentions.notify-volume", 1.0D));
        float pitch = (float) (config == null ? 1.0D : config.getDouble("chat.mentions.notify-pitch", 1.0D));

        for (UUID targetId : mentionedPlayers) {
            Player target = Bukkit.getPlayer(targetId);
            if (target == null || !target.isOnline()) {
                continue;
            }
            ActionBarUtil.sendActionBar(target, actionBar);
            if (sound != null) {
                target.playSound(target.getLocation(), sound, volume, pitch);
            }
        }
    }

    private Sound resolveSound(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }

        try {
            return Sound.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ignored) {
            return null;
        }
    }

    private static final class MentionResult {
        private final String message;
        private final Set<UUID> mentionedPlayers;

        private MentionResult(String message) {
            this(message, new LinkedHashSet<>());
        }

        private MentionResult(String message, Set<UUID> mentionedPlayers) {
            this.message = message == null ? "" : message;
            this.mentionedPlayers = mentionedPlayers == null ? new LinkedHashSet<>() : mentionedPlayers;
        }
    }
}
