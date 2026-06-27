package com.knowplugins.ysubz.kaoscore.bukkit.auth.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.FuzzyReflection;
import com.comphenix.protocol.reflect.accessors.Accessors;
import com.comphenix.protocol.reflect.accessors.FieldAccessor;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.knowplugins.ysubz.kaoscore.bukkit.KaosCore;
import com.knowplugins.ysubz.kaoscore.bukkit.auth.profile.AuthProfile;
import com.knowplugins.ysubz.kaoscore.bukkit.auth.security.EncryptionUtil;
import com.knowplugins.ysubz.kaoscore.bukkit.auth.util.MojangAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import javax.crypto.SecretKey;
import java.lang.reflect.Method;
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

    public AuthPacketListener(KaosCore plugin) {
        this.plugin = plugin;
        this.protocolManager = ProtocolLibrary.getProtocolManager();
        this.serverKey = EncryptionUtil.generateKeyPair();
        this.registerListeners();
    }

    private void registerListeners() {
        protocolManager.addPacketListener(new PacketAdapter(this.plugin, ListenerPriority.HIGHEST, PacketType.Login.Client.START) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                KaosCore core = KaosCore.getInstance();
                if (core == null || core.getAuthManager() == null || core.getAuthManager().getRepository() == null) {
                    return;
                }

                PacketContainer packet = event.getPacket();
                WrappedGameProfile gameProfile = packet.getGameProfiles().read(0);
                if (gameProfile == null || gameProfile.getName() == null) return;

                String username = gameProfile.getName();

                event.setCancelled(true);

                Bukkit.getScheduler().runTaskAsynchronously(core, () -> {
                    KaosCore asyncCore = KaosCore.getInstance();
                    if (asyncCore == null || asyncCore.getAuthManager() == null) return;

                    AuthProfile profile = asyncCore.getAuthManager().getRepository().loadProfile(null, username).join();

                    boolean isPremium = profile != null && profile.isPremium();

                    if (profile != null && !profile.isRegistered() && !isPremium) {
                        isPremium = MojangAPI.isPremium(username);
                        asyncCore.getAuthManager().cacheMojangStatus(username, isPremium);
                    }

                    if (isPremium) {
                        byte[] verifyToken = EncryptionUtil.generateVerifyToken();
                        pendingLogins.put(username.toLowerCase(), new PendingLogin(username, verifyToken));

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
                        injectFakeStart(event.getPlayer(), username);
                    }
                });
            }
        });

        protocolManager.addPacketListener(new PacketAdapter(this.plugin, ListenerPriority.HIGHEST, PacketType.Login.Client.ENCRYPTION_BEGIN) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                event.setCancelled(true);

                // O SEGREDO: Pegar o NetworkManager de forma SÍNCRONA antes que o ProtocolLib jogue fora.
                Object networkManager;
                try {
                    networkManager = getNetworkManager(event.getPlayer());
                } catch (Exception e) {
                    plugin.getLogger().severe("Failed to resolve NetworkManager synchronously!");
                    e.printStackTrace();
                    return;
                }

                byte[] sharedSecretEncrypted = event.getPacket().getByteArrays().read(0);
                byte[] verifyTokenEncrypted = event.getPacket().getByteArrays().read(1);

                KaosCore core = KaosCore.getInstance();
                if (core == null) return;

                // Agora passamos o objeto de conexão vivo para a Thread assíncrona
                Bukkit.getScheduler().runTaskAsynchronously(core, () -> {
                    KaosCore asyncCore = KaosCore.getInstance();
                    if (asyncCore == null || asyncCore.getAuthManager() == null) return;

                    try {
                        byte[] decryptedToken = EncryptionUtil.decryptVerifyToken(serverKey.getPrivate(), verifyTokenEncrypted);

                        PendingLogin validSession = null;
                        String validSessionKey = null;

                        for (Map.Entry<String, PendingLogin> entry : pendingLogins.entrySet()) {
                            if (Arrays.equals(entry.getValue().verifyToken, decryptedToken)) {
                                validSession = entry.getValue();
                                validSessionKey = entry.getKey();
                                break;
                            }
                        }

                        if (validSession == null) {
                            kick(event, "§cSecurity token mismatch.");
                            return;
                        }

                        pendingLogins.remove(validSessionKey);

                        SecretKey sharedSecret = EncryptionUtil.decryptSharedSecret(serverKey.getPrivate(), sharedSecretEncrypted);
                        String serverHash = EncryptionUtil.getServerIdHash("", serverKey.getPublic(), sharedSecret);

                        UUID premiumUUID = MojangAPI.hasJoined(validSession.username, serverHash);

                        if (premiumUUID != null) {
                            // Ativamos a criptografia usando a referência estática extraída
                            enableNettyEncryption(networkManager, sharedSecret);
                            injectPremiumUUID(networkManager, premiumUUID);

                            asyncCore.getAuthManager().addVerifiedPremium(validSession.username, premiumUUID);
                            injectFakeStart(event.getPlayer(), validSession.username);
                        } else {
                            kick(event, "§cAuthentication with Mojang servers failed.");
                        }

                    } catch (Exception e) {
                        kick(event, "§cEncryption processing error.");
                        e.printStackTrace();
                    }
                });
            }
        });
    }

    private void enableNettyEncryption(Object networkManager, SecretKey secretKey) {
        try {
            Method encryptMethod = FuzzyReflection.fromClass(networkManager.getClass())
                    .getMethodByParameters("a", SecretKey.class);
            encryptMethod.invoke(networkManager, secretKey);
        } catch (Exception exc) {
            plugin.getLogger().severe("Failed to enable Netty encryption pipeline!");
            exc.printStackTrace();
        }
    }

    private void injectPremiumUUID(Object networkManager, UUID premiumUUID) {
        try {
            FieldAccessor uuidAccessor = Accessors.getFieldAccessorOrNull(networkManager.getClass(), "spoofedUUID", UUID.class);
            if (uuidAccessor != null) {
                uuidAccessor.set(networkManager, premiumUUID);
            }
        } catch (Exception exc) {
            plugin.getLogger().severe("Failed to inject premium UUID into NetworkManager!");
            exc.printStackTrace();
        }
    }

    private Object getNetworkManager(Player player) throws Exception {
        Object injector = null;

        // Abordagem 1: ProtocolLib TemporaryPlayerFactory
        try {
            injector = Class.forName("com.comphenix.protocol.injector.temporary.TemporaryPlayerFactory")
                    .getMethod("getInjectorFromPlayer", Player.class).invoke(null, player);
        } catch (Exception ignored) {}

        // Abordagem 2: Tentar buscar o campo direto da classe caso o factory falhe
        if (injector == null) {
            try {
                injector = Accessors.getFieldAccessorOrNull(player.getClass(), "injector", Object.class).get(player);
            } catch (Exception ignored) {}
        }

        if (injector == null) {
            throw new IllegalStateException("Failed to extract network injector from TemporaryPlayer.");
        }

        return Accessors.getFieldAccessorOrNull(injector.getClass(), "networkManager", Object.class).get(injector);
    }

    private void injectFakeStart(Player connection, String username) {
        // O Spigot 1.8.8 precisa do IP real ou um IP fake válido para aceitar o IP Forwarding
        // Usamos 127.0.0.1 para forçar a confiança do Spigot no pacote injetado
        String fakeIp = "127.0.0.1";

        PacketContainer startPacket = protocolManager.createPacket(PacketType.Login.Client.START);
        WrappedGameProfile profile = new WrappedGameProfile(UUID.randomUUID(), username);

        // Adicionamos a propriedade de IP Forwarding dentro do profile, se necessário
        // Mas a injeção via NetworkManager (spoofedUUID) já faz a maior parte do trabalho
        startPacket.getGameProfiles().write(0, profile);

        try {
            // Em vez de receber apenas o START, vamos simular a injeção completa
            // que o nLogin/FastLogin faz para enganar o Handshake do Spigot
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