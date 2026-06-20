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
            sender.sendMessage("§cYou do not have permission to use this command.");
            ModerationCommandUtil.play(sender, Sound.VILLAGER_NO);
            return;
        }

        if (args == null || args.length < 2) {
            sender.sendMessage("§cUsage: /settag <player> <tag|off>");
            ModerationCommandUtil.play(sender, Sound.VILLAGER_NO);
            return;
        }

        KaosCore plugin = KaosCore.getInstance();
        if (plugin == null || plugin.getTagManager() == null) {
            sender.sendMessage("§cTag system unavailable.");
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
                sender.sendMessage("§cJogador inválido.");
                return;
            }
            targetUuid = offline.getUniqueId();
            resolvedName = offline.getName() == null ? targetName : offline.getName();
        }

        String tagArg = args[1];
        if (tagArg.equalsIgnoreCase("off") || tagArg.equalsIgnoreCase("none") || tagArg.equalsIgnoreCase("reset")) {
            plugin.getTagManager().clearTag(targetUuid);
            sender.sendMessage("§a§lSETTAG ➔ §aTag removed from §f" + resolvedName + "§a.");
            if (target != null && target.isOnline()) {
                target.sendMessage("§aYour tag was removed by §f" + sender.getName() + "§a.");
            }
            return;
        }

        Tag tag = plugin.getTagManager().getTagByNameOrId(tagArg);
        if (tag == null) {
            sender.sendMessage("§cTag not found.");
            ModerationCommandUtil.play(sender, Sound.VILLAGER_NO);
            return;
        }

        if (target != null && target.isOnline() && !plugin.getTagManager().canUse(target, tag)) {
            sender.sendMessage("§cThat player does not have permission to use this tag.");
            ModerationCommandUtil.play(sender, Sound.VILLAGER_NO);
            return;
        }

        boolean ok = plugin.getTagManager().setTag(targetUuid, tag.getId());
        if (!ok) {
            sender.sendMessage("§cUnable to set the tag.");
            ModerationCommandUtil.play(sender, Sound.VILLAGER_NO);
            return;
        }

        sender.sendMessage("§a§lSETTAG ➔ §aTag for §f" + resolvedName + " §awas updated to: §f" + tag.getName() + "§a.");
        if (target != null && target.isOnline()) {
            target.sendMessage("§aYour tag was set to §f" + tag.getName() + " §aby §f" + sender.getName() + "§a.");
        }
        ModerationCommandUtil.play(sender, Sound.LEVEL_UP);
    }
}
