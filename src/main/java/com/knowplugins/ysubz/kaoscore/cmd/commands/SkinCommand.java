package com.knowplugins.ysubz.kaoscore.cmd.commands;

import com.knowplugins.ysubz.kaoscore.KaosCore;
import com.knowplugins.ysubz.kaoscore.cmd.Commands;
import com.knowplugins.ysubz.kaoscore.managers.Managers;
import com.knowplugins.ysubz.kaoscore.managers.plugin.SkinRepository;
import com.knowplugins.ysubz.kaoscore.managers.plugin.SkinCacheRepository;
import com.knowplugins.ysubz.kaoscore.managers.plugin.SkinData;
import com.knowplugins.ysubz.kaoscore.utils.disguise.DisguiseApiService;
import com.knowplugins.ysubz.kaoscore.utils.menu.SkinMenu;
import com.knowplugins.ysubz.kaoscore.utils.placeholder.PlaceholderManager;
import com.knowplugins.ysubz.kaoscore.utils.skin.OriginalSkinManager;
import com.knowplugins.ysubz.kaoscore.utils.skin.MojangSkinFetcher;
import com.knowplugins.ysubz.kaoscore.utils.skin.SkinApplier;
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
            sender.sendMessage("§c§lSKIN ➔ §cApenas jogadores podem utilizar este comando.");
            return;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("kaoscore.member.skin")) {
            player.sendMessage("§c§lSKIN ➔ §cVocê não possui permissão para executar este comando.");
            player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("gui")) {
            SkinMenu.open(player);
            player.playSound(player.getLocation(), Sound.CLICK, 1.0f, 1.0f);
            return;
        }

        if (args.length != 1) {
            player.sendMessage("§c§lSKIN ➔ §cUso correto: /skin <nick> | /skin reset | /skin gui");
            player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1.0f, 1.0f);
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

    private static void setSkin(Player player, String targetSkin, boolean applyForSelf) {
        try {
            String cleanTarget = targetSkin == null ? "" : targetSkin.trim();
            if (cleanTarget.isEmpty() || cleanTarget.length() > 16) {
                player.sendMessage("§c§lSKIN ➔ §cNick inválido.");
                player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1.0f, 1.0f);
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
                        if (Managers.getDatabaseManager() != null) {
                            try {
                                SkinCacheRepository cache = new SkinCacheRepository(JavaPlugin.getPlugin(KaosCore.class), Managers.getDatabaseManager());
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
                            player.sendMessage("§c§lSKIN ➔ §cSkin não encontrada (nick não original) ou erro na API.");
                            player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1.0f, 1.0f);
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

                            if (Managers.getDatabaseManager() != null) {
                                SkinRepository repo = new SkinRepository(JavaPlugin.getPlugin(KaosCore.class), Managers.getDatabaseManager());
                                repo.saveAsync(player.getUniqueId(), cleanTarget, finalTextures.getValue(), finalTextures.getSignature());
                            }

                            SkinApplier applier = new SkinApplier(player);
                            if (applyForSelf) {
                                applier.applySkinForSelf();
                            } else {
                                applier.applySkinForOthers();
                            }

                            String message = applyForSelf
                                    ? "§a§lSKIN ➔ §aSua skin foi alterada para: " + cleanTarget
                                    : "§a§lSKIN ➔ §aSkin do disfarce aplicada para os outros jogadores: " + cleanTarget;
                            message = PlaceholderManager.replacePlaceholders(message, player);
                            player.sendMessage(message);
                            player.playSound(player.getLocation(), Sound.LEVEL_UP, 1.0f, 1.0f);

                        } catch (Exception exception) {
                            player.sendMessage("§c§lSKIN ➔ §cOcorreu um erro ao aplicar a textura da skin.");
                            player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1.0f, 1.0f);
                            exception.printStackTrace();
                        }
                    });

                } catch (Exception exception) {
                    plugin.getLogger().warning("[KaosCore] Erro ao buscar skin '" + cleanTarget + "': " + exception.getMessage());
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        player.sendMessage("§c§lSKIN ➔ §cSkin não encontrada ou erro na API.");
                        player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1.0f, 1.0f);
                    });
                }
            });

        } catch (Exception exception) {
            player.sendMessage("§c§lSKIN ➔ §cOcorreu um erro interno.");
            player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1.0f, 1.0f);
            exception.printStackTrace();
        }
    }

    public static void resetSkin(Player player) {
        try {
            UUID id = player.getUniqueId();
            Property originalProperty = OriginalSkinManager.get(id);
            if (originalProperty != null) {
                GameProfile profile = ((CraftPlayer) player).getProfile();
                profile.getProperties().removeAll("textures");
                profile.getProperties().put("textures", originalProperty);

                new SkinApplier(player).applySkinForSelf();

                String message = "§a§lSKIN ➔ §aSua skin foi resetada para a original.";
                message = PlaceholderManager.replacePlaceholders(message, player);
                player.sendMessage(message);
                player.playSound(player.getLocation(), Sound.LEVEL_UP, 1.0f, 1.0f);

                if (Managers.getDatabaseManager() != null) {
                    SkinRepository repo = new SkinRepository(JavaPlugin.getPlugin(KaosCore.class), Managers.getDatabaseManager());
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
                        if (Managers.getDatabaseManager() != null) {
                            try {
                                SkinCacheRepository cache = new SkinCacheRepository(JavaPlugin.getPlugin(KaosCore.class), Managers.getDatabaseManager());
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
                                // In servers with pirates, fall back to pool skins if Mojang doesn't know this name.
                                KaosCore core = KaosCore.getInstance();
                                if (core != null && core.getSkinPoolManager() != null) {
                                    core.getSkinPoolManager().applyJoinSkin(player);
                                } else {
                                    player.sendMessage("§c§lSKIN ➔ §cSua skin original não foi encontrada.");
                                    player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1.0f, 1.0f);
                                }
                                return;
                            }

                            GameProfile profile = ((CraftPlayer) player).getProfile();
                            profile.getProperties().removeAll("textures");
                            profile.getProperties().put("textures", finalFetchedOriginal);

                            new SkinApplier(player).applySkinForSelf();

                            String message = "§a§lSKIN ➔ §aSua skin foi resetada para a original.";
                            message = PlaceholderManager.replacePlaceholders(message, player);
                            player.sendMessage(message);
                            player.playSound(player.getLocation(), Sound.LEVEL_UP, 1.0f, 1.0f);

                            if (Managers.getDatabaseManager() != null) {
                                SkinRepository repo = new SkinRepository(JavaPlugin.getPlugin(KaosCore.class), Managers.getDatabaseManager());
                                repo.deleteAsync(id);
                            }
                        } catch (Exception ex) {
                            player.sendMessage("§c§lSKIN ➔ §cOcorreu um erro ao resetar a skin.");
                            player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1.0f, 1.0f);
                        }
                    });

                } catch (Exception exception) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        player.sendMessage("§c§lSKIN ➔ §cOcorreu um erro ao resetar a skin.");
                        player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1.0f, 1.0f);
                    });
                }
            });

        } catch (Exception exception) {
            player.sendMessage("§c§lSKIN ➔ §cOcorreu um erro ao resetar a skin.");
            player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1.0f, 1.0f);
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

