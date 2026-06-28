package com.knowplugins.ysubz.kaoscore.bukkit.disguise.command;

import com.knowplugins.ysubz.kaoscore.bukkit.KaosCore;
import com.knowplugins.ysubz.kaoscore.bukkit.cmd.Commands;
import com.knowplugins.ysubz.kaoscore.bukkit.cmd.list.vip.SkinCommand;
import com.knowplugins.ysubz.kaoscore.bukkit.disguise.DisguiseManager;
import com.knowplugins.ysubz.kaoscore.bukkit.managers.Managers;
import com.knowplugins.ysubz.kaoscore.bukkit.disguise.repository.DisguiseRepository;
import com.knowplugins.ysubz.kaoscore.bukkit.util.placeholder.PlaceholderManager;
import com.mojang.authlib.GameProfile;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class UnDisguiseCommand extends Commands {

    public UnDisguiseCommand() {
        super("undisguise", "unfake", "unnick");
    }

    @Override
    public void perform(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return;
        }

        Player player = (Player) sender;
        DisguiseManager disguiseManager = Managers.getDisguiseManager();
        if (disguiseManager == null) {
            player.sendMessage("§cDisguise system unavailable.");
            return;
        }

        if (!DisguiseManager.isDisguised(player)) {
            player.sendMessage("§cYou are not disguised.");
            player.playSound(player.getLocation(), Sound.CLICK, 1.0f, 1.0f);
            return;
        }

        try {
            String previousEntry = player.getName();
            String originalName = disguiseManager.popOriginalName(player);
            if (originalName == null) originalName = player.getName();

            GameProfile profile = ((CraftPlayer) player).getProfile();
            changeName(profile, originalName);

            SkinCommand.resetSkin(player);
            player.setDisplayName(originalName);

            disguiseManager.removeDisguise(player);

            if (KaosCore.getInstance() != null && KaosCore.getInstance().getTagManager() != null) {
                KaosCore.getInstance().getTagManager().refreshTag(player, previousEntry);
            }

            SkinCommand.resetSkin(player, false);

            String message = "§aYour disguise has been removed.";
            message = PlaceholderManager.replacePlaceholders(message, player);
            player.sendMessage(message);

            player.playSound(player.getLocation(), Sound.NOTE_PLING, 1.0f, 1.0f);

            DisguiseRepository repo = new DisguiseRepository(org.bukkit.plugin.java.JavaPlugin.getPlugin(KaosCore.class), Managers.getDatabaseManager());
            repo.saveAsync(player.getUniqueId(), false, null, null);

        } catch (Exception exception) {
            player.sendMessage("§cAn error occurred while removing the disguise.");
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