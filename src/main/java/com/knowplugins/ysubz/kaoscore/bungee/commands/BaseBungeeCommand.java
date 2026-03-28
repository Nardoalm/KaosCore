package com.knowplugins.ysubz.kaoscore.bungee;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public abstract class BaseBungeeCommand extends Command {

    protected final BungeeKaosCore plugin;

    protected BaseBungeeCommand(BungeeKaosCore plugin, String name, String... aliases) {
        super(name, null, aliases);
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
        if (plugin.getPermissions().hasPermission(sender, permission)) {
            return true;
        }
        Texts.send(sender, message);
        return false;
    }

    protected void reply(CommandSender sender, String message) {
        Texts.send(sender, message);
    }
}


