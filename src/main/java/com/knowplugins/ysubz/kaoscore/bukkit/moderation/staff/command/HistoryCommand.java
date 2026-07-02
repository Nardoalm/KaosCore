package com.knowplugins.ysubz.kaoscore.bukkit.moderation.staff.command;

import com.knowplugins.ysubz.kaoscore.bukkit.KaosCore;
import com.knowplugins.ysubz.kaoscore.bukkit.cmd.Commands;
import com.knowplugins.ysubz.kaoscore.bukkit.moderation.ModerationCommandUtil;
import com.knowplugins.ysubz.kaoscore.bukkit.manager.MainManager;
import com.knowplugins.ysubz.kaoscore.bukkit.manager.player.PunishmentManager;
import com.knowplugins.ysubz.kaoscore.bukkit.util.punishment.PunishmentEntry;
import com.knowplugins.ysubz.kaoscore.bukkit.util.punishment.PunishmentTime;
import com.knowplugins.ysubz.kaoscore.bukkit.util.punishment.PunishmentType;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;

import java.util.List;

public class HistoryCommand extends Commands {

    public HistoryCommand() {
        super("history", "punishhistory", "historico");
    }

    @Override
    public void perform(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission("kaoscore.staff")) {
            sender.sendMessage("§cYou do not have permission to use this command.");
            ModerationCommandUtil.play(sender, Sound.CLICK);
            return;
        }

        if (args.length < 1) {
            sender.sendMessage("§cUsage: /history <player> [limit]");
            ModerationCommandUtil.play(sender, Sound.CLICK);
            return;
        }

        PunishmentManager punishmentManager = MainManager.getPunishmentManager();
        if (punishmentManager == null) {
            sender.sendMessage("§cPunishment system unavailable.");
            ModerationCommandUtil.play(sender, Sound.CLICK);
            return;
        }

        ModerationCommandUtil.Target target = ModerationCommandUtil.resolveTarget(args[0]);
        int limit = 10;
        if (args.length >= 2) {
            try {
                limit = Math.max(1, Math.min(50, Integer.parseInt(args[1])));
            } catch (NumberFormatException ignored) {
                sender.sendMessage("§cInvalid limit.");
                ModerationCommandUtil.play(sender, Sound.CLICK);
                return;
            }
        }

        punishmentManager.getHistory(target.getUuid(), target.getName(), limit).thenAccept(history -> Bukkit.getScheduler().runTask(KaosCore.getInstance(), () -> sendHistory(sender, target.getName(), history)));
    }

    private void sendHistory(CommandSender sender, String targetName, List<PunishmentEntry> history) {
        if (history == null || history.isEmpty()) {
            sender.sendMessage("§eNo punishments found for §f" + targetName + "§e.");
            ModerationCommandUtil.play(sender, Sound.CLICK);
            return;
        }

        sender.sendMessage("§ePunishment history for §f" + targetName + "§e:");
        for (PunishmentEntry entry : history) {
            sender.sendMessage(formatEntry(entry));
        }
        ModerationCommandUtil.play(sender, Sound.NOTE_PLING);
    }

    private String formatEntry(PunishmentEntry entry) {
        if (entry == null) {
            return "§7- Invalid register";
        }

        String type = entry.getType().name();
        String date = PunishmentTime.formatDate(entry.getCreatedAt());
        String reason = entry.getReason() == null ? "No reason provided" : entry.getReason();
        String author = entry.getPunisherName() == null || entry.getPunisherName().trim().isEmpty() ? "CONSOLE" : entry.getPunisherName();
        String extra = "";

        if (entry.getType() == PunishmentType.BAN || entry.getType() == PunishmentType.MUTE) {
            extra = entry.isPermanent() ? " §8(perm)" : " §8(" + PunishmentTime.formatDate(entry.getExpiresAt()) + ")";
            if (!entry.isActive()) {
                extra += " §7[removed]";
            }
        }

        return "§7- §f" + type + " §8| §7" + date + " §8| §7" + author + " §8| §f" + reason + extra;
    }
}
