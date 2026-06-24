package com.knowplugins.ysubz.kaoscore.cmd.commands.staff;

import com.knowplugins.ysubz.kaoscore.KaosCore;
import com.knowplugins.ysubz.kaoscore.cmd.Commands;
import com.knowplugins.ysubz.kaoscore.managers.Managers;
import com.knowplugins.ysubz.kaoscore.managers.player.PunishmentManager;
import com.knowplugins.ysubz.kaoscore.utils.punishment.PunishmentEntry;
import com.knowplugins.ysubz.kaoscore.utils.punishment.PunishmentTime;
import com.knowplugins.ysubz.kaoscore.utils.punishment.PunishmentType;
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
        if (!sender.hasPermission("kaoscore.staff.history")) {
            sender.sendMessage("§cYou do not have permission to use this command.");
            ModerationCommandUtil.play(sender, Sound.CLICK);
            return;
        }

        if (args.length < 1) {
            sender.sendMessage("§cUsage: /history <player> [limit]");
            ModerationCommandUtil.play(sender, Sound.CLICK);
            return;
        }

        PunishmentManager punishmentManager = Managers.getPunishmentManager();
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
            sender.sendMessage("§e§lHISTORY ➔ §eNo punishments found for §f" + targetName + "§e.");
            ModerationCommandUtil.play(sender, Sound.CLICK);
            return;
        }

        sender.sendMessage("§6§lHISTORY ➔ §ePunishment history for §f" + targetName + "§e:");
        for (PunishmentEntry entry : history) {
            sender.sendMessage(formatEntry(entry));
        }
        ModerationCommandUtil.play(sender, Sound.NOTE_PLING);
    }

    private String formatEntry(PunishmentEntry entry) {
        if (entry == null) {
            return "§7- Registro inválido";
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
