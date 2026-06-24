package com.knowplugins.ysubz.kaoscore.cmd.commands.staff;

import com.knowplugins.ysubz.kaoscore.KaosCore;
import com.knowplugins.ysubz.kaoscore.cmd.Commands;
import com.knowplugins.ysubz.kaoscore.managers.plugin.DiscordWebhookManager;
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
        if (!sender.hasPermission("kaoscore.staff.kickar")) {
            sender.sendMessage("§c§lKICK ➔ §cVocê não tem permissão para usar esse comando.");
            ModerationCommandUtil.play(sender, Sound.CLICK);
            return;
        }

        if (args.length < 1) {
            sender.sendMessage("§c§lKICK ➔ §cUso correto: /kick <jogador> [motivo]");
            ModerationCommandUtil.play(sender, Sound.CLICK);
            return;
        }

        ModerationCommandUtil.Target target = ModerationCommandUtil.resolveTarget(args[0]);
        Player player = target.getPlayer();
        if (player == null || !player.isOnline()) {
            sender.sendMessage("§c§lKICK ➔ §cEsse jogador não está online.");
            ModerationCommandUtil.play(sender, Sound.CLICK);
            return;
        }

        if (sender instanceof Player && player.getUniqueId().equals(((Player) sender).getUniqueId())) {
            sender.sendMessage("§c§lKICK ➔ §cVocê não pode se expulsar.");
            ModerationCommandUtil.play(sender, Sound.CLICK);
            return;
        }

        String reason = ModerationCommandUtil.buildReason(args, 1, "Sem motivo informado");
        String senderName = ModerationCommandUtil.getSenderName(sender);
        java.util.UUID senderUuid = ModerationCommandUtil.getSenderUuid(sender);

        player.kickPlayer(ModerationCommandUtil.buildKickScreen(reason, senderName));

        KaosCore plugin = KaosCore.getInstance();
        DiscordWebhookManager webhookManager = plugin == null ? null : plugin.getWebhookManager();
        if (webhookManager != null) {
            webhookManager.logKick(target.getName(), target.getUuid(), senderName, senderUuid, reason);
        }

        sender.sendMessage("§a§lKICK ➔ §a" + target.getName() + " foi expulso do servidor.");
        ModerationCommandUtil.play(sender, Sound.LEVEL_UP);

        for (Player online : Bukkit.getOnlinePlayers()) {
            online.sendMessage("§e§lKICK ➔ §e" + target.getName() + " §efoi expulso. Motivo: §7" + reason);
        }
    }
}

