package com.knowplugins.ysubz.kaoscore.utils.disguise;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.knowplugins.ysubz.kaoscore.KaosCore;
import com.knowplugins.ysubz.kaoscore.cmd.commands.SkinCommand;
import com.knowplugins.ysubz.kaoscore.managers.Managers;
import com.knowplugins.ysubz.kaoscore.managers.player.DisguiseManager;
import com.knowplugins.ysubz.kaoscore.managers.plugin.DisguiseRepository;
import com.knowplugins.ysubz.kaoscore.managers.plugin.SkinRepository;
import com.knowplugins.ysubz.kaoscore.utils.skin.SkinApplier;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class DisguiseApiService {

    private DisguiseApiService() {
    }

    public static boolean isDisguised(Player player) {
        return DisguiseManager.isDisguised(player);
    }

    public static String getDisguiseNick(Player player) {
        DisguiseManager disguiseManager = Managers.getDisguiseManager();
        return disguiseManager == null ? null : disguiseManager.getDisguiseNick(player);
    }

    public static String getOriginalNick(Player player) {
        if (player == null) {
            return null;
        }

        DisguiseManager disguiseManager = Managers.getDisguiseManager();
        if (disguiseManager != null) {
            String originalName = disguiseManager.getOriginalName(player);
            if (originalName != null && !originalName.trim().isEmpty()) {
                return originalName;
            }
        }

        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(player.getUniqueId());
        if (offlinePlayer != null && offlinePlayer.getName() != null && !offlinePlayer.getName().trim().isEmpty()) {
            return offlinePlayer.getName();
        }

        return player.getName();
    }

    public static boolean applyDisguise(Player player, String nick) {
        if (!applyNick(player, nick)) {
            return false;
        }

        deleteStoredSkin(player);
        new SkinApplier(player).applySkinForOthers();
        return true;
    }

    public static CompletableFuture<Boolean> applyDisguise(Player player, String nick, String skinName) {
        if (!applyNick(player, nick)) {
            return CompletableFuture.completedFuture(false);
        }

        if (skinName == null || skinName.trim().isEmpty()) {
            deleteStoredSkin(player);
            new SkinApplier(player).applySkinForOthers();
            return CompletableFuture.completedFuture(true);
        }

        return fetchSkin(skinName).thenApply(skinData -> {
            if (skinData == null || skinData.value == null) {
                return false;
            }

            Bukkit.getScheduler().runTask(getPlugin(), () -> {
                if (player == null || !player.isOnline()) {
                    return;
                }

                try {
                    GameProfile profile = ((CraftPlayer) player).getProfile();
                    profile.getProperties().removeAll("textures");
                    profile.getProperties().put("textures", new Property("textures", skinData.value, skinData.signature));
                    saveSkin(player, skinName, skinData.value, skinData.signature);
                    new SkinApplier(player).applySkinForOthers();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            return true;
        });
    }

    public static boolean applyRandomDisguise(Player player) {
        DisguiseManager disguiseManager = Managers.getDisguiseManager();
        if (disguiseManager == null) {
            return false;
        }

        String nick = disguiseManager.getRandomNick();
        if (!applyNick(player, nick)) {
            return false;
        }

        try {
            SkinMatrix.SkinData skinData = SkinMatrix.getRandomSkin();
            GameProfile profile = ((CraftPlayer) player).getProfile();
            profile.getProperties().removeAll("textures");
            profile.getProperties().put("textures", new Property("textures", skinData.value, skinData.signature));
            saveSkin(player, "random-disguise", skinData.value, skinData.signature);
            new SkinApplier(player).applySkinForOthers();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static CompletableFuture<Boolean> removeDisguise(Player player) {
        if (player == null) {
            return CompletableFuture.completedFuture(false);
        }

        try {
            DisguiseManager disguiseManager = Managers.getDisguiseManager();
            String originalName = disguiseManager == null ? null : disguiseManager.popOriginalName(player);
            if (originalName == null || originalName.trim().isEmpty()) {
                originalName = player.getName();
            }

            GameProfile profile = ((CraftPlayer) player).getProfile();
            changeName(profile, originalName);
            player.setDisplayName(originalName);
            player.setPlayerListName(originalName);

            if (disguiseManager != null) {
                disguiseManager.removeDisguise(player);
            }

            saveDisguise(player, false, null);
            deleteStoredSkin(player);

            Property originalProperty = SkinCommand.ORIGINAL_SKINS.remove(player.getUniqueId());
            if (originalProperty != null) {
                profile.getProperties().removeAll("textures");
                profile.getProperties().put("textures", originalProperty);
                new SkinApplier(player).applySkinForSelf();
                return CompletableFuture.completedFuture(true);
            }

            String realName = getOfflineName(player.getUniqueId());
            if (realName == null || realName.trim().isEmpty()) {
                new SkinApplier(player).applySkinForSelf();
                return CompletableFuture.completedFuture(true);
            }

            return fetchSkin(realName).thenApply(skinData -> {
                Bukkit.getScheduler().runTask(getPlugin(), () -> {
                    if (player == null || !player.isOnline()) {
                        return;
                    }

                    try {
                        if (skinData != null && skinData.value != null) {
                            GameProfile liveProfile = ((CraftPlayer) player).getProfile();
                            liveProfile.getProperties().removeAll("textures");
                            liveProfile.getProperties().put("textures", new Property("textures", skinData.value, skinData.signature));
                        }
                        new SkinApplier(player).applySkinForSelf();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

                return true;
            });
        } catch (Exception e) {
            e.printStackTrace();
            return CompletableFuture.completedFuture(false);
        }
    }

    private static boolean applyNick(Player player, String nick) {
        if (player == null || nick == null || nick.trim().isEmpty() || nick.length() > 16) {
            return false;
        }

        DisguiseManager disguiseManager = Managers.getDisguiseManager();
        if (disguiseManager == null) {
            return false;
        }

        String currentNick = disguiseManager.getDisguiseNick(player);
        if (DisguiseManager.isNickInUse(nick) && (currentNick == null || !currentNick.equalsIgnoreCase(nick))) {
            return false;
        }

        try {
            disguiseManager.recordOriginalName(player);
            GameProfile profile = ((CraftPlayer) player).getProfile();
            cacheOriginalSkin(player, profile);
            changeName(profile, nick);
            player.setDisplayName(nick);
            player.setPlayerListName(nick);
            disguiseManager.addInDisguise(player, nick);
            saveDisguise(player, true, nick);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void cacheOriginalSkin(Player player, GameProfile profile) {
        if (player == null || profile == null) {
            return;
        }

        UUID id = player.getUniqueId();
        if (SkinCommand.ORIGINAL_SKINS.containsKey(id)) {
            return;
        }

        Collection<Property> textures = profile.getProperties().get("textures");
        if (textures != null && !textures.isEmpty()) {
            SkinCommand.ORIGINAL_SKINS.put(id, textures.iterator().next());
        }
    }

    private static void saveDisguise(Player player, boolean disguised, String nick) {
        if (player == null || Managers.getDatabaseManager() == null) {
            return;
        }

        DisguiseRepository repository = new DisguiseRepository(getPlugin(), Managers.getDatabaseManager());
        repository.saveAsync(player.getUniqueId(), disguised, "nick", nick);
    }

    private static void saveSkin(Player player, String skinName, String value, String signature) {
        if (player == null || Managers.getDatabaseManager() == null) {
            return;
        }

        SkinRepository repository = new SkinRepository(getPlugin(), Managers.getDatabaseManager());
        repository.saveAsync(player.getUniqueId(), skinName, value, signature);
    }

    private static void deleteStoredSkin(Player player) {
        if (player == null || Managers.getDatabaseManager() == null) {
            return;
        }

        SkinRepository repository = new SkinRepository(getPlugin(), Managers.getDatabaseManager());
        repository.deleteAsync(player.getUniqueId());
    }

    private static CompletableFuture<RemoteSkinData> fetchSkin(String targetSkin) {
        CompletableFuture<RemoteSkinData> future = new CompletableFuture<>();

        Bukkit.getScheduler().runTaskAsynchronously(getPlugin(), () -> {
            try {
                URL url = new URL("https://api.ashcon.app/mojang/v2/user/" + targetSkin);
                InputStreamReader reader = new InputStreamReader(url.openStream());
                JsonObject json = new JsonParser().parse(reader).getAsJsonObject();
                JsonObject textureProperty = json.getAsJsonObject("textures").getAsJsonObject("raw");

                String value = textureProperty.get("value").getAsString();
                String signature = textureProperty.has("signature") && !textureProperty.get("signature").isJsonNull()
                        ? textureProperty.get("signature").getAsString()
                        : null;

                future.complete(new RemoteSkinData(value, signature));
            } catch (Exception e) {
                future.complete(null);
            }
        });

        return future;
    }

    private static void changeName(GameProfile profile, String name) throws Exception {
        Field nameField = profile.getClass().getDeclaredField("name");
        nameField.setAccessible(true);
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(nameField, nameField.getModifiers() & ~Modifier.FINAL);
        nameField.set(profile, name);
    }

    private static String getOfflineName(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
        return offlinePlayer == null ? null : offlinePlayer.getName();
    }

    private static KaosCore getPlugin() {
        return JavaPlugin.getPlugin(KaosCore.class);
    }

    private static final class RemoteSkinData {
        private final String value;
        private final String signature;

        private RemoteSkinData(String value, String signature) {
            this.value = value;
            this.signature = signature;
        }
    }
}
