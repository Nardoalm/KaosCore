package com.knowplugins.ysubz.kaoscore.bukkit.auth.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.injector.netty.channel.NettyChannelInjector;
import com.comphenix.protocol.injector.temporary.TemporaryPlayerFactory;
import com.comphenix.protocol.reflect.accessors.Accessors;
import com.comphenix.protocol.reflect.accessors.FieldAccessor;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.knowplugins.ysubz.kaoscore.bukkit.KaosCore;
import com.knowplugins.ysubz.kaoscore.bukkit.auth.profile.AuthProfile;
import com.knowplugins.ysubz.kaoscore.bukkit.auth.security.EncryptionUtil;
import com.knowplugins.ysubz.kaoscore.bukkit.auth.util.MojangAPI;
import com.knowplugins.ysubz.kaoscore.bukkit.managers.Managers;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import javax.crypto.SecretKey;
import java.security.KeyPair;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AuthPacketListener {

    private final KaosCore plugin;
    private final ProtocolManager protocolManager;
    private final KeyPair serverKey;

    private final Map<String, PendingLogin> pendingLogins = new ConcurrentHashMap<>();
    public final Map<String, UUID> verifiedPremiumPlayers = new ConcurrentHashMap<>();
    public final Map<String, Boolean> bypassedPlayers = new ConcurrentHashMap<>();

    public AuthPacketListener(KaosCore plugin) {
        this.plugin = plugin;
        this.protocolManager = ProtocolLibrary.getProtocolManager();
        this.serverKey = EncryptionUtil.generateKeyPair();
        this.registerListeners();
    }

    private void registerListeners() {
        protocolManager.addPacketListener(new PacketAdapter(plugin, ListenerPriority.HIGHEST, PacketType.Login.Client.START) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                String ip = event.getPlayer().getAddress().getAddress().getHostAddress();
                String username = event.getPacket().getGameProfiles().read(0).getName();

                if (bypassedPlayers.containsKey(ip)) {
                    bypassedPlayers.remove(ip);
                    return;
                }

                event.setCancelled(true);

                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    AuthProfile profile = Managers.getAuthManager().getRepository().loadProfile(null, username).join();

                    boolean isPremium = profile.isPremium();
                    if (!profile.isRegistered()) {
                        isPremium = MojangAPI.isPremium(username);
                    }

                    if (isPremium) {
                        byte[] verifyToken = EncryptionUtil.generateVerifyToken();
                        pendingLogins.put(ip, new PendingLogin(username, verifyToken));

                        PacketContainer request = protocolManager.createPacket(PacketType.Login.Server.ENCRYPTION_BEGIN);
                        request.getStrings().write(0, "");
                        request.getSpecificModifier(java.security.PublicKey.class).write(0, serverKey.getPublic());
                        request.getByteArrays().write(0, verifyToken);

                        try {
                            protocolManager.sendServerPacket(event.getPlayer(), request);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        bypassedPlayers.put(ip, true);
                        injectFakeStart(event.getPlayer(), username);
                    }
                });
            }
        });

        protocolManager.addPacketListener(new PacketAdapter(plugin, ListenerPriority.HIGHEST, PacketType.Login.Client.ENCRYPTION_BEGIN) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                event.setCancelled(true);
                String ip = event.getPlayer().getAddress().getAddress().getHostAddress();
                PendingLogin pending = pendingLogins.remove(ip);

                if (pending == null) {
                    kick(event, "§cInvalid login session.");
                    return;
                }

                byte[] sharedSecretEncrypted = event.getPacket().getByteArrays().read(0);
                byte[] verifyTokenEncrypted = event.getPacket().getByteArrays().read(1);

                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    try {
                        byte[] decryptedToken = EncryptionUtil.decryptVerifyToken(serverKey.getPrivate(), verifyTokenEncrypted);
                        if (!Arrays.equals(pending.verifyToken, decryptedToken)) {
                            kick(event, "§cSecurity token mismatch.");
                            return;
                        }

                        SecretKey sharedSecret = EncryptionUtil.decryptSharedSecret(serverKey.getPrivate(), sharedSecretEncrypted);
                        String serverHash = EncryptionUtil.getServerIdHash("", serverKey.getPublic(), sharedSecret);

                        UUID premiumUUID = MojangAPI.hasJoined(pending.username, serverHash);

                        if (premiumUUID != null) {
                            verifiedPremiumPlayers.put(pending.username, premiumUUID);
                            bypassedPlayers.put(ip, true);

                            // A INJEÇÃO SUPREMA: Engana o Spigot para assumir o UUID Original.
                            injectPremiumUUID(event.getPlayer(), premiumUUID);

                            injectFakeStart(event.getPlayer(), pending.username);
                        } else {
                            kick(event, "§cAuthentication with Mojang failed.");
                        }

                    } catch (Exception e) {
                        kick(event, "§cEncryption error.");
                        e.printStackTrace();
                    }
                });
            }
        });
    }

    private void injectPremiumUUID(Player player, UUID premiumUUID) {
        try {
            // Puxa o injetor de canal temporário do ProtocolLib
            NettyChannelInjector injectorContainer = (NettyChannelInjector) Accessors.getMethodAccessorOrNull(
                    TemporaryPlayerFactory.class, "getInjectorFromPlayer", Player.class
            ).invoke(null, player);

            // Acessa a variável networkManager (onde a conexão reside)
            FieldAccessor managerAccessor = Accessors.getFieldAccessorOrNull(
                    NettyChannelInjector.class, "networkManager", Object.class
            );
            Object networkManager = managerAccessor.get(injectorContainer);

            // Seta o "spoofedUUID" que o Spigot lê caso o servidor tenha o bungeecord=true na spigot.yml
            Class<?> managerClass = networkManager.getClass();
            FieldAccessor uuidAccessor = Accessors.getFieldAccessorOrNull(managerClass, "spoofedUUID", UUID.class);
            uuidAccessor.set(networkManager, premiumUUID);

        } catch (Exception exc) {
            plugin.getLogger().severe("Failed to inject premium UUID into NetworkManager!");
            exc.printStackTrace();
        }
    }

    private void injectFakeStart(Player connection, String username) {
        PacketContainer startPacket = protocolManager.createPacket(PacketType.Login.Client.START);
        startPacket.getGameProfiles().write(0, new WrappedGameProfile(UUID.randomUUID(), username));
        try {
            protocolManager.receiveClientPacket(connection, startPacket, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void kick(PacketEvent event, String reason) {
        PacketContainer kickPacket = protocolManager.createPacket(PacketType.Login.Server.DISCONNECT);
        kickPacket.getChatComponents().write(0, WrappedChatComponent.fromText(reason));
        try {
            protocolManager.sendServerPacket(event.getPlayer(), kickPacket);
        } catch (Exception ignored) {}
    }

    private static class PendingLogin {
        String username;
        byte[] verifyToken;
        PendingLogin(String username, byte[] verifyToken) {
            this.username = username;
            this.verifyToken = verifyToken;
        }
    }
}