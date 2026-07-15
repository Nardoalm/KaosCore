package com.knowplugins.ysubz.kaoscore.bukkit.util.skin;

import com.knowplugins.ysubz.kaoscore.bukkit.KaosCore;
import com.knowplugins.ysubz.kaoscore.bukkit.manager.player.VanishManager;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import net.minecraft.server.v1_8_R3.PacketPlayOutPlayerInfo;
import net.minecraft.server.v1_8_R3.PacketPlayOutRespawn;
import net.minecraft.server.v1_8_R3.WorldServer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class SkinApplier {

    private final Player player;
    private final KaosCore plugin;

    public SkinApplier(Player player) {
        this.player = player;
        this.plugin = JavaPlugin.getPlugin(KaosCore.class);
    }

    public void applySkinForSelf() {
        if (player == null || !player.isOnline()) return;
        new BukkitRunnable() {
            @Override
            public void run() {
                applyPacketsToSelf();
                applyPacketsToOthers();
            }
        }.runTask(plugin);
    }

    public void applySkinForOthers() {
        if (player == null || !player.isOnline()) return;
        new BukkitRunnable() {
            @Override
            public void run() {
                applyPacketsToOthers();
            }
        }.runTask(plugin);
    }

    private void applyPacketsToSelf() {
        if (!(player instanceof CraftPlayer) || !player.isOnline()) return;
        try {
            if (player.isInsideVehicle()) player.leaveVehicle();
            Location currentLocation = player.getLocation().clone();

            CraftPlayer craftPlayer = (CraftPlayer) player;
            EntityPlayer entityPlayer = craftPlayer.getHandle();
            WorldServer worldServer = (WorldServer) entityPlayer.world;

            PacketPlayOutPlayerInfo removeInfo = new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER, entityPlayer);
            PacketPlayOutPlayerInfo addInfo = new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, entityPlayer);

            PacketPlayOutRespawn respawn = new PacketPlayOutRespawn(
                    worldServer.dimension,
                    worldServer.getDifficulty(),
                    worldServer.getWorldData().getType(),
                    entityPlayer.playerInteractManager.getGameMode()
            );

            entityPlayer.playerConnection.sendPacket(removeInfo);
            entityPlayer.playerConnection.sendPacket(addInfo);
            entityPlayer.playerConnection.sendPacket(respawn);

            player.teleport(currentLocation);
            player.updateInventory();
            player.setExp(player.getExp());
            player.setWalkSpeed(player.getWalkSpeed());
            player.getInventory().setHeldItemSlot(player.getInventory().getHeldItemSlot());

            try {
                player.getClass().getDeclaredMethod("updateScaledHealth").invoke(player);
            } catch (Exception ignored) {}

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void applyPacketsToOthers() {
        boolean hardPracticeEnabled = Bukkit.getPluginManager().isPluginEnabled("HardPractice");

        if (hardPracticeEnabled) {
            try {
                live.pvphard.practice.feature.visibility.VisibilityService visibilityService =
                        live.pvphard.practice.HardPractice.getInstance().getService(live.pvphard.practice.feature.visibility.VisibilityService.class);
                if (visibilityService != null) {
                    for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                        if (onlinePlayer.equals(player)) continue;
                        onlinePlayer.hidePlayer(player);
                    }
                    visibilityService.updateVisibility(player);
                    return;
                }
            } catch (Exception ignored) {}
        }

        live.pvphard.practice.core.profile.ProfileService hpProfileService = null;
        live.pvphard.practice.core.profile.Profile ghostProfile = null;

        if (hardPracticeEnabled) {
            try {
                hpProfileService = live.pvphard.practice.HardPractice.getInstance().getService(live.pvphard.practice.core.profile.ProfileService.class);
                if (hpProfileService != null) {
                    ghostProfile = hpProfileService.getProfile(player.getUniqueId());
                }
            } catch (Exception ignored) {}
        }

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer.equals(player)) continue;

            onlinePlayer.hidePlayer(player);

            if (VanishManager.isVanished(player) && !onlinePlayer.hasPermission("kaoscore.staff.vanish")) {
                continue;
            }

            if (hardPracticeEnabled && hpProfileService != null && ghostProfile != null) {
                if (ghostProfile.getState() == live.pvphard.practice.core.profile.enums.ProfileState.SPECTATING) {
                    live.pvphard.practice.core.profile.Profile viewerProfile = hpProfileService.getProfile(onlinePlayer.getUniqueId());
                    if (viewerProfile != null && viewerProfile.getState() == live.pvphard.practice.core.profile.enums.ProfileState.PLAYING) {
                        continue;
                    }
                }
            }

            onlinePlayer.showPlayer(player);
        }
    }
}