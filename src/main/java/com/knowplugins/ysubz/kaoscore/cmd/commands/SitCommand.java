package com.knowplugins.ysubz.kaoscore.cmd.commands;

import com.knowplugins.ysubz.kaoscore.cmd.Commands;
import com.knowplugins.ysubz.kaoscore.managers.Managers;
import com.knowplugins.ysubz.kaoscore.managers.player.SitManager;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SitCommand extends Commands {

    public SitCommand() {
        super("sit", "sentar");
    }

    @Override
    public void perform(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c§lSIT ➔ §cApenas jogadores podem utilizar este comando.");
            return;
        }

        Player player = (Player) sender;
        SitManager sitManager = Managers.getSitManager();

        if (sitManager == null) {
            player.sendMessage("§c§lSIT ➔ §cO sistema de assentos está temporariamente indisponível.");
            return;
        }

        if (!sitManager.hasPermission(player)) {
            player.sendMessage("§c§lSIT ➔ §cVocê não tem permissão para executar esse comando.");
            player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        if (!sitManager.sit(player)) {
            player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1.0f, 1.0f);
        } else {
            player.sendMessage("§e§lSIT ➔ §eVocê sentou. Pressiona §6SHIFT §epara levantar.");
        }
    }
}