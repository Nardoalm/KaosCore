package com.knowplugins.ysubz.kaoscore.cmd.commands.staff;

import com.knowplugins.ysubz.kaoscore.cmd.Commands;
import com.knowplugins.ysubz.kaoscore.managers.Managers;
import com.knowplugins.ysubz.kaoscore.managers.player.PunishmentManager;
import com.knowplugins.ysubz.kaoscore.utils.punishment.PunishmentEntry;
import com.knowplugins.ysubz.kaoscore.utils.punishment.PunishmentType;
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
            sender.sendMessage("§c§lCHEATBAN ➔ §cUsage: /cheatban <player>");
            ModerationCommandUtil.play(sender, Sound.VILLAGER_NO);
            return;
        }

        if (!sender.hasPermission("kaoscore.staff.banir")) {
            sender.sendMessage("§c§lCHEATBAN ➔ §cYou do not have permission to use this command.");
            ModerationCommandUtil.play(sender, Sound.VILLAGER_NO);
            return;
        }

        PunishmentManager punishmentManager = Managers.getPunishmentManager();
        if (punishmentManager == null) {
            sender.sendMessage("§c§lCHEATBAN ➔ §cPunishment system unavailable.");
            ModerationCommandUtil.play(sender, Sound.VILLAGER_NO);
            return;
        }

        ModerationCommandUtil.Target target = ModerationCommandUtil.resolveTarget(args[0]);
        Player onlineTarget = target.getPlayer();
        if (onlineTarget == null || !onlineTarget.isOnline()) {
            sender.sendMessage("§c§lCHEATBAN ➔ §cJogador não existe ou não está online.");
            ModerationCommandUtil.play(sender, Sound.VILLAGER_NO);
            return;
        }

        if (sender instanceof Player && target.getUuid() != null && target.getUuid().equals(((Player) sender).getUniqueId())) {
            sender.sendMessage("§c§lCHEATBAN ➔ §cYou cannot ban yourself!");
            ModerationCommandUtil.play(sender, Sound.VILLAGER_NO);
            return;
        }

        String reason = "Uso de trapaças";
        long expiresAt = System.currentTimeMillis() + (30L * 86_400_000L);
        UUID punisherUuid = ModerationCommandUtil.getSenderUuid(sender);
        String punisherName = ModerationCommandUtil.getSenderName(sender);

        PunishmentEntry punishment = punishmentManager.punish(PunishmentType.BAN, target.getUuid(), target.getName(), punisherUuid, punisherName, reason, expiresAt, "CHEATBAN");
        if (punishment == null) {
            sender.sendMessage("§c§lCHEATBAN ➔ §cNão foi possível aplicar o CHEATBAN.");
            ModerationCommandUtil.play(sender, Sound.VILLAGER_NO);
            return;
        }

        Bukkit.getBanList(BanList.Type.NAME).addBan(target.getName(), reason, new Date(expiresAt), punisherName);

        sender.sendMessage(ChatColor.GREEN + "§a§lCHEATBAN ➔ §a" + target.getName() + " was banned for 30 days successfully!");
        ModerationCommandUtil.play(sender, Sound.EXPLODE);

        onlineTarget.getWorld().strikeLightningEffect(onlineTarget.getLocation());
        onlineTarget.kickPlayer(ModerationCommandUtil.buildBanScreen(punishment));

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage("§c§lCHEATBAN ➔ §c" + target.getName() + " §cwas banned for cheating.");
            p.playSound(p.getLocation(), Sound.ARROW_HIT, 1.0f, 1.0f);
        }
    }
}
