package com.knowplugins.ysubz.kaoscore.cmd.commands;

import com.knowplugins.ysubz.kaoscore.KaosCore;
import com.knowplugins.ysubz.kaoscore.cmd.Commands;
import com.knowplugins.ysubz.kaoscore.managers.Managers;
import com.knowplugins.ysubz.kaoscore.managers.player.DisguiseManager;
import com.knowplugins.ysubz.kaoscore.managers.plugin.DisguiseRepository;
import com.knowplugins.ysubz.kaoscore.managers.plugin.SkinRepository;
import com.knowplugins.ysubz.kaoscore.utils.disguise.SkinMatrix;
import com.knowplugins.ysubz.kaoscore.utils.placeholder.PlaceholderManager;
import com.knowplugins.ysubz.kaoscore.utils.skin.SkinApplier;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class DisguiseCommand extends Commands {

    public DisguiseCommand() {
        super("disguise", "nick", "fake");
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

        DisguiseManager disguiseManager = Managers.getDisguiseManager();

        if (args.length == 0 && DisguiseManager.isDisguised(player)) {
            try {
                String originalName = disguiseManager.popOriginalName(player);
                if (originalName == null) originalName = player.getName();

                GameProfile profile = ((CraftPlayer) player).getProfile();
                changeName(profile, originalName);

                SkinCommand.resetSkin(player);
                player.setDisplayName(originalName);
                player.setPlayerListName(originalName);

                disguiseManager.removeDisguise(player);
                new SkinApplier(player).applySkinForSelf();

                String message = "§a§lSUCESSO ➔ §aSeu disfarce foi removido.";
                message = PlaceholderManager.replacePlaceholders(message, player);
                player.sendMessage(message);

                DisguiseRepository repo = new DisguiseRepository(org.bukkit.plugin.java.JavaPlugin.getPlugin(KaosCore.class), Managers.getDatabaseManager());
                repo.saveAsync(player.getUniqueId(), false, null, null);

            } catch (Exception exception) {
                player.sendMessage("§c§lERRO ➔ §cOcorreu um erro ao remover o disfarce.");
                exception.printStackTrace();
            }
            return;
        }

        // LOGICA PARA APLICAR DISFARCE
        String newNick;
        boolean isRandom = false;

        if (args.length == 0) {
            newNick = disguiseManager.getRandomNick();
            isRandom = true;
        } else {
            newNick = args[0];
        }

        if (newNick.length() > 16) {
            player.sendMessage("§c§lERRO ➔ §cO nick não pode ter mais de 16 caracteres.");
            return;
        }

        if (disguiseManager.isNickInUse(newNick)) {
            player.sendMessage("§c§lERRO ➔ §cEsse nick já está em uso.");
            return;
        }

        try {
            disguiseManager.recordOriginalName(player);
            GameProfile profile = ((CraftPlayer) player).getProfile();

            changeName(profile, newNick);

            player.setDisplayName(newNick);
            player.setPlayerListName(newNick);

            if (isRandom) {
                SkinMatrix.SkinData data = SkinMatrix.getRandomSkin();
                profile.getProperties().removeAll("textures");
                profile.getProperties().put("textures", new Property("textures", data.value, data.signature));

                if (Managers.getDatabaseManager() != null) {
                    SkinRepository repo = new SkinRepository(org.bukkit.plugin.java.JavaPlugin.getPlugin(KaosCore.class), Managers.getDatabaseManager());
                    repo.saveAsync(player.getUniqueId(), "random-disguise", data.value, data.signature);
                }

                new SkinApplier(player).applySkinForOthers();

                String message = "§a§lSUCESSO ➔ §aDisfarce aleatório aplicado: §e" + newNick;
                message = PlaceholderManager.replacePlaceholders(message, player);
                player.sendMessage(message);
            } else {
                if (player.hasPermission("nardocore.staff.disguise")) {
                    String message = "§eBuscando skin para o nick §f" + newNick + "§e...";
                    message = PlaceholderManager.replacePlaceholders(message, player);
                    player.sendMessage(message);
                    SkinCommand.setSkinForOthers(player, newNick);
                } else {
                    new SkinApplier(player).applySkinForOthers();

                    String message = "§a§lSUCESSO ➔ §aVocê agora é: §e" + newNick;
                    message = PlaceholderManager.replacePlaceholders(message, player);
                    player.sendMessage(message);
                }
            }

            disguiseManager.addInDisguise(player, newNick);
            player.playSound(player.getLocation(), Sound.LEVEL_UP, 1.0f, 1.0f);

            DisguiseRepository repo = new DisguiseRepository(org.bukkit.plugin.java.JavaPlugin.getPlugin(KaosCore.class), Managers.getDatabaseManager());
            repo.saveAsync(player.getUniqueId(), true, "nick", newNick);

        } catch (Exception exception) {
            player.sendMessage("§c§lERRO ➔ §cErro ao aplicar disfarce.");
            exception.printStackTrace();
        }
    }

    private void changeName(GameProfile profile, String name) throws Exception {
        Field nameField = profile.getClass().getDeclaredField("name");
        nameField.setAccessible(true);
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(nameField, nameField.getModifiers() & ~Modifier.FINAL);
        nameField.set(profile, name);
    }
}
