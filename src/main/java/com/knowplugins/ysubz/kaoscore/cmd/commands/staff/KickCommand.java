package com.knowplugins.ysubz.kaoscore.cmd.commands.staff;

import com.knowplugins.ysubz.kaoscore.KaosCore;
import com.knowplugins.ysubz.kaoscore.cmd.Commands;
import com.knowplugins.ysubz.kaoscore.managers.player.DisguiseManager;
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
            sender.sendMessage("§cYou do not have permission to use this command.");
            ModerationCommandUtil.play(sender, Sound.CLICK);
            return;
        }

        if (args.length < 1) {
            sender.sendMessage("§cUsage: /kick <player> [reason]");
            ModerationCommandUtil.play(sender, Sound.CLICK);
            return;
        }

        ModerationCommandUtil.Target target = ModerationCommandUtil.resolveTarget(args[0]);
        Player player = target.getPlayer();

        if (player == null) {
            player = DisguiseManager.getOnlinePlayerByName(args[0]);
        }

        if (player == null || !player.isOnline()) {
            sender.sendMessage("§cThat player is not online.");
            ModerationCommandUtil.play(sender, Sound.CLICK);
            return;
        }

        if (sender instanceof Player && player.getUniqueId().equals(((Player) sender).getUniqueId())) {
            sender.sendMessage("§cYou cannot kick yourself.");
            ModerationCommandUtil.play(sender, Sound.CLICK);
            return;
        }

        String reason = ModerationCommandUtil.buildReason(args, 1, "No reason provided");
        String senderName = ModerationCommandUtil.getSenderName(sender);
        java.util.UUID senderUuid = ModerationCommandUtil.getSenderUuid(sender);

        player.kickPlayer(ModerationCommandUtil.buildKickScreen(reason, senderName));

        KaosCore plugin = KaosCore.getInstance();
        DiscordWebhookManager webhookManager = plugin == null ? null : plugin.getWebhookManager();
        if (webhookManager != null) {
            webhookManager.logKick(target.getName(), target.getUuid(), senderName, senderUuid, reason);
        }

        sender.sendMessage("§a" + target.getName() + " was kicked from the server.");
        ModerationCommandUtil.play(sender, Sound.NOTE_PLING);

        for (Player online : Bukkit.getOnlinePlayers()) {
            online.sendMessage("§e" + target.getName() + " §ewas kicked. Reason: §7" + reason);
        }
    }
}
