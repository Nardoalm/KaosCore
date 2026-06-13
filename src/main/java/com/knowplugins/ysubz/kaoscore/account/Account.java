package com.knowplugins.ysubz.kaoscore.account;

import com.google.common.base.MoreObjects;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.UUID;

public final class Account {

    private final UUID id;
    private String name;
    private String lastKnownIp;
    private boolean premium;

    public Account(UUID id, String name) {
        this.id = id == null ? new UUID(0L, 0L) : id;
        this.name = name == null ? "" : name.trim();
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name == null ? "" : name.trim();
    }

    public String getLastKnownIp() {
        return lastKnownIp;
    }

    public void setLastKnownIp(String lastKnownIp) {
        this.lastKnownIp = lastKnownIp == null ? "" : lastKnownIp.trim();
    }

    public boolean isPremium() {
        return premium;
    }

    public void setPremium(boolean premium) {
        this.premium = premium;
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(id);
    }

    public boolean isOnline() {
        Player player = getPlayer();
        return player != null && player.isOnline();
    }

    public void sendMessage(String message) {
        Player player = getPlayer();
        if (player != null && message != null) {
            player.sendMessage(message);
        }
    }

    public void sendMessage(BaseComponent... components) {
        Player player = getPlayer();
        if (player != null && components != null) {
            player.spigot().sendMessage(components);
        }
    }

    public void playSound(Sound sound) {
        Player player = getPlayer();
        if (player != null && sound != null) {
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Account)) {
            return false;
        }
        Account other = (Account) obj;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("name", name)
                .add("premium", premium)
                .add("lastKnownIp", lastKnownIp)
                .toString();
    }
}
