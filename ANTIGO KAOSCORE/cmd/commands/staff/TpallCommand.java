package com.knowplugins.ysubz.kaoscore.bukkit.cmd.commands.staff;

import com.knowplugins.ysubz.kaoscore.bukkit.cmd.Commands;
import com.knowplugins.ysubz.kaoscore.bukkit.managers.player.DisguiseManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;

public final class TpallCommand extends Commands {

    public TpallCommand() {
        super("tpall", "teleportall");
    }

    @Override
    public void perform(CommandSender sender, String label, String[] args) {
        if (sender == null) {
            return;
        }

        if (!sender.hasPermission("kaoscore.staff.tpall")) {
            sender.sendMessage("§c§lTPALL ➔ §cVocê não possui permissão para executar este comando.");
            ModerationCommandUtil.play(sender, Sound.CLICK);
            return;
        }

        Player target;

        if (args == null || args.length == 0 || (args.length == 1 && args[0].equalsIgnoreCase("here"))) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§c§lTPALL ➔ §cUso: /tpall <jogador> (no console).");
                return;
            }
            target = (Player) sender;
        } else {
            target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                target = DisguiseManager.getOnlinePlayerByName(args[0]);
            }
            if (target == null || !target.isOnline()) {
                sender.sendMessage("§c§lTPALL ➔ §cJogador não encontrado: §f" + args[0]);
                ModerationCommandUtil.play(sender, Sound.CLICK);
                return;
            }
        }

        Location to = target.getLocation();
        int moved = 0;

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online == null || !online.isOnline()) {
                continue;
            }
            if (online.equals(target)) {
                continue;
            }
            online.teleport(to, PlayerTeleportEvent.TeleportCause.COMMAND);
            moved++;
        }

        sender.sendMessage("§a§lTPALL ➔ §aTeleportados §f" + moved + " §ajogadores para §f" + target.getName() + "§a.");
        ModerationCommandUtil.play(sender, Sound.LEVEL_UP);
    }
}

