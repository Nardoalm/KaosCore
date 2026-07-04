package com.knowplugins.ysubz.kaoscore.bukkit.moderation.staff.command;

import com.knowplugins.ysubz.kaoscore.bukkit.KaosCore;
import com.knowplugins.ysubz.kaoscore.bukkit.command.Commands;
import com.knowplugins.ysubz.kaoscore.bukkit.moderation.ModerationCommandUtil;
import com.knowplugins.ysubz.kaoscore.bukkit.manager.MainManager;
import com.knowplugins.ysubz.kaoscore.bukkit.manager.player.PunishmentManager;
import com.knowplugins.ysubz.kaoscore.bukkit.util.punishment.PunishmentEntry;
import com.knowplugins.ysubz.kaoscore.bukkit.util.punishment.PunishmentTime;
import com.knowplugins.ysubz.kaoscore.bukkit.util.punishment.PunishmentType;
import com.knowplugins.ysubz.kaoscore.bukkit.util.tag.manager.TagManager;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
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
        if (!sender.hasPermission("kaoscore.staff")) {
            sender.sendMessage("§cYou do not have permission to use this command.");
            ModerationCommandUtil.play(sender, Sound.CLICK);
            return;
        }

        if (args.length < 2) {
            sender.sendMessage("§cUsage: /ban <player> <duration|perm> [reason]");
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
        if (target.getName().isEmpty()) {
            sender.sendMessage("§cInvalid player.");
            ModerationCommandUtil.play(sender, Sound.CLICK);
            return;
        }

        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (target.getUuid() != null && target.getUuid().equals(player.getUniqueId())) {
                sender.sendMessage("§cYou cannot ban yourself.");
                ModerationCommandUtil.play(sender, Sound.CLICK);
                return;
            }
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

        PunishmentEntry punishment = punishmentManager.punish(PunishmentType.BAN, target.getUuid(), target.getName(), punisherUuid, punisherName, reason, expiresAt, "BAN");
        if (punishment == null) {
            sender.sendMessage("§cUnable to apply the ban.");
            ModerationCommandUtil.play(sender, Sound.CLICK);
            return;
        }

        Date expiration = punishment.isPermanent() ? null : new Date(punishment.getExpiresAt());
        Bukkit.getBanList(BanList.Type.NAME).addBan(target.getName(), reason, expiration, punisherName);

        Player onlineTarget = target.getPlayer();
        if (onlineTarget != null && onlineTarget.isOnline()) {
            onlineTarget.getWorld().strikeLightningEffect(onlineTarget.getLocation());
            onlineTarget.getWorld().playEffect(onlineTarget.getLocation(), Effect.EXPLOSION, 1);
            onlineTarget.kickPlayer(ModerationCommandUtil.buildBanScreen(punishment));
        }

        String targetTagColor = "";

        if(KaosCore.getInstance() != null) {
            TagManager tagManager = KaosCore.getInstance().getTagManager();
            targetTagColor = tagManager.getTag(onlineTarget).getColor();
        }

        sender.sendMessage("§a" + target.getName() + " was banned" + (punishment.isPermanent() ? " permanently" : " for " + PunishmentTime.formatRemaining(punishment.getExpiresAt())) + ".");
        ModerationCommandUtil.play(sender, Sound.EXPLODE);

        for (Player online : Bukkit.getOnlinePlayers()) {
            online.sendMessage("§c§l✘ §e" + targetTagColor + target.getName() + " §cwas banned. Reason: §7" + reason);
            online.playSound(online.getLocation(), Sound.ARROW_HIT, 1.0f, 1.0f);
        }
    }
}
