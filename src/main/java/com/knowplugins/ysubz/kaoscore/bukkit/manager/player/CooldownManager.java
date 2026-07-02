package com.knowplugins.ysubz.kaoscore.bukkit.manager.player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CooldownManager {

    private final Map<String, Map<UUID, Long>> cooldowns = new HashMap<>();

    public void setCooldown(String type, UUID playerId, long seconds) {
        if (!cooldowns.containsKey(type)) {
            cooldowns.put(type, new HashMap<>());
        }
        cooldowns.get(type).put(playerId, System.currentTimeMillis() + (seconds * 1000));
    }

    public long getCooldown(String type, UUID playerId) {
        if (!cooldowns.containsKey(type)) return 0;
        Long cooldown = cooldowns.get(type).get(playerId);
        if (cooldown == null) return 0;
        long timeLeft = (cooldown - System.currentTimeMillis()) / 1000;
        return timeLeft > 0 ? timeLeft : 0;
    }

    public boolean hasCooldown(String type, UUID playerId) {
        return getCooldown(type, playerId) > 0;
    }

    public void removeCooldown(String type, UUID playerId) {
        if (cooldowns.containsKey(type)) {
            cooldowns.get(type).remove(playerId);
        }
    }
}
