package com.knowplugins.ysubz.kaoscore.bukkit.listener.player;

import com.knowplugins.ysubz.kaoscore.bukkit.KaosCore;
import com.knowplugins.ysubz.kaoscore.bukkit.account.AccountController;
import com.knowplugins.ysubz.kaoscore.bukkit.manager.MainManager;
import com.knowplugins.ysubz.kaoscore.bukkit.disguise.DisguiseManager;
import com.knowplugins.ysubz.kaoscore.bukkit.manager.player.ScoreboardManager;
import com.mojang.authlib.GameProfile;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class PlayerQuitListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerQuitEarly(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;

        DisguiseManager disguiseManager = MainManager.getDisguiseManager();
        if (disguiseManager != null && DisguiseManager.isDisguised(player)) {
            try {
                String originalName = disguiseManager.getOriginalName(player);

                // Se o manager perdeu o nome real, forçamos a busca pela UUID no Bukkit
                if (originalName == null || originalName.trim().isEmpty()) {
                    OfflinePlayer op = Bukkit.getOfflinePlayer(player.getUniqueId());
                    originalName = op != null ? op.getName() : null;
                }

                if (originalName != null && !originalName.trim().isEmpty()) {
                    GameProfile profile = ((CraftPlayer) player).getProfile();
                    Field nameField = profile.getClass().getDeclaredField("name");
                    nameField.setAccessible(true);

                    Field modifiersField = Field.class.getDeclaredField("modifiers");
                    modifiersField.setAccessible(true);
                    modifiersField.setInt(nameField, nameField.getModifiers() & ~Modifier.FINAL);

                    nameField.set(profile, originalName);
                    player.setDisplayName(originalName);
                }
            } catch (Exception ignored) {}
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;

        event.setQuitMessage(null);

        KaosCore plugin = KaosCore.getInstance();
        AccountController accounts = plugin == null ? null : plugin.getAccountController();
        if (accounts != null) {
            accounts.markOffline(player);
        }
        if (plugin != null && plugin.getTagManager() != null) {
            plugin.getTagManager().removeTagEntries(player);
        }

        ScoreboardManager.remove(player);

        if (MainManager.getNpcManager() != null) {
            MainManager.getNpcManager().cleanup(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuitLate(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;

        DisguiseManager disguiseManager = MainManager.getDisguiseManager();
        if (disguiseManager != null && DisguiseManager.isDisguised(player)) {
            disguiseManager.removeDisguise(player);
        }
    }
}