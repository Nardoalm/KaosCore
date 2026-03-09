package com.knowplugins.ysubz.kaoscore.utils.placeholder;

import com.knowplugins.ysubz.kaoscore.KaosCore;
import com.knowplugins.ysubz.kaoscore.clan.Clan;
import com.knowplugins.ysubz.kaoscore.clan.ClanRole;
import com.knowplugins.ysubz.kaoscore.managers.Managers;
import com.knowplugins.ysubz.kaoscore.managers.MedalManager;
import com.knowplugins.ysubz.kaoscore.managers.player.DisguiseManager;
import com.knowplugins.ysubz.kaoscore.medal.Medal;
import com.knowplugins.ysubz.kaoscore.tag.Tag;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.UUID;

public class KaosCoreExpansion extends PlaceholderExpansion {

    private final KaosCore plugin;

    public KaosCoreExpansion(KaosCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "kaoscore";
    }

    @Override
    public String getAuthor() {
        return "NardoDev";
    }

    @Override
    public String getVersion() {
        if (plugin == null || plugin.getDescription() == null) {
            return "1.0";
        }
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String params) {
        if (player == null || params == null) {
            return "";
        }

        KaosCore core = KaosCore.getInstance();
        if (core == null) {
            return "";
        }

        Tag tag = core.getTagManager() == null ? null : core.getTagManager().getTag(player);
        MedalManager medalManager = core.getMedalManager();
        Medal medal = medalManager == null ? null : medalManager.getMedal(player);
        Clan clan = shouldHideClan(player) || core.getClanManager() == null ? null : core.getClanManager().getClan(player);

        if (params.equalsIgnoreCase("tag_name")) {
            return clean(tag == null ? null : tag.getName());
        }

        if (params.equalsIgnoreCase("tag_prefix")) {
            return safe(tag == null ? null : tag.getPrefix());
        }

        if (params.equalsIgnoreCase("tag_color")) {
            return safe(tag == null ? null : tag.getColor());
        }

        if (params.equalsIgnoreCase("tag_suffix")) {
            return safe(tag == null ? null : tag.getSuffix());
        }

        if (params.equalsIgnoreCase("tag_position")) {
            return tag == null ? "" : String.valueOf(tag.getPosition());
        }

        if (params.equalsIgnoreCase("tag_id")) {
            return safe(tag == null ? null : tag.getId());
        }

        if (params.equalsIgnoreCase("tag_display")) {
            if (tag == null) return "";
            return safe(tag.getPrefix()) + safe(tag.getName()) + safe(tag.getSuffix());
        }

        if (params.equalsIgnoreCase("medal_name")) {
            return safe(medal == null ? null : medal.getName());
        }

        if (params.equalsIgnoreCase("medal_id")) {
            return safe(medal == null ? null : medal.getId());
        }

        if (params.equalsIgnoreCase("medal_chat")) {
            return medalManager == null ? "" : safe(medalManager.getChatDisplay(player));
        }

        if (params.equalsIgnoreCase("medal_nametag")) {
            return medalManager == null ? "" : safe(medalManager.getNametagDisplay(player));
        }

        if (params.equalsIgnoreCase("medal_chat_static")) {
            return medalManager == null ? "" : safe(medalManager.getStaticChatDisplay(player));
        }

        if (params.equalsIgnoreCase("medal_nametag_static")) {
            return medalManager == null ? "" : safe(medalManager.getStaticNametagDisplay(player));
        }

        if (params.equalsIgnoreCase("medal_animated")) {
            return medal != null && medal.isAnimated() ? "true" : "false";
        }

        if (params.equalsIgnoreCase("friend_count")) {
            return Managers.getFriendManager() == null ? "0" : String.valueOf(Managers.getFriendManager().getFriendCount(player.getUniqueId()));
        }

        if (params.equalsIgnoreCase("friend_online_count")) {
            return Managers.getFriendManager() == null ? "0" : String.valueOf(Managers.getFriendManager().getOnlineFriendCount(player.getUniqueId()));
        }

        if (params.equalsIgnoreCase("clan_tag")) {
            if (clan == null) return "";
            String color = safe(clan.getTagColor());
            String cTag = safe(clan.getTag());
            if (cTag.isEmpty()) return "";
            return color + "[" + cTag + "]";
        }

        if (params.equalsIgnoreCase("clan_name")) {
            return safe(clan == null ? null : clan.getName());
        }

        if (params.equalsIgnoreCase("clan_member_count")) {
            if (clan == null || clan.getMembers() == null) return "0";
            return String.valueOf(clan.getMembers().size());
        }

        if (params.equalsIgnoreCase("clan_online_count")) {
            if (clan == null || clan.getMembers() == null) return "0";
            int onlineCount = 0;
            for (UUID memberId : clan.getMembers().keySet()) {
                Player online = Bukkit.getPlayer(memberId);
                if (online != null && online.isOnline()) onlineCount++;
            }
            return String.valueOf(onlineCount);
        }

        if (params.equalsIgnoreCase("clan_role")) {
            if (clan == null) return "";
            ClanRole role = clan.getRole(player.getUniqueId());
            return role == null ? "" : role.name();
        }

        if (params.equalsIgnoreCase("clan_leader_name")) {
            if (clan == null || clan.getLeader() == null) return "";
            OfflinePlayer leader = Bukkit.getOfflinePlayer(clan.getLeader());
            return leader == null ? "" : safe(leader.getName());
        }

        if (params.equalsIgnoreCase("clan_wins")) {
            return clan == null ? "0" : String.valueOf(clan.getWins());
        }

        if (params.equalsIgnoreCase("clan_losses")) {
            return clan == null ? "0" : String.valueOf(clan.getLosses());
        }

        if (params.equalsIgnoreCase("clan_elo")) {
            return clan == null ? "0" : String.valueOf(clan.getElo());
        }

        return "";
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String clean(String value) {
        if (value == null) return "";
        return value.replaceAll("(?i)&[0-9A-FK-OR]", "").replaceAll("(?i)§[0-9A-FK-OR]", "");
    }

    private boolean shouldHideClan(Player player) {
        if (player == null) {
            return false;
        }
        return DisguiseManager.isDisguised(player) && Managers.getDisguiseManager() != null;
    }
}
