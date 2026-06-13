package com.knowplugins.ysubz.kaoscore.cmd.commands.staff;

import com.knowplugins.ysubz.kaoscore.cmd.Commands;
import com.knowplugins.ysubz.kaoscore.managers.Managers;
import com.knowplugins.ysubz.kaoscore.managers.player.PunishmentManager;
import com.knowplugins.ysubz.kaoscore.utils.punishment.PunishmentEntry;
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
        if (!sender.hasPermission("kaoscore.staff.warn")) {
            sender.sendMessage("§c§lWARN ➔ §cYou do not have permission to use this command.");
            ModerationCommandUtil.play(sender, Sound.VILLAGER_NO);
            return;
        }

        if (args.length < 2) {
            sender.sendMessage("§c§lWARN ➔ §cUsage: /warn <player> <reason>");
            ModerationCommandUtil.play(sender, Sound.VILLAGER_NO);
            return;
        }

        PunishmentManager punishmentManager = Managers.getPunishmentManager();
        if (punishmentManager == null) {
            sender.sendMessage("§c§lWARN ➔ §cPunishment system unavailable.");
            ModerationCommandUtil.play(sender, Sound.VILLAGER_NO);
            return;
        }

        ModerationCommandUtil.Target target = ModerationCommandUtil.resolveTarget(args[0]);
        if (target.getName().isEmpty()) {
            sender.sendMessage("§c§lWARN ➔ §cInvalid player.");
            ModerationCommandUtil.play(sender, Sound.VILLAGER_NO);
            return;
        }

        UUID punisherUuid = ModerationCommandUtil.getSenderUuid(sender);
        String punisherName = ModerationCommandUtil.getSenderName(sender);
        String reason = ModerationCommandUtil.buildReason(args, 1, "No reason provided");

        PunishmentEntry warning = punishmentManager.warn(target.getUuid(), target.getName(), punisherUuid, punisherName, reason, "WARN");
        if (warning == null) {
            sender.sendMessage("§c§lWARN ➔ §cUnable to register the warning.");
            ModerationCommandUtil.play(sender, Sound.VILLAGER_NO);
            return;
        }

        sender.sendMessage("§a§lWARN ➔ §aWarning issued to " + target.getName() + ".");
        ModerationCommandUtil.play(sender, Sound.LEVEL_UP);

        Player onlineTarget = target.getPlayer();
        if (onlineTarget != null && onlineTarget.isOnline()) {
            onlineTarget.sendMessage("§e§lWARN ➔ §eYou received a warning.");
            onlineTarget.sendMessage("§eReason: §f" + reason);
            onlineTarget.playSound(onlineTarget.getLocation(), Sound.NOTE_BASS, 1.0f, 1.0f);
        }
    }
}
