package com.knowplugins.ysubz.kaoscore.bungee;

import com.knowplugins.ysubz.kaoscore.bungee.bootstrap.BungeeKaosCore;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

final class KaosCoreReloadCommand extends BaseBungeeCommand {

    KaosCoreReloadCommand(BungeeKaosCore plugin) {
        super(plugin, "kaoscore");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!require(sender, "kaoscore.staff.reload", "§c§lKAOSCORE ➔ §cVocê não tem permissão para usar esse comando.")) {
            return;
        }
        if (args.length != 1 || !"reload".equalsIgnoreCase(args[0])) {
            reply(sender, "§e§lKAOSCORE ➔ §eUse: /kaoscore reload");
            return;
        }

        try {
            plugin.reloadSystems();
            reply(sender, "§a§lKAOSCORE ➔ §aAs configurações foram recarregadas com sucesso.");
        } catch (Exception exception) {
            reply(sender, "§c§lKAOSCORE ➔ §cFalha ao recarregar. Veja o console.");
            plugin.getLogger().severe("Falha ao executar /kaoscore reload: " + exception.getMessage());
        }
    }
}
