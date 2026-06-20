package com.knowplugins.ysubz.kaoscore.managers.player;

import com.knowplugins.ysubz.kaoscore.KaosCore;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class DisguiseManager {
    private static final Set<UUID> DISGUISED = new HashSet<>();
    private static final Map<UUID, String> DISGUISE_NICKS = new HashMap<>();
    private static final Map<UUID, String> ORIGINAL_NAMES = new HashMap<>();

    private final List<String> defaultNicks = Arrays.asList(
            "SrGusta", "ziSnaake", "yNandoXP_", "SrLogann", "iPowerPvP",
            "zFuzion_", "LuanPvP", "Darkness_z", "uSkyWars_", "MasterSkill"
    );

    private final List<String> allNicks = new ArrayList<>();


    public void recordOriginalName(Player player) {
        if (player == null) return;
        ORIGINAL_NAMES.putIfAbsent(player.getUniqueId(), player.getName());
    }

    public void setOriginalName(Player player, String name) {
        if (player == null || name == null) return;
        ORIGINAL_NAMES.put(player.getUniqueId(), name);
    }

    public String popOriginalName(Player player) {
        if (player == null) return null;
        return ORIGINAL_NAMES.remove(player.getUniqueId());
    }

    public String getOriginalName(Player player) {
        if (player == null) return null;
        return ORIGINAL_NAMES.get(player.getUniqueId());
    }

    public String getOriginalName(UUID uuid) {
        if (uuid == null) return null;
        return ORIGINAL_NAMES.get(uuid);
    }

    public void addInDisguise(Player player) {
        if (player == null) return;
        DISGUISED.add(player.getUniqueId());
    }

    public void updateTabName(Player player) {
        if (player == null) return;

        if (isDisguised(player)) {
            String disguiseNick = getDisguiseNick(player);
            if (disguiseNick != null && !disguiseNick.isEmpty()) {
                player.setPlayerListName(disguiseNick);
            }
        } else {
            player.setPlayerListName(null);
        }
    }

    public void addInDisguise(Player player, String nick) {
        if (player == null) return;
        DISGUISED.add(player.getUniqueId());
        if (nick != null) {
            DISGUISE_NICKS.put(player.getUniqueId(), nick);
        }

        player.setPlayerListName(nick);

        if (Bukkit.getPluginManager().getPlugin("KaosCore") != null) {
            KaosCore core = KaosCore.getInstance();
            if (core.getTagTeamManager() != null) {
                for (Player online : Bukkit.getOnlinePlayers()) {
                    core.getTagTeamManager().refreshPlayer(online);
                }
            }
        }
    }

    public void addInDisguise(UUID uuid) {
        if (uuid == null) return;
        DISGUISED.add(uuid);
    }

    public void addInDisguise(UUID uuid, String nick) {
        if (uuid == null) return;
        DISGUISED.add(uuid);
        if (nick != null) {
            DISGUISE_NICKS.put(uuid, nick);
        }
    }

    public void removeDisguise(Player player) {
        if (player == null) return;

        String originalName = ORIGINAL_NAMES.get(player.getUniqueId());
        DISGUISED.remove(player.getUniqueId());
        DISGUISE_NICKS.remove(player.getUniqueId());
        ORIGINAL_NAMES.remove(player.getUniqueId());

        if (originalName != null && !originalName.isEmpty()) {
            player.setPlayerListName(originalName);
        } else {
            player.setPlayerListName(null);
        }

        if (Bukkit.getPluginManager().getPlugin("KaosCore") != null) {
            KaosCore core = KaosCore.getInstance();
            if (core.getTagTeamManager() != null) {
                for (Player online : Bukkit.getOnlinePlayers()) {
                    core.getTagTeamManager().refreshPlayer(online);
                }
            }
        }
    }

    public static boolean isDisguised(Player player) {
        if (player == null) return false;
        return DISGUISED.contains(player.getUniqueId());
    }

    public static boolean isNickInUse(String nick) {
        if (nick == null) return false;
        for (String s : DISGUISE_NICKS.values()) {
            if (nick.equalsIgnoreCase(s)) return true;
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (nick.equalsIgnoreCase(p.getName())) return true;
            if (p.getDisplayName() != null && nick.equalsIgnoreCase(p.getDisplayName())) return true;
        }
        return false;
    }


    public void loadNicksFile(JavaPlugin plugin) {
        File file = new File(plugin.getDataFolder(), "nicks.yml");

        if (!file.exists()) {
            plugin.getDataFolder().mkdirs();
            try {
                file.createNewFile();

                FileConfiguration config = YamlConfiguration.loadConfiguration(file);

                List<String> extraNicks = Arrays.asList(
                        "Vexiity", "Zoltrus", "KyroPvP", "ImNotHack", "SlowMotion",
                        "Darkness_PT", "LethalGhost", "VortexNuker", "Swiftly", "FrostByte",
                        "iZack_", "MysterioBR", "NightWalk", "ExoticPanda", "VelocityHits",
                        "SilentSteps", "HyperCombo", "ArcticFox_", "LunarPvP", "ShadowBound",
                        "GrizzlyBear", "IronWill", "SkyFaller", "Noxious", "ZenithPlayer", "pescador67"
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
            return "Disfarcado";
        }
        Random random = new Random();
        for (int i = 0; i < 10; i++) {
            String candidate = this.allNicks.get(random.nextInt(this.allNicks.size()));
            if (!isNickInUse(candidate)) return candidate;
        }
        return this.allNicks.get(random.nextInt(this.allNicks.size()));
    }

    public static UUID getUuidByNick(String nick) {
        if (nick == null) return null;
        for (java.util.Map.Entry<UUID, String> e : DISGUISE_NICKS.entrySet()) {
            if (e.getValue() != null && e.getValue().equalsIgnoreCase(nick)) return e.getKey();
        }
        return null;
    }

    public static Player getPlayerByNick(String nick) {
        if (nick == null) return null;

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getName().equalsIgnoreCase(nick)) return p;
            if (p.getDisplayName() != null && p.getDisplayName().equalsIgnoreCase(nick)) return p;
        }
        UUID u = getUuidByNick(nick);
        if (u != null) return org.bukkit.Bukkit.getPlayer(u);
        return null;
    }

    public static Player getOnlinePlayerByName(String name) {
        return getPlayerByNick(name);
    }

    public String getDisguiseNick(Player player) {
        if (player == null) return null;
        return DISGUISE_NICKS.get(player.getUniqueId());
    }

    public String getDisguiseNick(UUID uuid) {
        if (uuid == null) return null;
        return DISGUISE_NICKS.get(uuid);
    }
}
