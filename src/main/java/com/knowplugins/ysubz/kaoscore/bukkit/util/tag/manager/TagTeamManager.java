package com.knowplugins.ysubz.kaoscore.bukkit.util.tag.manager;

import com.knowplugins.ysubz.kaoscore.bukkit.KaosCore;
import com.knowplugins.ysubz.kaoscore.bukkit.clan.Clan;
import com.knowplugins.ysubz.kaoscore.bukkit.clan.manager.ClanManager;
import com.knowplugins.ysubz.kaoscore.bukkit.disguise.DisguiseManager;
import com.knowplugins.ysubz.kaoscore.bukkit.integration.practice.KaosPracticeBridge;
import com.knowplugins.ysubz.kaoscore.bukkit.manager.MainManager;
import com.knowplugins.ysubz.kaoscore.bukkit.manager.player.MedalManager;
import com.knowplugins.ysubz.kaoscore.bukkit.manager.player.VanishManager;
import com.knowplugins.ysubz.kaoscore.bukkit.util.tag.Tag;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class TagTeamManager {

    private static final String TEAM_PREFIX = "kc";
    private static final int SPECTATOR_PRIORITY = 9500;

    private final KaosCore plugin;
    private final TagManager tagManager;
    private final MedalManager medalManager;
    private final KaosPracticeBridge practiceBridge;
    private final ConcurrentMap<UUID, Scoreboard> trackedBoards;
    private final Set<String> practiceHiddenPairs;
    private BukkitTask syncTask;

    public TagTeamManager(KaosCore plugin, TagManager tagManager, MedalManager medalManager) {
        this.plugin = plugin;
        this.tagManager = tagManager;
        this.medalManager = medalManager;
        this.practiceBridge = new KaosPracticeBridge();
        this.trackedBoards = new ConcurrentHashMap<>();
        this.practiceHiddenPairs = ConcurrentHashMap.newKeySet();
    }

    public void start() {
        if (plugin == null) {
            return;
        }

        stop();
        syncTask = Bukkit.getScheduler().runTaskTimer(plugin, this::syncAll, 1L, 10L);
    }

    public void stop() {
        if (syncTask != null) {
            syncTask.cancel();
            syncTask = null;
        }

        Set<Scoreboard> scoreboards = new HashSet<>(trackedBoards.values());
        for (Player online : Bukkit.getOnlinePlayers()) {
            Scoreboard scoreboard = online.getScoreboard();
            if (scoreboard != null) {
                scoreboards.add(scoreboard);
            }
        }
        if (Bukkit.getScoreboardManager() != null) {
            Scoreboard mainScoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
            if (mainScoreboard != null) {
                scoreboards.add(mainScoreboard);
            }
        }

        for (Scoreboard scoreboard : scoreboards) {
            clearManagedTeams(scoreboard);
        }

        trackedBoards.clear();
        practiceHiddenPairs.clear();
    }

    public void refreshAll() {
        if (plugin == null) {
            return;
        }

        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, this::refreshAll);
            return;
        }

        syncAll();
    }

    public void refreshViewer(Player viewer) {
        if (plugin == null || viewer == null || !viewer.isOnline()) {
            return;
        }

        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, () -> refreshViewer(viewer));
            return;
        }

        Scoreboard scoreboard = resolveScoreboard(viewer);
        if (scoreboard == null) {
            return;
        }

        refreshViewer(viewer, scoreboard, collectPracticeStates());
    }

    public void refreshPlayer(Player player, String... staleEntries) {
        if (plugin == null || player == null) {
            return;
        }

        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, () -> refreshPlayer(player, staleEntries));
            return;
        }

        Set<String> entries = collectEntries(player, staleEntries);
        KaosPracticeBridge.PracticeState playerState = practiceBridge.getState(player);

        for (Player viewer : Bukkit.getOnlinePlayers()) {
            Scoreboard scoreboard = resolveScoreboard(viewer);
            if (scoreboard == null) {
                continue;
            }

            updateVisibility(viewer, player, practiceBridge.getState(viewer), playerState);
            applyTag(scoreboard, player, entries, playerState);
        }
    }

    public void removePlayer(Player player, String... staleEntries) {
        if (plugin == null || player == null) {
            return;
        }

        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, () -> removePlayer(player, staleEntries));
            return;
        }

        Set<String> entries = collectEntries(player, staleEntries);
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            Scoreboard scoreboard = resolveScoreboard(viewer);
            if (scoreboard != null) {
                removeEntries(scoreboard, entries);
                cleanupEmptyTeams(scoreboard);
            }
            clearPracticeVisibility(viewer, player);
        }

        trackedBoards.remove(player.getUniqueId());
        String playerId = player.getUniqueId().toString();
        practiceHiddenPairs.removeIf(pair -> pair.startsWith(playerId + ":") || pair.endsWith(":" + playerId));
    }

    private void syncAll() {
        Set<UUID> onlinePlayers = new HashSet<>();
        Map<UUID, KaosPracticeBridge.PracticeState> practiceStates = collectPracticeStates();

        for (Player viewer : Bukkit.getOnlinePlayers()) {
            onlinePlayers.add(viewer.getUniqueId());
            Scoreboard scoreboard = resolveScoreboard(viewer);
            if (scoreboard != null) {
                refreshViewer(viewer, scoreboard, practiceStates);
                cleanupEmptyTeams(scoreboard);
            }
        }

        trackedBoards.keySet().retainAll(onlinePlayers);
        practiceHiddenPairs.removeIf(pair -> {
            String[] split = pair.split(":");
            if (split.length != 2) {
                return true;
            }
            try {
                return Bukkit.getPlayer(UUID.fromString(split[0])) == null || Bukkit.getPlayer(UUID.fromString(split[1])) == null;
            } catch (IllegalArgumentException ignored) {
                return true;
            }
        });
    }

    private void refreshViewer(Player viewer, Scoreboard scoreboard, Map<UUID, KaosPracticeBridge.PracticeState> practiceStates) {
        if (viewer == null || scoreboard == null) {
            return;
        }

        trackedBoards.put(viewer.getUniqueId(), scoreboard);
        KaosPracticeBridge.PracticeState viewerState = practiceStates.get(viewer.getUniqueId());

        for (Player target : Bukkit.getOnlinePlayers()) {
            KaosPracticeBridge.PracticeState targetState = practiceStates.get(target.getUniqueId());
            updateVisibility(viewer, target, viewerState, targetState);
            applyTag(scoreboard, target, collectEntries(target), targetState);
        }

        KaosPracticeBridge.PracticeState selfState = practiceStates.get(viewer.getUniqueId());
        applyTag(scoreboard, viewer, collectEntries(viewer), selfState);
    }

    private void applyTag(Scoreboard scoreboard, Player player, Set<String> staleEntries, KaosPracticeBridge.PracticeState practiceState) {
        if (scoreboard == null || player == null) {
            return;
        }

        NametagStyle style = resolveStyle(player, practiceState);
        if (style == null) {
            removeEntries(scoreboard, staleEntries);
            return;
        }

        DisguiseManager disguiseManager = MainManager.getDisguiseManager();
        String currentEntry = (disguiseManager != null && DisguiseManager.isDisguised(player))
                ? safe(disguiseManager.getDisguiseNick(player))
                : safe(player.getName());

        if (currentEntry.isEmpty()) {
            currentEntry = safe(player.getName());
        }

        String expectedTeamName = buildTeamName(style);
        Team currentTeam = scoreboard.getEntryTeam(currentEntry);

        if (currentTeam != null && currentTeam.getName().equals(expectedTeamName)) {
            if (currentTeam.getPrefix().equals(style.getPrefix()) && currentTeam.getSuffix().equals(style.getSuffix())) {
                return;
            }
        }

        Team targetTeam = getOrCreateTeam(scoreboard, style);
        if (targetTeam == null) {
            return;
        }

        try {
            String newPrefix = style.getPrefix();
            if (!newPrefix.equals(targetTeam.getPrefix())) {
                targetTeam.setPrefix(newPrefix);
            }

            String newSuffix = style.getSuffix();
            if (!newSuffix.equals(targetTeam.getSuffix())) {
                targetTeam.setSuffix(newSuffix);
            }

            for (Team otherTeam : scoreboard.getTeams()) {
                if (otherTeam != null && !otherTeam.getName().equals(expectedTeamName) && otherTeam.getName().startsWith(TEAM_PREFIX)) {
                    if (otherTeam.hasEntry(currentEntry)) {
                        otherTeam.removeEntry(currentEntry);
                    }
                    for (String stale : staleEntries) {
                        if (!stale.equals(currentEntry) && otherTeam.hasEntry(stale)) {
                            otherTeam.removeEntry(stale);
                        }
                    }
                }
            }

            for (String stale : staleEntries) {
                if (!stale.equals(currentEntry) && targetTeam.hasEntry(stale)) {
                    targetTeam.removeEntry(stale);
                }
            }

            if (!targetTeam.hasEntry(currentEntry)) {
                targetTeam.addEntry(currentEntry);
            }

            try {
                if (!player.getPlayerListName().equals(currentEntry)) {
                    player.setPlayerListName(currentEntry);
                }
            } catch (Exception ignored) {
            }

        } catch (IllegalStateException ignored) {
        } catch (Exception e) {
            plugin.getLogger().warning("Error applying tag to " + player.getName() + ": " + e.getMessage());
        }
    }

    private Team getOrCreateTeam(Scoreboard scoreboard, NametagStyle style) {
        if (scoreboard == null || style == null) {
            return null;
        }

        String teamName = buildTeamName(style);

        try {
            Team team = scoreboard.getTeam(teamName);
            if (team != null) {
                return team;
            }
            return scoreboard.registerNewTeam(teamName);
        } catch (IllegalArgumentException e) {
            return scoreboard.getTeam(teamName);
        } catch (IllegalStateException e) {
            plugin.getLogger().warning("Failed to create team " + teamName + ": " + e.getMessage());
            return null;
        }
    }

    private NametagStyle resolveStyle(Player player, KaosPracticeBridge.PracticeState practiceState) {
        if (player == null) {
            return null;
        }

        // BLOQUEIO SUPREMO: Prioridade máxima para lutadores ativos dos eventos
        try {
            us.alleypvp.practice.AlleyPractice alley = us.alleypvp.practice.AlleyPractice.getInstance();
            if (alley != null) {
                us.alleypvp.practice.feature.event.EventService eventService = alley.getService(us.alleypvp.practice.feature.event.EventService.class);
                if (eventService != null) {
                    us.alleypvp.practice.feature.event.model.ActiveEvent activeEvent = eventService.getActiveEvent();
                    if (activeEvent != null && activeEvent.isParticipant(player.getUniqueId())) {
                        us.alleypvp.practice.feature.event.model.EventParticipant participant = activeEvent.getParticipant(player.getUniqueId());
                        if (participant != null) {
                            if (participant.getState() == us.alleypvp.practice.feature.event.EventPlayerState.FIGHTING) {
                                String color = "§a"; // Cor Genérica (FFA)
                                us.alleypvp.practice.feature.event.EventType type = activeEvent.getDefinition().getType();
                                if (type == us.alleypvp.practice.feature.event.EventType.SUMO ||
                                        type == us.alleypvp.practice.feature.event.EventType.BRACKETS ||
                                        type == us.alleypvp.practice.feature.event.EventType.GULAG) {
                                    if (player.getUniqueId().equals(activeEvent.getCurrentFighterA())) {
                                        color = "§c";
                                    } else if (player.getUniqueId().equals(activeEvent.getCurrentFighterB())) {
                                        color = "§9";
                                    }
                                }
                                String teamKey = "event_" + color + ":" + styleSignature(color, "");
                                return new NametagStyle(teamKey, color, "", 50);
                            }
                            // O pulo do gato: Se estiver WAITING ou SPECTATING ele ignora essa trava e continua a leitura para setar a tag base.
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}

        if (practiceState != null && practiceState.isSpectating()) {
            return new NametagStyle("practice_spectator", "§8", "", SPECTATOR_PRIORITY);
        }

        if (practiceState != null && practiceState.isOverrideDefaultNametag()) {
            String prefix = trimTeamPart(resolveText(player, practiceState.getPrefix()) + safe(practiceState.getNameColor()));
            String suffix = trimTeamPart(resolveText(player, practiceState.getSuffix()));
            int priority = practiceState.getPriority() > 0 ? practiceState.getPriority() : 100;
            String teamKey = "practice_" + safe(practiceState.getTeamId()) + safe(practiceState.getNameColor()) + ":" + styleSignature(prefix, suffix);
            return new NametagStyle(teamKey, prefix, suffix, priority);
        }

        Tag tag = null;
        if (tagManager != null) {
            tag = tagManager.getTag(player);
        }

        if (tag == null) {
            return null;
        }

        Clan clan = null;
        ClanManager clanManager = MainManager.getClanManager();
        if (clanManager != null && !DisguiseManager.isDisguised(player)) {
            clan = clanManager.getClan(player);
        }

        String clanKey = clan == null ? "" : safe(clan.getName());
        String clanTagValue = "";
        String clanSuffix = "";
        if (clan != null) {
            clanTagValue = safe(clan.getTag());
            if (!clanTagValue.isEmpty()) {
                clanSuffix = " " + clan.getNametagFormattedTag();
            }
        }

        String tagPrefix = resolveText(player, tag.getPrefix());
        String tagColor = resolveText(player, tag.getColor());
        String tagSuffix = resolveText(player, tag.getSuffix());

        if (!clanTagValue.isEmpty() && !clanSuffix.isEmpty()) {
            String strippedSuffix = ChatColor.stripColor(safe(tagSuffix));
            String token = "[" + clanTagValue + "]";
            if (strippedSuffix != null && strippedSuffix.contains(token)) {
                clanSuffix = "";
            }
        }

        String tailColor = lastResettingColor(tagColor);
        if (tailColor.isEmpty()) {
            tailColor = lastResettingColor(tagPrefix);
        }

        String prefix;
        if (!tailColor.isEmpty()) {
            int available = 16 - tailColor.length();
            String trimmedBody = available <= 0 ? "" : trimTeamPart(tagPrefix, available);
            prefix = trimTeamPart(trimmedBody + tailColor);
        } else {
            prefix = trimTeamPart(tagPrefix);
        }

        String suffix = trimTeamPart(tagSuffix + clanSuffix);
        String key = tag.getId()
                + ":" + (clanKey.isEmpty() ? "none" : clanKey.toLowerCase(Locale.ROOT))
                + ":" + styleSignature(prefix, suffix);

        return new NametagStyle(key, prefix, suffix, tag.getPosition());
    }

    private void removeEntries(Scoreboard scoreboard, Set<String> entries) {
        if (scoreboard == null || entries == null || entries.isEmpty()) {
            return;
        }

        for (Team team : scoreboard.getTeams()) {
            if (team == null || team.getName() == null || !team.getName().startsWith(TEAM_PREFIX)) {
                continue;
            }

            for (String entry : entries) {
                if (safe(entry).isEmpty()) {
                    continue;
                }

                try {
                    if (team.hasEntry(entry)) {
                        team.removeEntry(entry);
                    }
                } catch (IllegalStateException ignored) {
                } catch (Exception e) {
                    plugin.getLogger().warning("Error removing entry " + entry + " from team " + team.getName() + ": " + e.getMessage());
                }
            }
        }
    }

    private Set<String> collectEntries(Player player, String... extraEntries) {
        LinkedHashSet<String> entries = new LinkedHashSet<>();
        if (player != null) {
            entries.add(safe(player.getName()));

            DisguiseManager disguiseManager = MainManager.getDisguiseManager();
            if (disguiseManager != null) {
                entries.add(safe(disguiseManager.getDisguiseNick(player)));
                entries.add(safe(disguiseManager.getOriginalName(player)));
            }
        }

        if (extraEntries != null) {
            for (String entry : extraEntries) {
                entries.add(safe(entry));
            }
        }

        entries.remove("");
        return entries;
    }

    private void updateVisibility(Player viewer, Player target, KaosPracticeBridge.PracticeState viewerState, KaosPracticeBridge.PracticeState targetState) {
        if (viewer == null || target == null || viewer.equals(target)) {
            return;
        }

        String key = pairKey(viewer, target);
        boolean shouldHideForPractice = shouldHideForPractice(viewerState, targetState);

        if (shouldHideForPractice) {
            if (practiceHiddenPairs.add(key)) {
                viewer.hidePlayer(target);
            }
            return;
        }

        clearPracticeVisibility(viewer, target);
    }

    private boolean shouldHideForPractice(KaosPracticeBridge.PracticeState viewerState, KaosPracticeBridge.PracticeState targetState) {
        if (viewerState == null || !viewerState.isInMatch()) {
            return false;
        }

        if (targetState == null) {
            return true;
        }

        if (targetState.isSpectating() || !targetState.isInMatch()) {
            return true;
        }

        String viewerMatchId = safe(viewerState.getMatchId());
        String targetMatchId = safe(targetState.getMatchId());
        if (viewerMatchId.isEmpty() || targetMatchId.isEmpty()) {
            return true;
        }

        return !viewerMatchId.equalsIgnoreCase(targetMatchId);
    }

    private void clearPracticeVisibility(Player viewer, Player target) {
        if (viewer == null || target == null || viewer.equals(target)) {
            return;
        }

        String key = pairKey(viewer, target);
        if (!practiceHiddenPairs.remove(key)) {
            return;
        }

        viewer.showPlayer(target);
        if (VanishManager.isVanished(target) && !viewer.hasPermission("kaoscore.staff.vanish")) {
            viewer.hidePlayer(target);
        }
    }

    private String pairKey(Player viewer, Player target) {
        return viewer.getUniqueId() + ":" + target.getUniqueId();
    }

    private Map<UUID, KaosPracticeBridge.PracticeState> collectPracticeStates() {
        Map<UUID, KaosPracticeBridge.PracticeState> states = new ConcurrentHashMap<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            states.put(online.getUniqueId(), practiceBridge.getState(online));
        }
        return states;
    }

    private Scoreboard resolveScoreboard(Player viewer) {
        if (viewer == null || Bukkit.getScoreboardManager() == null) {
            return null;
        }

        Scoreboard scoreboard = viewer.getScoreboard();
        if (scoreboard != null) {
            return scoreboard;
        }

        return Bukkit.getScoreboardManager().getMainScoreboard();
    }

    private String buildTeamName(NametagStyle style) {
        int position = style == null ? 9999 : style.getPriority();
        if (position < 0) {
            position = 0;
        }
        if (position > 9999) {
            position = 9999;
        }

        String id = style == null ? "tag" : safe(style.getKey());
        int hash = Math.abs(id.hashCode());
        String suffix = String.format("%08x", hash);
        return TEAM_PREFIX + String.format("%04d", position) + suffix;
    }

    private void cleanupEmptyTeams(Scoreboard scoreboard) {
        if (scoreboard == null) {
            return;
        }

        Set<Team> teams = new HashSet<>(scoreboard.getTeams());
        for (Team team : teams) {
            if (team == null) {
                continue;
            }

            String name = team.getName();
            if (name == null || !name.startsWith(TEAM_PREFIX) || name.length() != TEAM_PREFIX.length() + 12) {
                continue;
            }

            try {
                Set<String> entries = team.getEntries();
                if (entries == null || entries.isEmpty()) {
                    team.unregister();
                }
            } catch (IllegalStateException ignored) {
            } catch (Exception e) {
                plugin.getLogger().warning("Error cleaning up team " + name + ": " + e.getMessage());
            }
        }
    }

    private void clearManagedTeams(Scoreboard scoreboard) {
        if (scoreboard == null) {
            return;
        }

        Set<Team> teams = new HashSet<>(scoreboard.getTeams());
        for (Team team : teams) {
            if (team == null) {
                continue;
            }

            String name = team.getName();
            if (name == null || !name.startsWith(TEAM_PREFIX)) {
                continue;
            }

            try {
                team.unregister();
            } catch (IllegalStateException ignored) {
            } catch (Exception e) {
                plugin.getLogger().warning("Error clearing team " + name + ": " + e.getMessage());
            }
        }
    }

    private String trimTeamPart(String value) {
        return trimTeamPart(value, 16);
    }

    private String trimTeamPart(String value, int maxLen) {
        String safeValue = safe(value);
        if (safeValue.isEmpty() || maxLen <= 0) {
            return "";
        }

        int length = safeValue.length();
        int end = Math.min(length, maxLen);

        while (end > 0) {
            char last = safeValue.charAt(end - 1);
            if (last == ChatColor.COLOR_CHAR) {
                end--;
                continue;
            }
            if (end < length && Character.isHighSurrogate(last)) {
                end--;
                continue;
            }
            break;
        }

        if (end <= 0) {
            return "";
        }

        return safeValue.substring(0, end);
    }

    private String format(String value) {
        return ChatColor.translateAlternateColorCodes('&', safe(value));
    }

    private String resolveText(Player player, String value) {
        String text = safe(value);
        if (text.isEmpty()) {
            return "";
        }

        if (player != null && Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            try {
                text = PlaceholderAPI.setPlaceholders(player, text);
            } catch (Exception ignored) {
            }
        }

        text = text.replace(" %kaoscore_clan_tag%", "").replace("%kaoscore_clan_tag%", "");

        return format(text);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String styleSignature(String prefix, String suffix) {
        return Integer.toHexString((safe(prefix) + "|" + safe(suffix)).hashCode());
    }

    private String lastResettingColor(String text) {
        String value = safe(text);
        if (value.length() < 2) {
            return "";
        }

        for (int i = value.length() - 2; i >= 0; i--) {
            if (value.charAt(i) != ChatColor.COLOR_CHAR) {
                continue;
            }

            char code = Character.toLowerCase(value.charAt(i + 1));
            if ((code >= '0' && code <= '9') || (code >= 'a' && code <= 'f') || code == 'r') {
                return "" + ChatColor.COLOR_CHAR + code;
            }
        }

        return "";
    }

    public void applyLowestValueTag(Player player) {
        if (player == null || tagManager == null) {
            return;
        }

        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, () -> applyLowestValueTag(player));
            return;
        }

        Tag lowestTag = null;
        int highestPosition = -1;

        try {
            Field tagsByIdField = tagManager.getClass().getDeclaredField("tagsById");
            tagsByIdField.setAccessible(true);
            Map<String, Tag> tagsById = (Map<String, Tag>) tagsByIdField.get(tagManager);

            for (Tag tag : tagsById.values()) {
                if (tag == null) continue;
                if (tag.getPosition() > highestPosition) {
                    highestPosition = tag.getPosition();
                    lowestTag = tag;
                }
            }
        } catch (Exception ignored) {
        }

        if (lowestTag == null) {
            return;
        }

        String tagPrefix = resolveText(player, lowestTag.getPrefix());
        String tagColor = resolveText(player, lowestTag.getColor());
        String tagSuffix = resolveText(player, lowestTag.getSuffix());

        String tailColor = lastResettingColor(tagColor);
        if (tailColor.isEmpty()) {
            tailColor = lastResettingColor(tagPrefix);
        }

        String prefix;
        if (!tailColor.isEmpty()) {
            int available = 16 - tailColor.length();
            String trimmedBody = available <= 0 ? "" : trimTeamPart(tagPrefix, available);
            prefix = trimTeamPart(trimmedBody + tailColor);
        } else {
            prefix = trimTeamPart(tagPrefix);
        }

        String suffix = trimTeamPart(tagSuffix);
        NametagStyle lowestStyle = new NametagStyle("lowest_disguise_" + lowestTag.getId(), prefix, suffix, lowestTag.getPosition());

        Set<String> entries = collectEntries(player);
        DisguiseManager disguiseManager = MainManager.getDisguiseManager();
        String currentEntry = (disguiseManager != null && DisguiseManager.isDisguised(player))
                ? safe(disguiseManager.getDisguiseNick(player))
                : player.getName();

        Team targetTeam = getOrCreateTeam(player.getScoreboard(), lowestStyle);
        if (targetTeam == null) return;

        player.setDisplayName(prefix + currentEntry + suffix);

        for (Player viewer : Bukkit.getOnlinePlayers()) {
            Scoreboard scoreboard = resolveScoreboard(viewer);
            if (scoreboard == null) continue;

            Team team = getOrCreateTeam(scoreboard, lowestStyle);
            if (team == null) continue;

            try {
                if (!prefix.equals(team.getPrefix())) team.setPrefix(prefix);
                if (!suffix.equals(team.getSuffix())) team.setSuffix(suffix);

                for (Team otherTeam : scoreboard.getTeams()) {
                    if (otherTeam != null && otherTeam != team && otherTeam.getName() != null && otherTeam.getName().startsWith(TEAM_PREFIX)) {
                        if (otherTeam.hasEntry(currentEntry)) {
                            otherTeam.removeEntry(currentEntry);
                        }
                    }
                }

                if (!team.hasEntry(currentEntry)) {
                    team.addEntry(currentEntry);
                }
            } catch (Exception ignored) {
            }
        }
    }

    private static final class NametagStyle {
        private final String key;
        private final String prefix;
        private final String suffix;
        private final int priority;

        private NametagStyle(String key, String prefix, String suffix, int priority) {
            this.key = key == null ? "" : key;
            this.prefix = prefix == null ? "" : prefix;
            this.suffix = suffix == null ? "" : suffix;
            this.priority = priority;
        }

        public String getKey() {
            return key;
        }

        public String getPrefix() {
            return prefix;
        }

        public String getSuffix() {
            return suffix;
        }

        public int getPriority() {
            return priority;
        }
    }
}