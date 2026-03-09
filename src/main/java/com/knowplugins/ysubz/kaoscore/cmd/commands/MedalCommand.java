package com.knowplugins.ysubz.kaoscore.cmd.commands;

import com.knowplugins.ysubz.kaoscore.KaosCore;
import com.knowplugins.ysubz.kaoscore.cmd.Commands;
import com.knowplugins.ysubz.kaoscore.managers.MedalManager;
import com.knowplugins.ysubz.kaoscore.medal.Medal;
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
        super("medalha", "medal", "medals");
        this.plugin = plugin;
    }

    @Override
    public void perform(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cApenas jogadores podem usar este comando!");
            return;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("nardocore.medal.use")) {
            player.sendMessage("§cVocê não tem permissão para usar medalhas.");
            return;
        }

        MedalManager medalManager = plugin == null ? null : plugin.getMedalManager();
        if (medalManager == null) {
            player.sendMessage("§cSistema de medalhas indisponível.");
            return;
        }

        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("limpar") || args[0].equalsIgnoreCase("clear") || args[0].equalsIgnoreCase("none")) {
                medalManager.clearMedal(player);
                player.sendMessage("§aSua medalha foi removida.");
                return;
            }

            Medal selected = medalManager.getMedalByNameOrId(args[0]);
            if (selected == null || !medalManager.canUse(player, selected)) {
                player.sendMessage("§cMedalha não encontrada ou sem permissão.");
                return;
            }

            medalManager.setMedal(player, selected.getId());
            player.sendMessage("§aMedalha alterada para: §f" + selected.getName());
            return;
        }

        List<Medal> availableMedals = medalManager.getAvailableMedals(player);
        if (availableMedals.isEmpty()) {
            player.sendMessage("§cVocê não possui medalhas disponíveis.");
            return;
        }

        player.sendMessage("§6Medalhas disponíveis: §7(clique para equipar)");
        TextComponent line = new TextComponent("");
        for (int i = 0; i < availableMedals.size(); i++) {
            Medal medal = availableMedals.get(i);
            TextComponent component = new TextComponent(medal.getChatFrame(0) + "§f" + medal.getName());
            component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/medalha " + medal.getId()));
            component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§7Clique para equipar §f" + medal.getName()).create()));
            line.addExtra(component);
            if (i < availableMedals.size() - 1) {
                line.addExtra(new TextComponent("§7, "));
            }
        }
        player.spigot().sendMessage(line);
        player.sendMessage("§7Use §f/medalha limpar §7para remover a medalha atual.");
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        if (!(sender instanceof Player) || args.length != 1 || plugin == null || plugin.getMedalManager() == null) {
            return Collections.emptyList();
        }

        Player player = (Player) sender;
        if (!player.hasPermission("nardocore.medal.use")) {
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
