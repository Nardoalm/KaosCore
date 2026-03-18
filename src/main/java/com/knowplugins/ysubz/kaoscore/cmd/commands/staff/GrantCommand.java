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
        super("grant", "atribuir", "setar", "conceder");
    }

    @Override
    public void perform(CommandSender sender, String label, String[] args) {
        if (sender == null) {
            return;
        }

        if (!hasPerm(sender)) {
            sender.sendMessage("§c§lGRANT ➔ §cVocê não possui permissão para executar este comando.");
            ModerationCommandUtil.play(sender, Sound.VILLAGER_NO);
            return;
        }

        PermissionManager permissions = Managers.getPermissionManager();
        if (permissions == null) {
            sender.sendMessage("§c§lGRANT ➔ §cSistema de permissões indisponível.");
            return;
        }

        if (args == null || args.length < 2) {
            sender.sendMessage("§e§lGRANT ➔ §eUso: /grant <jogador> <grupo> [duração] [motivo]");
            return;
        }

        String targetName = args[0];
        String groupName = args[1];

        if (permissions.getGroup(groupName) == null) {
            sender.sendMessage("§c§lGRANT ➔ §cGrupo não encontrado: §f" + groupName);
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

        String reason = ModerationCommandUtil.buildReason(args, reasonIndex, "Sem motivo informado");
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
                    sender.sendMessage("§a§lGRANT ➔ §aGrant aplicado em §f" + finalResolvedName + " §a(" + groupName + ").");
                    ModerationCommandUtil.play(sender, Sound.LEVEL_UP);
                });
            } catch (Exception e) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    sender.sendMessage("§c§lGRANT ➔ §cErro ao aplicar grant.");
                    ModerationCommandUtil.play(sender, Sound.VILLAGER_NO);
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

