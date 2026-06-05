package com.knowplugins.ysubz.kaoscore.bungee.bootstrap;

import com.knowplugins.ysubz.kaoscore.bungee.BungeeDatabase;
import com.knowplugins.ysubz.kaoscore.bungee.BungeePunishmentMessageManager;
import com.knowplugins.ysubz.kaoscore.bungee.commands.BungeeCommands;
import com.knowplugins.ysubz.kaoscore.bungee.listeners.BungeeConnectionListener;
import com.knowplugins.ysubz.kaoscore.bungee.listeners.BungeeStaffCommandLogListener;
import com.knowplugins.ysubz.kaoscore.bungee.repository.BungeePlayerAddressRepository;
import com.knowplugins.ysubz.kaoscore.bungee.services.*;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public final class BungeeKaosCore extends Plugin {

    private Configuration config;
    private Configuration permissionsConfig;
    private Configuration punishConfig;
    private Configuration webhooksConfig;
    private Configuration whitelistConfig;

    private BungeeDatabase database;
    private BungeePunishmentMessageManager punishmentMessages;
    private BungeeDiscordWebhookManager webhookManager;
    private BungeePunishmentService punishments;
    private BungeePermissionService permissions;
    private BungeePlayerAddressRepository playerAddresses;
    private BungeeWhitelistService whitelist;
    private BungeeDisguiseService disguises;
    private BungeeSkinService skins;
    private BungeeClanService clans;
    private BungeeFriendService friends;

    @Override
    public void onEnable() {
        getDataFolder().mkdirs();
        ensureResource("config.yml");
        ensureResource("permissions.yml");
        ensureResource("punish.yml");
        ensureResource("webhooks.yml");
        ensureResource("whitelist.yml");

        reloadSystems();

        ProxyServer.getInstance().getPluginManager().registerListener(this, new BungeeConnectionListener(this));
        ProxyServer.getInstance().getPluginManager().registerListener(this, new BungeeStaffCommandLogListener(this));

        BungeeCommands.registerAll(this);
        getLogger().info("KaosCore Bungee carregado.");
    }

    @Override
    public void onDisable() {
        if (database != null) {
            database.disconnect();
        }
    }

    public synchronized void reloadSystems() {
        try {
            this.config = loadConfig("config.yml");
            this.permissionsConfig = loadConfig("permissions.yml");
            this.punishConfig = loadConfig("punish.yml");
            this.webhooksConfig = loadConfig("webhooks.yml");
            this.whitelistConfig = loadConfig("whitelist.yml");
        } catch (Exception exception) {
            throw new IllegalStateException("Falha ao carregar configuracoes do modulo bungee", exception);
        }

        if (database != null) {
            database.disconnect();
        }
        database = new BungeeDatabase(this, config);
        if (!database.connect(config)) {
            throw new IllegalStateException("Falha ao conectar no banco do modulo bungee");
        }

        if (punishmentMessages == null) {
            punishmentMessages = new BungeePunishmentMessageManager(punishConfig);
        } else {
            punishmentMessages.reload(punishConfig);
        }

        if (webhookManager == null) {
            webhookManager = new BungeeDiscordWebhookManager(this, webhooksConfig);
        } else {
            webhookManager.reload(webhooksConfig);
        }

        playerAddresses = new BungeePlayerAddressRepository(this, database);
        punishments = new BungeePunishmentService(this, database, webhookManager);
        permissions = new BungeePermissionService(this, database, permissionsConfig);
        whitelist = new BungeeWhitelistService(this, new File(getDataFolder(), "whitelist.yml"), whitelistConfig);
        disguises = new BungeeDisguiseService(this, database, playerAddresses);
        skins = new BungeeSkinService(this, database);
        clans = new BungeeClanService(this, database, playerAddresses);
        friends = new BungeeFriendService(this, database, playerAddresses);
    }

    public Configuration getMainConfig() {
        return config;
    }

    public Configuration getPermissionsConfig() {
        return permissionsConfig;
    }

    public Configuration getPunishConfig() {
        return punishConfig;
    }

    public Configuration getWebhooksConfig() {
        return webhooksConfig;
    }

    public Configuration getWhitelistConfig() {
        return whitelistConfig;
    }

    public BungeePunishmentMessageManager getPunishmentMessages() {
        return punishmentMessages;
    }

    public BungeeDiscordWebhookManager getWebhookManager() {
        return webhookManager;
    }

    public BungeePunishmentService getPunishments() {
        return punishments;
    }

    public BungeePermissionService getPermissions() {
        return permissions;
    }

    public BungeePlayerAddressRepository getPlayerAddresses() {
        return playerAddresses;
    }

    public BungeeWhitelistService getWhitelist() {
        return whitelist;
    }

    public BungeeDisguiseService getDisguises() {
        return disguises;
    }

    public BungeeSkinService getSkins() {
        return skins;
    }

    public BungeeClanService getClans() {
        return clans;
    }

    public BungeeFriendService getFriends() {
        return friends;
    }

    private Configuration loadConfig(String name) throws Exception {
        return ConfigurationProvider.getProvider(YamlConfiguration.class).load(new File(getDataFolder(), name));
    }

    private void ensureResource(String name) {
        File target = new File(getDataFolder(), name);
        if (target.exists()) {
            return;
        }

        try (InputStream input = getResourceAsStream(name)) {
            if (input == null) {
                return;
            }
            Files.copy(input, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception ignored) {
            try (InputStream input = getResourceAsStream(name); FileOutputStream output = new FileOutputStream(target)) {
                if (input == null) {
                    return;
                }
                byte[] buffer = new byte[4096];
                int read;
                while ((read = input.read(buffer)) >= 0) {
                    output.write(buffer, 0, read);
                }
            } catch (Exception exception) {
                throw new IllegalStateException("Falha ao criar recurso padrao: " + name, exception);
            }
        }
    }
}
