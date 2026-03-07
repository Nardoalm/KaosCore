package com.knowplugins.ysubz.kaoscore.api;

import com.knowplugins.ysubz.kaoscore.KaosCore;
import com.knowplugins.ysubz.kaoscore.clan.Clan;
import com.knowplugins.ysubz.kaoscore.managers.ClanManager;
import com.knowplugins.ysubz.kaoscore.managers.TagManager;
import com.knowplugins.ysubz.kaoscore.tag.Tag;
import org.bukkit.entity.Player;

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
}
