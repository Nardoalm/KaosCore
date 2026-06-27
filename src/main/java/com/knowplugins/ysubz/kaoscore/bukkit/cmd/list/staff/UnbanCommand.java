package com.knowplugins.ysubz.kaoscore.bukkit.cmd.list.staff;

import com.knowplugins.ysubz.kaoscore.bukkit.cmd.Commands;
import com.knowplugins.ysubz.kaoscore.bukkit.managers.Managers;
import com.knowplugins.ysubz.kaoscore.bukkit.managers.player.PunishmentManager;
import com.knowplugins.ysubz.kaoscore.bukkit.utils.punishment.PunishmentType;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import java.util.UUID;

public class UnbanCommand extends Commands {
    public UnbanCommand(){
        super("unban", "unb", "desbanir", "removerban");
    }

    @Override
    public void perform(CommandSender sender, String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "§cUsage: /unban <player>");
            ModerationCommandUtil.play(sender, Sound.CLICK);
            return;
        }

        if (!sender.hasPermission("kaoscore.staff")) {
            sender.sendMessage("§cYou do not have permission to use this command.");
            ModerationCommandUtil.play(sender, Sound.CLICK);
            return;
        }

        PunishmentManager punishmentManager = Managers.getPunishmentManager();
        ModerationCommandUtil.Target target = ModerationCommandUtil.resolveTarget(args[0]);
        UUID senderUuid = ModerationCommandUtil.getSenderUuid(sender);
        String senderName = ModerationCommandUtil.getSenderName(sender);

        boolean removed = punishmentManager != null && punishmentManager.clearActive(PunishmentType.BAN, target.getUuid(), target.getName(), senderUuid, senderName, "Ban manually removed");

        BanList banList = Bukkit.getBanList(BanList.Type.NAME);
        BanEntry legacyBan = banList.getBanEntry(target.getName());
        if (legacyBan != null) {
            banList.pardon(target.getName());
        }

        if (!removed && legacyBan == null) {
            sender.sendMessage("§cThat player is not banned.");
            ModerationCommandUtil.play(sender, Sound.CLICK);
            return;
        }

        sender.sendMessage("§aPlayer " + target.getName() + " is no longer banned!");
        ModerationCommandUtil.play(sender, Sound.NOTE_PLING);
    }
}
