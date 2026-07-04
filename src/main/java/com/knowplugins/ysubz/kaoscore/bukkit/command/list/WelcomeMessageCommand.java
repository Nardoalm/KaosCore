package com.knowplugins.ysubz.kaoscore.bukkit.command.list;

import com.knowplugins.ysubz.kaoscore.bukkit.KaosCore;
import com.knowplugins.ysubz.kaoscore.bukkit.command.Commands;
import com.knowplugins.ysubz.kaoscore.bukkit.manager.MainManager;
import com.knowplugins.ysubz.kaoscore.bukkit.manager.player.JoinMessageManager;
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
            sender.sendMessage("§cOnly players can use this command.");
            return;
        }

        Player player = (Player) sender;
        KaosCore plugin = KaosCore.getInstance();
        if (plugin == null || !plugin.getConfig().getBoolean("features.welcome-messages.enabled", true)) {
            player.sendMessage("§cWelcome messages are disabled.");
            player.playSound(player.getLocation(), Sound.CLICK, 1.0f, 1.0f);
            return;
        }

        if (!player.hasPermission("kaoscore.joinmessage")) {
            player.sendMessage("§cYou do not have permission to use this command.");
            player.playSound(player.getLocation(), Sound.CLICK, 1.0f, 1.0f);
            return;
        }

        JoinMessageManager manager = MainManager.getJoinMessageManager();
        if (manager == null) {
            player.sendMessage("§cSystem unavailable.");
            player.playSound(player.getLocation(), Sound.CLICK, 1.0f, 1.0f);
            return;
        }

        manager.openMenu(player);
        player.playSound(player.getLocation(), Sound.CLICK, 1.0f, 1.0f);
    }
}
