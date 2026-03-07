package com.knowplugins.ysubz.kaoscore.managers.player;

import com.knowplugins.ysubz.kaoscore.KaosCore;
import com.knowplugins.ysubz.kaoscore.managers.Managers;
import com.knowplugins.ysubz.kaoscore.managers.TagManager;
import com.knowplugins.ysubz.kaoscore.tag.Tag;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class TagTeamManager {

    private static final String TEAM_PREFIX = "kc";

    private final KaosCore plugin;
    private final TagManager tagManager;
    private final ConcurrentMap<UUID, Scoreboard> trackedBoards;
    private BukkitTask syncTask;

    public TagTeamManager(KaosCore plugin, TagManager tagManager) {
        this.plugin = plugin;
        this.tagManager = tagManager;
        this.trackedBoards = new ConcurrentHashMap<>();
    }

    public void start() {
        if (plugin == null) {
            return;
        }

        stop();
        syncTask = Bukkit.getScheduler().runTaskTimer(plugin, this::syncChangedBoards, 1L, 20L);
    }

    public void stop() {
        if (syncTask != null) {
            syncTask.cancel();
            syncTask = null;
        }
        trackedBoards.clear();
    }

    public void refreshAll() {
        if (plugin == null) {
            return;
        }

        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, this::refreshAll);
            return;
        }

        for (Player viewer : Bukkit.getOnlinePlayers()) {
            refreshViewer(viewer);
        }
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

        trackedBoards.put(viewer.getUniqueId(), scoreboard);

        for (Player target : Bukkit.getOnlinePlayers()) {
            applyTag(scoreboard, target, collectEntries(target));
        }
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
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            Scoreboard scoreboard = resolveScoreboard(viewer);
            if (scoreboard != null) {
                applyTag(scoreboard, player, entries);
            }
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
        }
    }

    private void syncChangedBoards() {
        Set<UUID> onlinePlayers = new HashSet<>();

        for (Player viewer : Bukkit.getOnlinePlayers()) {
            onlinePlayers.add(viewer.getUniqueId());

            Scoreboard currentBoard = resolveScoreboard(viewer);
            Scoreboard previousBoard = trackedBoards.put(viewer.getUniqueId(), currentBoard);

            if (currentBoard != null && previousBoard != currentBoard) {
                refreshViewer(viewer);
            }
        }

        trackedBoards.keySet().retainAll(onlinePlayers);
    }

    private void applyTag(Scoreboard scoreboard, Player player, Set<String> staleEntries) {
        if (scoreboard == null || player == null) {
            return;
        }

        removeEntries(scoreboard, staleEntries);

        Tag tag = tagManager == null ? null : tagManager.getTag(player);
        if (tag == null) {
            return;
        }

        String currentEntry = safe(player.getName());
        if (currentEntry.isEmpty()) {
            return;
        }

        Team team = getOrCreateTeam(scoreboard, tag);
        if (team == null) {
            return;
        }

        String prefix = trim(format(tag.getPrefix()));
        String suffix = trim(format(tag.getSuffix()));

        if (!prefix.equals(team.getPrefix())) {
            team.setPrefix(prefix);
        }

        if (!suffix.equals(team.getSuffix())) {
            team.setSuffix(suffix);
        }

        if (!team.getEntries().contains(currentEntry)) {
            team.addEntry(currentEntry);
        }
    }

    private Team getOrCreateTeam(Scoreboard scoreboard, Tag tag) {
        if (scoreboard == null || tag == null) {
            return null;
        }

        String teamName = buildTeamName(tag);
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

    private String buildTeamName(Tag tag) {
        int position = tag == null ? 9999 : tag.getPosition();
        if (position < 0) {
            position = 0;
        }
        if (position > 9999) {
            position = 9999;
        }

        String id = tag == null ? "tag" : safe(tag.getId());
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

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
