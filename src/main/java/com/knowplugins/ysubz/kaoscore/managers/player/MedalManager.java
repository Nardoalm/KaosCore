package com.knowplugins.ysubz.kaoscore.managers.player;

import com.knowplugins.ysubz.kaoscore.KaosCore;
import com.knowplugins.ysubz.kaoscore.managers.Managers;
import com.knowplugins.ysubz.kaoscore.managers.plugin.repository.PlayerMedalRepository;
import com.knowplugins.ysubz.kaoscore.utils.medal.Medal;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MedalManager {

    private final KaosCore plugin;
    private final Map<String, Medal> medalsById;
    private final Map<String, Medal> medalsByName;
    private final Map<UUID, String> selectedMedals;
    private final PlayerMedalRepository playerMedalRepository;
    private FileConfiguration medalsConfig;

    public MedalManager(KaosCore plugin) {
        this.plugin = plugin;
        this.medalsById = new LinkedHashMap<>();
        this.medalsByName = new ConcurrentHashMap<>();
        this.selectedMedals = new ConcurrentHashMap<>();
        this.playerMedalRepository = new PlayerMedalRepository(plugin, Managers.getDatabaseManager());
        loadMedals();
        loadSelectedMedals();
    }

    public final void loadMedals() {
        if (plugin == null) {
            return;
        }

        File file = new File(plugin.getDataFolder(), "medals.yml");
        if (!file.exists()) {
            plugin.saveResource("medals.yml", false);
        }

        this.medalsConfig = YamlConfiguration.loadConfiguration(file);
        this.medalsById.clear();
        this.medalsByName.clear();

        ConfigurationSection section = medalsConfig.getConfigurationSection("medals");
        if (section == null) {
            section = medalsConfig.getRoot();
        }
        if (section == null) {
            return;
        }

        for (String key : section.getKeys(false)) {
            if (key == null) {
                continue;
            }

            ConfigurationSection medalSection = section.getConfigurationSection(key);
            if (medalSection == null) {
                continue;
            }

            String id = normalize(key);
            if (id.isEmpty()) {
                continue;
            }

            String name = value(medalSection.getString("name"), key);
            String permission = value(medalSection.getString("permission"), "");

            List<String> chatFrames = translatedFrames(medalSection, "chat-frames", medalSection.getString("chat"));
            List<String> nametagFrames = translatedFrames(medalSection, "nametag-frames", medalSection.getString("nametag"));

            if (chatFrames.isEmpty() && !nametagFrames.isEmpty()) {
                chatFrames = new ArrayList<>(nametagFrames);
            }
            if (nametagFrames.isEmpty() && !chatFrames.isEmpty()) {
                nametagFrames = new ArrayList<>(chatFrames);
            }

            Medal medal = new Medal(id, name, permission, chatFrames, nametagFrames);
            medalsById.put(id, medal);
            medalsByName.put(normalize(name), medal);
        }
    }

    private void loadSelectedMedals() {
        if (plugin == null || Managers.getDatabaseManager() == null || !Managers.getDatabaseManager().isConnected()) {
            return;
        }

        selectedMedals.clear();
        Map<UUID, String> loaded = playerMedalRepository.loadAll();
        for (Map.Entry<UUID, String> entry : loaded.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                selectedMedals.put(entry.getKey(), normalize(entry.getValue()));
            }
        }
    }

    public Medal getMedal(Player player) {
        if (player == null) {
            return null;
        }

        String selectedId = selectedMedals.get(player.getUniqueId());
        Medal selected = resolveMedal(selectedId);
        if (selected != null && canUse(player, selected)) {
            return selected;
        }

        if (selectedId != null) {
            selectedMedals.remove(player.getUniqueId());
            deleteSelectedMedal(player.getUniqueId());
        }

        return null;
    }

    public boolean setMedal(Player player, String medalNameOrId) {
        if (player == null || medalNameOrId == null) {
            return false;
        }

        Medal medal = resolveMedal(medalNameOrId);
        if (medal == null || !canUse(player, medal)) {
            return false;
        }

        selectedMedals.put(player.getUniqueId(), medal.getId());
        saveSelectedMedal(player.getUniqueId(), medal.getId());
        refreshPlayer(player);
        return true;
    }

    public void clearMedal(Player player) {
        if (player == null) {
            return;
        }

        selectedMedals.remove(player.getUniqueId());
        deleteSelectedMedal(player.getUniqueId());
        refreshPlayer(player);
    }

    public Medal getMedalByNameOrId(String medalNameOrId) {
        return resolveMedal(medalNameOrId);
    }

    public List<Medal> getAvailableMedals(Player player) {
        if (player == null) {
            return Collections.emptyList();
        }

        List<Medal> list = new ArrayList<>();
        for (Medal medal : medalsById.values()) {
            if (medal != null && canUse(player, medal)) {
                list.add(medal);
            }
        }
        list.sort(Comparator.comparing(Medal::getName, String.CASE_INSENSITIVE_ORDER));
        return list;
    }

    public boolean canUse(Player player, Medal medal) {
        if (player == null || medal == null) {
            return false;
        }

        String permission = medal.getPermission();
        return permission == null || permission.trim().isEmpty() || player.hasPermission(permission);
    }

    public String getChatDisplay(Player player) {
        return getStaticChatDisplay(player);
    }

    public String getNametagDisplay(Player player) {
        return getStaticNametagDisplay(player);
    }

    public String getStaticChatDisplay(Player player) {
        Medal medal = getMedal(player);
        return medal == null ? "" : medal.getStaticChat();
    }

    public String getStaticNametagDisplay(Player player) {
        Medal medal = getMedal(player);
        return medal == null ? "" : medal.getStaticNametag();
    }

    private Medal resolveMedal(String medalNameOrId) {
        if (medalNameOrId == null) {
            return null;
        }

        String normalized = normalize(medalNameOrId);
        if (normalized.isEmpty()) {
            return null;
        }

        Medal byId = medalsById.get(normalized);
        if (byId != null) {
            return byId;
        }

        return medalsByName.get(normalized);
    }

    private List<String> translatedFrames(ConfigurationSection section, String listPath, String singlePathValue) {
        List<String> values = section.getStringList(listPath);
        List<String> frames = new ArrayList<>();

        if (values != null) {
            for (String value : values) {
                if (value != null && !value.trim().isEmpty()) {
                    frames.add(translate(value));
                }
            }
        }

        if (frames.isEmpty() && singlePathValue != null && !singlePathValue.trim().isEmpty()) {
            frames.add(translate(singlePathValue));
        }

        return frames;
    }

    private void saveSelectedMedal(UUID playerId, String medalId) {
        if (playerId == null || medalId == null) {
            return;
        }
        playerMedalRepository.saveAsync(playerId, normalize(medalId));
    }

    private void deleteSelectedMedal(UUID playerId) {
        if (playerId == null) {
            return;
        }
        playerMedalRepository.deleteAsync(playerId);
    }

    private void refreshPlayer(Player player) {
        if (player == null || plugin == null || plugin.getTagManager() == null) {
            return;
        }
        plugin.getTagManager().refreshTag(player);
    }

    private String translate(String text) {
        if (text == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    private String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.trim().toLowerCase(Locale.ROOT);
    }

    private String value(String text, String fallback) {
        if (text == null) {
            return fallback == null ? "" : fallback;
        }
        return text;
    }
}
