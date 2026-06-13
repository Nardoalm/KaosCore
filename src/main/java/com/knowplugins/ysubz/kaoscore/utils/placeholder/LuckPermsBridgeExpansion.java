package com.knowplugins.ysubz.kaoscore.utils.placeholder;

import com.knowplugins.ysubz.kaoscore.KaosCore;
import com.knowplugins.ysubz.kaoscore.managers.Managers;
import com.knowplugins.ysubz.kaoscore.managers.permission.EffectivePermissions;
import com.knowplugins.ysubz.kaoscore.managers.permission.PermissionGroup;
import com.knowplugins.ysubz.kaoscore.managers.permission.PermissionManager;
import com.knowplugins.ysubz.kaoscore.utils.punishment.PunishmentTime;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Bridge de placeholders do LuckPerms para o sistema interno de permissões do KaosCore.
 * Registra apenas quando LuckPerms não estiver instalado para evitar conflito de identifiers.
 */
public final class LuckPermsBridgeExpansion extends PlaceholderExpansion {

    private final KaosCore plugin;

    public LuckPermsBridgeExpansion(KaosCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "luckperms";
    }

    @Override
    public String getAuthor() {
        return "KaosCore";
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

        PermissionManager permissionManager = Managers.getPermissionManager();
        if (permissionManager == null) {
            return "";
        }

        EffectivePermissions effective = permissionManager.getEffective(player.getUniqueId());
        if (effective == null) {
            return "";
        }

        String lower = params.toLowerCase(Locale.ROOT);

        switch (lower) {
            case "group_name":
            case "primary_group_name":
                return safe(effective.getPrimaryGroup());
            case "prefix":
                return colorize(effective.getPrefix());
            case "suffix":
                return colorize(effective.getSuffix());
            case "inherited_groups":
                return joinSortedGroups(effective.getInheritedGroups(), permissionManager);
            case "current_priority":
            case "primary_group_priority":
                return String.valueOf(getGroupPriority(permissionManager, effective.getPrimaryGroup()));
            case "highest_priority_weight":
                return String.valueOf(getHighestPriority(permissionManager, effective.getInheritedGroups()));
            case "player_unique_id":
                return player.getUniqueId().toString();
            case "player_name":
                return safe(player.getName());
            default:
                break;
        }

        if (lower.startsWith("group_expiry_time_")) {
            String group = params.substring("group_expiry_time_".length());
            return formatExpiryTime(resolveGroupExpiry(effective, group));
        }

        if (lower.startsWith("expiry_time_")) {
            String permission = params.substring("expiry_time_".length());
            Long expiresAt = resolveUserPermissionExpiry(effective, permission);
            return expiresAt == null ? "" : formatExpiryTime(expiresAt);
        }

        if (lower.startsWith("inherited_expiry_time_")) {
            String permission = params.substring("inherited_expiry_time_".length());
            Long expiresAt = resolveInheritedPermissionExpiry(permissionManager, effective, permission);
            return expiresAt == null ? "" : formatExpiryTime(expiresAt);
        }

        if (lower.startsWith("has_permission_")) {
            String permission = params.substring("has_permission_".length());
            return hasPermission(player, permission) ? "yes" : "no";
        }

        if (lower.startsWith("check_permission_")) {
            String permission = params.substring("check_permission_".length());
            return hasPermission(player, permission) ? "true" : "false";
        }

        if (lower.startsWith("inherits_group_")) {
            String group = params.substring("inherits_group_".length());
            return effective.getInheritedGroups().contains(normalize(group)) ? "true" : "false";
        }

        if (lower.startsWith("on_track_")) {
            String track = params.substring("on_track_".length());
            return isOnTrack(effective, track) ? "true" : "false";
        }

        if (lower.startsWith("has_groups_on_track_")) {
            String track = params.substring("has_groups_on_track_".length());
            return isOnTrack(effective, track) ? "true" : "false";
        }

        return "";
    }

    private boolean hasPermission(Player player, String permissionRaw) {
        if (player == null) {
            return false;
        }
        String permission = safe(permissionRaw);
        if (permission.isEmpty()) {
            return false;
        }

        if (player.hasPermission(permission)) {
            return true;
        }

        // Compat: configs que trocam '.' por '_' em placeholders.
        if (!permission.contains(".") && permission.contains("_")) {
            String fallback = permission.replace('_', '.');
            return player.hasPermission(fallback);
        }

        return false;
    }

    private Long resolveGroupExpiry(EffectivePermissions effective, String groupRaw) {
        if (effective == null) {
            return null;
        }

        String group = normalize(groupRaw);
        if (group.isEmpty()) {
            return null;
        }

        Long expiresAt = effective.getDirectGroupExpiryAtByName().get(group);
        if (expiresAt == null) {
            return null;
        }
        return expiresAt <= 0L ? 0L : expiresAt;
    }

    private Long resolveUserPermissionExpiry(EffectivePermissions effective, String permissionRaw) {
        if (effective == null) {
            return null;
        }
        String permission = normalizePermission(permissionRaw);
        if (permission.isEmpty()) {
            return null;
        }
        Long expiresAt = effective.getUserPermissionExpiryAtByNode().get(permission);
        if (expiresAt == null) {
            return null;
        }
        return expiresAt <= 0L ? 0L : expiresAt;
    }

    private Long resolveInheritedPermissionExpiry(PermissionManager permissionManager, EffectivePermissions effective, String permissionRaw) {
        if (permissionManager == null || effective == null) {
            return null;
        }

        String permission = normalizePermission(permissionRaw);
        if (permission.isEmpty()) {
            return null;
        }

        // Se existe uma node direta no usuário, não é "inherited".
        if (effective.getUserPermissionExpiryAtByNode().containsKey(permission)) {
            return null;
        }

        long best = -1L;
        for (String groupName : effective.getDirectGroups()) {
            if (groupName == null || groupName.isEmpty()) {
                continue;
            }

            Boolean value = permissionManager.getGroupPermissionValue(groupName, permission);
            if (value == null || !value) {
                continue;
            }

            Long expiresAt = effective.getDirectGroupExpiryAtByName().get(groupName);
            long exp = expiresAt == null ? 0L : expiresAt;
            if (exp <= 0L) {
                return 0L; // permanente
            }
            if (best < exp) {
                best = exp;
            }
        }

        return best < 0L ? null : best;
    }

    private boolean isOnTrack(EffectivePermissions effective, String trackRaw) {
        if (effective == null) {
            return false;
        }

        String track = safe(trackRaw).trim();
        if (track.isEmpty() || plugin == null) {
            return false;
        }

        List<String> groups = plugin.getConfig().getStringList("permissions.tracks." + track);
        if (groups == null || groups.isEmpty()) {
            return false;
        }

        Set<String> set = new HashSet<>();
        for (String g : groups) {
            String n = normalize(g);
            if (!n.isEmpty()) {
                set.add(n);
            }
        }

        if (set.isEmpty()) {
            return false;
        }

        for (String groupName : effective.getInheritedGroups()) {
            if (set.contains(normalize(groupName))) {
                return true;
            }
        }
        return false;
    }

    private String joinSortedGroups(Set<String> groups, PermissionManager permissionManager) {
        if (groups == null || groups.isEmpty()) {
            return "";
        }

        List<String> list = new ArrayList<>(groups);
        list.sort((a, b) -> {
            int pa = getGroupPriority(permissionManager, a);
            int pb = getGroupPriority(permissionManager, b);
            int cmp = Integer.compare(pb, pa);
            if (cmp != 0) {
                return cmp;
            }
            return safe(a).compareToIgnoreCase(safe(b));
        });

        return String.join(", ", list);
    }

    private int getGroupPriority(PermissionManager permissionManager, String groupName) {
        if (permissionManager == null) {
            return 0;
        }
        PermissionGroup group = permissionManager.getGroup(groupName);
        return group == null ? 0 : group.getPriority();
    }

    private int getHighestPriority(PermissionManager permissionManager, Set<String> groups) {
        if (permissionManager == null || groups == null || groups.isEmpty()) {
            return 0;
        }
        int best = 0;
        for (String name : groups) {
            PermissionGroup g = permissionManager.getGroup(name);
            if (g != null && g.getPriority() > best) {
                best = g.getPriority();
            }
        }
        return best;
    }

    private String formatExpiryTime(long expiresAt) {
        return PunishmentTime.formatRemaining(expiresAt);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String colorize(String value) {
        return ChatColor.translateAlternateColorCodes('&', safe(value));
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizePermission(String value) {
        String permission = safe(value).trim();
        if (permission.isEmpty()) {
            return "";
        }

        // Primeiro tenta literal.
        String normalized = normalize(permission);
        if (normalized.contains(".") || !normalized.contains("_")) {
            return normalized;
        }

        // Compat: troca '_' por '.' quando o config usa placeholders assim.
        return normalize(permission.replace('_', '.'));
    }
}
