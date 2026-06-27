package com.knowplugins.ysubz.kaoscore.bukkit.cmd.commands.staff;

import com.knowplugins.ysubz.kaoscore.KaosCore;
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
            sender.sendMessage("§c§lNPC ➔ §cApenas jogadores podem usar esse comando.");
            return;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("kaoscore.admin.setnpc")) {
            player.sendMessage("§c§lNPC ➔ §cVocê não tem permissão para usar esse comando.");
            player.playSound(player.getLocation(), Sound.CLICK, 1.0f, 1.0f);
            return;
        }

        if (args == null || args.length < 1) {
            player.sendMessage("§c§lNPC ➔ §cUso: /setnpc <id> [skinNick]");
            player.playSound(player.getLocation(), Sound.CLICK, 1.0f, 1.0f);
            return;
        }

        KaosCore plugin = KaosCore.getInstance();
        NPCManager manager = Managers.getNpcManager();
        if (plugin == null || manager == null) {
            player.sendMessage("§c§lNPC ➔ §cSistema de NPC desativado. Ative em config.yml: npc-spawn.enabled: true");
            player.playSound(player.getLocation(), Sound.CLICK, 1.0f, 1.0f);
            return;
        }

        String id = args[0];
        String skinNick = args.length >= 2 ? args[1] : null;
        Location loc = player.getLocation();

        boolean ok = manager.upsertNpc(id, loc, skinNick);
        if (!ok) {
            player.sendMessage("§c§lNPC ➔ §cNão foi possível salvar o NPC.");
            player.playSound(player.getLocation(), Sound.CLICK, 1.0f, 1.0f);
            return;
        }

        player.sendMessage("§a§lNPC ➔ §aNPC '" + id + "' salvo em §fnpcs.yml§a.");
        player.playSound(player.getLocation(), Sound.LEVEL_UP, 1.0f, 1.0f);
    }
}

