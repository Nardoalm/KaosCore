package com.knowplugins.ysubz.kaoscore.utils.skin;

import com.knowplugins.ysubz.kaoscore.KaosCore;
import com.knowplugins.ysubz.kaoscore.managers.player.VanishManager;
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
        try {
            sendSelfRefreshPackets();
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

    private void sendSelfRefreshPackets() {
        if (!(player instanceof CraftPlayer) || !player.isOnline()) return;
        if (player.isInsideVehicle()) player.leaveVehicle();
        Location currentLocation = player.getLocation().clone();

        CraftPlayer craftPlayer = (CraftPlayer) player;
        EntityPlayer entityPlayer = craftPlayer.getHandle();
        WorldServer worldServer = (WorldServer) entityPlayer.world;

        entityPlayer.playerConnection.sendPacket(new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER, entityPlayer));
        entityPlayer.playerConnection.sendPacket(new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, entityPlayer));

        entityPlayer.playerConnection.sendPacket(new PacketPlayOutRespawn(
                worldServer.dimension,
                worldServer.getDifficulty(),
                worldServer.getWorldData().getType(),
                entityPlayer.playerInteractManager.getGameMode()
        ));

        player.teleport(currentLocation);
    }

    private void applyPacketsToOthers() {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer.equals(player)) continue;

            onlinePlayer.hidePlayer(player);
            onlinePlayer.showPlayer(player);

            if (VanishManager.isVanished(player) && !onlinePlayer.hasPermission("kaoscore.staff.vanish")) {
                onlinePlayer.hidePlayer(player);
            }
        }
    }
}
