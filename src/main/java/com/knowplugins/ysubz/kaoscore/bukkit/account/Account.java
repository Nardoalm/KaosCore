package com.knowplugins.ysubz.kaoscore.bukkit.account;

import com.google.common.base.MoreObjects;
import com.knowplugins.ysubz.kaoscore.bukkit.KaosCore;
import com.knowplugins.ysubz.kaoscore.bukkit.manager.MainManager;
import com.knowplugins.ysubz.kaoscore.bukkit.manager.player.PunishmentManager;
import com.knowplugins.ysubz.kaoscore.bukkit.util.punishment.PunishmentEntry;
import com.knowplugins.ysubz.kaoscore.bukkit.util.punishment.PunishmentType;
import com.knowplugins.ysubz.kaoscore.bukkit.util.tag.Tag;
import com.knowplugins.ysubz.kaoscore.bukkit.util.title.TitleAPI;
import live.pvphard.practice.core.profile.Profile;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.UUID;

public final class Account extends Profile {

    private final UUID id;
    private String name;
    private String lastKnownIp;
    private boolean premium;
    private String passwordHash;
    private Tag tag;
    private KaosCore plugin = KaosCore.getInstance();
    private boolean banned;
    private PunishmentType punishmentType;

    public Account(UUID id, String name) {
        super(id, name);
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

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public boolean isRegistered() {
        return passwordHash != null && !passwordHash.isEmpty();
    }

    public Tag getTag() {
        return tag;
    }

    public void setAccountTag(Tag tag) {
        this.tag = tag;
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

    public void playSound(Sound sound, float volume, float pitch) {
        Player player = getPlayer();
        if (player != null && sound != null) {
            player.playSound(player.getLocation(), sound, volume, pitch);
        }
    }

    public void sendTitle(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        Player player = getPlayer();
        if (player == null) return;
        TitleAPI.sendTitle(player, title, subtitle, fadeIn, stay, fadeOut);
    }

    public void sendTitle(String title, String subtitle) {
        Player player = getPlayer();
        if (player == null) return;
        TitleAPI.sendTitle(player, title, subtitle, 10, 20, 10);
    }

    public void sendTitle(String title) {
        Player player = getPlayer();
        if (player == null) return;
        TitleAPI.sendTitle(player, title, "", 10, 20, 10);
    }

    @Override
    public boolean isInLobbyOrInQueue() {
        return super.isInLobbyOrInQueue();
    }

    public String getColoredName() {
        if (!plugin.isEnabled()) {
            return getName();
        }

        if(plugin.getTagManager() != null) {
            return plugin.getTagManager().getTag(getPlayer()).getColor() + getName();
        }
        return getName();
    }

    public String getTagColor() {
        if (plugin == null) {
            return "";
        }

        if(plugin.getTagManager() != null) {
            return plugin.getTagManager().getTag(getPlayer()).getColor();
        }
        return "";
    }

    public void setTag(Tag tag) {
        if(plugin == null) return;
        if(plugin.getTagManager() == null) return;
        if(tag == null) return;

        plugin.getTagManager().setTag(getPlayer(), tag.getId());
    }

    public void setTag(String tag) {
        if(plugin == null) return;
        if(plugin.getTagManager() == null) return;
        if(tag == null) return;

        Tag seletedTag = plugin.getTagManager().getTagByNameOrId(tag);
        if(seletedTag == null) return;

        plugin.getTagManager().setTag(getPlayer(), seletedTag.getId());
    }

//    public void ban() {
//        if(plugin == null) return;
//        if(plugin.getTagManager() == null) return;
//        if(tag == null) return;
//        if(MainManager.getPunishmentManager() == null) return;
//
//        MainManager.getPunishmentManager().punish(punishmentType, getId(), getName(),  reason, expiresAt, "BAN");
//    }

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
                .add("passwordHash", passwordHash)
                .toString();
    }
}