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
import com.knowplugins.ysubz.kaoscore.bukkit.util.Logger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import javax.crypto.SecretKey;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.KeyPair;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * IMPORTANTE - Histórico do bug do NetworkManager resolvido:
 *
 * AetherSpigot and other high-performance forks alter the Netty pipeline,
 * making Channel injection extremely volatile. Furthermore, comparing IPs via
 * .toString() causes mismatches (e.g., "/127.0.0.1" vs "127.0.0.1").
 *
 * SOLUÇÃO SUPREMA:
 * We bypass the Netty pipeline entirely and access Minecraft's native
 * `ServerConnection.h` (which is a synchronized List of all active NetworkManagers).
 * We then compare the InetSocketAddress host and port natively, ensuring a 100%
 * accurate match regardless of string formatting or fork optimizations.
 */
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
        this.startPendingLoginCleanupTask();
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
                        pendingLogins.put(username.toLowerCase(), new PendingLogin(username, verifyToken, event.getPlayer()));

                        PacketContainer request = protocolManager.createPacket(PacketType.Login.Server.ENCRYPTION_BEGIN);
                        request.getStrings().write(0, "");
                        request.getSpecificModifier(java.security.PublicKey.class).write(0, serverKey.getPublic());
                        request.getByteArrays().write(0, verifyToken);

                        try {
                            protocolManager.sendServerPacket(event.getPlayer(), request);
                        } catch (Exception e) {
                            Logger.severe("Failed to send ENCRYPTION_BEGIN to " + username);
                            e.printStackTrace();
                        }
                    } else {
                        injectFakeStart(event.getPlayer(), username, null);
                    }
                });
            }
        });

        protocolManager.addPacketListener(new PacketAdapter(this.plugin, ListenerPriority.HIGHEST, PacketType.Login.Client.ENCRYPTION_BEGIN) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                event.setCancelled(true);

                final Object networkManager;
                try {
                    networkManager = getNetworkManager(event.getPlayer());
                } catch (Throwable e) {
                    Logger.severe("Failed to resolve NetworkManager - falling back to cracked login (no premium verification) for this connection.");
                    e.printStackTrace();

                    String fallbackUsername = findPendingUsernameForPlayer(event.getPlayer());
                    if (fallbackUsername != null) {
                        KaosCore fallbackCore = KaosCore.getInstance();
                        if (fallbackCore != null) {
                            Bukkit.getScheduler().runTaskAsynchronously(fallbackCore, () ->
                                    injectFakeStart(event.getPlayer(), fallbackUsername, null));
                        }
                    } else {
                        kick(event, "§cInternal authentication error. Please reconnect.");
                    }
                    return;
                }

                final byte[] sharedSecretEncrypted = event.getPacket().getByteArrays().read(0);
                final byte[] verifyTokenEncrypted = event.getPacket().getByteArrays().read(1);

                KaosCore core = KaosCore.getInstance();
                if (core == null) return;

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
                            enableNettyEncryption(networkManager, sharedSecret);
                            injectPremiumUUID(networkManager, premiumUUID);

                            asyncCore.getAuthManager().addVerifiedPremium(validSession.username, premiumUUID);

                            // Directly preload, mark as premium, save, and cache the profile
                            // because AsyncPlayerPreLoginEvent does NOT fire for premium players
                            // under our direct LoginListener injection flow.
                            try {
                                AuthProfile profile = asyncCore.getAuthManager().getRepository().loadProfile(premiumUUID, validSession.username).join();
                                if (profile != null) {
                                    profile.setPremium(true);
                                    asyncCore.getAuthManager().getRepository().saveProfile(profile);
                                    asyncCore.getAuthManager().cacheProfile(profile);
                                    Logger.info("[AuthDebug] Directly preloaded, marked premium, and cached profile for " + validSession.username);
                                }
                            } catch (Exception ex) {
                                Logger.severe("Failed to preload premium profile in AuthPacketListener for " + validSession.username + ": " + ex.getMessage());
                            }

                            injectFakeStart(event.getPlayer(), validSession.username, premiumUUID);
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
            java.lang.reflect.Field channelField = networkManager.getClass().getDeclaredField("channel");
            channelField.setAccessible(true);
            io.netty.channel.Channel channel = (io.netty.channel.Channel) channelField.get(networkManager);

            // Locate the encryption-enabled boolean field by type, not by name,
            // since field names vary between Spigot forks (e.g. AetherSpigot uses 'g' for Logger).
            java.lang.reflect.Field encryptionField = null;
            for (java.lang.reflect.Field field : networkManager.getClass().getDeclaredFields()) {
                if (field.getType() == boolean.class && !java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                    field.setAccessible(true);
                    encryptionField = field;
                    // Take the last non-static boolean field — that's the encryption flag in all known forks.
                }
            }
            if (encryptionField != null) {
                encryptionField.setBoolean(networkManager, true);
            }

            io.netty.channel.ChannelPipeline pipeline = channel.pipeline();

            Class<?> minecraftEncryptionClass = Class.forName("net.minecraft.server.v1_8_R3.MinecraftEncryption");
            Method aMethod = minecraftEncryptionClass.getMethod("a", int.class, java.security.Key.class);
            Object cipherDecrypt = aMethod.invoke(null, 2, secretKey);
            Object cipherEncrypt = aMethod.invoke(null, 1, secretKey);

            Class<?> packetDecrypterClass = Class.forName("net.minecraft.server.v1_8_R3.PacketDecrypter");
            Class<?> packetEncrypterClass = Class.forName("net.minecraft.server.v1_8_R3.PacketEncrypter");

            Object decrypter = packetDecrypterClass.getConstructor(javax.crypto.Cipher.class).newInstance(cipherDecrypt);
            Object encrypter = packetEncrypterClass.getConstructor(javax.crypto.Cipher.class).newInstance(cipherEncrypt);

            List<String> names = pipeline.names();

            // Only add handlers if they are not already present (avoids Duplicate handler name)
            if (!names.contains("decrypt")) {
                String splitterName = names.contains("splitter") ? "splitter" : null;
                if (splitterName == null) {
                    for (String name : names) {
                        if (name.toLowerCase().contains("splitter") || pipeline.get(name).getClass().getSimpleName().contains("Splitter")) {
                            splitterName = name;
                            break;
                        }
                    }
                }
                if (splitterName != null) {
                    pipeline.addBefore(splitterName, "decrypt", (io.netty.channel.ChannelHandler) decrypter);
                } else if (names.contains("decoder")) {
                    pipeline.addBefore("decoder", "decrypt", (io.netty.channel.ChannelHandler) decrypter);
                } else if (names.contains("packet_handler")) {
                    pipeline.addBefore("packet_handler", "decrypt", (io.netty.channel.ChannelHandler) decrypter);
                } else {
                    pipeline.addFirst("decrypt", (io.netty.channel.ChannelHandler) decrypter);
                }
            }

            if (!names.contains("encrypt")) {
                String prependerName = names.contains("prepender") ? "prepender" : null;
                if (prependerName == null) {
                    for (String name : names) {
                        if (name.toLowerCase().contains("prepender") || pipeline.get(name).getClass().getSimpleName().contains("Prepender")) {
                            prependerName = name;
                            break;
                        }
                    }
                }
                if (prependerName != null) {
                    pipeline.addBefore(prependerName, "encrypt", (io.netty.channel.ChannelHandler) encrypter);
                } else if (names.contains("encoder")) {
                    pipeline.addBefore("encoder", "encrypt", (io.netty.channel.ChannelHandler) encrypter);
                } else if (names.contains("packet_handler")) {
                    pipeline.addBefore("packet_handler", "encrypt", (io.netty.channel.ChannelHandler) encrypter);
                } else {
                    pipeline.addFirst("encrypt", (io.netty.channel.ChannelHandler) encrypter);
                }
            }

            Logger.info("Netty encryption pipeline successfully enabled dynamically.");
        } catch (Exception exc) {
            Logger.severe("Failed to enable Netty encryption pipeline dynamically! Trying native fallback.");
            exc.printStackTrace();
            try {
                // Native fallback: let NetworkManager handle encryption itself.
                // Check first whether decrypt/encrypt are already in the pipeline to avoid duplicates.
                java.lang.reflect.Field channelField2 = networkManager.getClass().getDeclaredField("channel");
                channelField2.setAccessible(true);
                io.netty.channel.Channel ch = (io.netty.channel.Channel) channelField2.get(networkManager);
                if (ch != null && ch.pipeline().get("decrypt") == null) {
                    Method encryptMethod = FuzzyReflection.fromClass(networkManager.getClass())
                            .getMethodByParameters("a", SecretKey.class);
                    encryptMethod.setAccessible(true);
                    encryptMethod.invoke(networkManager, secretKey);
                }
            } catch (Exception e) {
                Logger.severe("Native fallback method failed as well!");
                e.printStackTrace();
            }
        }
    }

    private void injectPremiumUUID(Object networkManager, UUID premiumUUID) {
        try {
            FieldAccessor uuidAccessor = Accessors.getFieldAccessorOrNull(networkManager.getClass(), "spoofedUUID", UUID.class);
            if (uuidAccessor != null) {
                uuidAccessor.set(networkManager, premiumUUID);
            }
        } catch (Exception exc) {
            Logger.severe("Failed to inject premium UUID into NetworkManager!");
            exc.printStackTrace();
        }
    }

    /**
     * Resolve o NetworkManager iterando a lista nativa do Minecraft Server.
     * Isso bypassa qualquer modificação maluca que forks (AetherSpigot) façam
     * no Netty, além de comparar o IP de forma estruturada.
     */
    private Object getNetworkManager(Player player) throws Exception {
        String nmsPkg = getNmsPackage();

        Class<?> minecraftServerClass = Class.forName(nmsPkg + ".MinecraftServer");
        Class<?> serverConnectionClass = Class.forName(nmsPkg + ".ServerConnection");
        Class<?> networkManagerClass = Class.forName(nmsPkg + ".NetworkManager");

        Method getServerMethod = minecraftServerClass.getMethod("getServer");
        Object minecraftServer = getServerMethod.invoke(null);

        Object serverConnection = null;
        for (Method m : minecraftServerClass.getMethods()) {
            if (m.getParameterCount() == 0
                    && !java.lang.reflect.Modifier.isStatic(m.getModifiers())
                    && m.getReturnType().equals(serverConnectionClass)) {
                Object result = m.invoke(minecraftServer);
                if (result != null) {
                    serverConnection = result;
                    break;
                }
            }
        }

        if (serverConnection == null) {
            throw new IllegalStateException("Unable to retrieve ServerConnection instance from MinecraftServer.");
        }

        List<?> networkManagers = null;
        for (java.lang.reflect.Field f : serverConnectionClass.getDeclaredFields()) {
            if (List.class.isAssignableFrom(f.getType())) {
                if (f.getGenericType().getTypeName().contains("NetworkManager")) {
                    f.setAccessible(true);
                    networkManagers = (List<?>) f.get(serverConnection);
                    break;
                }
            }
        }

        if (networkManagers == null) {
            throw new IllegalStateException("Could not locate the NetworkManager list on ServerConnection.");
        }

        InetSocketAddress targetAddr = player.getAddress();
        if (targetAddr == null) {
            throw new IllegalStateException("Player address is null, cannot match NetworkManager.");
        }

        synchronized (networkManagers) {
            for (Object nm : networkManagers) {
                Method getSocketMethod = null;
                for (Method m : networkManagerClass.getMethods()) {
                    if (m.getReturnType().equals(SocketAddress.class) && m.getParameterCount() == 0) {
                        getSocketMethod = m;
                        break;
                    }
                }
                if (getSocketMethod == null) continue;

                SocketAddress sa = (SocketAddress) getSocketMethod.invoke(nm);
                if (sa instanceof InetSocketAddress) {
                    InetSocketAddress inetSa = (InetSocketAddress) sa;

                    // COMPARAÇÃO NATIVA INFALÍVEL: Host numérico e Porta exata.
                    if (inetSa.getAddress().getHostAddress().equals(targetAddr.getAddress().getHostAddress()) &&
                            inetSa.getPort() == targetAddr.getPort()) {
                        return nm;
                    }
                }
            }
        }

        throw new IllegalStateException("Could not match NetworkManager by address for " + targetAddr.toString());
    }

    private String getNmsPackage() {
        return "net.minecraft.server.v1_8_R3";
    }

    private void injectFakeStart(Player connection, String username, UUID forcedUUID) {
        UUID uuidToUse = (forcedUUID != null) ? forcedUUID : offlineUuid(username);

        try {
            Object networkManager = getNetworkManager(connection);
            java.lang.reflect.Field channelField = networkManager.getClass().getDeclaredField("channel");
            channelField.setAccessible(true);
            io.netty.channel.Channel channel = (io.netty.channel.Channel) channelField.get(networkManager);

            channel.eventLoop().execute(() -> {
                try {
                    // Directly manipulate LoginListener state instead of injecting through the Netty
                    // pipeline, which is unreliable (exceptions in the event loop are swallowed silently).
                    Class<?> loginListenerClass = Class.forName("net.minecraft.server.v1_8_R3.LoginListener");
                    Object loginListener = null;

                    for (java.lang.reflect.Field f : networkManager.getClass().getDeclaredFields()) {
                        f.setAccessible(true);
                        try {
                            Object val = f.get(networkManager);
                            if (val != null && loginListenerClass.isInstance(val)) {
                                loginListener = val;
                                break;
                            }
                        } catch (Exception ignored) {}
                    }

                    if (loginListener == null) {
                        throw new IllegalStateException("LoginListener not found in NetworkManager fields");
                    }

                    // Set the GameProfile field (search by type to handle field renames in forks)
                    com.mojang.authlib.GameProfile gameProfile = new com.mojang.authlib.GameProfile(uuidToUse, username);
                    for (java.lang.reflect.Field f : loginListener.getClass().getDeclaredFields()) {
                        if (!java.lang.reflect.Modifier.isStatic(f.getModifiers())
                                && com.mojang.authlib.GameProfile.class.isAssignableFrom(f.getType())) {
                            f.setAccessible(true);
                            f.set(loginListener, gameProfile);
                            break;
                        }
                    }

                    // Set login state to READY_TO_ACCEPT / READY (search by enum type, find appropriate constant)
                    boolean stateSet = false;
                    for (java.lang.reflect.Field f : loginListener.getClass().getDeclaredFields()) {
                        if (!java.lang.reflect.Modifier.isStatic(f.getModifiers()) && f.getType().isEnum()) {
                            f.setAccessible(true);
                            Object targetConstant = null;
                            // 1. Try READY_TO_ACCEPT (NMS 1.8.8 standard name)
                            for (Object constant : f.getType().getEnumConstants()) {
                                if ("READY_TO_ACCEPT".equalsIgnoreCase(constant.toString())) {
                                    targetConstant = constant;
                                    break;
                                }
                            }
                            // 2. Try READY (fallback for other versions)
                            if (targetConstant == null) {
                                for (Object constant : f.getType().getEnumConstants()) {
                                    if ("READY".equalsIgnoreCase(constant.toString())) {
                                        targetConstant = constant;
                                        break;
                                    }
                                }
                            }
                            // 3. Containment fallback
                            if (targetConstant == null) {
                                for (Object constant : f.getType().getEnumConstants()) {
                                    String constStr = constant.toString();
                                    if (constStr.contains("READY") || constStr.contains("ACCEPT")) {
                                        targetConstant = constant;
                                        break;
                                    }
                                }
                            }
                            if (targetConstant != null) {
                                f.set(loginListener, targetConstant);
                                Logger.info("Premium login state field " + f.getName() + " set to " + targetConstant.toString() + " for " + username);
                                stateSet = true;
                            }
                            break;
                        }
                    }

                    if (!stateSet) {
                        throw new IllegalStateException("Could not find or set a suitable READY / READY_TO_ACCEPT enum state in LoginListener");
                    }

                    Logger.info("Premium login state set to READY for " + username + " (" + uuidToUse + ")");

                    // Immediately invoke LoginListener.c() (the tick method) on the Bukkit main
                    // thread so loginPlayer() is called without waiting for the next server tick.
                    // This prevents a race condition where the timeout counter hits 600 before
                    // loginPlayer() is triggered, causing "Took too long to log in" even though
                    // READY was successfully set.
                    final Object finalLoginListener = loginListener;
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        try {
                            for (java.lang.reflect.Method m : finalLoginListener.getClass().getDeclaredMethods()) {
                                if ("c".equals(m.getName()) && m.getParameterCount() == 0
                                        && m.getReturnType() == void.class) {
                                    m.setAccessible(true);
                                    m.invoke(finalLoginListener);
                                    break;
                                }
                            }
                        } catch (Exception ex) {
                            // LoginListener.c() may throw if player was already processed — ignore.
                            Logger.severe("LoginListener tick invocation failed for " + username + ": " + ex.getMessage());
                        }
                    });

                } catch (Exception e) {
                    Logger.severe("Failed to set LoginListener READY state for " + username + ". Trying pipeline fallback.");
                    e.printStackTrace();
                    // Fallback: inject PacketPlayInLoginStart through the pipeline
                    try {
                        PacketContainer startPacket = protocolManager.createPacket(PacketType.Login.Client.START);
                        startPacket.getGameProfiles().write(0, new WrappedGameProfile(uuidToUse, username));
                        Object nmsPacket = startPacket.getHandle();
                        channel.pipeline().fireChannelRead(nmsPacket);
                    } catch (Exception ex) {
                        Logger.severe("Pipeline fallback also failed for " + username);
                        ex.printStackTrace();
                    }
                }
            });

        } catch (Exception e) {
            Logger.severe("Failed to get NetworkManager/channel for " + username + ". Trying ProtocolLib fallback.");
            e.printStackTrace();
            try {
                PacketContainer startPacket = protocolManager.createPacket(PacketType.Login.Client.START);
                startPacket.getGameProfiles().write(0, new WrappedGameProfile(uuidToUse, username));
                protocolManager.receiveClientPacket(connection, startPacket, false);
            } catch (Exception ex) {
                Logger.severe("ProtocolLib fallback also failed for " + username);
                ex.printStackTrace();
            }
        }
    }

    private UUID offlineUuid(String username) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private void kick(PacketEvent event, String reason) {
        PacketContainer kickPacket = protocolManager.createPacket(PacketType.Login.Server.DISCONNECT);
        kickPacket.getChatComponents().write(0, WrappedChatComponent.fromText(reason));
        try {
            protocolManager.sendServerPacket(event.getPlayer(), kickPacket);
        } catch (Exception ignored) {}
    }

    private void startPendingLoginCleanupTask() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            long now = System.currentTimeMillis();
            pendingLogins.entrySet().removeIf(entry -> (now - entry.getValue().createdAt) > 30_000L);
        }, 20L * 30, 20L * 30);
    }

    private static class PendingLogin {
        String username;
        byte[] verifyToken;
        long createdAt;
        Player player;

        PendingLogin(String username, byte[] verifyToken, Player player) {
            this.username = username;
            this.verifyToken = verifyToken;
            this.createdAt = System.currentTimeMillis();
            this.player = player;
        }
    }

    private String findPendingUsernameForPlayer(Player player) {
        for (PendingLogin pending : pendingLogins.values()) {
            if (pending.player != null && pending.player.equals(player)) {
                return pending.username;
            }
        }
        return null;
    }
}