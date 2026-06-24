package com.knowplugins.ysubz.kaoscore.cmd.commands.staff;

import com.knowplugins.ysubz.kaoscore.cmd.Commands;
import com.knowplugins.ysubz.kaoscore.managers.Managers;
import com.knowplugins.ysubz.kaoscore.managers.player.PunishmentManager;
import com.knowplugins.ysubz.kaoscore.utils.punishment.PunishmentType;
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
            sender.sendMessage(ChatColor.RED + "§c§lUNBAN ➔ §cUso correto: /unban <jogador>");
            ModerationCommandUtil.play(sender, Sound.CLICK);
            return;
        }

        if (!sender.hasPermission("kaoscore.staff.banir")) {
            sender.sendMessage("§c§lUNBAN ➔ §cVocê não tem permissão para usar esse comando.");
            ModerationCommandUtil.play(sender, Sound.CLICK);
            return;
        }

        PunishmentManager punishmentManager = Managers.getPunishmentManager();
        ModerationCommandUtil.Target target = ModerationCommandUtil.resolveTarget(args[0]);
        UUID senderUuid = ModerationCommandUtil.getSenderUuid(sender);
        String senderName = ModerationCommandUtil.getSenderName(sender);

        boolean removed = punishmentManager != null && punishmentManager.clearActive(PunishmentType.BAN, target.getUuid(), target.getName(), senderUuid, senderName, "Banimento removido manualmente");

        BanList banList = Bukkit.getBanList(BanList.Type.NAME);
        BanEntry legacyBan = banList.getBanEntry(target.getName());
        if (legacyBan != null) {
            banList.pardon(target.getName());
        }

        if (!removed && legacyBan == null) {
            sender.sendMessage( "§c§lUNBAN ➔ §cEsse jogador não está banido.");
            ModerationCommandUtil.play(sender, Sound.CLICK);
            return;
        }

        sender.sendMessage("§a§lUNBAN ➔ §aO jogador " + target.getName() + " não está mais banido!");
        ModerationCommandUtil.play(sender, Sound.LEVEL_UP);
    }
}

