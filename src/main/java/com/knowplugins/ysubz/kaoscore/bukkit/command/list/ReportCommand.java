package com.knowplugins.ysubz.kaoscore.bukkit.command.list;

import com.knowplugins.ysubz.kaoscore.bukkit.KaosCore;
import com.knowplugins.ysubz.kaoscore.bukkit.command.Commands;
import com.knowplugins.ysubz.kaoscore.bukkit.manager.MainManager;
import com.knowplugins.ysubz.kaoscore.bukkit.disguise.DisguiseManager;
import com.knowplugins.ysubz.kaoscore.bukkit.util.tag.Tag;
import com.lunarclient.apollo.Apollo;
import com.lunarclient.apollo.module.notification.Notification;
import com.lunarclient.apollo.module.notification.NotificationModule;
import com.lunarclient.apollo.recipients.Recipients;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ReportCommand extends Commands {

    private static final String RECEIVE_PERMISSION = "kaoscore.staff.report";
    private static final long COOLDOWN_MS = 15_000L;

    private final Map<UUID, Long> lastReportAt = new ConcurrentHashMap<>();

    public ReportCommand() {
        super("report", "reportar");
        Bukkit.getMessenger().registerOutgoingPluginChannel(JavaPlugin.getPlugin(KaosCore.class), "CB|Binary");
    }

    @Override
    public void perform(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return;
        }

        Player reporter = (Player) sender;

        if (args == null || args.length < 2) {
            reporter.sendMessage("§cUsage: /report <player> <reason>");
            reporter.playSound(reporter.getLocation(), Sound.CLICK, 1.0f, 1.0f);
            return;
        }

        long now = System.currentTimeMillis();
        Long last = lastReportAt.get(reporter.getUniqueId());
        if (last != null && (now - last) < COOLDOWN_MS) {
            reporter.sendMessage("§cPlease wait a moment before sending another report.");
            reporter.playSound(reporter.getLocation(), Sound.CLICK, 1.0f, 1.0f);
            return;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            target = DisguiseManager.getOnlinePlayerByName(args[0]);
        }

        if (target == null || !target.isOnline()) {
            reporter.sendMessage("§cPlayer not found: §f" + args[0]);
            reporter.playSound(reporter.getLocation(), Sound.CLICK, 1.0f, 1.0f);
            return;
        }

        if (target.equals(reporter)) {
            reporter.sendMessage("§cYou cannot report yourself.");
            reporter.playSound(reporter.getLocation(), Sound.CLICK, 1.0f, 1.0f);
            return;
        }

        String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length)).trim();
        if (reason.isEmpty()) {
            reporter.sendMessage("§cEnter a reason.");
            reporter.playSound(reporter.getLocation(), Sound.CLICK, 1.0f, 1.0f);
            return;
        }

        String targetDisplay = target.getName();
        String reporterDisplay = reporter.getName();

        DisguiseManager disguiseManager = MainManager.getDisguiseManager();
        if (disguiseManager != null) {
            if (DisguiseManager.isDisguised(target)) {
                String originalTarget = disguiseManager.getOriginalName(target);
                if (originalTarget != null) {
                    targetDisplay = originalTarget + " §7(" + target.getName() + "§7)";
                }
            }
            if (DisguiseManager.isDisguised(reporter)) {
                String originalReporter = disguiseManager.getOriginalName(reporter);
                if (originalReporter != null) {
                    reporterDisplay = originalReporter;
                }
            }
        }

        String targetColor = getTagColor(target);
        String reporterColor = getTagColor(reporter);

        String base = "§8➥ " + targetColor + targetDisplay
                + " §7was reported by " + reporterColor + reporterDisplay
                + " §7for: §f" + reason + " ";

        TextComponent message = new TextComponent(base);
        TextComponent go = new TextComponent("§a§l[GO]");
        go.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/go " + target.getName()));
        go.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Click to go to " + targetDisplay).create()));
        message.addExtra(go);

        String titleNotification = "§c§lNew Report!";
        String bodyNotification = targetColor + target.getName() + " §ffor §e" + (reason.length() > 20 ? reason.substring(0, 17) + "..." : reason);

        String cleanTitle = ChatColor.stripColor(titleNotification);
        String cleanBody = ChatColor.stripColor(bodyNotification);

        if (cleanTitle.isEmpty()) cleanTitle = "New Report";
        if (cleanBody.isEmpty()) cleanBody = "A player was reported.";

        NotificationModule notificationModule = Apollo.getModuleManager().getModule(NotificationModule.class);

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online == null || !online.isOnline()) {
                continue;
            }
            if (!online.hasPermission(RECEIVE_PERMISSION)) {
                continue;
            }

            online.spigot().sendMessage(message);

            sendCheatBreakerNotification(online, titleNotification, bodyNotification);

            if (notificationModule != null) {
                com.lunarclient.apollo.player.ApolloPlayer apolloPlayer = com.lunarclient.apollo.Apollo.getPlayerManager()
                        .getPlayer(online.getUniqueId())
                        .orElse(null);

                if (apolloPlayer != null) {
                    notificationModule.displayNotification(
                            Recipients.of(Collections.singletonList(apolloPlayer)),
                            Notification.builder()
                                    .title(GsonComponentSerializer.gson().serialize(Component.text(cleanTitle)))
                                    .description(GsonComponentSerializer.gson().serialize(Component.text(cleanBody)))
                                    .displayTime(Duration.ofSeconds(5))
                                    .build()
                    );
                }
            }
        }

        lastReportAt.put(reporter.getUniqueId(), now);
        reporter.sendMessage("§aYour report was sent to staff.");
        reporter.playSound(reporter.getLocation(), Sound.NOTE_PLING, 1.0f, 1.0f);
    }

    private void sendCheatBreakerNotification(Player player, String title, String message) {
        try {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(b);
            out.writeByte(32);
            out.writeUTF(ChatColor.stripColor(title));
            out.writeUTF(ChatColor.stripColor(message));
            out.writeLong(5000L);
            player.sendPluginMessage(JavaPlugin.getPlugin(KaosCore.class), "CB|Binary", b.toByteArray());
        } catch (Exception ignored) {
        }
    }

    private String getTagColor(Player player) {
        KaosCore core = KaosCore.getInstance();
        if (core == null || core.getTagManager() == null || player == null) {
            return "";
        }
        Tag tag = core.getTagManager().getTag(player);
        if (tag == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', tag.getColor() == null ? "" : tag.getColor());
    }
}