package com.knowplugins.ysubz.kaoscore.listeners.player;

import com.knowplugins.ysubz.kaoscore.integration.practice.PracticeModeBlocker;
import org.bukkit.Effect;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.util.Vector;

public class DoubleJumpListener implements Listener {

    private static final String FLY_PERMISSION = "kaoscore.member.fly";

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (!player.hasPermission(FLY_PERMISSION) &&
                (player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE)) {
            player.setAllowFlight(!PracticeModeBlocker.isBlocked(player));
        }
    }

    @EventHandler
    public void onToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();

        if (player.hasPermission(FLY_PERMISSION) || player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }

        if (PracticeModeBlocker.check(player, false)) {
            event.setCancelled(true);
            player.setAllowFlight(false);
            player.setFlying(false);
            return;
        }

        event.setCancelled(true);
        player.setAllowFlight(false);
        player.setFlying(false);

        Vector jump = player.getLocation().getDirection().multiply(1.5).setY(1.0);
        player.setVelocity(jump);

        player.playSound(player.getLocation(), Sound.BAT_TAKEOFF, 1.0f, 1.0f);
        player.playEffect(player.getLocation(), Effect.BLAZE_SHOOT, 15);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        if (player.hasPermission(FLY_PERMISSION) || player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }

        if (PracticeModeBlocker.isBlocked(player)) {
            if (player.getAllowFlight()) {
                player.setAllowFlight(false);
            }
            return;
        }

        if (((Entity) player).isOnGround() && !player.getAllowFlight()) {
            player.setAllowFlight(true);
        }
    }
}
