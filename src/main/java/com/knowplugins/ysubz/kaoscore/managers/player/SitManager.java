package com.knowplugins.ysubz.kaoscore.managers.player;

import com.knowplugins.ysubz.kaoscore.KaosCore;
import com.knowplugins.ysubz.kaoscore.integration.practice.KaosPracticeBridge;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class SitManager {

    private static final String PERMISSION = "kaoscore.vip.sit";
    private final KaosCore plugin;
    private final KaosPracticeBridge practiceBridge;
    private final ConcurrentMap<UUID, ArmorStand> seatsByPlayer;
    private final ConcurrentMap<UUID, UUID> seatOwnersByStand;

    public SitManager(KaosCore plugin) {
        this.plugin = plugin;
        this.practiceBridge = new KaosPracticeBridge();
        this.seatsByPlayer = new ConcurrentHashMap<>();
        this.seatOwnersByStand = new ConcurrentHashMap<>();
    }

    public boolean hasPermission(Player player) {
        return player != null && player.hasPermission(PERMISSION);
    }

    public boolean sit(Player player) {
        if (!hasPermission(player) || !canUseMechanic(player)) {
            return false;
        }

        if (isSitting(player)) {
            unsit(player);
            return true;
        }

        cleanupSeat(player);

        if (player.isInsideVehicle() || player.getPassenger() != null) {
            return false;
        }

        Location location = player.getLocation().clone().add(0.0D, -1.45D, 0.0D);
        ArmorStand seat = player.getWorld().spawn(location, ArmorStand.class);
        seat.setVisible(false);
        seat.setGravity(false);
        seat.setMarker(true);
        seat.setBasePlate(false);
        seat.setArms(false);
        seat.setSmall(false);
        seat.setCustomNameVisible(false);
        seat.setRemoveWhenFarAway(false);

        if (!seat.setPassenger(player)) {
            seat.remove();
            return false;
        }

        seatsByPlayer.put(player.getUniqueId(), seat);
        seatOwnersByStand.put(seat.getUniqueId(), player.getUniqueId());
        return true;
    }

    public boolean carry(Player carrier, Player target) {
        if (!hasPermission(carrier) || !canCarry(carrier, target)) {
            return false;
        }

        return carrier.setPassenger(target);
    }

    public boolean launch(Player carrier) {
        if (!hasPermission(carrier) || !canUseMechanic(carrier)) {
            return false;
        }

        Entity passenger = carrier.getPassenger();
        if (!(passenger instanceof Player)) {
            return false;
        }

        Player launched = (Player) passenger;
        if (!canBeAffected(launched)) {
            return false;
        }

        if (!carrier.eject()) {
            return false;
        }

        Vector vector = carrier.getLocation().getDirection().normalize().multiply(1.5D);
        vector.setY(1.0D);

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!launched.isOnline()) {
                return;
            }
            launched.setVelocity(vector);
        });

        return true;
    }

    public void handleSeatDismount(Player player, Entity dismounted) {
        if (player == null || !(dismounted instanceof ArmorStand)) {
            return;
        }

        ArmorStand seat = (ArmorStand) dismounted;
        UUID owner = seatOwnersByStand.get(seat.getUniqueId());
        if (owner == null || !owner.equals(player.getUniqueId())) {
            return;
        }

        removeSeat(seat);
    }

    public void cleanup(Player player) {
        if (player == null) {
            return;
        }

        cleanupSeat(player);

        if (player.getPassenger() != null) {
            Entity passenger = player.getPassenger();
            player.eject();
            if (passenger instanceof Player) {
                ((Player) passenger).setFallDistance(0.0F);
            }
        }

        if (player.isInsideVehicle()) {
            Entity vehicle = player.getVehicle();
            if (vehicle instanceof ArmorStand) {
                removeSeat((ArmorStand) vehicle);
            } else {
                player.leaveVehicle();
            }
        }
    }

    public void cleanupAll() {
        for (Map.Entry<UUID, ArmorStand> entry : seatsByPlayer.entrySet()) {
            ArmorStand seat = entry.getValue();
            if (seat != null && !seat.isDead()) {
                seat.remove();
            }
        }
        seatsByPlayer.clear();
        seatOwnersByStand.clear();
    }

    private boolean canCarry(Player carrier, Player target) {
        if (carrier == null || target == null || carrier.equals(target)) {
            return false;
        }

        if (carrier.hasMetadata("NPC") || target.hasMetadata("NPC")) {
            return false;
        }

        if (!canUseMechanic(carrier) || !canBeAffected(target)) {
            return false;
        }

        if (!carrier.getWorld().equals(target.getWorld())) {
            return false;
        }

        if (carrier.getPassenger() != null || carrier.isInsideVehicle()) {
            return false;
        }

        if (target.getPassenger() != null || target.isInsideVehicle()) {
            return false;
        }

        return !isSitting(carrier) && !isSitting(target);
    }

    private boolean canUseMechanic(Player player) {
        if (player == null || !player.isOnline() || player.isDead() || VanishManager.isVanished(player)) {
            return false;
        }

        if (!practiceBridge.isAvailable()) {
            return true;
        }

        KaosPracticeBridge.PracticeState state = practiceBridge.getState(player);
        if (state.isInMatch() || state.isSpectating() || practiceBridge.isQueueing(player)) {
            return false;
        }

        return practiceBridge.isInLobby(player);
    }

    private boolean canBeAffected(Player player) {
        return player != null && player.isOnline() && !player.isDead() && !VanishManager.isVanished(player) && canUseMechanic(player);
    }

    private boolean isSitting(Player player) {
        if (player == null) {
            return false;
        }

        if (player.isInsideVehicle() && player.getVehicle() instanceof ArmorStand) {
            ArmorStand vehicle = (ArmorStand) player.getVehicle();
            UUID owner = seatOwnersByStand.get(vehicle.getUniqueId());
            if (owner != null && owner.equals(player.getUniqueId()) && !vehicle.isDead()) {
                return true;
            }
        }

        ArmorStand seat = seatsByPlayer.get(player.getUniqueId());
        if (seat == null) {
            return false;
        }

        return !seat.isDead() && player.isInsideVehicle() && seat.equals(player.getVehicle());
    }

    private void unsit(Player player) {
        if (player == null) {
            return;
        }

        ArmorStand seat = seatsByPlayer.get(player.getUniqueId());
        if (seat == null && player.isInsideVehicle() && player.getVehicle() instanceof ArmorStand) {
            ArmorStand vehicle = (ArmorStand) player.getVehicle();
            UUID owner = seatOwnersByStand.get(vehicle.getUniqueId());
            if (owner != null && owner.equals(player.getUniqueId())) {
                seat = vehicle;
            }
        }

        if (seat == null) {
            return;
        }

        if (player.isInsideVehicle() && seat.equals(player.getVehicle())) {
            player.leaveVehicle();
        }

        removeSeat(seat);
    }

    public void unsitIfSitting(Player player) {
        if (isSitting(player)) {
            unsit(player);
        }
    }

    private void cleanupSeat(Player player) {
        if (player == null) {
            return;
        }

        ArmorStand seat = seatsByPlayer.get(player.getUniqueId());
        if (seat != null) {
            removeSeat(seat);
            return;
        }

        if (player.isInsideVehicle() && player.getVehicle() instanceof ArmorStand) {
            ArmorStand vehicle = (ArmorStand) player.getVehicle();
            if (seatOwnersByStand.containsKey(vehicle.getUniqueId())) {
                removeSeat(vehicle);
            }
        }
    }

    private void removeSeat(ArmorStand seat) {
        if (seat == null) {
            return;
        }

        UUID owner = seatOwnersByStand.remove(seat.getUniqueId());
        if (owner != null) {
            seatsByPlayer.remove(owner);
        }

        Entity passenger = seat.getPassenger();
        if (passenger != null) {
            seat.eject();
        }

        if (!seat.isDead()) {
            seat.remove();
        }
    }
}
