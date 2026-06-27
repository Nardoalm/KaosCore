package com.knowplugins.ysubz.kaoscore.bukkit.cmd.commands;

import com.knowplugins.ysubz.kaoscore.bukkit.KaosCore;
import com.knowplugins.ysubz.kaoscore.bukkit.cmd.Commands;
import com.knowplugins.ysubz.kaoscore.bukkit.managers.player.MedalManager;
import com.knowplugins.ysubz.kaoscore.bukkit.utils.medal.Medal;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class MedalCommand extends Commands {

    private final KaosCore plugin;

    public MedalCommand(KaosCore plugin) {
        super("medalha", "medal", "medals", "medalhas");
        this.plugin = plugin;
    }

    @Override
    public void perform(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return;
        }

        Player player = (Player) sender;

        MedalManager medalManager = plugin == null ? null : plugin.getMedalManager();
        if (medalManager == null) {
            player.sendMessage("§cMedal system unavailable.");
            return;
        }

        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("limpar") || args[0].equalsIgnoreCase("clear") || args[0].equalsIgnoreCase("none")) {
                medalManager.clearMedal(player);
                player.sendMessage("§aYour medal was removed.");
                return;
            }

            Medal selected = medalManager.getMedalByNameOrId(args[0]);
            if (selected == null || !medalManager.canUse(player, selected)) {
                player.sendMessage("§cMedal not found or you do not have permission.");
                return;
            }

            medalManager.setMedal(player, selected.getId());
            player.sendMessage("§aMedal changed to " + selected.getName().toLowerCase() + "§a.");
            return;
        }

        List<Medal> availableMedals = medalManager.getAvailableMedals(player);
        if (availableMedals.isEmpty()) {
            player.sendMessage("§cYou do not have any medals.");
            return;
        }

        TextComponent line = new TextComponent("§aYour medals: ");
        for (int i = 0; i < availableMedals.size(); i++) {
            Medal medal = availableMedals.get(i);
                TextComponent component = new TextComponent(medal.getStaticChat());
            component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/medalha " + medal.getId()));
            component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§7Click to equip §f" + medal.getStaticChat()).create()));
            line.addExtra(component);
            if (i < availableMedals.size() - 1) {
                line.addExtra(new TextComponent("§f, "));
            }
        }
        player.spigot().sendMessage(line);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        if (!(sender instanceof Player) || args.length != 1 || plugin == null || plugin.getMedalManager() == null) {
            return Collections.emptyList();
        }

        Player player = (Player) sender;
        if (!player.hasPermission("kaoscore.medal.use")) {
            return Collections.emptyList();
        }

        List<String> suggestions = plugin.getMedalManager().getAvailableMedals(player).stream()
                .map(Medal::getId)
                .collect(Collectors.toList());
        suggestions.add("limpar");

        return suggestions.stream()
                .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(args[0].toLowerCase(Locale.ROOT)))
                .collect(Collectors.toList());
    }
}
