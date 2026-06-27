package com.knowplugins.ysubz.kaoscore.bukkit.utils.placeholder;

import com.knowplugins.ysubz.kaoscore.bukkit.KaosCore;
import com.knowplugins.ysubz.kaoscore.bukkit.clan.Clan;
import com.knowplugins.ysubz.kaoscore.bukkit.clan.ClanRole;
import com.knowplugins.ysubz.kaoscore.bukkit.managers.Managers;
import com.knowplugins.ysubz.kaoscore.bukkit.managers.player.MedalManager;
import com.knowplugins.ysubz.kaoscore.bukkit.disguise.DisguiseManager;
import com.knowplugins.ysubz.kaoscore.bukkit.managers.permission.EffectivePermissions;
import com.knowplugins.ysubz.kaoscore.bukkit.managers.permission.PermissionGroup;
import com.knowplugins.ysubz.kaoscore.bukkit.managers.permission.PermissionManager;
import com.knowplugins.ysubz.kaoscore.bukkit.utils.medal.Medal;
import com.knowplugins.ysubz.kaoscore.bukkit.utils.punishment.PunishmentTime;
import com.knowplugins.ysubz.kaoscore.bukkit.utils.tag.Tag;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
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
        return "ysubz";
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
            return medalManager == null ? "" : safe(medalManager.getStaticChatDisplay(player));
        }

        if (params.equalsIgnoreCase("medal_nametag")) {
            return medalManager == null ? "" : safe(medalManager.getStaticNametagDisplay(player));
        }

        if (params.equalsIgnoreCase("medal_chat_static")) {
            return medalManager == null ? "" : safe(medalManager.getStaticChatDisplay(player));
        }

        if (params.equalsIgnoreCase("medal_nametag_static")) {
            return medalManager == null ? "" : safe(medalManager.getStaticNametagDisplay(player));
        }

        if (params.equalsIgnoreCase("friend_count")) {
            return Managers.getFriendManager() == null ? "0" : String.valueOf(Managers.getFriendManager().getFriendCount(player.getUniqueId()));
        }

        if (params.equalsIgnoreCase("friend_online_count")) {
            return Managers.getFriendManager() == null ? "0" : String.valueOf(Managers.getFriendManager().getOnlineFriendCount(player.getUniqueId()));
        }

        if (params.equalsIgnoreCase("clan_tag")) {
            if (clan == null) return "";
            return safe(clan.getFormattedTag());
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

        // --- Permissoes (sistema proprio KaosCore) ---
        PermissionManager permissionManager = Managers.getPermissionManager();
        EffectivePermissions effective = permissionManager == null ? null : permissionManager.getEffective(player.getUniqueId());
        if (effective != null) {
            String lower = params.toLowerCase(Locale.ROOT);

            if (lower.equals("group_name") || lower.equals("primary_group_name")) {
                return safe(effective.getPrimaryGroup());
            }

            if (lower.equals("prefix")) {
                return colorize(effective.getPrefix());
            }

            if (lower.equals("suffix")) {
                return colorize(effective.getSuffix());
            }

            if (lower.equals("inherited_groups")) {
                return joinSortedGroups(effective.getInheritedGroups(), permissionManager);
            }

            if (lower.equals("current_priority") || lower.equals("primary_group_priority")) {
                return String.valueOf(getGroupPriority(permissionManager, effective.getPrimaryGroup()));
            }

            if (lower.equals("highest_priority_weight")) {
                return String.valueOf(getHighestPriority(permissionManager, effective.getInheritedGroups()));
            }

            if (lower.equals("player_unique_id")) {
                return player.getUniqueId().toString();
            }

            if (lower.equals("player_name")) {
                return safe(player.getName());
            }

            if (lower.startsWith("group_expiry_time_")) {
                String group = params.substring("group_expiry_time_".length());
                Long expiresAt = effective.getDirectGroupExpiryAtByName().get(normalize(group));
                if (expiresAt == null) return "";
                return PunishmentTime.formatRemaining(expiresAt <= 0L ? 0L : expiresAt);
            }

            if (lower.startsWith("expiry_time_")) {
                String node = params.substring("expiry_time_".length());
                Long expiresAt = effective.getUserPermissionExpiryAtByNode().get(normalizePermission(node));
                if (expiresAt == null) return "";
                return PunishmentTime.formatRemaining(expiresAt <= 0L ? 0L : expiresAt);
            }

            if (lower.startsWith("inherited_expiry_time_")) {
                String node = params.substring("inherited_expiry_time_".length());
                String perm = normalizePermission(node);
                if (perm.isEmpty()) return "";
                if (effective.getUserPermissionExpiryAtByNode().containsKey(perm)) return "";

                long best = -1L;
                for (String groupName : effective.getDirectGroups()) {
                    Boolean value = permissionManager.getGroupPermissionValue(groupName, perm);
                    if (value == null || !value) continue;
                    Long expiresAt = effective.getDirectGroupExpiryAtByName().get(groupName);
                    long exp = expiresAt == null ? 0L : expiresAt;
                    if (exp <= 0L) {
                        return PunishmentTime.formatRemaining(0L);
                    }
                    if (best < exp) best = exp;
                }

                return best < 0L ? "" : PunishmentTime.formatRemaining(best);
            }

            if (lower.startsWith("has_permission_")) {
                String node = params.substring("has_permission_".length());
                return hasPermissionCompat(player, node) ? "yes" : "no";
            }

            if (lower.startsWith("check_permission_")) {
                String node = params.substring("check_permission_".length());
                return hasPermissionCompat(player, node) ? "true" : "false";
            }

            if (lower.startsWith("inherits_group_")) {
                String group = params.substring("inherits_group_".length());
                return effective.getInheritedGroups().contains(normalize(group)) ? "true" : "false";
            }

            if (lower.startsWith("on_track_")) {
                String track = params.substring("on_track_".length());
                return isOnTrack(core, effective, track) ? "true" : "false";
            }

            if (lower.startsWith("has_groups_on_track_")) {
                String track = params.substring("has_groups_on_track_".length());
                return isOnTrack(core, effective, track) ? "true" : "false";
            }
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

    private String colorize(String value) {
        return ChatColor.translateAlternateColorCodes('&', safe(value));
    }

    private boolean hasPermissionCompat(Player player, String permissionRaw) {
        if (player == null) return false;
        String permission = safe(permissionRaw).trim();
        if (permission.isEmpty()) return false;
        if (player.hasPermission(permission)) return true;
        if (!permission.contains(".") && permission.contains("_")) {
            return player.hasPermission(permission.replace('_', '.'));
        }
        return false;
    }

    private String normalize(String value) {
        if (value == null) return "";
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizePermission(String value) {
        String permission = safe(value).trim();
        if (permission.isEmpty()) return "";
        String normalized = normalize(permission);
        if (normalized.contains(".") || !normalized.contains("_")) {
            return normalized;
        }
        return normalize(permission.replace('_', '.'));
    }

    private int getGroupPriority(PermissionManager permissionManager, String groupName) {
        if (permissionManager == null) return 0;
        PermissionGroup group = permissionManager.getGroup(groupName);
        return group == null ? 0 : group.getPriority();
    }

    private int getHighestPriority(PermissionManager permissionManager, Set<String> groups) {
        if (permissionManager == null || groups == null || groups.isEmpty()) return 0;
        int best = 0;
        for (String name : groups) {
            PermissionGroup g = permissionManager.getGroup(name);
            if (g != null && g.getPriority() > best) best = g.getPriority();
        }
        return best;
    }

    private String joinSortedGroups(Set<String> groups, PermissionManager permissionManager) {
        if (groups == null || groups.isEmpty()) return "";
        List<String> list = new ArrayList<>(groups);
        list.sort((a, b) -> {
            int pa = getGroupPriority(permissionManager, a);
            int pb = getGroupPriority(permissionManager, b);
            int cmp = Integer.compare(pb, pa);
            if (cmp != 0) return cmp;
            return safe(a).compareToIgnoreCase(safe(b));
        });
        return String.join(", ", list);
    }

    private boolean isOnTrack(KaosCore core, EffectivePermissions effective, String trackRaw) {
        if (core == null || effective == null) return false;
        String track = safe(trackRaw).trim();
        if (track.isEmpty()) return false;
        List<String> trackGroups = core.getConfig().getStringList("permissions.tracks." + track);
        if (trackGroups == null || trackGroups.isEmpty()) return false;

        Set<String> allowed = new HashSet<>();
        for (String g : trackGroups) {
            String n = normalize(g);
            if (!n.isEmpty()) allowed.add(n);
        }
        if (allowed.isEmpty()) return false;

        for (String g : effective.getInheritedGroups()) {
            if (allowed.contains(normalize(g))) return true;
        }
        return false;
    }

    private boolean shouldHideClan(Player player) {
        if (player == null) {
            return false;
        }
        return DisguiseManager.isDisguised(player) && Managers.getDisguiseManager() != null;
    }
}
