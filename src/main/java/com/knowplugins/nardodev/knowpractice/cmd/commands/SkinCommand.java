package com.knowplugins.nardodev.knowpractice.cmd.commands;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.knowplugins.nardodev.knowpractice.cmd.Commands;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.knowplugins.nardodev.knowpractice.managers.plugin.SkinRepository;
import com.knowplugins.nardodev.knowpractice.managers.Managers;

import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SkinCommand extends Commands {

    public static final Map<UUID, Property> ORIGINAL_SKINS = new HashMap<>();

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
        try {
            GameProfile profile = ((CraftPlayer) player).getProfile();

            UUID id = player.getUniqueId();
            if (!ORIGINAL_SKINS.containsKey(id)) {
                Collection<Property> textures = profile.getProperties().get("textures");
                if (!textures.isEmpty()) {
                    ORIGINAL_SKINS.put(id, textures.iterator().next());
                }
            }

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

                            refreshPlayer(player);
                            forceRefreshSelf(player);

                            player.sendMessage("§a§lSUCESSO ➔ §aSua skin foi alterada para: " + targetSkin);
                            player.playSound(player.getLocation(), Sound.LEVEL_UP, 1.0f, 1.0f);

                            if (Managers.getDatabaseManager() != null) {
                                SkinRepository repo = new SkinRepository(org.bukkit.plugin.java.JavaPlugin.getPlugin(com.knowplugins.nardodev.knowpractice.KnowPractice.class), Managers.getDatabaseManager());
                                repo.saveAsync(player.getUniqueId(), targetSkin, value, signature);
                            }

                            // Reapply after a short delay to avoid client-side stale skin issues
                            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                try {
                                    profile.getProperties().removeAll("textures");
                                    profile.getProperties().put("textures", new Property("textures", value, signature));
                                    refreshPlayer(player);
                                    forceRefreshSelf(player);
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }
                            }, 4L);

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
            if (ORIGINAL_SKINS.containsKey(id)) {
                GameProfile profile = ((CraftPlayer) player).getProfile();
                profile.getProperties().removeAll("textures");
                profile.getProperties().put("textures", ORIGINAL_SKINS.get(id));
                ORIGINAL_SKINS.remove(id);

                refreshPlayer(player);

                forceRefreshSelf(player);

                player.sendMessage("§a§lSUCESSO ➔ §aSua skin foi resetada para a original.");
                player.playSound(player.getLocation(), Sound.LEVEL_UP, 1.0f, 1.0f);

                if (Managers.getDatabaseManager() != null) {
                    SkinRepository repo = new SkinRepository(org.bukkit.plugin.java.JavaPlugin.getPlugin(com.knowplugins.nardodev.knowpractice.KnowPractice.class), Managers.getDatabaseManager());
                    repo.deleteAsync(id);
                }
                return;
            }

            JavaPlugin plugin = JavaPlugin.getProvidingPlugin(SkinCommand.class);
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    URL url = new URL("https://api.ashcon.app/mojang/v2/user/" + org.bukkit.Bukkit.getOfflinePlayer(id).getName());
                    InputStreamReader reader = new InputStreamReader(url.openStream());
                    JsonObject json = new JsonParser().parse(reader).getAsJsonObject();
                    JsonObject textureProperty = json.getAsJsonObject("textures").getAsJsonObject("raw");

                    String value = textureProperty.get("value").getAsString();
                    String signature = textureProperty.get("signature").getAsString();

                    Bukkit.getScheduler().runTask(plugin, () -> {
                        try {
                            GameProfile profile = ((CraftPlayer) player).getProfile();
                            profile.getProperties().removeAll("textures");
                            profile.getProperties().put("textures", new Property("textures", value, signature));

                            refreshPlayer(player);
                            forceRefreshSelf(player);

                            player.sendMessage("§a§lSUCESSO ➔ §aSua skin foi resetada para a original.");
                            player.playSound(player.getLocation(), Sound.LEVEL_UP, 1.0f, 1.0f);
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

    public static void forceRefreshSelf(Player player) {
        org.bukkit.plugin.java.JavaPlugin plugin = JavaPlugin.getProvidingPlugin(SkinCommand.class);
        org.bukkit.Location loc = player.getLocation();
        org.bukkit.Location up = loc.clone().add(0, 0.5, 0);
        Bukkit.getScheduler().runTask(plugin, () -> player.teleport(up));
        Bukkit.getScheduler().runTaskLater(plugin, () -> player.teleport(loc), 2L);
    }

    public static void refreshPlayer(Player player) {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            onlinePlayer.hidePlayer(player);
            onlinePlayer.showPlayer(player);
        }
    }
}
