package com.knowplugins.ysubz.kaoscore.managers.player;

import com.knowplugins.ysubz.kaoscore.KaosCore;
import com.knowplugins.ysubz.kaoscore.integration.practice.KaosPracticeBridge;
import com.knowplugins.ysubz.kaoscore.managers.Managers;
import com.knowplugins.ysubz.kaoscore.managers.TagManager;
import com.knowplugins.ysubz.kaoscore.tag.Tag;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class TagTeamManager {

    private static final String TEAM_PREFIX = "kc";
    private static final int SPECTATOR_PRIORITY = 9500;

    private final KaosCore plugin;
    private final TagManager tagManager;
    private final KaosPracticeBridge practiceBridge;
    private final ConcurrentMap<UUID, Scoreboard> trackedBoards;
    private final Set<String> practiceHiddenPairs;
    private BukkitTask syncTask;

    public TagTeamManager(KaosCore plugin, TagManager tagManager) {
        this.plugin = plugin;
        this.tagManager = tagManager;
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
            }
            clearPracticeVisibility(viewer, player);
        }

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
    }

    private void applyTag(Scoreboard scoreboard, Player player, Set<String> staleEntries, KaosPracticeBridge.PracticeState practiceState) {
        if (scoreboard == null || player == null) {
            return;
        }

        removeEntries(scoreboard, staleEntries);

        NametagStyle style = resolveStyle(player, practiceState);
        if (style == null) {
            return;
        }

        String currentEntry = safe(player.getName());
        if (currentEntry.isEmpty()) {
            return;
        }

        Team team = getOrCreateTeam(scoreboard, style);
        if (team == null) {
            return;
        }

        if (!style.getPrefix().equals(team.getPrefix())) {
            team.setPrefix(style.getPrefix());
        }

        if (!style.getSuffix().equals(team.getSuffix())) {
            team.setSuffix(style.getSuffix());
        }

        if (!team.getEntries().contains(currentEntry)) {
            team.addEntry(currentEntry);
        }
    }

    private Team getOrCreateTeam(Scoreboard scoreboard, NametagStyle style) {
        if (scoreboard == null || style == null) {
            return null;
        }

        String teamName = buildTeamName(style);
        Team team = scoreboard.getTeam(teamName);
        if (team != null) {
            return team;
        }

        try {
            return scoreboard.registerNewTeam(teamName);
        } catch (IllegalArgumentException ignored) {
            return scoreboard.getTeam(teamName);
        }
    }

    private NametagStyle resolveStyle(Player player, KaosPracticeBridge.PracticeState practiceState) {
        if (player == null) {
            return null;
        }

        if (practiceState != null && practiceState.isSpectating()) {
            return new NametagStyle("practice_spectator", "§8", "", SPECTATOR_PRIORITY);
        }

        if (practiceState != null && practiceState.isOverrideDefaultNametag()) {
            String prefix = trim(resolveText(player, practiceState.getPrefix()) + safe(practiceState.getNameColor()));
            String suffix = trim(resolveText(player, practiceState.getSuffix()));
            int priority = practiceState.getPriority() > 0 ? practiceState.getPriority() : 100;
            String teamKey = "practice_" + safe(practiceState.getTeamId()) + safe(practiceState.getNameColor());
            return new NametagStyle(teamKey, prefix, suffix, priority);
        }

        Tag tag = tagManager == null ? null : tagManager.getTag(player);
        if (tag == null) {
            return null;
        }

        return new NametagStyle(tag.getId(), trim(resolveText(player, tag.getPrefix())), trim(resolveText(player, tag.getSuffix())), tag.getPosition());
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
                if (!safe(entry).isEmpty() && team.getEntries().contains(entry)) {
                    team.removeEntry(entry);
                }
            }
        }
    }

    private Set<String> collectEntries(Player player, String... extraEntries) {
        LinkedHashSet<String> entries = new LinkedHashSet<>();
        if (player != null) {
            entries.add(safe(player.getName()));

            DisguiseManager disguiseManager = Managers.getDisguiseManager();
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
        boolean shouldHideForPractice = viewerState != null && viewerState.isInMatch() && targetState != null && targetState.isSpectating();

        if (shouldHideForPractice) {
            if (practiceHiddenPairs.add(key)) {
                viewer.hidePlayer(target);
            }
            return;
        }

        clearPracticeVisibility(viewer, target);
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
        if (VanishManager.isVanished(target) && !viewer.hasPermission("nardocore.staff.vanish")) {
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
        String suffix = String.format("%06x", hash & 0xFFFFFF);
        return TEAM_PREFIX + String.format("%04d", position) + suffix;
    }

    private String trim(String value) {
        String safeValue = safe(value);
        if (safeValue.length() <= 16) {
            return safeValue;
        }
        return safeValue.substring(0, 16);
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

        return format(text);
    }

    private String safe(String value) {
        return value == null ? "" : value;
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
