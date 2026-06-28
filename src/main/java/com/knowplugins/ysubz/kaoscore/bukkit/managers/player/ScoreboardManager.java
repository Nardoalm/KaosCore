package com.knowplugins.ysubz.kaoscore.bukkit.managers.player;

import com.knowplugins.ysubz.kaoscore.bukkit.KaosCore;
import com.knowplugins.ysubz.kaoscore.bukkit.util.placeholder.PlaceholderManager;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ScoreboardManager {

    private static final Map<UUID, BoardState> BOARDS = new HashMap<>();
    private static final String[] DEFAULT_ANIMATION = {
            "§f§lF§b§lREEP",
            "§b§lF§f§lR§b§lEEP",
            "§b§lFR§f§lE§b§lEP",
            "§b§lFRE§f§lE§b§lP",
            "§b§lFREE§f§lP",
            "§f§lFREEP",
            "§f§lFREEP",
            "§b§lFREEP",
            "§b§lFREEP",
            "§f§lFREEP",
            "§f§lFREEP"
    };
    private static final String[] LINE_ENTRIES = {
            "§0", "§1", "§2", "§3", "§4",
            "§5", "§6", "§7", "§8", "§9",
            "§a", "§b", "§c", "§d", "§e"
    };

    private static int index = 0;
    private static String[] animation = DEFAULT_ANIMATION;
    private static List<String> configuredLines = defaultLines(null);
    private static BukkitRunnable task;

    public static void setup() {
        KaosCore plugin = KaosCore.getInstance();
        if (plugin == null) {
            return;
        }

        if (task != null) {
            task.cancel();
            task = null;
        }

        if (!plugin.getConfig().getBoolean("features.scoreboard.enabled", true)) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                remove(player);
            }
            return;
        }

        animation = loadAnimation(plugin.getConfig());
        configuredLines = loadLines(plugin.getConfig());

        int period = Math.max(2, plugin.getConfig().getInt("features.scoreboard.update-ticks", 4));
        task = new BukkitRunnable() {
            @Override
            public void run() {
                index++;
                if (index >= animation.length) {
                    index = 0;
                }

                for (Player player : Bukkit.getOnlinePlayers()) {
                    update(player);
                }
            }
        };
        task.runTaskTimer(plugin, 0L, period);
    }

    public static void reload() {
        setup();
    }

    public static void update(Player player) {
        if (player == null) {
            return;
        }

        BoardState state = BOARDS.get(player.getUniqueId());
        if (state == null || state.lineCount != configuredLines.size() || state.objective == null || state.scoreboard.getObjective("sidebar") == null) {
            state = createBoard(configuredLines.size());
            BOARDS.put(player.getUniqueId(), state);
        }

        if (player.getScoreboard() != state.scoreboard) {
            player.setScoreboard(state.scoreboard);
        }

        state.objective.setDisplayName(animation[index]);
        for (int i = 0; i < configuredLines.size(); i++) {
            updateLine(state.scoreboard, "line" + i, renderLine(player, configuredLines.get(i)));
        }
    }

    public static void remove(Player player) {
        if (player == null) {
            return;
        }
        BOARDS.remove(player.getUniqueId());
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }

    private static BoardState createBoard(int lineCount) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective("sidebar", "dummy");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        for (int i = 0; i < lineCount; i++) {
            Team team = scoreboard.registerNewTeam("line" + i);
            String entry = LINE_ENTRIES[i];
            team.addEntry(entry);
            objective.getScore(entry).setScore(lineCount - i);
        }

        return new BoardState(scoreboard, objective, lineCount);
    }

    private static void updateLine(Scoreboard scoreboard, String teamName, String text) {
        Team team = scoreboard.getTeam(teamName);
        if (team == null) {
            return;
        }

        String[] split = splitLine(text);
        if (!team.getPrefix().equals(split[0])) {
            team.setPrefix(split[0]);
        }
        if (!team.getSuffix().equals(split[1])) {
            team.setSuffix(split[1]);
        }
    }

    private static String renderLine(Player player, String raw) {
        String line = raw == null ? "" : raw;
        line = line.replace("{player}", PlaceholderManager.getDisplayName(player));
        line = line.replace("{online}", String.valueOf(Bukkit.getOnlinePlayers().size()));
        line = PlaceholderManager.replacePlaceholders(line, player);
        line = PlaceholderAPI.setPlaceholders(player, line);
        return colorize(line);
    }

    private static String[] splitLine(String text) {
        String line = text == null ? "" : text;
        String prefix = trimForScoreboard(line, 16);
        String remaining = line.substring(Math.min(prefix.length(), line.length()));
        String suffix = trimForScoreboard(ChatColor.getLastColors(prefix) + remaining, 16);
        return new String[]{prefix, suffix};
    }

    private static String trimForScoreboard(String text, int maxLength) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        if (text.length() <= maxLength) {
            return text;
        }

        String trimmed = text.substring(0, maxLength);
        if (trimmed.endsWith("§")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private static String[] loadAnimation(FileConfiguration config) {
        List<String> frames = config.getStringList("features.scoreboard.title-animation");
        if (frames == null || frames.isEmpty()) {
            return DEFAULT_ANIMATION;
        }

        List<String> colored = new ArrayList<>();
        for (String frame : frames) {
            if (frame != null && !frame.trim().isEmpty()) {
                colored.add(colorize(frame));
            }
        }
        return colored.isEmpty() ? DEFAULT_ANIMATION : colored.toArray(new String[0]);
    }

    private static List<String> loadLines(FileConfiguration config) {
        List<String> lines = config.getStringList("features.scoreboard.lines");
        if (lines == null || lines.isEmpty()) {
            return defaultLines(config);
        }

        List<String> sanitized = new ArrayList<>();
        for (String line : lines) {
            sanitized.add(line == null ? "" : line);
            if (sanitized.size() >= LINE_ENTRIES.length) {
                break;
            }
        }

        return sanitized.isEmpty() ? defaultLines(config) : sanitized;
    }

    private static List<String> defaultLines(FileConfiguration config) {
        List<String> defaults = new ArrayList<>();
        String lobbyLine = config == null ? "&fLobby: &a#1" : config.getString("features.scoreboard.lobby-line", "&fLobby: &a#1");
        String bottom = config == null ? "&apvphard.live" : config.getString("features.scoreboard.bottom", "&apvphard.live");
        defaults.add("");
        defaults.add("&fRank: &7%kaoscore_prefix%");
        defaults.add("");
        defaults.add("&fOnline: &a{online}");
        defaults.add(lobbyLine);
        defaults.add("");
        defaults.add(bottom);
        return defaults;
    }

    private static String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }

    private static final class BoardState {
        private final Scoreboard scoreboard;
        private final Objective objective;
        private final int lineCount;

        private BoardState(Scoreboard scoreboard, Objective objective, int lineCount) {
            this.scoreboard = scoreboard;
            this.objective = objective;
            this.lineCount = lineCount;
        }
    }
}
