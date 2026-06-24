package com.knowplugins.ysubz.kaoscore.utils.disguise;

import com.knowplugins.ysubz.kaoscore.KaosCore;
import com.knowplugins.ysubz.kaoscore.managers.Managers;
import com.knowplugins.ysubz.kaoscore.managers.player.DisguiseManager;
import com.knowplugins.ysubz.kaoscore.managers.plugin.repository.DisguiseRepository;
import com.knowplugins.ysubz.kaoscore.managers.plugin.repository.SkinRepository;
import com.knowplugins.ysubz.kaoscore.utils.skin.MojangSkinFetcher;
import com.knowplugins.ysubz.kaoscore.utils.skin.OriginalSkinManager;
import com.knowplugins.ysubz.kaoscore.utils.skin.SkinApplier;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
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
        String previousEntry = player == null ? null : player.getName();
        if (!applyNick(player, nick)) {
            return false;
        }

        deleteStoredSkin(player);
        refreshTag(player, previousEntry);
        new SkinApplier(player).applySkinForOthers();
        new SkinApplier(player).applySkinForSelf();
        return true;
    }

    public static CompletableFuture<Boolean> applyDisguise(Player player, String nick, String skinName) {
        String previousEntry = player == null ? null : player.getName();
        if (!applyNick(player, nick)) {
            return CompletableFuture.completedFuture(false);
        }

        refreshTag(player, previousEntry);

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

    public static boolean applyDisguiseWithTextures(Player player, String nick, String skinName, String value, String signature) {
        String previousEntry = player == null ? null : player.getName();
        if (!applyNick(player, nick)) {
            return false;
        }

        refreshTag(player, previousEntry);

        if (value == null || value.trim().isEmpty()) {
            deleteStoredSkin(player);
            new SkinApplier(player).applySkinForOthers();
            return true;
        }

        try {
            GameProfile profile = ((CraftPlayer) player).getProfile();
            profile.getProperties().removeAll("textures");
            profile.getProperties().put("textures", new Property("textures", value, signature));
            saveSkin(player, skinName, value, signature);
            new SkinApplier(player).applySkinForOthers();
            return true;
        } catch (Exception exception) {
            exception.printStackTrace();
            return false;
        }
    }

    public static boolean applyRandomDisguise(Player player) {
        DisguiseManager disguiseManager = Managers.getDisguiseManager();
        if (disguiseManager == null) {
            return false;
        }

        String previousEntry = player == null ? null : player.getName();
        String nick = disguiseManager.getRandomNick();
        if (!applyNick(player, nick)) {
            return false;
        }

        refreshTag(player, previousEntry);

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

        String previousEntry = player.getName();

        try {
            DisguiseManager disguiseManager = Managers.getDisguiseManager();
            String originalName = disguiseManager == null ? null : disguiseManager.popOriginalName(player);
            if (originalName == null || originalName.trim().isEmpty()) {
                originalName = player.getName();
            }
            final String finalOriginalName = originalName;

            GameProfile profile = ((CraftPlayer) player).getProfile();
            changeName(profile, originalName);
            player.setDisplayName(originalName);
            player.setPlayerListName(originalName);

            if (disguiseManager != null) {
                disguiseManager.removeDisguise(player);
            }

            saveDisguise(player, false, null);
            deleteStoredSkin(player);
            refreshTag(player, previousEntry);

            Property originalProperty = OriginalSkinManager.get(player.getUniqueId());
            if (originalProperty != null) {
                profile.getProperties().removeAll("textures");
                profile.getProperties().put("textures", originalProperty);
                new SkinApplier(player).applySkinForSelf();
                return CompletableFuture.completedFuture(true);
            }

            CompletableFuture<Boolean> future = new CompletableFuture<>();
            Bukkit.getScheduler().runTaskAsynchronously(getPlugin(), () -> {
                try {
                    Property fetchedOriginal = MojangSkinFetcher.fetchPropertyByName(finalOriginalName);
                    Bukkit.getScheduler().runTask(getPlugin(), () -> {
                        try {
                            if (player != null && player.isOnline()) {
                                if (fetchedOriginal != null && fetchedOriginal.getValue() != null && !fetchedOriginal.getValue().trim().isEmpty()) {
                                    GameProfile liveProfile = ((CraftPlayer) player).getProfile();
                                    liveProfile.getProperties().removeAll("textures");
                                    liveProfile.getProperties().put("textures", fetchedOriginal);
                                }
                                new SkinApplier(player).applySkinForSelf();
                            }
                            future.complete(true);
                        } catch (Exception exception) {
                            exception.printStackTrace();
                            future.complete(false);
                        }
                    });
                } catch (Exception exception) {
                    exception.printStackTrace();
                    Bukkit.getScheduler().runTask(getPlugin(), () -> future.complete(false));
                }
            });
            return future;
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
        OriginalSkinManager.cache(player, profile);
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
                String clean = targetSkin == null ? "" : targetSkin.trim();
                Property textures = MojangSkinFetcher.fetchPropertyByName(clean);
                if (textures == null || textures.getValue() == null || textures.getValue().trim().isEmpty()) {
                    future.complete(null);
                    return;
                }
                future.complete(new RemoteSkinData(textures.getValue(), textures.getSignature()));
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

    private static void refreshTag(Player player, String... staleEntries) {
        KaosCore plugin = getPlugin();
        if (plugin == null || plugin.getTagManager() == null || player == null) {
            return;
        }

        if (staleEntries == null || staleEntries.length == 0) {
            plugin.getTagManager().refreshTag(player);
            return;
        }

        plugin.getTagManager().refreshTag(player, staleEntries);
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
