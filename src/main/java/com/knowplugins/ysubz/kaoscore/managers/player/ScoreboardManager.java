package com.knowplugins.ysubz.kaoscore.managers.player;

import com.knowplugins.ysubz.kaoscore.KaosCore;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ScoreboardManager {

    private static final Map<UUID, Scoreboard> BOARDS = new HashMap<>();
    private static int index = 0;
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
    private static String[] animation = DEFAULT_ANIMATION;
    private static BukkitRunnable task;

    public static void setup() {
        KaosCore plugin = KaosCore.getInstance();
        if (plugin == null) {
            return;
        }

        if (!plugin.getConfig().getBoolean("features.scoreboard.enabled", true)) {
            return;
        }

        if (task != null) {
            task.cancel();
            task = null;
        }

        // Load animation frames from config (optional).
        java.util.List<String> frames = plugin.getConfig().getStringList("features.scoreboard.title-animation");
        if (frames != null && !frames.isEmpty()) {
            java.util.List<String> colored = new java.util.ArrayList<>();
            for (String frame : frames) {
                if (frame != null) {
                    colored.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', frame));
                }
            }
            if (!colored.isEmpty()) {
                animation = colored.toArray(new String[0]);
            } else {
                animation = DEFAULT_ANIMATION;
            }
        } else {
            animation = DEFAULT_ANIMATION;
        }

        int period = Math.max(2, plugin.getConfig().getInt("features.scoreboard.update-ticks", 4));

        task = new BukkitRunnable() {
            @Override
            public void run() {
                index++;
                if (index >= animation.length) index = 0;

                for (Player player : Bukkit.getOnlinePlayers()) {
                    update(player);
                }
            }
        };
        task.runTaskTimer(plugin, 0L, period);
    }

    public static void update(Player player) {
        Scoreboard sb = BOARDS.get(player.getUniqueId());
        Objective obj;

        if (sb == null || sb.getObjective("sidebar") == null || sb.getTeam("line0") == null) {
            sb = Bukkit.getScoreboardManager().getNewScoreboard();
            obj = sb.registerNewObjective("sidebar", "dummy");
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
            setupTeams(sb, obj);
            BOARDS.put(player.getUniqueId(), sb);
        } else {
            obj = sb.getObjective("sidebar");
        }

        if (player.getScoreboard() != sb) {
            player.setScoreboard(sb);
        }

        if (obj != null) {
            obj.setDisplayName(animation[index]);
            String rank = PlaceholderAPI.setPlaceholders(
                    player,
                    "%kaoscore_prefix%"
            );
            String onlinePlayers = "§a" + Bukkit.getOnlinePlayers().size();
            KaosCore plugin = KaosCore.getInstance();
            String bottom = plugin == null ? "§afreepmc.com.br" : org.bukkit.ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("features.scoreboard.bottom", "&afreepmc.com.br"));
            String lobbyLine = plugin == null ? "§fLobby: §a#1" : org.bukkit.ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("features.scoreboard.lobby-line", "&fLobby: &a#1"));
            updateLine(sb, "line5", "§fRank: §7", rank);
            updateLine(sb, "line3", "§fOnline: ", onlinePlayers);
            updateLine(sb, "line2", lobbyLine, "");
            updateLine(sb, "line0", bottom, "");
        }
    }

    private static void setupTeams(Scoreboard sb, Objective obj) {
        createTeamLine(sb, obj, "line6", "§1", 6);
        createTeamLine(sb, obj, "line5", "§2", 5); // Espaço
        createTeamLine(sb, obj, "line4", "§3", 4); // Rank
        createTeamLine(sb, obj, "line3", "§4", 3); // Espaço
        createTeamLine(sb, obj, "line2", "§5", 2); // Online
        createTeamLine(sb, obj, "line1", "§6", 1); // Espaço
        createTeamLine(sb, obj, "line0", "§0", 0); // Rodapé configurável
    }

    private static void createTeamLine(Scoreboard sb, Objective obj, String teamName, String entry, int score) {
        Team t = sb.registerNewTeam(teamName);
        t.addEntry(entry);
        obj.getScore(entry).setScore(score);
    }

    private static void updateLine(Scoreboard sb, String teamName, String prefix, String suffix) {
        Team t = sb.getTeam(teamName);
        if (t == null) return;

        if (prefix.length() > 16) prefix = prefix.substring(0, 16);
        if (suffix.length() > 16) suffix = suffix.substring(0, 16);

        if (!t.getPrefix().equals(prefix)) t.setPrefix(prefix);
        if (!t.getSuffix().equals(suffix)) t.setSuffix(suffix);
    }

    public static void remove(Player player) {
        BOARDS.remove(player.getUniqueId());
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }
}
