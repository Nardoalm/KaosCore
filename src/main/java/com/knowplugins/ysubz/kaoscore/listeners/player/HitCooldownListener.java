package com.knowplugins.ysubz.kaoscore.listeners.player;

import com.knowplugins.ysubz.kaoscore.KaosCore;
import com.knowplugins.ysubz.kaoscore.integration.practice.KaosPracticeBridge;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class HitCooldownListener implements Listener {

    private static final int CLASSIC_HIT_COOLDOWN_TICKS = 20;

    private final KaosPracticeBridge practiceBridge;

    public HitCooldownListener() {
        this.practiceBridge = new KaosPracticeBridge();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        Player victim = getPlayer(event.getEntity());
        Player attacker = getPlayer(event.getDamager());

        if (victim == null || attacker == null) {
            return;
        }

        KaosPracticeBridge.PracticeState victimState = practiceBridge.getState(victim);
        KaosPracticeBridge.PracticeState attackerState = practiceBridge.getState(attacker);

        if (!victimState.isInMatch() || !attackerState.isInMatch()) {
            return;
        }

        Bukkit.getScheduler().runTask(KaosCore.getInstance(), () -> {
            if (!victim.isOnline()) {
                return;
            }

            victim.setMaximumNoDamageTicks(CLASSIC_HIT_COOLDOWN_TICKS);
            victim.setNoDamageTicks(CLASSIC_HIT_COOLDOWN_TICKS);
        });
    }

    private Player getPlayer(Entity entity) {
        if (entity instanceof Player) {
            return (Player) entity;
        }

        if (entity instanceof Projectile) {
            Projectile projectile = (Projectile) entity;
            if (projectile.getShooter() instanceof Player) {
                return (Player) projectile.getShooter();
            }
        }

        return null;
    }
}
