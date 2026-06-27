package com.knowplugins.ysubz.kaoscore.bukkit.cmd.commands.staff;

import com.knowplugins.ysubz.kaoscore.bukkit.KaosCore;
import com.knowplugins.ysubz.kaoscore.bukkit.cmd.Commands;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;

public class KaosCoreCommand extends Commands {

    private final KaosCore plugin;

    public KaosCoreCommand(KaosCore plugin) {
        super("kaoscore");
        this.plugin = plugin;
    }

    @Override
    public void perform(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission("kaoscore.admin")) {
            sender.sendMessage("§cYou do not have permission to use this command.");
            if (sender instanceof org.bukkit.entity.Player) {
                ((org.bukkit.entity.Player) sender).playSound(((org.bukkit.entity.Player) sender).getLocation(), Sound.CLICK, 1.0f, 1.0f);
            }
            return;
        }

        if (args.length != 1 || !args[0].equalsIgnoreCase("reload")) {
            sender.sendMessage("§e§lKAOSCORE ➔ §eUsage: /kaoscore reload");
            return;
        }

        try {
            plugin.reloadSystems();
            sender.sendMessage("§a§lKAOSCORE ➔ §aConfiguration reloaded successfully.");
            if (sender instanceof org.bukkit.entity.Player) {
                ((org.bukkit.entity.Player) sender).playSound(((org.bukkit.entity.Player) sender).getLocation(), Sound.NOTE_PLING, 1.0f, 1.0f);
            }
        } catch (Exception ex) {
            sender.sendMessage("§cFailed to reload configuration. Check the console.");
            plugin.getLogger().severe("Failed to execute /kaoscore reload: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}
