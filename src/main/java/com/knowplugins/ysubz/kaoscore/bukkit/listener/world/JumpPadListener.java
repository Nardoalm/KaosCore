package com.knowplugins.ysubz.kaoscore.bukkit.listener.world;

import com.knowplugins.ysubz.kaoscore.bukkit.KaosCore;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.BlockFace;
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

    private static final Set<Location> PADS = new HashSet<>();
    private static final Set<UUID> NO_FALL = new HashSet<>();
    private static Material padMaterial = Material.SLIME_BLOCK;
    private static double forwardForce = 1.5;
    private static double upwardForce = 1.0;
    private static boolean soundEnabled = true;
    private static Sound soundType;
    private static boolean particlesEnabled = true;
    private static Effect particleType;
    private static boolean disableFallDamage = true;

    public JumpPadListener(KaosCore plugin) {
        loadCache(plugin.getConfig());
    }

    public static void loadCache(FileConfiguration config) {
        PADS.clear();
        for (int i = 1; i <= 4; i++) {
            String path = "jumppad.locations." + i;
            if (config.contains(path)) {
                org.bukkit.World world = Bukkit.getWorld(config.getString(path + ".world"));
                if (world != null) {
                    PADS.add(new Location(world, config.getInt(path + ".x"), config.getInt(path + ".y"), config.getInt(path + ".z")));
                }
            }
        }

        padMaterial = Material.matchMaterial(config.getString("jumppad.block", "SLIME_BLOCK"));
        if (padMaterial == null) {
            padMaterial = Material.SLIME_BLOCK;
        }

        forwardForce = config.getDouble("jumppad.forward-force", 1.5);
        upwardForce = config.getDouble("jumppad.upward-force", 1.0);
        soundEnabled = config.getBoolean("jumppad.effects.sound.enabled", true);

        try {
            soundType = Sound.valueOf(config.getString("jumppad.effects.sound.type", "BAT_TAKEOFF"));
        } catch (Exception e) {
            soundEnabled = false;
        }

        particlesEnabled = config.getBoolean("jumppad.effects.particles.enabled", true);
        try {
            particleType = Effect.valueOf(config.getString("jumppad.effects.particles.type", "MOBSPAWNER_FLAMES"));
        } catch (Exception e) {
            particlesEnabled = false;
        }

        disableFallDamage = config.getBoolean("jumppad.disable-fall-damage", true);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
                event.getFrom().getBlockY() == event.getTo().getBlockY() &&
                event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Location to = event.getTo();
        if (to.getBlock().getRelative(BlockFace.DOWN).getType() != padMaterial) {
            return;
        }

        Location blockLoc = to.getBlock().getRelative(BlockFace.DOWN).getLocation();
        if (!PADS.contains(blockLoc)) {
            return;
        }

        Player player = event.getPlayer();
        Vector velocity = player.getLocation().getDirection().setY(0).normalize().multiply(forwardForce).setY(upwardForce);
        player.setVelocity(velocity);

        if (soundEnabled && soundType != null) {
            player.playSound(to, soundType, 1.0F, 1.0F);
        }

        if (particlesEnabled && particleType != null) {
            player.getWorld().playEffect(to, particleType, 4);
        }

        if (disableFallDamage) {
            NO_FALL.add(player.getUniqueId());
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL && event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (NO_FALL.remove(player.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }
}