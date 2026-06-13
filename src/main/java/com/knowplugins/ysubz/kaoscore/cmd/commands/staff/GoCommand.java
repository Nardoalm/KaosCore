package com.knowplugins.ysubz.kaoscore.cmd.commands.staff;

import com.knowplugins.ysubz.kaoscore.cmd.Commands;
import com.knowplugins.ysubz.kaoscore.managers.player.DisguiseManager;
import com.knowplugins.ysubz.kaoscore.managers.player.VanishManager;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GoCommand extends Commands {

    public GoCommand() {
        super("go", "goto");
    }

    @Override
    public void perform(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c§lGO ➔ §cOnly players can use this command.");
            return;
        }

        if (!sender.hasPermission("kaoscore.staff.go")){
            sender.sendMessage("§c§lGO ➔ §cYou do not have permission to use this command.");
            return;
        }

        if(args.length != 1) {
            sender.sendMessage("§cUsage: /go <player>");
            return;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            target = DisguiseManager.getOnlinePlayerByName(args[0]);
        }
        if (target == null) {
            sender.sendMessage("§c§lGO ➔ §cPlayer not found.");
            return;
        }

        Player staff = (Player) sender;
        VanishManager.addVanish(staff);

        staff.playSound(staff.getLocation(), Sound.NOTE_PLING, 1.0f, 1.0f);
        staff.teleport(target.getLocation());

        String tagColor = PlaceholderAPI.setPlaceholders(target, "%kaoscore_tag_color%");
        sender.sendMessage("§a§lGO ➔ §aYou were silently teleported to " + tagColor + target.getName() + ".");
    }
}
