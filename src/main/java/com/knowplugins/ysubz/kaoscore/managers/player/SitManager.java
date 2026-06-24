package com.knowplugins.ysubz.kaoscore.managers.player;

import com.knowplugins.ysubz.kaoscore.KaosCore;
import com.knowplugins.ysubz.kaoscore.integration.practice.PracticeModeBlocker;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftEntity;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.material.Stairs;
import org.bukkit.util.Vector;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class SitManager {

    private static final String SEAT_CUSTOM_NAME = "KaosCoreSeat";
    private static final String SEAT_METADATA_KEY = "kaoscore_seat";
    private static final double BASE_Y_OFFSET = -1.7D;

    public static final ThreadLocal<Boolean> SPAWNING_SEAT = ThreadLocal.withInitial(() -> false);

    private final KaosCore plugin;
    private final ConcurrentMap<UUID, ArmorStand> seatsByPlayer;
    private final ConcurrentMap<UUID, UUID> seatOwnersByStand;

    public SitManager(KaosCore plugin) {
        this.plugin = plugin;
        this.seatsByPlayer = new ConcurrentHashMap<>();
        this.seatOwnersByStand = new ConcurrentHashMap<>();

        cleanupOrphanSeats();
    }

    public boolean hasPermission(Player player) {
        return player != null && (player.hasPermission("kaoscore.vip.sit") ||
                player.hasPermission("kaoscore.vip.sit") ||
                player.hasPermission("kaoscore.member.vip"));
    }

    public boolean sit(Player player) {
        if (!hasPermission(player) || !canUseMechanic(player)) return false;

        if (isSitting(player)) {
            unsit(player);
            return true;
        }

        if (player.isInsideVehicle() || player.getPassenger() != null || player.isFlying()) {
            player.sendMessage("§cYou cannot sit right now.");
            return false;
        }

        if (!((Entity) player).isOnGround()) {
            player.sendMessage("§cYou need to be on the ground to sit.");
            return false;
        }

        Location loc = player.getLocation().clone();
        loc.setX(loc.getBlockX() + 0.5D);
        loc.setZ(loc.getBlockZ() + 0.5D);
        loc.setPitch(0.0F);
        loc.setY(player.getLocation().getY() + BASE_Y_OFFSET);

        if (isLocationOccupied(loc)) {
            player.sendMessage("§cSomeone is already sitting here.");
            return false;
        }

        ArmorStand seat = spawnSeat(loc);
        mountWithDelay(seat, player);
        return true;
    }

    public boolean sitOnBlock(Player player, Block clickedBlock) {
        if (!hasPermission(player) || !canUseMechanic(player)) return false;
        if (player == null || clickedBlock == null) return false;

        if (isSitting(player)) unsit(player);
        if (player.isInsideVehicle() || player.getPassenger() != null) return false;

        Location loc = clickedBlock.getLocation().clone().add(0.5D, 0.0D, 0.5D);
        loc.setPitch(0.0F);
        loc.setYaw(player.getLocation().getYaw());

        double surface = 0.5D;
        if (clickedBlock.getType().name().contains("STEP") || clickedBlock.getType().name().contains("SLAB")) {
            surface = (clickedBlock.getData() & 0x8) != 0 ? 1.0D : 0.5D;
        } else if (clickedBlock.getState().getData() instanceof Stairs) {
            loc.setYaw(yawFromFace(((Stairs) clickedBlock.getState().getData()).getFacing()));
        }

        loc.add(0.0D, surface + BASE_Y_OFFSET, 0.0D);

        if (isLocationOccupied(loc)) {
            player.sendMessage("§cThis seat is already occupied.");
            return false;
        }

        ArmorStand seat = spawnSeat(loc);
        mountWithDelay(seat, player);
        return true;
    }

    public boolean carry(Player carrier, Player target) {
        if (!hasPermission(carrier) || !canInteract(carrier, target)) return false;
        boolean mounted = carrier.setPassenger(target);
        if (!mounted) mounted = mount(carrier, target);
        return mounted;
    }

    public boolean ride(Player rider, Player target) {
        if (!hasPermission(rider) || !canInteract(rider, target)) return false;

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (rider.isOnline() && target.isOnline() && !rider.isDead() && !target.isDead()) {
                boolean mounted = target.setPassenger(rider);
                if (!mounted) mount(target, rider);
            }
        }, 10L);

        return true;
    }

    public boolean launch(Player carrier) {
        if (!hasPermission(carrier)) return false;
        if (!(carrier.getPassenger() instanceof Player)) return false;
        if (!canUseMechanic(carrier)) return false;

        Player launched = (Player) carrier.getPassenger();
        carrier.eject();

        Vector vector = carrier.getLocation().getDirection().normalize().multiply(1.5D).setY(1.0D);
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (launched.isOnline()) launched.setVelocity(vector);
        });

        return true;
    }

    public void handleSeatDismount(Player player) {
        ArmorStand seat = seatsByPlayer.remove(player.getUniqueId());
        if (seat != null) {
            seatOwnersByStand.remove(seat.getUniqueId());
            seat.remove();

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Location loc = player.getLocation();
                loc.setY(loc.getY() + 0.5);
                player.teleport(loc);
            }, 1L);
        }
    }

    public void cleanup(Player player) {
        if (player == null) return;
        unsit(player);
        if (player.getPassenger() != null) player.eject();
    }

    public void cleanupAll() {
        for (ArmorStand seat : seatsByPlayer.values()) {
            if (seat != null && !seat.isDead()) seat.remove();
        }
        seatsByPlayer.clear();
        seatOwnersByStand.clear();
    }

    private void cleanupOrphanSeats() {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (org.bukkit.World world : Bukkit.getWorlds()) {
                for (ArmorStand stand : world.getEntitiesByClass(ArmorStand.class)) {
                    if (stand.hasMetadata(SEAT_METADATA_KEY)) stand.remove();
                }
            }
        }, 20L);
    }

    private ArmorStand spawnSeat(Location location) {
        SPAWNING_SEAT.set(true);
        try {
            ArmorStand seat = location.getWorld().spawn(location, ArmorStand.class);
            seat.setVisible(false);
            seat.setGravity(false);
            seat.setBasePlate(false);
            seat.setArms(false);
            seat.setSmall(true);
            seat.setMarker(false);
            seat.setCustomName(SEAT_CUSTOM_NAME);
            seat.setCustomNameVisible(false);
            seat.setMetadata(SEAT_METADATA_KEY, new FixedMetadataValue(plugin, true));
            return seat;
        } finally {
            SPAWNING_SEAT.set(false);
        }
    }

    private void mountWithDelay(ArmorStand seat, Player player) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (seat.isDead() || !seat.isValid() || !player.isOnline()) {
                seat.remove();
                return;
            }

            boolean mounted = seat.setPassenger(player);
            if (!mounted) mounted = mount(seat, player);

            if (mounted) {
                seatsByPlayer.put(player.getUniqueId(), seat);
                seatOwnersByStand.put(seat.getUniqueId(), player.getUniqueId());
            } else {
                seat.remove();
            }
        }, 2L);
    }

    private boolean isLocationOccupied(Location location) {
        for (Entity entity : location.getWorld().getNearbyEntities(location, 0.3, 0.3, 0.3)) {
            if (entity instanceof ArmorStand && entity.hasMetadata(SEAT_METADATA_KEY)) return true;
        }
        return false;
    }

    private boolean mount(Entity vehicle, Entity passenger) {
        if (vehicle == null || passenger == null) return false;
        try {
            net.minecraft.server.v1_8_R3.Entity nmsPassenger = ((CraftEntity) passenger).getHandle();
            net.minecraft.server.v1_8_R3.Entity nmsVehicle = ((CraftEntity) vehicle).getHandle();
            nmsPassenger.mount(nmsVehicle);
            return passenger.getVehicle() != null && passenger.getVehicle().equals(vehicle);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private float yawFromFace(BlockFace face) {
        switch (face) {
            case NORTH: return 180.0F;
            case SOUTH: return 0.0F;
            case WEST: return 90.0F;
            case EAST: return -90.0F;
            default: return 0.0F;
        }
    }

    private boolean canInteract(Player p1, Player p2) {
        if (p1 == p2 || p1.hasMetadata("NPC") || p2.hasMetadata("NPC")) return false;
        if (!canUseMechanic(p1) || !canUseMechanic(p2)) return false;
        if (p1.getPassenger() != null || p2.getPassenger() != null) return false;
        return !isSitting(p1) && !isSitting(p2);
    }

    private boolean canUseMechanic(Player player) {
        if (player == null || !player.isOnline() || player.isDead()) return false;
        if (PracticeModeBlocker.check(player, true)) return false;
        return true;
    }

    private boolean isSitting(Player player) {
        return seatsByPlayer.containsKey(player.getUniqueId());
    }

    private void unsit(Player player) {
        ArmorStand seat = seatsByPlayer.remove(player.getUniqueId());
        if (seat != null) {
            seatOwnersByStand.remove(seat.getUniqueId());
            seat.eject();
            seat.remove();
        }
    }
}