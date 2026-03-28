package com.knowplugins.ysubz.kaoscore.bungee;

import net.md_5.bungee.api.ProxyServer;

public final class BungeeCommands {

    private BungeeCommands() {
    }

    public static void registerAll(BungeeKaosCore plugin) {
        ProxyServer.getInstance().getPluginManager().registerCommand(plugin, new KaosCoreReloadCommand(plugin));
        ProxyServer.getInstance().getPluginManager().registerCommand(plugin, new BanBungeeCommand(plugin));
        ProxyServer.getInstance().getPluginManager().registerCommand(plugin, new CbanBungeeCommand(plugin));
        ProxyServer.getInstance().getPluginManager().registerCommand(plugin, new UnbanBungeeCommand(plugin));
        ProxyServer.getInstance().getPluginManager().registerCommand(plugin, new MuteBungeeCommand(plugin));
        ProxyServer.getInstance().getPluginManager().registerCommand(plugin, new UnmuteBungeeCommand(plugin));
        ProxyServer.getInstance().getPluginManager().registerCommand(plugin, new WarnBungeeCommand(plugin));
        ProxyServer.getInstance().getPluginManager().registerCommand(plugin, new KickBungeeCommand(plugin));
        ProxyServer.getInstance().getPluginManager().registerCommand(plugin, new HistoryBungeeCommand(plugin));
        ProxyServer.getInstance().getPluginManager().registerCommand(plugin, new AltsBungeeCommand(plugin));
        ProxyServer.getInstance().getPluginManager().registerCommand(plugin, new GrantBungeeCommand(plugin));
        ProxyServer.getInstance().getPluginManager().registerCommand(plugin, new UngrantBungeeCommand(plugin));
        ProxyServer.getInstance().getPluginManager().registerCommand(plugin, new GroupBungeeCommand(plugin));
        ProxyServer.getInstance().getPluginManager().registerCommand(plugin, new WhitelistBungeeCommand(plugin));
        ProxyServer.getInstance().getPluginManager().registerCommand(plugin, new DiscloseBungeeCommand(plugin));
        ProxyServer.getInstance().getPluginManager().registerCommand(plugin, new ReportBungeeCommand(plugin));
        ProxyServer.getInstance().getPluginManager().registerCommand(plugin, new DisguiseBungeeCommand(plugin));
        ProxyServer.getInstance().getPluginManager().registerCommand(plugin, new SkinBungeeCommand(plugin));
        ProxyServer.getInstance().getPluginManager().registerCommand(plugin, new ClanBungeeCommand(plugin));
        ProxyServer.getInstance().getPluginManager().registerCommand(plugin, new CcBungeeCommand(plugin));
        ProxyServer.getInstance().getPluginManager().registerCommand(plugin, new FriendBungeeCommand(plugin));
    }
}


