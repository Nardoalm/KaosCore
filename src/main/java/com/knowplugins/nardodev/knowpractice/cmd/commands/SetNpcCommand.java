package com.knowplugins.nardodev.knowpractice.cmd.commands;

import com.knowplugins.nardodev.knowpractice.KnowPractice;
import com.knowplugins.nardodev.knowpractice.cmd.Commands;
import com.knowplugins.nardodev.knowpractice.managers.Managers;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetNpcCommand extends Commands {

    public SetNpcCommand() {
        super("setnpc");
    }

    @Override
    public void perform(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c§lERRO ➔ §cApenas jogadores podem usar esse comando.");
            return;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("nardocore.admin.setnpc")) {
            player.sendMessage("§c§lERRO ➔ §cVocê não tem permissão para usar esse comando.");
            player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        KnowPractice plugin = KnowPractice.getInstance();
        Location loc = player.getLocation();

        plugin.getConfig().set("npc-spawn.world", loc.getWorld().getName());
        plugin.getConfig().set("npc-spawn.x", loc.getX());
        plugin.getConfig().set("npc-spawn.y", loc.getY());
        plugin.getConfig().set("npc-spawn.z", loc.getZ());
        plugin.getConfig().set("npc-spawn.yaw", (float) loc.getYaw());
        plugin.getConfig().set("npc-spawn.pitch", (float) loc.getPitch());
        plugin.saveConfig();

        if (Managers.getNpcManager() != null) {
            Managers.getNpcManager().spawnNPC(player, loc);
        }

        player.sendMessage("§a§lSUCESSO ➔ §aSpawn do NPC definido com sucesso!");
        player.playSound(player.getLocation(), Sound.LEVEL_UP, 1.0f, 1.0f);
    }
}
