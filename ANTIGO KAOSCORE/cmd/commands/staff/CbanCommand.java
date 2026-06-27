package com.knowplugins.ysubz.kaoscore.bukkit.cmd.commands.staff;

import com.knowplugins.ysubz.kaoscore.bukkit.cmd.Commands;
import com.knowplugins.ysubz.kaoscore.bukkit.managers.Managers;
import com.knowplugins.ysubz.kaoscore.bukkit.managers.player.PunishmentManager;
import com.knowplugins.ysubz.kaoscore.bukkit.utils.punishment.PunishmentEntry;
import com.knowplugins.ysubz.kaoscore.bukkit.utils.punishment.PunishmentType;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Date;
import java.util.UUID;

public class CbanCommand extends Commands {
    public CbanCommand(){
        super("cban", "cb", "cheatban");
    }

    @Override
    public void perform(CommandSender sender, String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage("§c§lCHEATBAN ➔ §cUso correto: /CHEATBAN <jogador>");
            ModerationCommandUtil.play(sender, Sound.CLICK);
            return;
        }

        if (!sender.hasPermission("kaoscore.staff.banir")) {
            sender.sendMessage("§c§lCHEATBAN ➔ §cVocê não tem permissão para usar esse comando.");
            ModerationCommandUtil.play(sender, Sound.CLICK);
            return;
        }

        PunishmentManager punishmentManager = Managers.getPunishmentManager();
        if (punishmentManager == null) {
            sender.sendMessage("§c§lCHEATBAN ➔ §cSistema de punições indisponível.");
            ModerationCommandUtil.play(sender, Sound.CLICK);
            return;
        }

        ModerationCommandUtil.Target target = ModerationCommandUtil.resolveTarget(args[0]);
        Player onlineTarget = target.getPlayer();
        if (onlineTarget == null || !onlineTarget.isOnline()) {
            sender.sendMessage("§c§lCHEATBAN ➔ §cJogador não existe ou não está online.");
            ModerationCommandUtil.play(sender, Sound.CLICK);
            return;
        }

        if (sender instanceof Player && target.getUuid() != null && target.getUuid().equals(((Player) sender).getUniqueId())) {
            sender.sendMessage("§c§lCHEATBAN ➔ §cVocê não pode se banir!");
            ModerationCommandUtil.play(sender, Sound.CLICK);
            return;
        }

        String reason = "Uso de trapaças";
        long expiresAt = System.currentTimeMillis() + (30L * 86_400_000L);
        UUID punisherUuid = ModerationCommandUtil.getSenderUuid(sender);
        String punisherName = ModerationCommandUtil.getSenderName(sender);

        PunishmentEntry punishment = punishmentManager.punish(PunishmentType.BAN, target.getUuid(), target.getName(), punisherUuid, punisherName, reason, expiresAt, "CHEATBAN");
        if (punishment == null) {
            sender.sendMessage("§c§lCHEATBAN ➔ §cNão foi possível aplicar o CHEATBAN.");
            ModerationCommandUtil.play(sender, Sound.CLICK);
            return;
        }

        Bukkit.getBanList(BanList.Type.NAME).addBan(target.getName(), reason, new Date(expiresAt), punisherName);

        sender.sendMessage(ChatColor.GREEN + "§a§lCHEATBAN ➔ §a" + target.getName() + " foi banido por 30 dias com sucesso!");
        ModerationCommandUtil.play(sender, Sound.EXPLODE);

        onlineTarget.getWorld().strikeLightningEffect(onlineTarget.getLocation());
        onlineTarget.kickPlayer(ModerationCommandUtil.buildBanScreen(punishment));

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage("§c§lBANIMENTO ➔ §c" + target.getName() + " §cfoi banido por uso de trapaças.");
            p.playSound(p.getLocation(), Sound.ARROW_HIT, 1.0f, 1.0f);
        }
    }
}

