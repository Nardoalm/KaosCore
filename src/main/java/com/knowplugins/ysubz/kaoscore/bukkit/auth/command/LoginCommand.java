package com.knowplugins.ysubz.kaoscore.bukkit.auth.command;

import com.knowplugins.ysubz.kaoscore.bukkit.KaosCore;
import com.knowplugins.ysubz.kaoscore.bukkit.command.Commands;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import live.pvphard.practice.HardPractice;
import live.pvphard.practice.feature.hotbar.HotbarService;
import live.pvphard.practice.feature.spawn.SpawnService;

public class LoginCommand extends Commands {

    private final KaosCore plugin;

    public LoginCommand(KaosCore plugin) {
        super("login");
        this.plugin = plugin;
    }

    @Override
    public void perform(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player)) return;
        Player player = (Player) sender;

        if (!plugin.getAuthManager().isPending(player.getUniqueId())) {
            player.sendMessage("§cYou are already authenticated.");
            return;
        }

        if (args.length != 1) {
            player.sendMessage("§cUsage: /login <password>");
            return;
        }

        if (!plugin.getAuthManager().login(player, args[0])) {
            player.sendMessage("§cIncorrect password or you are not registered.");
            return;
        }

        if(!plugin.getAuthManager().isPending(player.getUniqueId())) {
            if(Bukkit.getServer().getPluginManager().getPlugin("HardPractice") != null) {
                HardPractice.getInstance().getService(SpawnService.class).teleportToSpawn(player);
                HardPractice.getInstance().getService(HotbarService.class).applyHotbarItems(player);
            }
        }
    }
}