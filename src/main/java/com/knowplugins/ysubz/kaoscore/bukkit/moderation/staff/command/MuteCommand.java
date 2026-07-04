package com.knowplugins.ysubz.kaoscore.bukkit.moderation.staff.command;

import com.knowplugins.ysubz.kaoscore.bukkit.command.Commands;
import com.knowplugins.ysubz.kaoscore.bukkit.moderation.ModerationCommandUtil;
import com.knowplugins.ysubz.kaoscore.bukkit.manager.MainManager;
import com.knowplugins.ysubz.kaoscore.bukkit.disguise.DisguiseManager;
import com.knowplugins.ysubz.kaoscore.bukkit.manager.player.PunishmentManager;
import com.knowplugins.ysubz.kaoscore.bukkit.util.punishment.PunishmentEntry;
import com.knowplugins.ysubz.kaoscore.bukkit.util.punishment.PunishmentTime;
import com.knowplugins.ysubz.kaoscore.bukkit.util.punishment.PunishmentType;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class MuteCommand extends Commands {

    public MuteCommand() {
        super("mute", "tempmute", "silenciar");
    }

    @Override
    public void perform(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission("kaoscore.staff")) {
            sender.sendMessage("§cYou do not have permission to use this command.");
            ModerationCommandUtil.play(sender, Sound.CLICK);
            return;
        }

        if (args.length < 2) {
            sender.sendMessage("§cUsage: /mute <player> <duration|perm> [reason]");
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

        if (target.getPlayer() == null) {
            target.setPlayer(DisguiseManager.getOnlinePlayerByName(args[0]));
        }

        if (target.getName().isEmpty()) {
            sender.sendMessage("§cInvalid player.");
            ModerationCommandUtil.play(sender, Sound.CLICK);
            return;
        }

        long duration = PunishmentTime.parseDuration(args[1]);
        if (duration == Long.MIN_VALUE) {
            sender.sendMessage("§cInvalid duration. Example: 30d, 12h, 30m, or perm.");
            ModerationCommandUtil.play(sender, Sound.CLICK);
            return;
        }

        String reason = ModerationCommandUtil.buildReason(args, 2, "No reason provided");
        UUID punisherUuid = ModerationCommandUtil.getSenderUuid(sender);
        String punisherName = ModerationCommandUtil.getSenderName(sender);
        long expiresAt = duration == 0L ? 0L : System.currentTimeMillis() + duration;

        PunishmentEntry punishment = punishmentManager.punish(PunishmentType.MUTE, target.getUuid(), target.getName(), punisherUuid, punisherName, reason, expiresAt, "MUTE");
        if (punishment == null) {
            sender.sendMessage("§cUnable to apply the mute.");
            ModerationCommandUtil.play(sender, Sound.CLICK);
            return;
        }

        sender.sendMessage("§a" + target.getName() + " was muted" + (punishment.isPermanent() ? " permanently" : " for " + PunishmentTime.formatRemaining(punishment.getExpiresAt())) + ".");
        ModerationCommandUtil.play(sender, Sound.NOTE_PLING);

        Player onlineTarget = target.getPlayer();
        if (onlineTarget != null && onlineTarget.isOnline()) {
            onlineTarget.sendMessage(ModerationCommandUtil.buildMuteNotice(punishment));
            onlineTarget.playSound(onlineTarget.getLocation(), Sound.CLICK, 1.0f, 1.0f);
        }
    }
}
