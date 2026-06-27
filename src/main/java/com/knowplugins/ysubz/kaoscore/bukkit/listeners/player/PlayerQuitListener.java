package com.knowplugins.ysubz.kaoscore.bukkit.listeners.player;

import com.knowplugins.ysubz.kaoscore.bukkit.KaosCore;
import com.knowplugins.ysubz.kaoscore.bukkit.account.AccountController;
import com.knowplugins.ysubz.kaoscore.bukkit.managers.Managers;
import com.knowplugins.ysubz.kaoscore.bukkit.disguise.DisguiseManager;
import com.knowplugins.ysubz.kaoscore.bukkit.managers.player.ScoreboardManager;
import com.mojang.authlib.GameProfile;
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

        DisguiseManager disguiseManager = Managers.getDisguiseManager();
        if (disguiseManager != null && DisguiseManager.isDisguised(player)) {
            try {
                String originalName = disguiseManager.getOriginalName(player);
                if (originalName != null) {
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

        if (Managers.getNpcManager() != null) {
            Managers.getNpcManager().cleanup(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuitLate(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;

        DisguiseManager disguiseManager = Managers.getDisguiseManager();
        if (disguiseManager != null && DisguiseManager.isDisguised(player)) {
            disguiseManager.removeDisguise(player);
        }
    }
}