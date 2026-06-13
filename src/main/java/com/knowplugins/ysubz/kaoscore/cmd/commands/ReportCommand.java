package com.knowplugins.ysubz.kaoscore.cmd.commands;

import com.knowplugins.ysubz.kaoscore.KaosCore;
import com.knowplugins.ysubz.kaoscore.cmd.Commands;
import com.knowplugins.ysubz.kaoscore.managers.player.DisguiseManager;
import com.knowplugins.ysubz.kaoscore.utils.tag.Tag;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ReportCommand extends Commands {

    private static final String RECEIVE_PERMISSION = "kaoscore.staff.report";
    private static final long COOLDOWN_MS = 15_000L;

    private final Map<UUID, Long> lastReportAt = new ConcurrentHashMap<>();

    public ReportCommand() {
        super("report", "reportar");
    }

    @Override
    public void perform(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c§lREPORT ➔ §cOnly players can use this command.");
            return;
        }

        Player reporter = (Player) sender;

        if (args == null || args.length < 2) {
            reporter.sendMessage("§c§lREPORT ➔ §cUsage: /report <player> <reason>");
            reporter.playSound(reporter.getLocation(), Sound.VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        long now = System.currentTimeMillis();
        Long last = lastReportAt.get(reporter.getUniqueId());
        if (last != null && (now - last) < COOLDOWN_MS) {
            reporter.sendMessage("§c§lREPORT ➔ §cPlease wait a moment before sending another report.");
            reporter.playSound(reporter.getLocation(), Sound.VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            target = DisguiseManager.getOnlinePlayerByName(args[0]);
        }

        if (target == null || !target.isOnline()) {
            reporter.sendMessage("§c§lREPORT ➔ §cPlayer not found: §f" + args[0]);
            reporter.playSound(reporter.getLocation(), Sound.VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        if (target.equals(reporter)) {
            reporter.sendMessage("§c§lREPORT ➔ §cYou cannot report yourself.");
            reporter.playSound(reporter.getLocation(), Sound.VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        String reason = join(args, 1).trim();
        if (reason.isEmpty()) {
            reporter.sendMessage("§c§lREPORT ➔ §cEnter a reason.");
            reporter.playSound(reporter.getLocation(), Sound.VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        String targetColor = getTagColor(target);
        String reporterColor = getTagColor(reporter);

        String base = "§8➥ " + targetColor + target.getName()
                + " §7was reported by " + reporterColor + reporter.getName()
                + " §7for: §f" + reason + " ";

        TextComponent message = new TextComponent(base);
        TextComponent go = new TextComponent("§a§l[IR COM GO]");
        go.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/go " + target.getName()));
        go.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Click to go to " + target.getName()).create()));
        message.addExtra(go);

        int sent = 0;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online == null || !online.isOnline()) {
                continue;
            }
            if (!online.hasPermission(RECEIVE_PERMISSION)) {
                continue;
            }
            online.spigot().sendMessage(message);
            sent++;
        }

        lastReportAt.put(reporter.getUniqueId(), now);

        reporter.sendMessage(sent > 0
                ? "§a§lREPORT ➔ §aYour report was sent to staff."
                : "§e§lREPORT ➔ §eNo staff are online right now. Your report was recorded.");
        reporter.playSound(reporter.getLocation(), Sound.NOTE_PLING, 1.0f, 1.0f);
    }

    private String join(String[] args, int start) {
        if (args == null || start >= args.length) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < args.length; i++) {
            if (args[i] == null) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(args[i]);
        }
        return sb.toString();
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
