package com.knowplugins.ysubz.kaoscore.listeners.world;

import com.knowplugins.ysubz.kaoscore.KaosCore;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class JumpPadListener implements Listener {

    private final KaosCore plugin;
    private final Set<UUID> fallDamageProtection = new HashSet<>();

    public JumpPadListener(KaosCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
                event.getFrom().getBlockY() == event.getTo().getBlockY() &&
                event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        Location loc = player.getLocation();
        Block blockUnder = loc.getBlock().getRelative(0, -1, 0);

        FileConfiguration config = plugin.getConfig();
        String materialName = config.getString("jumppad.block", "SLIME_BLOCK");
        Material jumpPadMaterial = Material.matchMaterial(materialName);

        if (jumpPadMaterial == null) {
            jumpPadMaterial = Material.SLIME_BLOCK;
        }

        if (blockUnder.getType() == jumpPadMaterial) {
            if (!isRegisteredLocation(blockUnder.getLocation())) {
                return;
            }

            double forwardForce = config.getDouble("jumppad.forward-force", 1.5);
            double upwardForce = config.getDouble("jumppad.upward-force", 1.0);

            Vector velocity = player.getLocation().getDirection().multiply(forwardForce).setY(upwardForce);
            player.setVelocity(velocity);

            if (config.getBoolean("jumppad.effects.sound.enabled", true)) {
                String soundName = config.getString("jumppad.effects.sound.type", "BAT_TAKEOFF");
                try {
                    Sound sound = Sound.valueOf(soundName);
                    player.playSound(player.getLocation(), sound, 1.0F, 1.0F);
                } catch (IllegalArgumentException ignored) {}
            }

            if (config.getBoolean("jumppad.effects.particles.enabled", true)) {
                String particleName = config.getString("jumppad.effects.particles.type", "MOBSPAWNER_FLAMES");
                try {
                    Effect effect = Effect.valueOf(particleName);
                    player.getWorld().playEffect(player.getLocation(), effect, 4);
                } catch (IllegalArgumentException ignored) {}
            }

            if (config.getBoolean("jumppad.disable-fall-damage", true)) {
                fallDamageProtection.add(player.getUniqueId());
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            Player player = (Player) event.getEntity();
            if (fallDamageProtection.contains(player.getUniqueId())) {
                event.setCancelled(true);
                fallDamageProtection.remove(player.getUniqueId());
            }
        }
    }

    private boolean isRegisteredLocation(Location blockLoc) {
        FileConfiguration config = plugin.getConfig();
        for (int i = 1; i <= 4; i++) {
            String path = "jumppad.locations." + i;
            if (!config.contains(path)) continue;

            String world = config.getString(path + ".world");
            int x = config.getInt(path + ".x");
            int y = config.getInt(path + ".y");
            int z = config.getInt(path + ".z");

            if (blockLoc.getWorld() != null && blockLoc.getWorld().getName().equals(world) &&
                    blockLoc.getBlockX() == x && blockLoc.getBlockY() == y && blockLoc.getBlockZ() == z) {
                return true;
            }
        }
        return false;
    }
}