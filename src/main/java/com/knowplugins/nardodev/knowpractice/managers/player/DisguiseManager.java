package com.knowplugins.nardodev.knowpractice.managers.player;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class DisguiseManager {
    public static final List<Player> DISGUISED = new ArrayList<>();

    private final List<String> defaultNicks = Arrays.asList(
            "SrGusta", "ziSnaake", "yNandoXP_", "SrLogann", "iPowerPvP",
            "zFuzion_", "LuanPvP", "Darkness_z", "uSkyWars_", "MasterSkill"
    );

    private final List<String> allNicks = new ArrayList<>();


    public void addInDisguise(Player player) {
        if (!DISGUISED.contains(player)) {
            DISGUISED.add(player);
        }
    }

    public void removeDisguise(Player player) {
        DISGUISED.remove(player);
    }

    public static boolean isDisguised(Player player) {
        return DISGUISED.contains(player);
    }


    public void loadNicksFile(JavaPlugin plugin) {
        File file = new File(plugin.getDataFolder(), "nicks.yml");

        if (!file.exists()) {
            plugin.getDataFolder().mkdirs();
            try {
                file.createNewFile();

                FileConfiguration config = YamlConfiguration.loadConfiguration(file);

                List<String> extraNicks = Arrays.asList(
                        "Notch", "Jeb_", "FallenAngel", "ProKillerBR", "Ninja_"
                );

                config.set("nicks-extras", extraNicks);
                config.save(file);

            } catch (IOException exception) {
                exception.printStackTrace();
            }
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        List<String> fileNicks = config.getStringList("nicks-extras");

        this.allNicks.clear();

        this.allNicks.addAll(this.defaultNicks);
        if (fileNicks != null && !fileNicks.isEmpty()) {
            this.allNicks.addAll(fileNicks);
        }

        plugin.getLogger().info("Sistema de Disguise carregado: " + this.defaultNicks.size() + " nicks fixos e " + (fileNicks != null ? fileNicks.size() : 0) + " nicks do arquivo!");
    }

    public String getRandomNick() {
        if (this.allNicks.isEmpty()) {
            return "Steve";
        }
        Random random = new Random();
        return this.allNicks.get(random.nextInt(this.allNicks.size()));
    }
}