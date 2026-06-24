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
            sender.sendMessage("§c§lUNMUTE ➔ §cVocê não tem permissão para usar esse comando.");
            ModerationCommandUtil.play(sender, Sound.CLICK);
            return;
        }

        if (args.length < 1) {
            sender.sendMessage("§c§lUNMUTE ➔ §cUso correto: /unmute <jogador>");
            ModerationCommandUtil.play(sender, Sound.CLICK);
            return;
        }

        PunishmentManager punishmentManager = Managers.getPunishmentManager();
        if (punishmentManager == null) {
            sender.sendMessage("§c§lUNMUTE ➔ §cSistema de punições indisponível.");
            ModerationCommandUtil.play(sender, Sound.CLICK);
            return;
        }

        ModerationCommandUtil.Target target = ModerationCommandUtil.resolveTarget(args[0]);
        UUID senderUuid = ModerationCommandUtil.getSenderUuid(sender);
        String senderName = ModerationCommandUtil.getSenderName(sender);

        boolean removed = punishmentManager.clearActive(PunishmentType.MUTE, target.getUuid(), target.getName(), senderUuid, senderName, "Mute removido manualmente");
        if (!removed) {
            sender.sendMessage("§c§lUNMUTE ➔ §cEsse jogador não está mutado.");
            ModerationCommandUtil.play(sender, Sound.CLICK);
            return;
        }

        sender.sendMessage("§a§lUNMUTE ➔ §aMute removido de " + target.getName() + ".");
        ModerationCommandUtil.play(sender, Sound.LEVEL_UP);

        Player onlineTarget = target.getPlayer();
        if (onlineTarget != null && onlineTarget.isOnline()) {
            onlineTarget.sendMessage("§a§lUNMUTE ➔ §aSeu mute foi removido.");
            onlineTarget.playSound(onlineTarget.getLocation(), Sound.LEVEL_UP, 1.0f, 1.0f);
        }
    }
}

