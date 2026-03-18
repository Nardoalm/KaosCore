package com.knowplugins.ysubz.kaoscore.cmd.commands.staff;

import com.knowplugins.ysubz.kaoscore.cmd.Commands;
import com.knowplugins.ysubz.kaoscore.managers.Managers;
import com.knowplugins.ysubz.kaoscore.managers.world.NPCManager;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class RemoveNpcCommand extends Commands {

    public RemoveNpcCommand() {
        super("removenpc", "delnpc");
    }

    @Override
    public void perform(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c§lNPC ➔ §cApenas jogadores podem usar esse comando.");
            return;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("kaoscore.admin.setnpc")) {
            player.sendMessage("§c§lNPC ➔ §cVocê não tem permissão para usar esse comando.");
            player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        if (args == null || args.length < 1) {
            player.sendMessage("§c§lNPC ➔ §cUso: /removenpc <id>");
            player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        NPCManager manager = Managers.getNpcManager();
        if (manager == null) {
            player.sendMessage("§c§lNPC ➔ §cSistema de NPC desativado.");
            player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        String id = args[0];
        boolean ok = manager.removeNpc(id);
        if (!ok) {
            player.sendMessage("§c§lNPC ➔ §cNPC não encontrado: §f" + id);
            player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        player.sendMessage("§a§lNPC ➔ §aNPC removido: §f" + id);
        player.playSound(player.getLocation(), Sound.LEVEL_UP, 1.0f, 1.0f);
    }
}

