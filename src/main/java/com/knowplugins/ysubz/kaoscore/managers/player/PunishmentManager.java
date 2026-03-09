package com.knowplugins.ysubz.kaoscore.managers.player;

import com.knowplugins.ysubz.kaoscore.managers.plugin.PunishmentRepository;
import com.knowplugins.ysubz.kaoscore.punishment.PunishmentEntry;
import com.knowplugins.ysubz.kaoscore.punishment.PunishmentType;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CompletableFuture;

public class PunishmentManager {

    private final PunishmentRepository repository;
    private final ConcurrentMap<String, PunishmentEntry> activeByUuid;
    private final ConcurrentMap<String, PunishmentEntry> activeByName;

    public PunishmentManager(PunishmentRepository repository) {
        this.repository = repository;
        this.activeByUuid = new ConcurrentHashMap<>();
        this.activeByName = new ConcurrentHashMap<>();
    }

    public void loadActive() {
        activeByUuid.clear();
        activeByName.clear();

        List<PunishmentEntry> entries = repository.loadActive();
        for (PunishmentEntry entry : entries) {
            cache(entry);
        }
    }

    public PunishmentEntry getActiveBan(UUID uuid, String name) {
        return getActive(PunishmentType.BAN, uuid, name);
    }

    public PunishmentEntry getActiveMute(UUID uuid, String name) {
        return getActive(PunishmentType.MUTE, uuid, name);
    }

    public PunishmentEntry getActiveMute(Player player) {
        if (player == null) {
            return null;
        }
        return getActiveMute(player.getUniqueId(), player.getName());
    }

    public PunishmentEntry punish(PunishmentType type, UUID targetUuid, String targetName, UUID punisherUuid, String punisherName, String reason, long expiresAt, String source) {
        if (type == null || targetName == null || targetName.trim().isEmpty()) {
            return null;
        }

        clearActive(type, targetUuid, targetName, punisherUuid, punisherName, "Substituida por nova punicao");

        PunishmentEntry entry = new PunishmentEntry(0L, targetUuid, targetName, type, safe(reason), System.currentTimeMillis(), expiresAt, punisherUuid, safe(punisherName), safe(source), true, 0L, null, null, null);
        cache(entry);
        repository.createAsync(entry);
        return entry;
    }

    public PunishmentEntry warn(UUID targetUuid, String targetName, UUID punisherUuid, String punisherName, String reason, String source) {
        if (targetName == null || targetName.trim().isEmpty()) {
            return null;
        }

        PunishmentEntry entry = new PunishmentEntry(0L, targetUuid, targetName, PunishmentType.WARN, safe(reason), System.currentTimeMillis(), 0L, punisherUuid, safe(punisherName), safe(source), false, 0L, null, null, null);
        repository.createAsync(entry);
        return entry;
    }

    public CompletableFuture<List<PunishmentEntry>> getHistory(UUID targetUuid, String targetName, int limit) {
        return repository.loadHistory(targetUuid, targetName, limit);
    }

    public boolean clearActive(PunishmentType type, UUID targetUuid, String targetName, UUID removedByUuid, String removedByName, String removedReason) {
        PunishmentEntry active = getActive(type, targetUuid, targetName);
        if (active == null) {
            return false;
        }

        uncache(active);
        active.deactivate(removedByUuid, safe(removedByName), safe(removedReason));
        repository.revokeAsync(type, targetUuid, targetName, removedByUuid, safe(removedByName), safe(removedReason));
        return true;
    }

    private PunishmentEntry getActive(PunishmentType type, UUID uuid, String name) {
        PunishmentEntry entry = null;

        if (uuid != null) {
            entry = activeByUuid.get(uuidKey(type, uuid));
        }

        if (entry == null && name != null && !name.trim().isEmpty()) {
            entry = activeByName.get(nameKey(type, name));
        }

        if (entry == null) {
            return null;
        }

        if (!entry.isActive()) {
            uncache(entry);
            return null;
        }

        if (entry.isExpired()) {
            uncache(entry);
            entry.deactivate(null, "SYSTEM", "Punicao expirada");
            repository.expireAsync(type, entry.getTargetUuid(), entry.getTargetName());
            return null;
        }

        return entry;
    }

    private void cache(PunishmentEntry entry) {
        if (entry == null || !entry.isActive()) {
            return;
        }

        if (entry.getTargetUuid() != null) {
            activeByUuid.put(uuidKey(entry.getType(), entry.getTargetUuid()), entry);
        }

        if (entry.getTargetName() != null && !entry.getTargetName().trim().isEmpty()) {
            activeByName.put(nameKey(entry.getType(), entry.getTargetName()), entry);
        }
    }

    private void uncache(PunishmentEntry entry) {
        if (entry == null) {
            return;
        }

        if (entry.getTargetUuid() != null) {
            activeByUuid.remove(uuidKey(entry.getType(), entry.getTargetUuid()));
        }

        if (entry.getTargetName() != null && !entry.getTargetName().trim().isEmpty()) {
            activeByName.remove(nameKey(entry.getType(), entry.getTargetName()));
        }
    }

    private String uuidKey(PunishmentType type, UUID uuid) {
        return type.name() + ":UUID:" + uuid.toString();
    }

    private String nameKey(PunishmentType type, String name) {
        return type.name() + ":NAME:" + name.trim().toLowerCase();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
