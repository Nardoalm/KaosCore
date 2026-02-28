package com.knowplugins.nardodev.knowpractice.cmd.commands;

import com.knowplugins.nardodev.knowpractice.cmd.Commands;
import com.knowplugins.nardodev.knowpractice.managers.player.DisguiseManager;
import com.mojang.authlib.GameProfile;
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

                SkinCommand.resetSkin(player);

                player.setDisplayName(originalName);
                player.setPlayerListName(originalName);

                disguiseManager.removeDisguise(player);

                player.sendMessage("§a§lSUCESSO ➔ §aSeu disfarce foi removido e seu nick voltou a ser §e" + originalName);

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

        try {
            GameProfile profile = ((CraftPlayer) player).getProfile();

            Field nameField = profile.getClass().getDeclaredField("name");
            nameField.setAccessible(true);
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(nameField, nameField.getModifiers() & ~Modifier.FINAL);

            nameField.set(profile, newNick);

            player.setDisplayName(newNick);
            player.setPlayerListName(newNick);

            if (player.hasPermission("nardocore.staff.disguise")) {
                player.sendMessage("§eAplicando disfarce e buscando skin, aguarde...");
                SkinCommand.setSkin(player, newNick);
            } else {
                SkinCommand.refreshPlayer(player);
                player.sendMessage("§a§lSUCESSO ➔ §aVocê agora está disfarçado como: §e" + newNick);
                player.playSound(player.getLocation(), Sound.LEVEL_UP, 1.0f, 1.0f);
            }

            disguiseManager.addInDisguise(player);

        } catch (Exception exception) {
            player.sendMessage("§c§lERRO ➔ §cOcorreu um erro interno ao aplicar o disfarce.");
            player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1.0f, 1.0f);
            exception.printStackTrace();
        }
    }
}