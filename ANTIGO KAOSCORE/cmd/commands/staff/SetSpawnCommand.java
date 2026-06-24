package com.knowplugins.ysubz.kaoscore.cmd.commands.staff;

import com.knowplugins.ysubz.kaoscore.KaosCore;
import com.knowplugins.ysubz.kaoscore.cmd.Commands;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetSpawnCommand extends Commands {

    public SetSpawnCommand() {
        super("setlobby",  "spawnlocation");
    }

    @Override
    public void perform(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c§lSPAWN ➔ §cApenas jogadores podem usar esse comando.");
            return;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("kaoscore.admin.setspawn")) {
            player.sendMessage(ChatColor.RED + "§c§lSPAWN ➔ §cVocê não tem permissão para usar esse comando.");
            Player senderPlayer = (Player) sender;
            senderPlayer.playSound(senderPlayer.getLocation(), Sound.CLICK, 1.0f, 1.0f);
            return;
        }

        Location loc = player.getLocation();
        KaosCore plugin = KaosCore.getInstance();

        String serialized = loc.getWorld().getName() + ";" +
                loc.getX() + ";" +
                loc.getY() + ";" +
                loc.getZ() + ";" +
                loc.getYaw() + ";" +
                loc.getPitch();

        plugin.getConfig().set("spawn.location", serialized);
        plugin.saveConfig();

        player.sendMessage(ChatColor.GREEN + "§a§lSPAWN ➔ §aSpawn definido com sucesso!");
        player.playSound(player.getLocation(), Sound.LEVEL_UP, 1.0f, 1.0f);
    }
}
