package com.knowplugins.ysubz.kaoscore.cmd.commands.staff;

import com.knowplugins.ysubz.kaoscore.KaosCore;
import com.knowplugins.ysubz.kaoscore.cmd.Commands;
import com.knowplugins.ysubz.kaoscore.managers.Managers;
import com.knowplugins.ysubz.kaoscore.managers.plugin.PlayerAddressRepository;
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
        if (!sender.hasPermission("kaoscore.staff.alts")) {
            sender.sendMessage("§c§lALTS ➔ §cYou do not have permission to use this command.");
            ModerationCommandUtil.play(sender, Sound.VILLAGER_NO);
            return;
        }

        if (args.length < 1) {
            sender.sendMessage("§c§lALTS ➔ §cUsage: /alts <player>");
            ModerationCommandUtil.play(sender, Sound.VILLAGER_NO);
            return;
        }

        PlayerAddressRepository repository = Managers.getPlayerAddressRepository();
        if (repository == null) {
            sender.sendMessage("§c§lALTS ➔ §cAlt system unavailable.");
            ModerationCommandUtil.play(sender, Sound.VILLAGER_NO);
            return;
        }

        ModerationCommandUtil.Target target = ModerationCommandUtil.resolveTarget(args[0]);
        repository.findAlts(target.getName(), target.getUuid()).thenAccept(alts -> Bukkit.getScheduler().runTask(KaosCore.getInstance(), () -> sendResult(sender, target.getName(), alts)));
    }

    private void sendResult(CommandSender sender, String targetName, List<String> alts) {
        if (alts == null || alts.isEmpty()) {
            sender.sendMessage("§e§lALTS ➔ §eNo related accounts found for §f" + targetName + "§e.");
            ModerationCommandUtil.play(sender, Sound.VILLAGER_NO);
            return;
        }

        sender.sendMessage("§e§lALTS ➔ §eRelated accounts for §f" + targetName + "§e: §f" + String.join("§7, §f", alts));
        ModerationCommandUtil.play(sender, Sound.LEVEL_UP);
    }
}
