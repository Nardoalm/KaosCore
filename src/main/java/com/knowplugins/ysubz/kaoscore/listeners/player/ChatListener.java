package com.knowplugins.ysubz.kaoscore.listeners.player;

import com.knowplugins.ysubz.kaoscore.KaosCore;
import com.knowplugins.ysubz.kaoscore.managers.Managers;
import com.knowplugins.ysubz.kaoscore.managers.player.PunishmentManager;
import com.knowplugins.ysubz.kaoscore.managers.plugin.PunishmentMessageManager;
import com.knowplugins.ysubz.kaoscore.utils.punishment.PunishmentEntry;
import com.knowplugins.ysubz.kaoscore.utils.punishment.PunishmentTime;
import com.knowplugins.ysubz.kaoscore.utils.placeholder.PlaceholderManager;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ChatListener implements Listener {

    private static final long CHAT_COOLDOWN_MS = 2000L;
    private static final long SPAM_WINDOW_MS = 15000L;
    private static final int SPAM_REPEAT_THRESHOLD = 10;

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

        // Cooldown 2s (bypass: kaoscore.vip.cooldown)
        if (!p.hasPermission("kaoscore.vip.cooldown")) {
            Long last = lastChatAt.get(uuid);
            if (last != null && (now - last) < CHAT_COOLDOWN_MS) {
                long remaining = CHAT_COOLDOWN_MS - (now - last);
                long seconds = Math.max(1L, (long) Math.ceil(remaining / 1000.0));
                p.sendMessage("§cAguarde " + seconds + "s para enviar outra mensagem.");
                return;
            }
        }

        // Anti-spam: mesma mensagem repetida muitas vezes em pouco tempo -> kick por Spam
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

        boolean isVip = p.hasPermission("kaoscore.member.vip");
        String chatColor = isVip ? "§f" : "§7";

        if (isVip) {
            msg = ChatColor.translateAlternateColorCodes('&', msg);
            msg = msg.replaceAll("(?i)\\bgg\\b", "§3§lGG" + chatColor);
            msg = msg.replaceAll("(?i)\\bez\\b", "§a§lEZ!" + chatColor);
            msg = msg.replaceAll("(?i)\\blol\\b", "§e§lLOL" + chatColor);
            msg = msg.replaceAll("(?i)\\brip\\b", "§8§lRIP" + chatColor);
            msg = msg.replaceAll("(?i)\\bnice\\b", "§a§lNICE" + chatColor);
            msg = msg.replace(":)", "§e☺");
            msg = msg.replace("<3", "§c❤");
            msg = msg.replace(":(", "§7☹");
            msg = msg.replace(":o", "§6☻");
            msg = msg.replaceAll("(?i)\\bbruh\\b", "§7§lBRUH" + chatColor);
            msg = msg.replaceAll("(?i)\\bnoob\\b", "§7§lNOOB");
            msg = msg.replaceAll("(?i)\\bmid\\b", "§d§lMID");
            msg = msg.replaceAll("(?i)\\beasy\\b", "§a§lEASY");
            msg = msg.replace(":sword:", "⚔");
            msg = msg.replace(":shield:", "⛨");
            msg = msg.replace(":diamond:", "◆");
            msg = msg.replace(":star:", "✦");
            msg = msg.replace(":heart:", "❤");
            msg = msg.replace(":skull:", "☠");
            msg = msg.replace(":sun:", "☀");
            msg = msg.replace(":moon:", "☾");
            msg = msg.replaceAll("(?i)\\bwow\\b", "§6§lWOW");
        }
        String medal = PlaceholderAPI.setPlaceholders(p, "%kaoscore_medal_chat_static%");
        String tag = PlaceholderAPI.setPlaceholders(p, "%kaoscore_tag_prefix%");
        String displayName = PlaceholderManager.getDisplayName(p);

        String format = medal + tag + displayName + " §8» " + chatColor + msg;

        for (Player online : Bukkit.getOnlinePlayers()) {
            online.sendMessage(format);
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
        // Remove formatting so color variations don't bypass spam checks.
        s = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', s));
        if (s == null) {
            return "";
        }
        s = s.replaceAll("\\s+", " ").trim();
        return s;
    }
}

