package com.knowplugins.ysubz.kaoscore.cmd.commands.staff;

import com.knowplugins.ysubz.kaoscore.cmd.Commands;
import com.knowplugins.ysubz.kaoscore.managers.Managers;
import com.knowplugins.ysubz.kaoscore.managers.player.PunishmentManager;
import com.knowplugins.ysubz.kaoscore.utils.punishment.PunishmentEntry;
import com.knowplugins.ysubz.kaoscore.utils.punishment.PunishmentTime;
import com.knowplugins.ysubz.kaoscore.utils.punishment.PunishmentType;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Date;
import java.util.UUID;

public class BanCommand extends Commands {

    public BanCommand() {
        super("ban", "tempban", "banir");
    }

    @Override
    public void perform(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission("kaoscore.staff.banir")) {
            sender.sendMessage("§c§lBAN ➔ §cVocê não tem permissão para usar esse comando.");
            ModerationCommandUtil.play(sender, Sound.VILLAGER_NO);
            return;
        }

        if (args.length < 2) {
            sender.sendMessage("§c§lBAN ➔ §cUso correto: /ban <jogador> <tempo|perm> [motivo]");
            ModerationCommandUtil.play(sender, Sound.VILLAGER_NO);
            return;
        }

        PunishmentManager punishmentManager = Managers.getPunishmentManager();
        if (punishmentManager == null) {
            sender.sendMessage("§c§lBAN ➔ §cSistema de punições indisponível.");
            ModerationCommandUtil.play(sender, Sound.VILLAGER_NO);
            return;
        }

        ModerationCommandUtil.Target target = ModerationCommandUtil.resolveTarget(args[0]);
        if (target.getName().isEmpty()) {
            sender.sendMessage("§c§lBAN ➔ §cJogador inválido.");
            ModerationCommandUtil.play(sender, Sound.VILLAGER_NO);
            return;
        }

        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (target.getUuid() != null && target.getUuid().equals(player.getUniqueId())) {
                sender.sendMessage("§c§lBAN ➔ §cVocê não pode se banir.");
                ModerationCommandUtil.play(sender, Sound.VILLAGER_NO);
                return;
            }
        }

        long duration = PunishmentTime.parseDuration(args[1]);
        if (duration == Long.MIN_VALUE) {
            sender.sendMessage("§c§lBAN ➔ §cTempo inválido. Ex: 30d, 12h, 30m ou perm.");
            ModerationCommandUtil.play(sender, Sound.VILLAGER_NO);
            return;
        }

        String reason = ModerationCommandUtil.buildReason(args, 2, "Sem motivo informado");
        UUID punisherUuid = ModerationCommandUtil.getSenderUuid(sender);
        String punisherName = ModerationCommandUtil.getSenderName(sender);
        long expiresAt = duration == 0L ? 0L : System.currentTimeMillis() + duration;

        PunishmentEntry punishment = punishmentManager.punish(PunishmentType.BAN, target.getUuid(), target.getName(), punisherUuid, punisherName, reason, expiresAt, "BAN");
        if (punishment == null) {
            sender.sendMessage("§c§lBAN ➔ §cNão foi possível aplicar o banimento.");
            ModerationCommandUtil.play(sender, Sound.VILLAGER_NO);
            return;
        }

        Date expiration = punishment.isPermanent() ? null : new Date(punishment.getExpiresAt());
        Bukkit.getBanList(BanList.Type.NAME).addBan(target.getName(), reason, expiration, punisherName);

        Player onlineTarget = target.getPlayer();
        if (onlineTarget != null && onlineTarget.isOnline()) {
            onlineTarget.getWorld().strikeLightningEffect(onlineTarget.getLocation());
            onlineTarget.kickPlayer(ModerationCommandUtil.buildBanScreen(punishment));
        }

        sender.sendMessage("§a§lBAN ➔ §a" + target.getName() + " foi banido" + (punishment.isPermanent() ? " permanentemente" : " por " + PunishmentTime.formatRemaining(punishment.getExpiresAt())) + ".");
        ModerationCommandUtil.play(sender, Sound.EXPLODE);

        for (Player online : Bukkit.getOnlinePlayers()) {
            online.sendMessage("§c§lBANIMENTO ➔ §c" + target.getName() + " §cfoi banido. Motivo: §7" + reason);
            online.playSound(online.getLocation(), Sound.ARROW_HIT, 1.0f, 1.0f);
        }
    }
}

