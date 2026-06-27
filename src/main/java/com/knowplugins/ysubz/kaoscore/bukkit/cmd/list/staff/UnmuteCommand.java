package com.knowplugins.ysubz.kaoscore.bukkit.cmd.list.staff;

import com.knowplugins.ysubz.kaoscore.bukkit.cmd.Commands;
import com.knowplugins.ysubz.kaoscore.bukkit.managers.Managers;
import com.knowplugins.ysubz.kaoscore.bukkit.managers.player.PunishmentManager;
import com.knowplugins.ysubz.kaoscore.bukkit.utils.punishment.PunishmentType;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class UnmuteCommand extends Commands {

    public UnmuteCommand() {
        super("unmute", "desmutar");
    }

    @Override
    public void perform(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission("kaoscore.staff")) {
            sender.sendMessage("§cYou do not have permission to use this command.");
            ModerationCommandUtil.play(sender, Sound.CLICK);
            return;
        }

        if (args.length < 1) {
            sender.sendMessage("§cUsage: /unmute <player>");
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
        UUID senderUuid = ModerationCommandUtil.getSenderUuid(sender);
        String senderName = ModerationCommandUtil.getSenderName(sender);

        boolean removed = punishmentManager.clearActive(PunishmentType.MUTE, target.getUuid(), target.getName(), senderUuid, senderName, "Mute removido manualmente");
        if (!removed) {
            sender.sendMessage("§cThat player is not muted.");
            ModerationCommandUtil.play(sender, Sound.CLICK);
            return;
        }

        sender.sendMessage("§aMute removed from " + target.getName() + ".");
        ModerationCommandUtil.play(sender, Sound.NOTE_PLING);

        Player onlineTarget = target.getPlayer();
        if (onlineTarget != null && onlineTarget.isOnline()) {
            onlineTarget.sendMessage("§aYour mute has been removed.");
            onlineTarget.playSound(onlineTarget.getLocation(), Sound.NOTE_PLING, 1.0f, 1.0f);
        }
    }
}
