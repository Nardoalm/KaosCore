package com.knowplugins.ysubz.kaoscore.bukkit.staff.command;

import com.knowplugins.ysubz.kaoscore.bukkit.cmd.Commands;
import com.knowplugins.ysubz.kaoscore.bukkit.cmd.list.staff.ModerationCommandUtil;
import com.knowplugins.ysubz.kaoscore.bukkit.managers.Managers;
import com.knowplugins.ysubz.kaoscore.bukkit.managers.player.PunishmentManager;
import com.knowplugins.ysubz.kaoscore.bukkit.utils.punishment.PunishmentEntry;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class WarnCommand extends Commands {

    public WarnCommand() {
        super("warn", "advertir", "aviso");
    }

    @Override
    public void perform(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission("kaoscore.staff")) {
            sender.sendMessage("§cYou do not have permission to use this command.");
            ModerationCommandUtil.play(sender, Sound.CLICK);
            return;
        }

        if (args.length < 2) {
            sender.sendMessage("§cUsage: /warn <player> <reason>");
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
        if (target.getName().isEmpty()) {
            sender.sendMessage("§cInvalid player.");
            ModerationCommandUtil.play(sender, Sound.CLICK);
            return;
        }

        UUID punisherUuid = ModerationCommandUtil.getSenderUuid(sender);
        String punisherName = ModerationCommandUtil.getSenderName(sender);
        String reason = ModerationCommandUtil.buildReason(args, 1, "No reason provided");

        PunishmentEntry warning = punishmentManager.warn(target.getUuid(), target.getName(), punisherUuid, punisherName, reason, "WARN");
        if (warning == null) {
            sender.sendMessage("§cUnable to register the warning.");
            ModerationCommandUtil.play(sender, Sound.CLICK);
            return;
        }

        sender.sendMessage("§aWarning issued to " + target.getName() + ".");
        ModerationCommandUtil.play(sender, Sound.NOTE_PLING);

        Player onlineTarget = target.getPlayer();
        if (onlineTarget != null && onlineTarget.isOnline()) {
            onlineTarget.sendMessage("§eYou received a warning.");
            onlineTarget.sendMessage("§eReason: §f" + reason);
            onlineTarget.playSound(onlineTarget.getLocation(), Sound.NOTE_BASS, 1.0f, 1.0f);
        }
    }
}
