package com.knowplugins.ysubz.kaoscore.bukkit.cmd.commands;

import com.knowplugins.ysubz.kaoscore.KaosCore;
import com.knowplugins.ysubz.kaoscore.bukkit.cmd.Commands;
import com.knowplugins.ysubz.kaoscore.bukkit.managers.Managers;
import com.knowplugins.ysubz.kaoscore.bukkit.managers.player.WelcomeMessageManager;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class WelcomeMessageCommand extends Commands {

    public WelcomeMessageCommand() {
        super("welcomemessage", "wm", "welcome");
    }

    @Override
    public void perform(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c§lWELCOME ➔ §cApenas jogadores podem utilizar este comando.");
            return;
        }

        Player player = (Player) sender;
        KaosCore plugin = KaosCore.getInstance();
        if (plugin == null || !plugin.getConfig().getBoolean("features.welcome-messages.enabled", true)) {
            player.sendMessage("§c§lWELCOME ➔ §cSistema de mensagens de entrada está desativado.");
            player.playSound(player.getLocation(), Sound.CLICK, 1.0f, 1.0f);
            return;
        }

        if (!player.hasPermission("kaoscore.member.welcomeMessage")) {
            player.sendMessage("§c§lWELCOME ➔ §cVocê não possui permissão para usar este comando.");
            player.playSound(player.getLocation(), Sound.CLICK, 1.0f, 1.0f);
            return;
        }

        WelcomeMessageManager manager = Managers.getWelcomeMessageManager();
        if (manager == null) {
            player.sendMessage("§c§lWELCOME ➔ §cSistema indisponível.");
            player.playSound(player.getLocation(), Sound.CLICK, 1.0f, 1.0f);
            return;
        }

        manager.openMenu(player);
        player.playSound(player.getLocation(), Sound.CLICK, 1.0f, 1.0f);
    }
}

