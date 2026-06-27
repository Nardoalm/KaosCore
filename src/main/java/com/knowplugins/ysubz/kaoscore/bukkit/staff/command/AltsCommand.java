package com.knowplugins.ysubz.kaoscore.bukkit.staff.command;

import com.knowplugins.ysubz.kaoscore.bukkit.KaosCore;
import com.knowplugins.ysubz.kaoscore.bukkit.cmd.Commands;
import com.knowplugins.ysubz.kaoscore.bukkit.cmd.commands.staff.ModerationCommandUtil;
import com.knowplugins.ysubz.kaoscore.bukkit.managers.Managers;
import com.knowplugins.ysubz.kaoscore.bukkit.managers.plugin.repository.PlayerAddressRepository;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;

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

        PlayerAddressRepository repository = Managers.getPlayerAddressRepository();
        if (repository == null) {
            sender.sendMessage("§cAlt system unavailable.");
            ModerationCommandUtil.play(sender, Sound.CLICK);
            return;
        }

        ModerationCommandUtil.Target target = ModerationCommandUtil.resolveTarget(args[0]);
        repository.findAlts(target.getName(), target.getUuid()).thenAccept(alts -> Bukkit.getScheduler().runTask(KaosCore.getInstance(), () -> sendResult(sender, target.getName(), alts)));
    }

    private void sendResult(CommandSender sender, String targetName, List<String> alts) {
        if (alts == null || alts.isEmpty()) {
            sender.sendMessage("§cNo related accounts found for " + KaosCore.getInstance().getTagManager().getTag(Bukkit.getPlayer(targetName)).getColor() + targetName + "§c.");
            ModerationCommandUtil.play(sender, Sound.CLICK);
            return;
        }

        sender.sendMessage("§aRelated accounts for " + KaosCore.getInstance().getTagManager().getTag(Bukkit.getPlayer(sender.getName())).getColor() + targetName + "§a: \n§f ➔ §f" + String.join("§7, \n ➔ §f", alts));
        ModerationCommandUtil.play(sender, Sound.NOTE_PLING);
    }
}
