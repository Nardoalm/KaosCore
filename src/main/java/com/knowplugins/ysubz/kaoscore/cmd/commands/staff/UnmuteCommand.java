package com.knowplugins.ysubz.kaoscore.cmd.commands.staff;

import com.knowplugins.ysubz.kaoscore.cmd.Commands;
import com.knowplugins.ysubz.kaoscore.managers.Managers;
import com.knowplugins.ysubz.kaoscore.managers.player.PunishmentManager;
import com.knowplugins.ysubz.kaoscore.utils.punishment.PunishmentType;
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
        if (!sender.hasPermission("kaoscore.staff.mutar")) {
            sender.sendMessage("§cYou do not have permission to use this command.");
            ModerationCommandUtil.play(sender, Sound.VILLAGER_NO);
            return;
        }

        if (args.length < 1) {
            sender.sendMessage("§cUsage: /unmute <player>");
            ModerationCommandUtil.play(sender, Sound.VILLAGER_NO);
            return;
        }

        PunishmentManager punishmentManager = Managers.getPunishmentManager();
        if (punishmentManager == null) {
            sender.sendMessage("§cPunishment system unavailable.");
            ModerationCommandUtil.play(sender, Sound.VILLAGER_NO);
            return;
        }

        ModerationCommandUtil.Target target = ModerationCommandUtil.resolveTarget(args[0]);
        UUID senderUuid = ModerationCommandUtil.getSenderUuid(sender);
        String senderName = ModerationCommandUtil.getSenderName(sender);

        boolean removed = punishmentManager.clearActive(PunishmentType.MUTE, target.getUuid(), target.getName(), senderUuid, senderName, "Mute removido manualmente");
        if (!removed) {
            sender.sendMessage("§cThat player is not muted.");
            ModerationCommandUtil.play(sender, Sound.VILLAGER_NO);
            return;
        }

        sender.sendMessage("§a§lUNMUTE ➔ §aMute removed from " + target.getName() + ".");
        ModerationCommandUtil.play(sender, Sound.LEVEL_UP);

        Player onlineTarget = target.getPlayer();
        if (onlineTarget != null && onlineTarget.isOnline()) {
            onlineTarget.sendMessage("§a§lUNMUTE ➔ §aYour mute has been removed.");
            onlineTarget.playSound(onlineTarget.getLocation(), Sound.LEVEL_UP, 1.0f, 1.0f);
        }
    }
}
