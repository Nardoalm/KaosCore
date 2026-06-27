package com.knowplugins.ysubz.kaoscore.bukkit.cmd.list.staff;

import com.knowplugins.ysubz.kaoscore.bukkit.cmd.Commands;
import com.knowplugins.ysubz.kaoscore.bukkit.managers.Managers;
import com.knowplugins.ysubz.kaoscore.bukkit.managers.permission.PermissionManager;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class UngrantCommand extends Commands {

    private static final String PERMISSION = "kaoscore.admin";

    public UngrantCommand() {
        super("ungrant", "unrank");
    }

    @Override
    public void perform(CommandSender sender, String label, String[] args) {
        if (sender == null) {
            return;
        }

        if (!hasPerm(sender)) {
            sender.sendMessage("§cYou do not have permission to use this command.");
            ModerationCommandUtil.play(sender, Sound.CLICK);
            return;
        }

        PermissionManager permissions = Managers.getPermissionManager();
        if (permissions == null) {
            sender.sendMessage("§cPermission system unavailable.");
            return;
        }

        if (args == null || args.length < 2) {
            sender.sendMessage("§e§lUNGRANT ➔ §eUsage: /ungrant <player> <group> [reason]");
            return;
        }

        String targetName = args[0];
        String groupName = args[1];
        String reason = ModerationCommandUtil.buildReason(args, 2, "No reason provided");

        ModerationCommandUtil.Target target = ModerationCommandUtil.resolveTarget(targetName);
        UUID uuid = target.getUuid();
        String resolvedName = target.getName();
        if (resolvedName == null || resolvedName.trim().isEmpty()) {
            resolvedName = targetName;
        }

        if (uuid == null) {
            uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + resolvedName).getBytes(StandardCharsets.UTF_8));
        }

        UUID removerUuid = ModerationCommandUtil.getSenderUuid(sender);
        String removerName = ModerationCommandUtil.getSenderName(sender);

        UUID finalUuid = uuid;
        String finalResolvedName = resolvedName;

        JavaPlugin plugin = JavaPlugin.getProvidingPlugin(UngrantCommand.class);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean ok = permissions.ungrant(finalUuid, finalResolvedName, groupName, removerUuid, removerName, reason);
            Bukkit.getScheduler().runTask(plugin, () -> {
                sender.sendMessage(ok
                        ? "§a§lUNGRANT ➔ §aGrant removed from §f" + finalResolvedName + " §a(" + groupName + ")."
                        : "§cNo active grant found to remove.");
                ModerationCommandUtil.play(sender, ok ? Sound.NOTE_PLING : Sound.CLICK);
            });
        });
    }

    private boolean hasPerm(CommandSender sender) {
        if (!(sender instanceof Player)) {
            return true;
        }
        return sender.hasPermission(PERMISSION);
    }
}
