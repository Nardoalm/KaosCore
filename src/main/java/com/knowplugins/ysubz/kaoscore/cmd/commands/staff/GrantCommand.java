package com.knowplugins.ysubz.kaoscore.cmd.commands.staff;

import com.knowplugins.ysubz.kaoscore.cmd.Commands;
import com.knowplugins.ysubz.kaoscore.managers.Managers;
import com.knowplugins.ysubz.kaoscore.managers.permission.PermissionManager;
import com.knowplugins.ysubz.kaoscore.utils.punishment.PunishmentTime;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class GrantCommand extends Commands {

    private static final String PERMISSION = "kaoscore.staff.permissions";

    public GrantCommand() {
        super("grant", "atribuir", "setar", "conceder", "rank");
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
            sender.sendMessage("§eUsage: /grant <player> <group> [duration] [reason]");
            return;
        }

        String targetName = args[0];
        String groupName = args[1];

        if (permissions.getGroup(groupName) == null) {
            sender.sendMessage("§cGroup not found: §f" + groupName);
            return;
        }

        ModerationCommandUtil.Target target = ModerationCommandUtil.resolveTarget(targetName);
        UUID uuid = target.getUuid();
        String resolvedName = target.getName();
        if (resolvedName == null || resolvedName.trim().isEmpty()) {
            resolvedName = targetName;
        }

        if (uuid == null) {
            uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + resolvedName).getBytes(StandardCharsets.UTF_8));
        }

        long durationMs = 0L;
        int reasonIndex = 2;
        if (args.length >= 3) {
            long parsed = PunishmentTime.parseDuration(args[2]);
            if (parsed != Long.MIN_VALUE) {
                durationMs = parsed;
                reasonIndex = 3;
            }
        }

        String reason = ModerationCommandUtil.buildReason(args, reasonIndex, "No reason provided");
        UUID giverUuid = ModerationCommandUtil.getSenderUuid(sender);
        String giverName = ModerationCommandUtil.getSenderName(sender);

        UUID finalUuid = uuid;
        String finalResolvedName = resolvedName;
        long finalDurationMs = durationMs;
        JavaPlugin plugin = JavaPlugin.getProvidingPlugin(GrantCommand.class);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                    permissions.grant(finalUuid, finalResolvedName, groupName, giverUuid, giverName, finalDurationMs, reason);
                    Bukkit.getScheduler().runTask(plugin, () -> {
                    sender.sendMessage("§aGrant applied to §f" + finalResolvedName + " §a(" + groupName + ").");
                    ModerationCommandUtil.play(sender, Sound.NOTE_PLING);
                });
            } catch (Exception e) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    sender.sendMessage("§cError applying the grant.");
                    ModerationCommandUtil.play(sender, Sound.CLICK);
                });
                e.printStackTrace();
            }
        });
    }

    private boolean hasPerm(CommandSender sender) {
        if (!(sender instanceof Player)) {
            return true;
        }
        return sender.hasPermission(PERMISSION);
    }
}
