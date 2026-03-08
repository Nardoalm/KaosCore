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
    private static final String[] ANIMATION = {
            "§f§lF§a§lUSION",
            "§a§lF§f§lU§a§lSION",
            "§a§lFU§f§lS§a§lION",
            "§a§lFUS§f§lI§a§lON",
            "§a§lFUSI§f§lO§a§lN",
            "§a§lFUSIO§f§lN",
            "§f§lFUSION",
            "§f§lFUSION",
            "§a§lFUSION",
            "§a§lFUSION",
            "§f§lFUSION",
            "§f§lFUSION"
    };

    public static void setup() {
        new BukkitRunnable() {
            @Override
            public void run() {
                index++;
                if (index >= ANIMATION.length) index = 0;

                for (Player player : Bukkit.getOnlinePlayers()) {
                    update(player);
                }
            }
        }.runTaskTimer(KaosCore.getInstance(), 0L, 4L);
    }

    public static void update(Player player) {
        Scoreboard sb = BOARDS.get(player.getUniqueId());
        Objective obj;

        if (sb == null) {
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
            obj.setDisplayName(ANIMATION[index]);
            String rank = PlaceholderAPI.setPlaceholders(
                    player,
                    "%luckperms_prefix%"
            );
            String onlinePlayers = "§a" + Bukkit.getOnlinePlayers().size();
            updateLine(sb, "line5", "§fRank: §7", rank);
            updateLine(sb, "line3", "§fOnline: ", onlinePlayers);
            updateLine(sb, "line2", "§fLobby: §a#1", "");
        }
    }

    private static void setupTeams(Scoreboard sb, Objective obj) {
        createTeamLine(sb, obj, "line6", "§1", 6);
        createTeamLine(sb, obj, "line5", "§2", 5); // Espaço
        createTeamLine(sb, obj, "line4", "§3", 4); // Rank
        createTeamLine(sb, obj, "line3", "§4", 3); // Espaço
        createTeamLine(sb, obj, "line2", "§5", 2); // Online
        createTeamLine(sb, obj, "line1", "§6", 1); // Espaço

        obj.getScore("§6kaosmc.com.br").setScore(0);
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