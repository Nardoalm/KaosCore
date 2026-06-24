package com.knowplugins.ysubz.kaoscore.cmd.commands.staff;

import com.knowplugins.ysubz.kaoscore.cmd.Commands;
import com.knowplugins.ysubz.kaoscore.managers.Managers;
import com.knowplugins.ysubz.kaoscore.managers.permission.PermissionManager;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class UngrantCommand extends Commands {

    private static final String PERMISSION = "kaoscore.staff.permissions";

    public UngrantCommand() {
        super("ungrant");
    }

    @Override
    public void perform(CommandSender sender, String label, String[] args) {
        if (sender == null) {
            return;
        }

        if (!hasPerm(sender)) {
            sender.sendMessage("§c§lUNGRANT ➔ §cVocê não possui permissão para executar este comando.");
            ModerationCommandUtil.play(sender, Sound.CLICK);
            return;
        }

        PermissionManager permissions = Managers.getPermissionManager();
        if (permissions == null) {
            sender.sendMessage("§c§lUNGRANT ➔ §cSistema de permissões indisponível.");
            return;
        }

        if (args == null || args.length < 2) {
            sender.sendMessage("§e§lUNGRANT ➔ §eUso: /ungrant <jogador> <grupo> [motivo]");
            return;
        }

        String targetName = args[0];
        String groupName = args[1];
        String reason = ModerationCommandUtil.buildReason(args, 2, "Sem motivo informado");

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
                        ? "§a§lUNGRANT ➔ §aGrant removido de §f" + finalResolvedName + " §a(" + groupName + ")."
                        : "§c§lUNGRANT ➔ §cNenhum grant ativo encontrado para remover.");
                ModerationCommandUtil.play(sender, ok ? Sound.LEVEL_UP : Sound.CLICK);
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

