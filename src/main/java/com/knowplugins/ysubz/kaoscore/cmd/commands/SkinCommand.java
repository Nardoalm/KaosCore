package com.knowplugins.ysubz.kaoscore.cmd.commands;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.knowplugins.ysubz.kaoscore.KaosCore;
import com.knowplugins.ysubz.kaoscore.cmd.Commands;
import com.knowplugins.ysubz.kaoscore.managers.Managers;
import com.knowplugins.ysubz.kaoscore.managers.plugin.SkinRepository;
import com.knowplugins.ysubz.kaoscore.utils.placeholder.PlaceholderManager;
import com.knowplugins.ysubz.kaoscore.utils.skin.OriginalSkinManager;
import com.knowplugins.ysubz.kaoscore.utils.skin.SkinApplier;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.InputStreamReader;
import java.net.URL;
import java.util.UUID;

public class SkinCommand extends Commands {

    public SkinCommand() {
        super("skin", "setskin");
    }

    @Override
    public void perform(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c§lERRO ➔ §cApenas jogadores podem utilizar este comando.");
            return;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("nardocore.member.skin")) {
            player.sendMessage("§c§lERRO ➔ §cVocê não possui permissão para executar este comando.");
            player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        if (args.length != 1) {
            player.sendMessage("§c§lERRO ➔ §cUso correto: /skin <nick> ou /skin reset");
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
            GameProfile profile = ((CraftPlayer) player).getProfile();
            OriginalSkinManager.cache(player, profile);

            JavaPlugin plugin = JavaPlugin.getProvidingPlugin(SkinCommand.class);
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    URL url = new URL("https://api.ashcon.app/mojang/v2/user/" + targetSkin);
                    InputStreamReader reader = new InputStreamReader(url.openStream());
                    JsonObject json = new JsonParser().parse(reader).getAsJsonObject();
                    JsonObject textureProperty = json.getAsJsonObject("textures").getAsJsonObject("raw");

                    String value = textureProperty.get("value").getAsString();
                    String signature = textureProperty.get("signature").getAsString();

                    Bukkit.getScheduler().runTask(plugin, () -> {
                        try {
                            profile.getProperties().removeAll("textures");
                            profile.getProperties().put("textures", new Property("textures", value, signature));

                            if (Managers.getDatabaseManager() != null) {
                                SkinRepository repo = new SkinRepository(JavaPlugin.getPlugin(KaosCore.class), Managers.getDatabaseManager());
                                repo.saveAsync(player.getUniqueId(), targetSkin, value, signature);
                            }

                            SkinApplier applier = new SkinApplier(player);
                            if (applyForSelf) {
                                applier.applySkinForSelf();
                            } else {
                                applier.applySkinForOthers();
                            }

                            String message = applyForSelf
                                    ? "§a§lSUCESSO ➔ §aSua skin foi alterada para: " + targetSkin
                                    : "§a§lSUCESSO ➔ §aSkin do disfarce aplicada para os outros jogadores: " + targetSkin;
                            message = PlaceholderManager.replacePlaceholders(message, player);
                            player.sendMessage(message);
                            player.playSound(player.getLocation(), Sound.LEVEL_UP, 1.0f, 1.0f);

                        } catch (Exception exception) {
                            player.sendMessage("§c§lERRO ➔ §cOcorreu um erro ao aplicar a textura da skin.");
                            player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1.0f, 1.0f);
                            exception.printStackTrace();
                        }
                    });

                } catch (Exception exception) {
                    player.sendMessage("§c§lERRO ➔ §cSkin não encontrada ou erro na API.");
                    player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1.0f, 1.0f);
                }
            });

        } catch (Exception exception) {
            player.sendMessage("§c§lERRO ➔ §cOcorreu um erro interno.");
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

                String message = "§a§lSUCESSO ➔ §aSua skin foi resetada para a original.";
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
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    Property fetchedOriginal = OriginalSkinManager.fetchOriginal(id);
                    if (fetchedOriginal == null) {
                        throw new IllegalStateException("Original skin not found");
                    }

                    Bukkit.getScheduler().runTask(plugin, () -> {
                        try {
                            GameProfile profile = ((CraftPlayer) player).getProfile();
                            profile.getProperties().removeAll("textures");
                            profile.getProperties().put("textures", fetchedOriginal);

                            new SkinApplier(player).applySkinForSelf();

                            String message = "§a§lSUCESSO ➔ §aSua skin foi resetada para a original.";
                            message = PlaceholderManager.replacePlaceholders(message, player);
                            player.sendMessage(message);
                            player.playSound(player.getLocation(), Sound.LEVEL_UP, 1.0f, 1.0f);

                            if (Managers.getDatabaseManager() != null) {
                                SkinRepository repo = new SkinRepository(JavaPlugin.getPlugin(KaosCore.class), Managers.getDatabaseManager());
                                repo.deleteAsync(id);
                            }
                        } catch (Exception ex) {
                            player.sendMessage("§c§lERRO ➔ §cOcorreu um erro ao resetar a skin.");
                            player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1.0f, 1.0f);
                        }
                    });

                } catch (Exception exception) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        player.sendMessage("§c§lERRO ➔ §cOcorreu um erro ao resetar a skin.");
                        player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1.0f, 1.0f);
                    });
                }
            });

        } catch (Exception exception) {
            player.sendMessage("§c§lERRO ➔ §cOcorreu um erro ao resetar a skin.");
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
