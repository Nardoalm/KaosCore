package com.knowplugins.ysubz.kaoscore.bukkit.cmd.commands.staff;

import com.knowplugins.ysubz.kaoscore.bukkit.cmd.Commands;
import com.knowplugins.ysubz.kaoscore.bukkit.managers.Managers;
import com.knowplugins.ysubz.kaoscore.bukkit.managers.player.PunishmentManager;
import com.knowplugins.ysubz.kaoscore.bukkit.utils.punishment.PunishmentEntry;
import com.knowplugins.ysubz.kaoscore.bukkit.utils.punishment.PunishmentTime;
import com.knowplugins.ysubz.kaoscore.bukkit.utils.punishment.PunishmentType;
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
        if (!sender.hasPermission("kaoscore.staff.mutar")) {
            sender.sendMessage("§c§lMUTE ➔ §cVocê não tem permissão para usar esse comando.");
            ModerationCommandUtil.play(sender, Sound.CLICK);
            return;
        }

        if (args.length < 2) {
            sender.sendMessage("§c§lMUTE ➔ §cUso correto: /mute <jogador> <tempo|perm> [motivo]");
            ModerationCommandUtil.play(sender, Sound.CLICK);
            return;
        }

        PunishmentManager punishmentManager = Managers.getPunishmentManager();
        if (punishmentManager == null) {
            sender.sendMessage("§c§lMUTE ➔ §cSistema de punições indisponível.");
            ModerationCommandUtil.play(sender, Sound.CLICK);
            return;
        }

        ModerationCommandUtil.Target target = ModerationCommandUtil.resolveTarget(args[0]);
        if (target.getName().isEmpty()) {
            sender.sendMessage("§c§lMUTE ➔ §cJogador inválido.");
            ModerationCommandUtil.play(sender, Sound.CLICK);
            return;
        }

        long duration = PunishmentTime.parseDuration(args[1]);
        if (duration == Long.MIN_VALUE) {
            sender.sendMessage("§c§lMUTE ➔ §cTempo inválido. Ex: 30d, 12h, 30m ou perm.");
            ModerationCommandUtil.play(sender, Sound.CLICK);
            return;
        }

        String reason = ModerationCommandUtil.buildReason(args, 2, "Sem motivo informado");
        UUID punisherUuid = ModerationCommandUtil.getSenderUuid(sender);
        String punisherName = ModerationCommandUtil.getSenderName(sender);
        long expiresAt = duration == 0L ? 0L : System.currentTimeMillis() + duration;

        PunishmentEntry punishment = punishmentManager.punish(PunishmentType.MUTE, target.getUuid(), target.getName(), punisherUuid, punisherName, reason, expiresAt, "MUTE");
        if (punishment == null) {
            sender.sendMessage("§c§lMUTE ➔ §cNão foi possível aplicar o mute.");
            ModerationCommandUtil.play(sender, Sound.CLICK);
            return;
        }

        sender.sendMessage("§a§lMUTE ➔ §a" + target.getName() + " foi mutado" + (punishment.isPermanent() ? " permanentemente" : " por " + PunishmentTime.formatRemaining(punishment.getExpiresAt())) + ".");
        ModerationCommandUtil.play(sender, Sound.LEVEL_UP);

        Player onlineTarget = target.getPlayer();
        if (onlineTarget != null && onlineTarget.isOnline()) {
            onlineTarget.sendMessage(ModerationCommandUtil.buildMuteNotice(punishment));
            onlineTarget.playSound(onlineTarget.getLocation(), Sound.CLICK, 1.0f, 1.0f);
        }
    }
}

