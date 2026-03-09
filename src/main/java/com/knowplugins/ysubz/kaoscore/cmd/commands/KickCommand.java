package com.knowplugins.ysubz.kaoscore.cmd.commands;

import com.knowplugins.ysubz.kaoscore.cmd.Commands;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class KickCommand extends Commands {

    public KickCommand() {
        super("kick", "expulsar");
    }

    @Override
    public void perform(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission("nardocore.staff.kickar")) {
            sender.sendMessage("§c§lERRO ➔ §cVocê não tem permissão para usar esse comando.");
            ModerationCommandUtil.play(sender, Sound.VILLAGER_NO);
            return;
        }

        if (args.length < 1) {
            sender.sendMessage("§c§lERRO ➔ §cUso correto: /kick <jogador> [motivo]");
            ModerationCommandUtil.play(sender, Sound.VILLAGER_NO);
            return;
        }

        ModerationCommandUtil.Target target = ModerationCommandUtil.resolveTarget(args[0]);
        Player player = target.getPlayer();
        if (player == null || !player.isOnline()) {
            sender.sendMessage("§c§lERRO ➔ §cEsse jogador não está online.");
            ModerationCommandUtil.play(sender, Sound.VILLAGER_NO);
            return;
        }

        if (sender instanceof Player && player.getUniqueId().equals(((Player) sender).getUniqueId())) {
            sender.sendMessage("§c§lERRO ➔ §cVocê não pode se expulsar.");
            ModerationCommandUtil.play(sender, Sound.VILLAGER_NO);
            return;
        }

        String reason = ModerationCommandUtil.buildReason(args, 1, "Sem motivo informado");
        String senderName = ModerationCommandUtil.getSenderName(sender);

        player.kickPlayer(ModerationCommandUtil.buildKickScreen(reason, senderName));

        sender.sendMessage("§a§lSUCESSO ➔ §a" + target.getName() + " foi expulso do servidor.");
        ModerationCommandUtil.play(sender, Sound.LEVEL_UP);

        for (Player online : Bukkit.getOnlinePlayers()) {
            online.sendMessage("§e§lKICK ➔ §e" + target.getName() + " §efoi expulso. Motivo: §7" + reason);
        }
    }
}
