package com.knowplugins.ysubz.kaoscore.bukkit.clan.command;

import com.knowplugins.ysubz.kaoscore.bukkit.cmd.Commands;
import com.knowplugins.ysubz.kaoscore.bukkit.manager.MainManager;
import com.knowplugins.ysubz.kaoscore.bukkit.clan.Clan;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ClanChatCommand extends Commands {
    public ClanChatCommand() { super("cc", "clanchat"); }

    @Override
    public void perform(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player)) return;

        Player p = (Player) sender;

        if (args.length == 0) { p.sendMessage("§cUsage: /cc <message>"); return; }

        if (!MainManager.getClanManager().isInClan(p.getUniqueId())) { p.sendMessage("§cYou are not in a clan."); return; }

        String msg = String.join(" ", args);
        Clan clan = MainManager.getClanManager().getPlayerClan(p.getUniqueId());

        clan.getMembers().keySet().forEach(uuid -> {
            Player target = org.bukkit.Bukkit.getPlayer(uuid);
            if (target != null) {
                target.sendMessage("§6[CLAN] " + p.getDisplayName() + " §8» §7" + msg);
            }
        });
    }
}

