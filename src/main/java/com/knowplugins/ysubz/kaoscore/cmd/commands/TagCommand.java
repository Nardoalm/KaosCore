package com.knowplugins.ysubz.kaoscore.cmd.commands;

import com.knowplugins.ysubz.kaoscore.KaosCore;
import com.knowplugins.ysubz.kaoscore.tag.Tag;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class TagCommand implements CommandExecutor {

    private final KaosCore plugin;

    public TagCommand(KaosCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            return true;
        }

        Player player = (Player) sender;
        if (plugin == null || plugin.getTagManager() == null) {
            return true;
        }

        if (args != null && args.length > 0) {
            Tag selected = plugin.getTagManager().getTagByNameOrId(args[0]);
            if (selected == null) {
                return true;
            }

            if (!plugin.getTagManager().canUse(player, selected)) {
                return true;
            }

            plugin.getTagManager().setTag(player, selected.getId());
            return true;
        }

        player.sendMessage("Suas tags: ");

        List<Tag> availableTags = plugin.getTagManager().getAvailableTags(player);
        for (Tag tag : availableTags) {
            if (tag == null) {
                continue;
            }

            String displayName = format(tag.getColor()) + tag.getName();
            TextComponent component = new TextComponent(displayName);
            component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tag " + tag.getName()));
            component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Clique para equipar a tag").create()));
            player.spigot().sendMessage(component);
        }

        return true;
    }

    private String format(String text) {
        if (text == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
