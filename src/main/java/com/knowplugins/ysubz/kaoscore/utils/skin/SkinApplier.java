package com.knowplugins.ysubz.kaoscore.utils.skin;

import com.knowplugins.ysubz.kaoscore.KaosCore;
import com.knowplugins.ysubz.kaoscore.managers.player.VanishManager;
import org.bukkit.Bukkit;
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
        if (player == null || !player.isOnline()) {
            return;
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                applyPacketsToSelf();
                applyPacketsToOthers();
            }
        }.runTask(plugin);
    }

    private void applyPacketsToSelf() {
        try {
            player.updateInventory();
            player.setExp(player.getExp());
            player.setWalkSpeed(player.getWalkSpeed());

            player.getInventory().setHeldItemSlot(player.getInventory().getHeldItemSlot());

            try {
                player.getClass().getDeclaredMethod("updateScaledHealth").invoke(player);
            } catch (Exception ignored) {
            }

        } catch (Exception e) {
            plugin.getLogger().warning("Erro ao aplicar skin para self: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void applyPacketsToOthers() {
        try {
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (onlinePlayer.equals(player)) {
                    continue;
                }

                if (!onlinePlayer.canSee(player)) {
                    continue;
                }

                onlinePlayer.hidePlayer(player);
                onlinePlayer.showPlayer(player);

                if (VanishManager.isVanished(player) && !onlinePlayer.hasPermission("nardocore.staff.vanish")) {
                    onlinePlayer.hidePlayer(player);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Erro ao sincronizar skin com outros players: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void applyAsync(Runnable callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            applySkinForSelf();
            if (callback != null) {
                Bukkit.getScheduler().runTask(plugin, callback);
            }
        });
    }
}

