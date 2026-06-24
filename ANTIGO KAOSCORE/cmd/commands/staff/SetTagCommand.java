package com.knowplugins.ysubz.kaoscore.cmd.commands.staff;

import com.knowplugins.ysubz.kaoscore.KaosCore;
import com.knowplugins.ysubz.kaoscore.cmd.Commands;
import com.knowplugins.ysubz.kaoscore.managers.player.DisguiseManager;
import com.knowplugins.ysubz.kaoscore.utils.tag.Tag;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public final class SetTagCommand extends Commands {

    private static final String PERMISSION = "kaoscore.staff.settag";

    public SetTagCommand() {
        super("settag", "setartag");
    }

    @Override
    public void perform(CommandSender sender, String label, String[] args) {
        if (sender == null) {
            return;
        }

        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage("§c§lSETTAG ➔ §cVocê não tem permissão para usar esse comando.");
            ModerationCommandUtil.play(sender, Sound.CLICK);
            return;
        }

        if (args == null || args.length < 2) {
            sender.sendMessage("§c§lSETTAG ➔ §cUso: /settag <jogador> <tag|off>");
            ModerationCommandUtil.play(sender, Sound.CLICK);
            return;
        }

        KaosCore plugin = KaosCore.getInstance();
        if (plugin == null || plugin.getTagManager() == null) {
            sender.sendMessage("§c§lSETTAG ➔ §cSistema de tags indisponível.");
            return;
        }

        String targetName = args[0];
        Player target = DisguiseManager.getOnlinePlayerByName(targetName);

        UUID targetUuid;
        String resolvedName;

        if (target != null && target.isOnline()) {
            targetUuid = target.getUniqueId();
            resolvedName = target.getName();
        } else {
            OfflinePlayer offline = Bukkit.getOfflinePlayer(targetName);
            if (offline == null || offline.getUniqueId() == null) {
                sender.sendMessage("§c§lSETTAG ➔ §cJogador inválido.");
                return;
            }
            targetUuid = offline.getUniqueId();
            resolvedName = offline.getName() == null ? targetName : offline.getName();
        }

        String tagArg = args[1];
        if (tagArg.equalsIgnoreCase("off") || tagArg.equalsIgnoreCase("none") || tagArg.equalsIgnoreCase("reset")) {
            plugin.getTagManager().clearTag(targetUuid);
            sender.sendMessage("§a§lSETTAG ➔ §aTag removida de §f" + resolvedName + "§a.");
            if (target != null && target.isOnline()) {
                target.sendMessage("§aSua tag foi removida por §f" + sender.getName() + "§a.");
            }
            return;
        }

        Tag tag = plugin.getTagManager().getTagByNameOrId(tagArg);
        if (tag == null) {
            sender.sendMessage("§c§lSETTAG ➔ §cTag não encontrada.");
            ModerationCommandUtil.play(sender, Sound.CLICK);
            return;
        }

        if (target != null && target.isOnline() && !plugin.getTagManager().canUse(target, tag)) {
            sender.sendMessage("§c§lSETTAG ➔ §cEsse jogador não possui permissão para usar essa tag.");
            ModerationCommandUtil.play(sender, Sound.CLICK);
            return;
        }

        boolean ok = plugin.getTagManager().setTag(targetUuid, tag.getId());
        if (!ok) {
            sender.sendMessage("§c§lSETTAG ➔ §cNão foi possível setar a tag.");
            ModerationCommandUtil.play(sender, Sound.CLICK);
            return;
        }

        sender.sendMessage("§a§lSETTAG ➔ §aTag de §f" + resolvedName + " §aatualizada para: §f" + tag.getName() + "§a.");
        if (target != null && target.isOnline()) {
            target.sendMessage("§aSua tag foi definida para §f" + tag.getName() + " §apor §f" + sender.getName() + "§a.");
        }
        ModerationCommandUtil.play(sender, Sound.LEVEL_UP);
    }
}

