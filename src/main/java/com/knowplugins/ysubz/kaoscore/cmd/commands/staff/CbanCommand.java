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

    public CbanCommand() {
        super("cban", "cb", "cheatban");
    }

    @Override
    public void perform(CommandSender sender, String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage("§cUsage: /cheatban <player>");
            ModerationCommandUtil.play(sender, Sound.CLICK);
            return;
        }

        if (!sender.hasPermission("kaoscore.staff.banir")) {
            sender.sendMessage("§cYou do not have permission to use this command.");
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

        if (target == null || target.getUuid() == null) {
            sender.sendMessage("§cPlayer does not exist.");
            ModerationCommandUtil.play(sender, Sound.CLICK);
            return;
        }

        if (sender instanceof Player && target.getUuid().equals(((Player) sender).getUniqueId())) {
            sender.sendMessage("§cYou cannot ban yourself!");
            ModerationCommandUtil.play(sender, Sound.CLICK);
            return;
        }

        String reason = "Cheating";
        long expiresAt = System.currentTimeMillis() + (30L * 86_400_000L);
        UUID punisherUuid = ModerationCommandUtil.getSenderUuid(sender);
        String punisherName = ModerationCommandUtil.getSenderName(sender);

        PunishmentEntry punishment = punishmentManager.punish(PunishmentType.BAN, target.getUuid(), target.getName(), punisherUuid, punisherName, reason, expiresAt, "CHEATBAN");
        if (punishment == null) {
            sender.sendMessage("§cCould not apply CHEATBAN.");
            ModerationCommandUtil.play(sender, Sound.CLICK);
            return;
        }

        Bukkit.getBanList(BanList.Type.NAME).addBan(target.getName(), reason, new Date(expiresAt), punisherName);

        sender.sendMessage(ChatColor.GREEN + "§a" + target.getName() + " was banned for 30 days successfully!");
        ModerationCommandUtil.play(sender, Sound.EXPLODE);

        Player onlineTarget = target.getPlayer();
        if (onlineTarget != null && onlineTarget.isOnline()) {
            onlineTarget.getWorld().strikeLightningEffect(onlineTarget.getLocation());
            onlineTarget.kickPlayer(ModerationCommandUtil.buildBanScreen(punishment));
        }

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage("§c" + target.getName() + " §cwas banned for cheating.");
            p.playSound(p.getLocation(), Sound.ARROW_HIT, 1.0f, 1.0f);
        }
    }
}