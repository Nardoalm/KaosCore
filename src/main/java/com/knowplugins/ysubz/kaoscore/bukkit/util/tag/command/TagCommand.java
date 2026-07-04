package com.knowplugins.ysubz.kaoscore.bukkit.util.tag.command;

import com.knowplugins.ysubz.kaoscore.bukkit.KaosCore;
import com.knowplugins.ysubz.kaoscore.bukkit.command.Commands;
import com.knowplugins.ysubz.kaoscore.bukkit.manager.MainManager;
import com.knowplugins.ysubz.kaoscore.bukkit.disguise.DisguiseManager;
import com.knowplugins.ysubz.kaoscore.bukkit.util.tag.Tag;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TagCommand extends Commands {

    private final KaosCore plugin = KaosCore.getInstance();

    public TagCommand() {
        super("tag", "tags", "prefix");
    }

    @Override
    public void perform(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player)) {
            return;
        }

        Player player = (Player) sender;
        if (plugin == null || plugin.getTagManager() == null) {
            return;
        }

        if (args != null && args.length > 0) {
            Tag selected = plugin.getTagManager().getTagByNameOrId(args[0]);
            if (selected == null) {
                player.sendMessage("§cTag not found.");
                sendAvailableTags(player);
                return;
            }

            if(selected == plugin.getTagManager().getTag(player)){
                player.sendMessage("§cYou are already using this tag.");
                return;
            }

            if (!plugin.getTagManager().canUse(player, selected) && !DisguiseManager.isDisguised(player)) {
                player.sendMessage("§cYou do not have permission to use this tag.");
                sendAvailableTags(player);
                return;
            }

            DisguiseManager disguiseManager = MainManager.getDisguiseManager();

            plugin.getTagManager().setTag(player, selected.getId());

            if (disguiseManager != null && DisguiseManager.isDisguised(player)) {
                disguiseManager.refreshGlobalNametags(player, false);
            }

            player.sendMessage("§aTag changed to " + format(selected.getColor()) + selected.getName() + "§a.");
            return;
        }

        sendAvailableTags(player);
    }

    private void sendAvailableTags(Player player) {
        List<Tag> availableTags = plugin.getTagManager().getAvailableTags(player);
        if (availableTags.isEmpty()) {
            Tag fallback = plugin.getTagManager().getTagByNameOrId("membro");
            if (fallback != null) {
                availableTags = new ArrayList<>(Collections.singletonList(fallback));
            }
        }

        TextComponent line = new TextComponent("§aYour tags: ");

        for (int i = availableTags.size() - 1; i >= 0; i--) {
            Tag tag = availableTags.get(i);
            if (tag == null) continue;

            String displayName = format(tag.getColor()) + tag.getName();
            TextComponent component = new TextComponent(displayName);
            component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tag " + tag.getId()));
            component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Click to equip " + displayName).create()));
            line.addExtra(component);

            if (i > 0) {
                line.addExtra(new TextComponent(ChatColor.WHITE + ", "));
            }
        }

        if (line.getExtra() != null && !line.getExtra().isEmpty()) {
            player.spigot().sendMessage(line);
        } else {
            player.sendMessage("§cNo tags available.");
        }
    }

    private String format(String text) {
        return text == null ? "" : ChatColor.translateAlternateColorCodes('&', text);
    }
}