package com.knowplugins.ysubz.kaoscore.bukkit.auth.listener;

import com.knowplugins.ysubz.kaoscore.bukkit.KaosCore;
import com.knowplugins.ysubz.kaoscore.bukkit.auth.profile.AuthProfile;
import com.knowplugins.ysubz.kaoscore.bukkit.auth.util.MojangAPI;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.util.UUID;

public class AuthPreLoginListener implements Listener {

    private final KaosCore plugin;

    public AuthPreLoginListener(KaosCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void processPreLogin(AsyncPlayerPreLoginEvent event) {
        String name = event.getName();

        // Se o player passou pela verificação RSA da Mojang com sucesso,
        // o Spigot JÁ VAI ter puxado o UUID correto devido à injeção "spoofedUUID" no Netty.
        UUID finalUUID = event.getUniqueId();

        // Remove do cache de verificação (limpeza de segurança)
        UUID premiumUUID = plugin.getAuthPacketListener().verifiedPremiumPlayers.remove(name);

        try {
            AuthProfile profile = plugin.getAuthManager().getRepository().loadProfile(finalUUID, name).join();

            // Marca o perfil como premium internamente baseado na autenticação criptográfica que fizemos no pacote.
            if (premiumUUID != null) {
                profile.setPremium(true);
            } else if (!profile.isRegistered()) {
                // Prevenção extra para não registrar piratas com nome de original
                boolean isMojangPremium = MojangAPI.isPremium(name);
                if (isMojangPremium) {
                    event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                            "§c§lNICK PROTEGIDO\n\n§cEsse nick pertence a um jogador original.\n§cEscolha outro nome ou use um launcher premium.");
                    return;
                }
            }

            plugin.getAuthManager().cacheProfile(profile);

        } catch (Exception e) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, "§cFalha na comunicação com o banco de dados. Tente novamente.");
            e.printStackTrace();
        }
    }
}