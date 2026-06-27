package com.knowplugins.ysubz.kaoscore.bukkit.disguise;

import com.knowplugins.ysubz.kaoscore.bukkit.KaosCore;
import com.lunarclient.apollo.Apollo;
import com.lunarclient.apollo.module.nametag.*;
import com.lunarclient.apollo.recipients.Recipients;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
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
            "iZack_x", "iClaudio1256_", "VaterSeinSohn", "G2U", "RoadMore", "69Drunk", "ZeusYT_",
            "MaskedBetrayel", "Sincriterio", "SUW0", "rilsy", "MIBECON", "wreeperbeastmode", "luvniss",
            "Dqnd", "licorne_soiyeuse", "blokovac", "3rst", "_Kaori_Chan_", "AfterChange", "DogeBigEyes_",
            "due1", "iGen", "ManoEdson", "qSamra", "ImNotCrying_", "scuzii", "Aama", "69Expanded",
            "pookiechan", "bunyeet", "BerkeBetterPlays", "T6Q", "Norddur", "_ShiMoRin_", "Tsuk",
            "zTwoRod", "7rq", "Rick_Grimez", "HindSightQ", "Himmelsschimmer", "lovily", "Di3play",
            "iExcelsior", "Laiinox", "sender852", "dripgf", "verschlaft", "BasicHenry_", "ShiroDeCitron",
            "Dark_Senna", "Kay05", "cowtails", "__olek2k__", "overdohse", "Puxqh", "Vanify",
            "Colton229", "LA_Capone", "caughtfeelings", "Po4ka_", "Spanka", "MHF_Mineskin", "iFqncyou",
            "lovlx", "umrumie", "iiStxr", "lizukox", "YourCherry_", "Blirry", "yungdrib", "Lireaax",
            "dcvie", "Fanaa_", "Lounce", "The_Vizard", "LegacySFX", "Ylovoir", "Dolarr", "ReallyEasy",
            "Exttasy", "swashi", "gejj", "mvtsy", "xNullx", "oviate", "Volds", "Epode", "Neegah",
            "shiddenly", "Fruitopiaa", "malsiee", "_A4G", "abusings", "maxae", "Jackyzlex", "BigTayTay",
            "BooVs_sss", "NoHaxLegit", "qwrpy", "AhmedQ", "Waggen", "toxci", "dumb_child", "Ancil",
            "1000001", "2807", "EatThatP445", "oLa2930", "TildiGaming", "Kyandorux", "qKenji",
            "ThanksMojang", "Keiffes", "stariey", "Zyxess", "Lefty006", "kitten2004", "MiniDiggerTest",
            "Unapprovinq", "Badger", "Xaoa", "DaDLine", "imbitter", "Parveon", "AAAWojack", "Piska",
            "0hated", "iYagHan_", "notgb", "Exsaynor", "High_up", "KrissaN04", "Hacks", "Jefz",
            "revvlon", "mightness", "oWuv", "Therealbertje", "IDareYouDaddy", "BeamerGirl", "K1nqz",
            "Mavyoo", "RegeditG0D", "oFLODJES", "Trapzzi", "MrM4ng0", "Itz_Er", "StickyVibes",
            "beefqke", "Naquatic", "Bqrderos", "Sunn1x", "cupofcomfy", "Fluxin", "LighTiti",
            "Txrtled", "Px_Diamocles", "owoshiro", "LavRox_", "Dehlu", "Kamyxo", "bellscafe",
            "zillla", "DocLP", "RCE", "Bmbi", "Madilaine", "RumpelstyleOG", "Colombians", "fukboys",
            "Topoi", "12b", "SpopPvP", "Termirom", "iSxlated", "uwunuzzleme", "iChosenRoy",
            "qq6q", "_gxt", "DiscordApp", "ImDxlqs_", "felixiie", "Shotz08", "AhmedV", "IzDragon",
            "uhMarco", "iCustomi", "Breed444", "DOF", "Atser", "sfxnd", "zGhozttt", "herobrine1234_",
            "Lecuuu", "FullPrice", "Malabu", "Juggie", "lemrad", "MOBOMBER", "LinkGamer27",
            "beksmoel", "JJs300", "_Daxe_", "ChillWooper", "AyyThomas", "xFatTurtle", "matic1909",
            "iRailed", "lcco", "_k0ka", "mye", "_JustSimple_", "itally", "Addictq", "wMoonLight_",
            "Argonn_", "Happyman7o7", "jojitrash", "Resident", "YAKO_l", "myro", "obsion",
            "Shiftz", "dxvr", "Wispexe", "Clicks", "LanceCS", "daydreamingg", "CherrySodaYummy",
            "GuVi0609", "Kenzi", "Disorder", "xrazeer", "hshai", "maiid", "kvtsu", "CreationMC",
            "Kobraso", "Goldapfel", "Folkef1", "grassyterror10", "shel_x", "BangerLEBBYY", "OhSad",
            "kotka23", "zLucaz", "BoogieHoodie_", "Shainty", "subsallaround", "Relief_II", "tethlus",
            "SummerShqrk", "xJustRevengeS", "bottle_neck", "gradical", "HarryPhoenix", "drears",
            "redsi", "Linahh", "Madji0o0", "djoxonN", "Legionqr", "WaldePvP", "NothingFound",
            "Clouudy_", "GoBurn", "baebih", "dbxs", "Flete", "Kvwx", "Woze", "Zyro", "Vyke",
            "Qyze", "Nyxo", "Ryve", "Tyzo", "Xyke", "Lyvo", "Wyze", "Kyro", "Zyke", "Qyro",
            "Vyzo", "Myke", "Fyro", "Pyze", "Ryko", "Tyve", "Avex", "Kryx", "Zorv", "Vynx",
            "Qevo", "Ryxo", "Trez", "Nyvz", "Kyzx", "Wexo", "Zenv", "Qrix", "Vyqx", "Xero",
            "Kivo", "Jyro", "Lqvx", "Nexo", "Yvex", "Zyvx", "Kwze", "Twyx", "Qwex", "Pryx",
            "Fyvx", "Cyzo", "Vexo", "Qyvx", "Rexo", "Nyro", "Kovy", "Zevy", "Qiro", "Vyro",
            "Kyvo", "Rivo", "Tyro", "Luvo", "Nivo", "Zivo", "Axor", "Kyzen", "Vyzen", "Qyzen",
            "Ryzen", "Tyzen", "Fyzen", "Lyzen", "Nyzen", "Wyzen", "Qvex", "Kvex", "Rvex",
            "Tvex", "Lvex", "Pvex", "Mvex", "Nvex", "Wvex", "Zvex"
    );

    private final List<String> allNicks = new ArrayList<>();

    public DisguiseManager() {
        Bukkit.getMessenger().registerOutgoingPluginChannel(JavaPlugin.getPlugin(KaosCore.class), "CB|Binary");
    }

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
        KaosCore core = KaosCore.getInstance();
        if (core != null && core.getTagManager() != null) {
            core.getTagManager().refreshTag(player);
        }
        refreshGlobalNametags(player, false);
    }

    private String getFormattedName(Player player) {
        String baseName = DISGUISE_NICKS.getOrDefault(player.getUniqueId(), player.getName());
        KaosCore core = KaosCore.getInstance();
        if (core == null || core.getTagManager() == null) return baseName;

        com.knowplugins.ysubz.kaoscore.bukkit.utils.tag.Tag tag = core.getTagManager().getTag(player);
        if (tag == null) return baseName;

        String prefix = tag.getPrefix() == null ? "" : tag.getPrefix();
        String suffix = tag.getSuffix() == null ? "" : tag.getSuffix();

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            try {
                prefix = PlaceholderAPI.setPlaceholders(player, prefix);
                suffix = PlaceholderAPI.setPlaceholders(player, suffix);
            } catch (Exception ignored) {
            }
        }

        prefix = prefix.replace(" %kaoscore_clan_tag%", "").replace("%kaoscore_clan_tag%", "");
        suffix = suffix.replace(" %kaoscore_clan_tag%", "").replace("%kaoscore_clan_tag%", "");

        prefix = org.bukkit.ChatColor.translateAlternateColorCodes('&', prefix);
        suffix = org.bukkit.ChatColor.translateAlternateColorCodes('&', suffix);

        String tailColor = "";
        for (int i = prefix.length() - 2; i >= 0; i--) {
            if (prefix.charAt(i) == org.bukkit.ChatColor.COLOR_CHAR) {
                char code = Character.toLowerCase(prefix.charAt(i + 1));
                if ((code >= '0' && code <= '9') || (code >= 'a' && code <= 'f') || code == 'r') {
                    tailColor = "" + org.bukkit.ChatColor.COLOR_CHAR + code;
                    break;
                }
            }
        }
        return prefix + tailColor + baseName + suffix;
    }

    public void refreshGlobalNametags(Player target, boolean reset) {
        if (target == null) return;

        NametagModule nametagModule = Apollo.getModuleManager().getModule(NametagModule.class);
        if (nametagModule != null) {
            if (reset || !DISGUISED.contains(target.getUniqueId())) {
                // Se for reset ou o jogador não estiver disfarçado, limpa a tag forçada.
                // O Lunar vai voltar a exibir a tag normal gerada pelo Scoreboard do Bukkit
                nametagModule.resetNametag(Recipients.ofEveryone(), target.getUniqueId());
            } else {
                // Caso contrário, injetamos o nome formatado forçado usando a API moderna.
                String formattedName = getFormattedName(target);
                nametagModule.overrideNametag(Recipients.ofEveryone(), target.getUniqueId(), Nametag.builder()
                        .lines(Collections.singletonList(LegacyComponentSerializer.legacySection().deserialize(formattedName)))
                        .build()
                );
            }
        }
    }

    private byte[] buildCBUpdateNametagsPacket(Map<UUID, List<String>> playersMap) {
        try {
            ByteBuf buffer = Unpooled.buffer();

            int packetId = 6;
            while ((packetId & 0xFFFFFF80) != 0x0) {
                buffer.writeByte((packetId & 0x7F) | 0x80);
                packetId >>>= 7;
            }
            buffer.writeByte(packetId);

            int mapSize = (playersMap == null) ? -1 : playersMap.size();
            int tempSize = mapSize;
            if (tempSize == -1) {
                buffer.writeByte(0xFF);
            } else {
                while ((tempSize & 0xFFFFFF80) != 0x0) {
                    buffer.writeByte((tempSize & 0x7F) | 0x80);
                    tempSize >>>= 7;
                }
                buffer.writeByte(tempSize);
            }

            if (playersMap != null) {
                for (Map.Entry<UUID, List<String>> entry : playersMap.entrySet()) {
                    UUID uuid = entry.getKey();
                    List<String> tags = entry.getValue();

                    buffer.writeLong(uuid.getMostSignificantBits());
                    buffer.writeLong(uuid.getLeastSignificantBits());

                    int tagsSize = tags.size();
                    int tempTagsSize = tagsSize;
                    while ((tempTagsSize & 0xFFFFFF80) != 0x0) {
                        buffer.writeByte((tempTagsSize & 0x7F) | 0x80);
                        tempTagsSize >>>= 7;
                    }
                    buffer.writeByte(tempTagsSize);

                    for (String tag : tags) {
                        byte[] stringBytes = tag.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                        int stringLen = stringBytes.length;
                        while ((stringLen & 0xFFFFFF80) != 0x0) {
                            buffer.writeByte((stringLen & 0x7F) | 0x80);
                            stringLen >>>= 7;
                        }
                        buffer.writeByte(stringLen);
                        buffer.writeBytes(stringBytes);
                    }
                }
            }

            byte[] packetData = new byte[buffer.readableBytes()];
            buffer.readBytes(packetData);
            return packetData;
        } catch (Exception e) {
            return null;
        }
    }

    public void addInDisguise(Player player, String nick) {
        if (player == null) return;
        DISGUISED.add(player.getUniqueId());
        if (nick != null) {
            DISGUISE_NICKS.put(player.getUniqueId(), nick);
        }

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
        refreshGlobalNametags(player, false);
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

        refreshGlobalNametags(player, true);

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
                        "MysterioBR", "NightWalk", "ExoticPanda", "VelocityHits",
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