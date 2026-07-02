package com.knowplugins.ysubz.kaoscore.bukkit.util.skin.command;

import com.knowplugins.ysubz.kaoscore.bukkit.KaosCore;
import com.knowplugins.ysubz.kaoscore.bukkit.cmd.Commands;
import com.knowplugins.ysubz.kaoscore.bukkit.manager.MainManager;
import com.knowplugins.ysubz.kaoscore.bukkit.util.skin.repository.SkinRepository;
import com.knowplugins.ysubz.kaoscore.bukkit.util.skin.repository.SkinCacheRepository;
import com.knowplugins.ysubz.kaoscore.bukkit.manager.plugin.SkinData;
import com.knowplugins.ysubz.kaoscore.bukkit.disguise.api.DisguiseApiService;
import com.knowplugins.ysubz.kaoscore.bukkit.util.menu.SkinMenu;
import com.knowplugins.ysubz.kaoscore.bukkit.util.placeholder.PlaceholderManager;
import com.knowplugins.ysubz.kaoscore.bukkit.util.skin.OriginalSkinManager;
import com.knowplugins.ysubz.kaoscore.bukkit.util.skin.MojangSkinFetcher;
import com.knowplugins.ysubz.kaoscore.bukkit.util.skin.SkinApplier;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public class SkinCommand extends Commands {

    public SkinCommand() {
        super("skin", "setskin");
    }

    @Override
    public void perform(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("kaoscore.member.skin")) {
            player.sendMessage("§cYou do not have permission to execute this command.");
            player.playSound(player.getLocation(), Sound.CLICK, 1.0f, 1.0f);
            return;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("gui")) {
            SkinMenu.open(player);
            player.playSound(player.getLocation(), Sound.CLICK, 1.0f, 1.0f);
            return;
        }

        if (args.length != 1) {
            player.sendMessage("§eUsage of §6/skin§e:");
            player.sendMessage(" §e* §6/skin <nick>");
            player.sendMessage(" §e* §6/skin reset");
            player.sendMessage(" §e* §6/skin gui");
            player.playSound(player.getLocation(), Sound.CLICK, 1.0f, 1.0f);
            return;
        }

        String targetSkin = args[0];

        if (targetSkin.equalsIgnoreCase("reset")) {
            resetSkin(player);
            return;
        }

        setSkin(player, targetSkin);
    }

    public static void setSkin(Player player, String targetSkin) {
        setSkin(player, targetSkin, true);
    }

    public static void setSkinForOthers(Player player, String targetSkin) {
        setSkin(player, targetSkin, false);
    }

    public static void setSkin(Player player, String targetSkin, boolean applyForSelf) {
        try {
            String cleanTarget = targetSkin == null ? "" : targetSkin.trim();
            if (cleanTarget.isEmpty() || cleanTarget.length() > 16) {
                player.sendMessage("§cInvalid nickname.");
                player.playSound(player.getLocation(), Sound.CLICK, 1.0f, 1.0f);
                return;
            }

            GameProfile profile = ((CraftPlayer) player).getProfile();
            OriginalSkinManager.cache(player, profile);

            JavaPlugin plugin = JavaPlugin.getProvidingPlugin(SkinCommand.class);
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    Property textures = null;
                    try {
                        textures = MojangSkinFetcher.fetchPropertyByName(cleanTarget);
                    } catch (Exception ignored) {
                    }

                    if (textures == null || textures.getValue() == null || textures.getValue().trim().isEmpty()) {
                        if (MainManager.getDatabaseManager() != null) {
                            try {
                                SkinCacheRepository cache = new SkinCacheRepository(JavaPlugin.getPlugin(KaosCore.class), MainManager.getDatabaseManager());
                                SkinData cached = cache.loadSync(cleanTarget);
                                if (cached != null && cached.value != null && !cached.value.trim().isEmpty()) {
                                    textures = new Property("textures", cached.value, cached.signature);
                                }
                            } catch (Exception ignored) {
                            }
                        }
                    }

                    if (textures == null || textures.getValue() == null || textures.getValue().trim().isEmpty()) {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            player.sendMessage("§cSkin not found (non-original nickname) or API error.");
                            player.playSound(player.getLocation(), Sound.CLICK, 1.0f, 1.0f);
                        });
                        return;
                    }

                    final Property finalTextures = textures;
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        try {
                            if (!player.isOnline()) {
                                return;
                            }

                            GameProfile liveProfile = ((CraftPlayer) player).getProfile();
                            liveProfile.getProperties().removeAll("textures");
                            liveProfile.getProperties().put("textures", finalTextures);

                            if (MainManager.getDatabaseManager() != null) {
                                SkinRepository repo = new SkinRepository(JavaPlugin.getPlugin(KaosCore.class), MainManager.getDatabaseManager());
                                repo.saveAsync(player.getUniqueId(), cleanTarget, finalTextures.getValue(), finalTextures.getSignature());

                                SkinCacheRepository cache = new SkinCacheRepository(JavaPlugin.getPlugin(KaosCore.class), MainManager.getDatabaseManager());
                                cache.saveAsync(cleanTarget, finalTextures.getValue(), finalTextures.getSignature());
                            }

                            SkinApplier applier = new SkinApplier(player);
                            if (applyForSelf) {
                                applier.applySkinForSelf();
                            } else {
                                applier.applySkinForOthers();
                            }

                            String message = applyForSelf
                                    ? "§aYour skin has been changed to §6" + cleanTarget + "§a."
                                    : "§aDisguise skin applied for other players §6" + cleanTarget + "§a.";
                            message = PlaceholderManager.replacePlaceholders(message, player);
                            player.sendMessage(message);
                            player.playSound(player.getLocation(), Sound.NOTE_PLING, 1.0f, 1.0f);

                        } catch (Exception exception) {
                            player.sendMessage("§cAn error occurred while applying the skin texture.");
                            player.playSound(player.getLocation(), Sound.CLICK, 1.0f, 1.0f);
                            exception.printStackTrace();
                        }
                    });

                } catch (Exception exception) {
                    plugin.getLogger().warning("[KaosCore] Error fetching skin '" + cleanTarget + "': " + exception.getMessage());
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        player.sendMessage("§cSkin not found or API error.");
                        player.playSound(player.getLocation(), Sound.CLICK, 1.0f, 1.0f);
                    });
                }
            });

        } catch (Exception exception) {
            player.sendMessage("§cAn internal error occurred.");
            player.playSound(player.getLocation(), Sound.CLICK, 1.0f, 1.0f);
            exception.printStackTrace();
        }
    }

    public static void resetSkin(Player player) {
        resetSkin(player, true);
    }

    public static void resetSkin(Player player, boolean sendMessage) {
        try {
            UUID id = player.getUniqueId();
            Property originalProperty = OriginalSkinManager.get(id);
            if (originalProperty != null) {
                GameProfile profile = ((CraftPlayer) player).getProfile();
                profile.getProperties().removeAll("textures");
                profile.getProperties().put("textures", originalProperty);

                new SkinApplier(player).applySkinForSelf();

                if (sendMessage) {
                    String message = "§aYour skin has been reset to the original.";
                    message = PlaceholderManager.replacePlaceholders(message, player);
                    player.sendMessage(message);
                }
                player.playSound(player.getLocation(), Sound.NOTE_PLING, 1.0f, 1.0f);

                if (MainManager.getDatabaseManager() != null) {
                    SkinRepository repo = new SkinRepository(JavaPlugin.getPlugin(KaosCore.class), MainManager.getDatabaseManager());
                    repo.deleteAsync(id);
                }
                return;
            }

            JavaPlugin plugin = JavaPlugin.getProvidingPlugin(SkinCommand.class);
            String originalName = DisguiseApiService.getOriginalNick(player);
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    Property fetchedOriginal = null;
                    try {
                        fetchedOriginal = MojangSkinFetcher.fetchPropertyByName(originalName);
                    } catch (Exception ignored) {
                    }

                    if (fetchedOriginal == null || fetchedOriginal.getValue() == null || fetchedOriginal.getValue().trim().isEmpty()) {
                        if (MainManager.getDatabaseManager() != null) {
                            try {
                                SkinCacheRepository cache = new SkinCacheRepository(JavaPlugin.getPlugin(KaosCore.class), MainManager.getDatabaseManager());
                                SkinData cached = cache.loadSync(originalName);
                                if (cached != null && cached.value != null && !cached.value.trim().isEmpty()) {
                                    fetchedOriginal = new Property("textures", cached.value, cached.signature);
                                }
                            } catch (Exception ignored) {
                            }
                        }
                    }

                    final Property finalFetchedOriginal = fetchedOriginal;
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        try {
                            if (!player.isOnline()) {
                                return;
                            }

                            if (finalFetchedOriginal == null || finalFetchedOriginal.getValue() == null || finalFetchedOriginal.getValue().trim().isEmpty()) {
                                KaosCore core = KaosCore.getInstance();
                                if (core != null && core.getSkinPoolManager() != null) {
                                    core.getSkinPoolManager().applyJoinSkin(player);
                                } else {
                                    player.sendMessage("§cYour original skin was not found.");
                                    player.playSound(player.getLocation(), Sound.CLICK, 1.0f, 1.0f);
                                }
                                return;
                            }

                            GameProfile profile = ((CraftPlayer) player).getProfile();
                            profile.getProperties().removeAll("textures");
                            profile.getProperties().put("textures", finalFetchedOriginal);

                            new SkinApplier(player).applySkinForSelf();

                            if (sendMessage) {
                                String message = "§aYour skin has been reset to the original.";
                                message = PlaceholderManager.replacePlaceholders(message, player);
                                player.sendMessage(message);
                            }
                            player.playSound(player.getLocation(), Sound.NOTE_PLING, 1.0f, 1.0f);

                            if (MainManager.getDatabaseManager() != null) {
                                SkinRepository repo = new SkinRepository(JavaPlugin.getPlugin(KaosCore.class), MainManager.getDatabaseManager());
                                repo.deleteAsync(id);
                            }
                        } catch (Exception ex) {
                            player.sendMessage("§cAn error occurred while resetting the skin.");
                            player.playSound(player.getLocation(), Sound.CLICK, 1.0f, 1.0f);
                        }
                    });

                } catch (Exception exception) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        player.sendMessage("§cAn error occurred while resetting the skin.");
                        player.playSound(player.getLocation(), Sound.CLICK, 1.0f, 1.0f);
                    });
                }
            });

        } catch (Exception exception) {
            player.sendMessage("§cAn error occurred while resetting the skin.");
            player.playSound(player.getLocation(), Sound.CLICK, 1.0f, 1.0f);
            exception.printStackTrace();
        }
    }

    public static void refreshPlayer(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        new SkinApplier(player).applySkinForSelf();
    }

    public static void cacheOriginalSkin(Player player) {
        OriginalSkinManager.cache(player);
    }

    public static void cacheOriginalSkin(Player player, GameProfile profile) {
        OriginalSkinManager.cache(player, profile);
    }
}
