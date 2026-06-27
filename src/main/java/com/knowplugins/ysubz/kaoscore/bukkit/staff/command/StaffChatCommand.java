package com.knowplugins.ysubz.kaoscore.bukkit.staff.command;

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

public class StaffChatCommand extends Commands {

    public StaffChatCommand() {
        super("staffchat", "sc");
    }

    @Override
    public void perform(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can run this command!");
            return;
        }

        Player playerSender = (Player) sender;

        if (!playerSender.hasPermission("kaoscore.staff")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return;
        }

        if (args == null || args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Usage: /staffchat <message>");
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

        String formattedMessage = ChatColor.translateAlternateColorCodes('&', "&e[StaffChat] ") + tagColor + realName + "§f: " + rawMessage;

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online != null && online.isOnline() && online.hasPermission("kaoscore.staff")) {
                online.sendMessage(formattedMessage);
            }
        }
    }
}