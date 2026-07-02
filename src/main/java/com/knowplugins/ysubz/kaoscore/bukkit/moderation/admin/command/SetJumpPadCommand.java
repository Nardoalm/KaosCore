package com.knowplugins.ysubz.kaoscore.bukkit.moderation.admin.command;

import com.knowplugins.ysubz.kaoscore.bukkit.KaosCore;
import com.knowplugins.ysubz.kaoscore.bukkit.cmd.Commands;
import com.knowplugins.ysubz.kaoscore.bukkit.listener.world.JumpPadListener;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class SetJumpPadCommand extends Commands {

    public SetJumpPadCommand() {
        super("setjumppad");
    }

    @Override
    public void perform(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("kaoscore.admin.jumppad")) {
            player.sendMessage("§cYou do not have permission to execute this command.");
            player.playSound(player.getLocation(), Sound.CLICK, 1.0f, 1.0f);
            return;
        }

        if (args.length < 1) {
            player.sendMessage("§cUsage: /setjumppad <1-4>");
            player.playSound(player.getLocation(), Sound.CLICK, 1.0f, 1.0f);
            return;
        }

        int id;
        try {
            id = Integer.parseInt(args[0]);
            if (id < 1 || id > 4) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            player.sendMessage("§cThe ID must be a number between 1 and 4.");
            player.playSound(player.getLocation(), Sound.CLICK, 1.0f, 1.0f);
            return;
        }

        Block blockUnder = player.getLocation().getBlock().getRelative(BlockFace.DOWN);
        FileConfiguration config = KaosCore.getInstance().getConfig();
        String path = "jumppad.locations." + id;

        config.set(path + ".world", blockUnder.getWorld().getName());
        config.set(path + ".x", blockUnder.getX());
        config.set(path + ".y", blockUnder.getY());
        config.set(path + ".z", blockUnder.getZ());

        KaosCore.getInstance().saveConfig();
        JumpPadListener.loadCache(config);

        player.sendMessage("§aJumpPad #" + id + " successfully set at your current location block!");
        player.playSound(player.getLocation(), Sound.NOTE_PLING, 1.0f, 1.0f);
    }
}