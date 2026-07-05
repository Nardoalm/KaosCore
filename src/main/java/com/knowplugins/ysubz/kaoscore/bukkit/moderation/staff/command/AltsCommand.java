package com.knowplugins.ysubz.kaoscore.bukkit.moderation.staff.command;

import com.knowplugins.ysubz.kaoscore.bukkit.KaosCore;
import com.knowplugins.ysubz.kaoscore.bukkit.command.Commands;
import com.knowplugins.ysubz.kaoscore.bukkit.moderation.ModerationCommandUtil;
import com.knowplugins.ysubz.kaoscore.bukkit.manager.MainManager;
import com.knowplugins.ysubz.kaoscore.bukkit.manager.plugin.repository.PlayerAddressRepository;
import com.knowplugins.ysubz.kaoscore.bukkit.util.tag.Tag;
import com.knowplugins.ysubz.kaoscore.bukkit.util.tag.manager.TagManager;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class AltsCommand extends Commands {

    public AltsCommand() {
        super("alts", "alt", "contasalternativas");
    }

    @Override
    public void perform(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission("kaoscore.staff")) {
            sender.sendMessage("§cYou do not have permission to use this command.");
            ModerationCommandUtil.play(sender, Sound.CLICK);
            return;
        }

        if (args.length < 1) {
            sender.sendMessage("§cUsage: /alts <player>");
            ModerationCommandUtil.play(sender, Sound.CLICK);
            return;
        }

        PlayerAddressRepository repository = MainManager.getPlayerAddressRepository();
        if (repository == null) {
            sender.sendMessage("§cAlt system unavailable.");
            ModerationCommandUtil.play(sender, Sound.CLICK);
            return;
        }

        ModerationCommandUtil.Target target = ModerationCommandUtil.resolveTarget(args[0]);
        repository.findAlts(target.getName(), target.getUuid()).thenAccept(alts ->
                Bukkit.getScheduler().runTask(KaosCore.getInstance(), () -> sendResult(sender, target.getName(), alts))
        );
    }

    private void sendResult(CommandSender sender, String targetName, List<String> alts) {
        TagManager tagManager = KaosCore.getInstance().getTagManager();
        String color = "§e";

        if (tagManager != null) {
            Player onlineTarget = Bukkit.getPlayerExact(targetName);
            if (onlineTarget != null) {
                Tag tag = tagManager.getTag(onlineTarget);
                if (tag != null && tag.getColor() != null) {
                    color = tag.getColor();
                }
            }
        }

        if (alts == null || alts.isEmpty()) {
            sender.sendMessage("§cNo related accounts found for " + color + targetName + "§c.");
            ModerationCommandUtil.play(sender, Sound.CLICK);
            return;
        }

        sender.sendMessage("§aRelated accounts for " + color + targetName + "§a: \n§f ➔ §f" + String.join("§7, \n ➔ §f", alts));
        ModerationCommandUtil.play(sender, Sound.NOTE_PLING);
    }
}