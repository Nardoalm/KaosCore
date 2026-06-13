package com.knowplugins.ysubz.kaoscore.bungee.commands;

import com.knowplugins.ysubz.kaoscore.bungee.bootstrap.BungeeKaosCore;
import com.knowplugins.ysubz.kaoscore.bungee.util.Texts;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public abstract class BaseBungeeCommand extends Command {

    protected final BungeeKaosCore plugin;

    protected BaseBungeeCommand(BungeeKaosCore plugin, String name, String... aliases) {
        super(name, "", aliases);
        this.plugin = plugin;
    }

    protected ProxiedPlayer playerOnly(CommandSender sender) {
        if (!(sender instanceof ProxiedPlayer)) {
            Texts.send(sender, "§cApenas jogadores podem usar este comando.");
            return null;
        }
        return (ProxiedPlayer) sender;
    }

    protected boolean require(CommandSender sender, String permission, String message) {
        if (plugin.getPermissions(). (sender, permission)) {
            return true;
        }
        Texts.send(sender, message);
        return false;
    }

    protected void reply(CommandSender sender, String message) {
        Texts.send(sender, message);
    }
}