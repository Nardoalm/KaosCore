package com.knowplugins.ysubz.kaoscore.bukkit.cmd.list.staff;

import com.knowplugins.ysubz.kaoscore.bukkit.KaosCore;
import com.knowplugins.ysubz.kaoscore.bukkit.cmd.Commands;
import com.knowplugins.ysubz.kaoscore.bukkit.managers.Managers;
import com.knowplugins.ysubz.kaoscore.bukkit.managers.world.NPCManager;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class SetNpcCommand extends Commands {

    public SetNpcCommand() {
        super("setnpc");
    }

    @Override
    public void perform(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c§lNPC ➔ §cOnly players can use this command.");
            return;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("kaoscore.admin.setnpc")) {
            player.sendMessage("§c§lNPC ➔ §cYou do not have permission to use this command.");
            player.playSound(player.getLocation(), Sound.CLICK, 1.0f, 1.0f);
            return;
        }

        if (args == null || args.length < 1) {
            player.sendMessage("§c§lNPC ➔ §cUsage: /setnpc <id> [skinNick]");
            player.playSound(player.getLocation(), Sound.CLICK, 1.0f, 1.0f);
            return;
        }

        KaosCore plugin = KaosCore.getInstance();
        NPCManager manager = Managers.getNpcManager();
        if (plugin == null || manager == null || !manager.isEnabled()) {
            player.sendMessage("§c§lNPC ➔ §cNPCs are disabled in npcs.yml.");
            player.playSound(player.getLocation(), Sound.CLICK, 1.0f, 1.0f);
            return;
        }

        String id = args[0];
        String skinNick = args.length >= 2 ? args[1] : null;
        Location loc = player.getLocation();

        boolean ok = manager.upsertNpc(id, loc, skinNick);
        if (!ok) {
            player.sendMessage("§c§lNPC ➔ §cUnable to save the NPC.");
            player.playSound(player.getLocation(), Sound.CLICK, 1.0f, 1.0f);
            return;
        }

        player.sendMessage("§a§lNPC ➔ §aNPC '" + id + "' saved in §fnpcs.yml§a.");
        player.playSound(player.getLocation(), Sound.NOTE_PLING, 1.0f, 1.0f);
    }
}
