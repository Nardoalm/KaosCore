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
            "brunao77", "vxRj", "arthur_slk", "pedrinbr", "matheusA",
            "caio_021", "rafa_xp", "nando_", "duduz1", "luc4s",
            "igorzin__", "renanz", "guilherme_", "andrex_", "yurizin__",
            "samuca77", "lkszin__", "cauazin__", "pedrao_", "luan77",
            "skyzx", "void_77", "n1ghtfall", "storm_x", "darkrj",
            "lunar7", "blvck_", "ice_021", "firezz", "frostzin__",
            "px7", "tz_", "qxr", "hz_77", "rxp",
            "zx21", "krlx", "dxx_", "zzrj", "nxzin__",
            "brayan_", "vitorrj", "edu_021", "arthur7k", "felps_",
            "danielx", "marcos77", "henrique_", "gabx_", "thiagox",
            "devvx", "codexr", "jav4_", "scriptx", "backend_",
            "rootzin__", "stack_", "nullzin__", "commit_", "push_",
            "xxvx", "zrx_", "k9_", "vx__", "rxx_",
            "tzin__", "mz_", "hzx_", "xrz_", "qz_",
            "guh021", "arthur_08", "brunin7", "vx2010", "kaua_77",
            "yuri021", "pedro_08", "joaov_", "caiobr", "rafazin__",
            "slkzin__", "mlk77", "trx_", "drx_", "kratosbr",
            "zeus_77", "aresx_", "odinbr", "thorzin__", "loki_021",
            "favelado_", "cria77", "tropa021", "mandela_", "fluxo77",
            "rolezin__", "quebrada_", "visao77", "resenha_", "bolado77",
            "pimenta_", "farinha77", "cafezinho_", "arroz_021", "feijao77",
            "paozinho__", "suco77", "refri_", "doce021", "biscoito77"
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
        player.setPlayerListName(null);

        if (Bukkit.getPluginManager().getPlugin("KaosCore") != null) {
            KaosCore core = KaosCore.getInstance();
            if (core.getTagManager() != null) {
                core.getTagManager().refreshTag(player);
            }
        }
    }

    public void addInDisguise(Player player, String nick) {
        if (player == null) return;
        DISGUISED.add(player.getUniqueId());
        if (nick != null) {
            DISGUISE_NICKS.put(player.getUniqueId(), nick);
        }

        player.setPlayerListName(null);

        if (Bukkit.getPluginManager().getPlugin("KaosCore") != null) {
            KaosCore core = KaosCore.getInstance();
            if (core.getTagTeamManager() != null) {
                for (Player online : Bukkit.getOnlinePlayers()) {
                    core.getTagTeamManager().refreshPlayer(online);
                }
            }
            if (core.getTagManager() != null) {
                core.getTagManager().refreshTag(player);
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

        DISGUISED.remove(player.getUniqueId());
        DISGUISE_NICKS.remove(player.getUniqueId());
        ORIGINAL_NAMES.remove(player.getUniqueId());

        player.setPlayerListName(null);

        if (Bukkit.getPluginManager().getPlugin("KaosCore") != null) {
            KaosCore core = KaosCore.getInstance();
            if (core.getTagTeamManager() != null) {
                for (Player online : Bukkit.getOnlinePlayers()) {
                    core.getTagTeamManager().refreshPlayer(online);
                }
            }
            if (core.getTagManager() != null) {
                core.getTagManager().refreshTag(player);
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