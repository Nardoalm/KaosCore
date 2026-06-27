package com.knowplugins.ysubz.kaoscore.bukkit.cmd.commands.staff;

import com.knowplugins.ysubz.kaoscore.bukkit.KaosCore;
import com.knowplugins.ysubz.kaoscore.bukkit.cmd.Commands;
import com.knowplugins.ysubz.kaoscore.bukkit.managers.Managers;
import com.knowplugins.ysubz.kaoscore.bukkit.disguise.DisguiseManager;
import com.knowplugins.ysubz.kaoscore.bukkit.managers.player.TagManager;
import com.knowplugins.ysubz.kaoscore.bukkit.utils.tag.Tag;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AdminChatCommand extends Commands {

    public AdminChatCommand() {
        super("adminchat", "ac");
    }

    @Override
    public void perform(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can run this command!");
            return;
        }

        Player playerSender = (Player) sender;

        if (!playerSender.hasPermission("kaoscore.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return;
        }

        if (args == null || args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Usage: /adminchat <message>");
            return;
        }

        String rawMessage = String.join(" ", args);
        KaosCore plugin = KaosCore.getInstance();
        if (plugin == null) return;

        TagManager tagManager = plugin.getTagManager();
        String tagColor = "";
        if (tagManager != null) {
            Tag tag = tagManager.getTag(playerSender);
            if (tag != null && tag.getColor() != null) {
                tagColor = ChatColor.translateAlternateColorCodes('&', tag.getColor());
            }
        }

        DisguiseManager disguiseManager = Managers.getDisguiseManager();
        String realName = playerSender.getName();

        if (disguiseManager != null && DisguiseManager.isDisguised(playerSender)) {
            String original = disguiseManager.getOriginalName(playerSender);
            if (original != null) {
                realName = original;
            }
        }

        String formattedMessage = ChatColor.translateAlternateColorCodes('&', "&c[AdminChat] ") + tagColor + realName + "§f: " + rawMessage;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player != null && player.isOnline() && player.hasPermission("kaoscore.admin")) {
                player.sendMessage(formattedMessage);
            }
        }
    }
}