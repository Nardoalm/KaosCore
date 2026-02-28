package com.knowplugins.nardodev.knowpractice.cmd.commands;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.knowplugins.nardodev.knowpractice.cmd.Commands;
import com.knowplugins.nardodev.knowpractice.managers.player.DisguiseManager;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.Collection;

public class DisguiseCommand extends Commands {

    public DisguiseCommand() {
        super("disguise", "nick", "fake", "disfarce");
    }

    @Override
    public void perform(CommandSender sender, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("§c§lERRO ➔ §cApenas jogadores podem utilizar este comando.");
            return;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("nardocore.member.disguise")) {
            player.sendMessage("§c§lERRO ➔ §cVocê não possui permissão para executar este comando.");
            player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        DisguiseManager disguiseManager = new DisguiseManager();

        if (args.length == 0 && DisguiseManager.isDisguised(player)) {
            try {
                String originalName = player.getName();
                GameProfile profile = ((CraftPlayer) player).getProfile();

                Field nameField = profile.getClass().getDeclaredField("name");
                nameField.setAccessible(true);
                Field modifiersField = Field.class.getDeclaredField("modifiers");
                modifiersField.setAccessible(true);
                modifiersField.setInt(nameField, nameField.getModifiers() & ~Modifier.FINAL);

                nameField.set(profile, originalName);

                if (DisguiseManager.ORIGINAL_SKINS.containsKey(player)) {
                    profile.getProperties().removeAll("textures");
                    profile.getProperties().put("textures", DisguiseManager.ORIGINAL_SKINS.get(player));
                    DisguiseManager.ORIGINAL_SKINS.remove(player);
                }

                player.setDisplayName(originalName);
                player.setPlayerListName(originalName);

                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    if (!onlinePlayer.equals(player)) {
                        onlinePlayer.hidePlayer(player);
                        onlinePlayer.showPlayer(player);
                    }
                }

                disguiseManager.removeDisguise(player);

                player.sendMessage("§a§lSUCESSO ➔ §aSeu disfarce foi removido e seu nick voltou a ser §e" + originalName);
                player.playSound(player.getLocation(), Sound.LEVEL_UP, 1.0f, 1.0f);

            } catch (Exception exception) {
                player.sendMessage("§c§lERRO ➔ §cOcorreu um erro ao remover o disfarce.");
                player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1.0f, 1.0f);
                exception.printStackTrace();
            }
            return;
        }

        String newNick;

        if (args.length == 0) {
            newNick = disguiseManager.getRandomNick();
        } else {
            newNick = args[0];
        }

        if (newNick.length() > 16) {
            player.sendMessage("§c§lERRO ➔ §cO nick não pode ter mais de 16 caracteres.");
            player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        if (player.hasPermission("nardocore.staff.disguise")) {
            player.sendMessage("§eAplicando disfarce e buscando skin, aguarde...");

            Bukkit.getScheduler().runTaskAsynchronously(JavaPlugin.getProvidingPlugin(getClass()), () -> {
                String textureValue = null;
                String textureSignature = null;

                try {
                    URL url = new URL("https://api.ashcon.app/mojang/v2/user/" + newNick);
                    InputStreamReader reader = new InputStreamReader(url.openStream());
                    JsonObject json = new JsonParser().parse(reader).getAsJsonObject();
                    JsonObject textureProperty = json.getAsJsonObject("textures").getAsJsonObject("raw");

                    textureValue = textureProperty.get("value").getAsString();
                    textureSignature = textureProperty.get("signature").getAsString();
                } catch (Exception ignored) {
                }

                String finalTextureValue = textureValue;
                String finalTextureSignature = textureSignature;

                Bukkit.getScheduler().runTask(JavaPlugin.getProvidingPlugin(getClass()), () -> {
                    applyDisguise(player, newNick, finalTextureValue, finalTextureSignature, disguiseManager);
                });
            });
        } else {
            applyDisguise(player, newNick, null, null, disguiseManager);
        }
    }

    private void applyDisguise(Player player, String newNick, String textureValue, String textureSignature, DisguiseManager manager) {
        try {
            GameProfile profile = ((CraftPlayer) player).getProfile();

            if (!DisguiseManager.ORIGINAL_SKINS.containsKey(player)) {
                Collection<Property> textures = profile.getProperties().get("textures");
                if (!textures.isEmpty()) {
                    DisguiseManager.ORIGINAL_SKINS.put(player, textures.iterator().next());
                }
            }

            Field nameField = profile.getClass().getDeclaredField("name");
            nameField.setAccessible(true);
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(nameField, nameField.getModifiers() & ~Modifier.FINAL);

            nameField.set(profile, newNick);

            if (textureValue != null && textureSignature != null) {
                profile.getProperties().removeAll("textures");
                profile.getProperties().put("textures", new Property("textures", textureValue, textureSignature));
            }

            player.setDisplayName(newNick);
            player.setPlayerListName(newNick);

            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (!onlinePlayer.equals(player)) {
                    onlinePlayer.hidePlayer(player);
                    onlinePlayer.showPlayer(player);
                }
            }

            manager.addInDisguise(player);

            player.sendMessage("§a§lSUCESSO ➔ §aVocê agora está disfarçado como: §e" + newNick);
            player.playSound(player.getLocation(), Sound.LEVEL_UP, 1.0f, 1.0f);

        } catch (Exception exception) {
            player.sendMessage("§c§lERRO ➔ §cOcorreu um erro interno ao aplicar o disfarce.");
            player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1.0f, 1.0f);
            exception.printStackTrace();
        }
    }
}