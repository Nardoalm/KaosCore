package com.knowplugins.ysubz.kaoscore.clan.cmds;

import com.knowplugins.ysubz.kaoscore.cmd.Commands;
import com.knowplugins.ysubz.kaoscore.managers.Managers;
import com.knowplugins.ysubz.kaoscore.clan.Clan;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CcCommand extends Commands {
    public CcCommand() { super("cc", "clanchat"); }

    @Override
    public void perform(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player)) return;

        Player p = (Player) sender;

        if (args.length == 0) { p.sendMessage("§cUso correto: /cc <mensagem>"); return; }

        if (!Managers.getClanManager().isInClan(p.getUniqueId())) { p.sendMessage("§cVocê não está em um clan."); return; }

        String msg = String.join(" ", args);
        Clan clan = Managers.getClanManager().getPlayerClan(p.getUniqueId());

        clan.getMembers().keySet().forEach(uuid -> {
            Player target = org.bukkit.Bukkit.getPlayer(uuid);
            if (target != null) {
                target.sendMessage("§6[CLAN] " + p.getDisplayName() + " §8» §7" + msg);
            }
        });
    }
}

