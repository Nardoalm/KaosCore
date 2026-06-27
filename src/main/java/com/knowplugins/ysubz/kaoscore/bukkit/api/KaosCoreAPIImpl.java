package com.knowplugins.ysubz.kaoscore.bukkit.api;

import com.knowplugins.ysubz.kaoscore.bukkit.KaosCore;
import com.knowplugins.ysubz.kaoscore.bukkit.clan.Clan;
import com.knowplugins.ysubz.kaoscore.bukkit.managers.player.ClanManager;
import com.knowplugins.ysubz.kaoscore.bukkit.managers.Managers;
import com.knowplugins.ysubz.kaoscore.bukkit.managers.player.PunishmentManager;
import com.knowplugins.ysubz.kaoscore.bukkit.managers.player.TagManager;
import com.knowplugins.ysubz.kaoscore.bukkit.utils.punishment.PunishmentEntry;
import com.knowplugins.ysubz.kaoscore.bukkit.utils.punishment.PunishmentType;
import com.knowplugins.ysubz.kaoscore.bukkit.managers.player.VanishManager;
import com.knowplugins.ysubz.kaoscore.bukkit.utils.tag.Tag;
import com.knowplugins.ysubz.kaoscore.bukkit.disguise.api.DisguiseApiService;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.UUID;

public class KaosCoreAPIImpl implements KaosCoreAPI {

    private final KaosCore plugin;

    public KaosCoreAPIImpl(KaosCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public KaosCore getPlugin() {
        return plugin;
    }

    @Override
    public ClanManager getClanManager() {
        return plugin == null ? null : plugin.getClanManager();
    }

    @Override
    public TagManager getTagManager() {
        return plugin == null ? null : plugin.getTagManager();
    }

    @Override
    public Clan getClan(Player player) {
        ClanManager clanManager = getClanManager();
        return clanManager == null ? null : clanManager.getClan(player);
    }

    @Override
    public Clan getClan(UUID playerId) {
        ClanManager clanManager = getClanManager();
        if (clanManager == null || playerId == null) {
            return null;
        }
        return clanManager.getPlayerClan(playerId);
    }

    @Override
    public Clan getClan(String clanName) {
        ClanManager clanManager = getClanManager();
        if (clanManager == null || clanName == null) {
            return null;
        }
        return clanManager.getClan(clanName);
    }

    @Override
    public Tag getTag(Player player) {
        TagManager tagManager = getTagManager();
        return tagManager == null ? null : tagManager.getTag(player);
    }

    @Override
    public boolean setTag(Player player, String tagNameOrId) {
        TagManager tagManager = getTagManager();
        return tagManager != null && tagManager.setTag(player, tagNameOrId);
    }

    @Override
    public boolean hasClan(Player player) {
        return player != null && hasClan(player.getUniqueId());
    }

    @Override
    public boolean hasClan(UUID playerId) {
        ClanManager clanManager = getClanManager();
        return clanManager != null && playerId != null && clanManager.isInClan(playerId);
    }

    @Override
    public boolean saveClan(Clan clan) {
        ClanManager clanManager = getClanManager();
        if (clanManager == null || clan == null) {
            return false;
        }
        clanManager.saveClan(clan);
        return true;
    }

    @Override
    public boolean setClanElo(Clan clan, int elo) {
        if (clan == null) {
            return false;
        }
        clan.setElo(elo);
        return saveClan(clan);
    }

    @Override
    public boolean addClanElo(Clan clan, int amount) {
        if (clan == null) {
            return false;
        }
        clan.setElo(clan.getElo() + amount);
        return saveClan(clan);
    }

    @Override
    public boolean setClanWins(Clan clan, int wins) {
        if (clan == null) {
            return false;
        }
        clan.setWins(wins);
        return saveClan(clan);
    }

    @Override
    public boolean addClanWin(Clan clan) {
        if (clan == null) {
            return false;
        }
        clan.addWin();
        return saveClan(clan);
    }

    @Override
    public boolean setClanLosses(Clan clan, int losses) {
        if (clan == null) {
            return false;
        }
        clan.setLosses(losses);
        return saveClan(clan);
    }

    @Override
    public boolean addClanLoss(Clan clan) {
        if (clan == null) {
            return false;
        }
        clan.addLoss();
        return saveClan(clan);
    }

    @Override
    public boolean updateClanStats(Clan clan, int elo, int wins, int losses) {
        if (clan == null) {
            return false;
        }
        clan.setElo(elo);
        clan.setWins(wins);
        clan.setLosses(losses);
        return saveClan(clan);
    }

    @Override
    public boolean setClanTagColor(Clan clan, String color) {
        ClanManager clanManager = getClanManager();
        if (clanManager == null || clan == null || color == null) {
            return false;
        }
        clanManager.setTagColor(clan, color);
        return true;
    }

    @Override
    public boolean isDisguised(Player player) {
        return DisguiseApiService.isDisguised(player);
    }

    @Override
    public String getDisguiseNick(Player player) {
        return DisguiseApiService.getDisguiseNick(player);
    }

    @Override
    public String getOriginalNick(Player player) {
        return DisguiseApiService.getOriginalNick(player);
    }

    @Override
    public boolean applyDisguise(Player player, String nick) {
        return DisguiseApiService.applyDisguise(player, nick);
    }

    @Override
    public CompletableFuture<Boolean> applyDisguise(Player player, String nick, String skinName) {
        return DisguiseApiService.applyDisguise(player, nick, skinName);
    }

    @Override
    public boolean applyRandomDisguise(Player player) {
        return DisguiseApiService.applyRandomDisguise(player);
    }

    @Override
    public CompletableFuture<Boolean> removeDisguise(Player player) {
        return DisguiseApiService.removeDisguise(player);
    }

    @Override
    public boolean isVanished(Player player) {
        return VanishManager.isVanished(player);
    }

    @Override
    public void addVanish(Player player) {
        VanishManager.addVanish(player);
    }

    @Override
    public void removeVanish(Player player) {
        VanishManager.removeVanish(player);
    }

    @Override
    public void setVanish(Player player, boolean vanished) {
        if (vanished) {
            addVanish(player);
        } else {
            removeVanish(player);
        }
    }

    @Override
    public PunishmentEntry getActiveBan(Player player) {
        PunishmentManager punishmentManager = Managers.getPunishmentManager();
        if (punishmentManager == null || player == null) {
            return null;
        }
        return punishmentManager.getActiveBan(player.getUniqueId(), player.getName());
    }

    @Override
    public PunishmentEntry getActiveMute(Player player) {
        PunishmentManager punishmentManager = Managers.getPunishmentManager();
        if (punishmentManager == null || player == null) {
            return null;
        }
        return punishmentManager.getActiveMute(player);
    }

    @Override
    public PunishmentEntry ban(UUID targetUuid, String targetName, UUID punisherUuid, String punisherName, String reason, long expiresAt, String source) {
        PunishmentManager punishmentManager = Managers.getPunishmentManager();
        if (punishmentManager == null) {
            return null;
        }
        return punishmentManager.punish(PunishmentType.BAN, targetUuid, targetName, punisherUuid, punisherName, reason, expiresAt, source);
    }

    @Override
    public PunishmentEntry mute(UUID targetUuid, String targetName, UUID punisherUuid, String punisherName, String reason, long expiresAt, String source) {
        PunishmentManager punishmentManager = Managers.getPunishmentManager();
        if (punishmentManager == null) {
            return null;
        }
        return punishmentManager.punish(PunishmentType.MUTE, targetUuid, targetName, punisherUuid, punisherName, reason, expiresAt, source);
    }

    @Override
    public PunishmentEntry warn(UUID targetUuid, String targetName, UUID punisherUuid, String punisherName, String reason, String source) {
        PunishmentManager punishmentManager = Managers.getPunishmentManager();
        if (punishmentManager == null) {
            return null;
        }
        return punishmentManager.warn(targetUuid, targetName, punisherUuid, punisherName, reason, source);
    }

    @Override
    public boolean unban(UUID targetUuid, String targetName, UUID removedByUuid, String removedByName, String removedReason) {
        PunishmentManager punishmentManager = Managers.getPunishmentManager();
        return punishmentManager != null && punishmentManager.clearActive(PunishmentType.BAN, targetUuid, targetName, removedByUuid, removedByName, removedReason);
    }

    @Override
    public boolean unmute(UUID targetUuid, String targetName, UUID removedByUuid, String removedByName, String removedReason) {
        PunishmentManager punishmentManager = Managers.getPunishmentManager();
        return punishmentManager != null && punishmentManager.clearActive(PunishmentType.MUTE, targetUuid, targetName, removedByUuid, removedByName, removedReason);
    }

    @Override
    public CompletableFuture<List<PunishmentEntry>> getPunishmentHistory(UUID targetUuid, String targetName, int limit) {
        PunishmentManager punishmentManager = Managers.getPunishmentManager();
        if (punishmentManager == null) {
            return CompletableFuture.completedFuture(java.util.Collections.emptyList());
        }
        return punishmentManager.getHistory(targetUuid, targetName, limit);
    }
}
