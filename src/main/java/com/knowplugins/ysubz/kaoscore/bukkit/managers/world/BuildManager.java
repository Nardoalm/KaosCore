package com.knowplugins.ysubz.kaoscore.bukkit.managers.world;

import org.bukkit.entity.Player;
import java.util.ArrayList;
import java.util.List;

public class BuildManager {

    private static final List<String> BUILDERS = new ArrayList<>();

    public static boolean isBuilder(Player player) {
        return BUILDERS.contains(player.getName());
    }

    public static void addBuilder(Player player) {
        if (!BUILDERS.contains(player.getName())) {
            BUILDERS.add(player.getName());
        }
    }

    public static void removeBuilder(Player player) {
        BUILDERS.remove(player.getName());
    }
}