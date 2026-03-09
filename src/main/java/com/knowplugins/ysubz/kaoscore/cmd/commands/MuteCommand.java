package com.knowplugins.ysubz.kaoscore.cmd.commands;

import com.knowplugins.ysubz.kaoscore.cmd.Commands;
import com.knowplugins.ysubz.kaoscore.managers.Managers;
import com.knowplugins.ysubz.kaoscore.managers.player.PunishmentManager;
import com.knowplugins.ysubz.kaoscore.punishment.PunishmentEntry;
import com.knowplugins.ysubz.kaoscore.punishment.PunishmentTime;
import com.knowplugins.ysubz.kaoscore.punishment.PunishmentType;
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
        if (!sender.hasPermission("nardocore.staff.mutar")) {
            sender.sendMessage("§c§lERRO ➔ §cVocê não tem permissão para usar esse comando.");
            ModerationCommandUtil.play(sender, Sound.VILLAGER_NO);
            return;
        }

        if (args.length < 2) {
            sender.sendMessage("§c§lERRO ➔ §cUso correto: /mute <jogador> <tempo|perm> [motivo]");
            ModerationCommandUtil.play(sender, Sound.VILLAGER_NO);
            return;
        }

        PunishmentManager punishmentManager = Managers.getPunishmentManager();
        if (punishmentManager == null) {
            sender.sendMessage("§c§lERRO ➔ §cSistema de punições indisponível.");
            ModerationCommandUtil.play(sender, Sound.VILLAGER_NO);
            return;
        }

        ModerationCommandUtil.Target target = ModerationCommandUtil.resolveTarget(args[0]);
        if (target.getName().isEmpty()) {
            sender.sendMessage("§c§lERRO ➔ §cJogador inválido.");
            ModerationCommandUtil.play(sender, Sound.VILLAGER_NO);
            return;
        }

        long duration = PunishmentTime.parseDuration(args[1]);
        if (duration == Long.MIN_VALUE) {
            sender.sendMessage("§c§lERRO ➔ §cTempo inválido. Ex: 30d, 12h, 30m ou perm.");
            ModerationCommandUtil.play(sender, Sound.VILLAGER_NO);
            return;
        }

        String reason = ModerationCommandUtil.buildReason(args, 2, "Sem motivo informado");
        UUID punisherUuid = ModerationCommandUtil.getSenderUuid(sender);
        String punisherName = ModerationCommandUtil.getSenderName(sender);
        long expiresAt = duration == 0L ? 0L : System.currentTimeMillis() + duration;

        PunishmentEntry punishment = punishmentManager.punish(PunishmentType.MUTE, target.getUuid(), target.getName(), punisherUuid, punisherName, reason, expiresAt, "MUTE");
        if (punishment == null) {
            sender.sendMessage("§c§lERRO ➔ §cNão foi possível aplicar o mute.");
            ModerationCommandUtil.play(sender, Sound.VILLAGER_NO);
            return;
        }

        sender.sendMessage("§a§lSUCESSO ➔ §a" + target.getName() + " foi mutado" + (punishment.isPermanent() ? " permanentemente" : " por " + PunishmentTime.formatRemaining(punishment.getExpiresAt())) + ".");
        ModerationCommandUtil.play(sender, Sound.LEVEL_UP);

        Player onlineTarget = target.getPlayer();
        if (onlineTarget != null && onlineTarget.isOnline()) {
            onlineTarget.sendMessage(ModerationCommandUtil.buildMuteNotice(punishment));
            onlineTarget.playSound(onlineTarget.getLocation(), Sound.VILLAGER_NO, 1.0f, 1.0f);
        }
    }
}
