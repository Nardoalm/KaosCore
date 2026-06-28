package com.knowplugins.ysubz.kaoscore.bukkit.api;

import com.knowplugins.ysubz.kaoscore.bukkit.KaosCore;
import com.knowplugins.ysubz.kaoscore.bukkit.clan.Clan;
import com.knowplugins.ysubz.kaoscore.bukkit.clan.manager.ClanManager;
import com.knowplugins.ysubz.kaoscore.bukkit.util.tag.manager.TagManager;
import com.knowplugins.ysubz.kaoscore.bukkit.util.punishment.PunishmentEntry;
import com.knowplugins.ysubz.kaoscore.bukkit.util.tag.Tag;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.UUID;

public interface KaosCoreAPI {

    KaosCore getPlugin();

    ClanManager getClanManager();

    TagManager getTagManager();

    Clan getClan(Player player);

    Clan getClan(UUID playerId);

    Clan getClan(String clanName);

    Tag getTag(Player player);

    boolean setTag(Player player, String tagNameOrId);

    boolean hasClan(Player player);

    boolean hasClan(UUID playerId);

    boolean saveClan(Clan clan);

    boolean setClanElo(Clan clan, int elo);

    boolean addClanElo(Clan clan, int amount);

    boolean setClanWins(Clan clan, int wins);

    boolean addClanWin(Clan clan);

    boolean setClanLosses(Clan clan, int losses);

    boolean addClanLoss(Clan clan);

    boolean updateClanStats(Clan clan, int elo, int wins, int losses);

    boolean setClanTagColor(Clan clan, String color);

    boolean isDisguised(Player player);

    String getDisguiseNick(Player player);

    String getOriginalNick(Player player);

    boolean applyDisguise(Player player, String nick);

    CompletableFuture<Boolean> applyDisguise(Player player, String nick, String skinName);

    boolean applyRandomDisguise(Player player);

    CompletableFuture<Boolean> removeDisguise(Player player);

    boolean isVanished(Player player);

    void addVanish(Player player);

    void removeVanish(Player player);

    void setVanish(Player player, boolean vanished);

    PunishmentEntry getActiveBan(Player player);

    PunishmentEntry getActiveMute(Player player);

    PunishmentEntry ban(UUID targetUuid, String targetName, UUID punisherUuid, String punisherName, String reason, long expiresAt, String source);

    PunishmentEntry mute(UUID targetUuid, String targetName, UUID punisherUuid, String punisherName, String reason, long expiresAt, String source);

    PunishmentEntry warn(UUID targetUuid, String targetName, UUID punisherUuid, String punisherName, String reason, String source);

    boolean unban(UUID targetUuid, String targetName, UUID removedByUuid, String removedByName, String removedReason);

    boolean unmute(UUID targetUuid, String targetName, UUID removedByUuid, String removedByName, String removedReason);

    CompletableFuture<List<PunishmentEntry>> getPunishmentHistory(UUID targetUuid, String targetName, int limit);
}
