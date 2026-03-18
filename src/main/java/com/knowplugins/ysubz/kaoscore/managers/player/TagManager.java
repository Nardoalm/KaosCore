package com.knowplugins.ysubz.kaoscore.managers.player;

import com.knowplugins.ysubz.kaoscore.KaosCore;
import com.knowplugins.ysubz.kaoscore.managers.Managers;
import com.knowplugins.ysubz.kaoscore.managers.plugin.PlayerTagRepository;
import com.knowplugins.ysubz.kaoscore.utils.tag.Tag;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TagManager {

    private final KaosCore plugin;
    private final Map<String, Tag> tagsById;
    private final Map<String, Tag> tagsByName;
    private final Map<UUID, String> selectedTags;
    private FileConfiguration tagsConfig;
    private final PlayerTagRepository playerTagRepository;

    public TagManager(KaosCore plugin) {
        this.plugin = plugin;
        this.tagsById = new LinkedHashMap<>();
        this.tagsByName = new ConcurrentHashMap<>();
        this.selectedTags = new ConcurrentHashMap<>();
        this.playerTagRepository = new PlayerTagRepository(plugin, Managers.getDatabaseManager());
        loadTags();
        loadSelectedTags();
    }

    public final void loadTags() {
        if (plugin == null) {
            return;
        }

        File file = new File(plugin.getDataFolder(), "tags.yml");
        if (!file.exists()) {
            plugin.saveResource("tags.yml", false);
        }

        this.tagsConfig = YamlConfiguration.loadConfiguration(file);
        this.tagsById.clear();
        this.tagsByName.clear();

        ConfigurationSection section = tagsConfig.getConfigurationSection("tags");
        if (section == null) {
            section = tagsConfig.getRoot();
        }
        if (section == null) {
            return;
        }

        for (String key : section.getKeys(false)) {
            if (key == null) {
                continue;
            }

            ConfigurationSection tagSection = section.getConfigurationSection(key);
            if (tagSection == null) {
                continue;
            }

            String id = normalize(key);
            if (id.isEmpty()) {
                continue;
            }

            String name = value(tagSection.getString("name"), key);
            String prefix = value(tagSection.getString("prefix"), "");
            String color = value(tagSection.getString("color"), "");
            String suffix = value(tagSection.getString("suffix"), "");
            String permission = value(tagSection.getString("permission"), "");
            int position = tagSection.getInt("position", 9999);

            Tag tag = new Tag(id, name, prefix, color, suffix, permission, position);
            tagsById.put(id, tag);
            tagsByName.put(normalize(name), tag);
            tagsByName.put(normalize(stripColors(name)), tag);
        }
    }

    private void loadSelectedTags() {
        if (plugin == null || Managers.getDatabaseManager() == null || !Managers.getDatabaseManager().isConnected()) {
            return;
        }

        selectedTags.clear();
        Map<UUID, String> loaded = playerTagRepository.loadAll();
        for (Map.Entry<UUID, String> entry : loaded.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                selectedTags.put(entry.getKey(), normalize(entry.getValue()));
            }
        }
    }

    public Tag getTag(Player player) {
        if (player == null) {
            return null;
        }

        String selectedId = selectedTags.get(player.getUniqueId());
        Tag selectedTag = resolveTag(selectedId);
        if (selectedTag != null && canUse(player, selectedTag)) {
            return selectedTag;
        }

        List<Tag> available = getAvailableTags(player);
        if (available.isEmpty()) {
            return null;
        }

        Tag first = available.get(0);
        if (first != null) {
            selectedTags.put(player.getUniqueId(), first.getId());
            saveSelectedTag(player.getUniqueId(), first.getId());
        }
        return first;
    }

    public boolean setTag(Player player, String tagNameOrId) {
        if (player == null || tagNameOrId == null) {
            return false;
        }

        Tag tag = resolveTag(tagNameOrId);
        if (tag == null || !canUse(player, tag)) {
            return false;
        }

        selectedTags.put(player.getUniqueId(), tag.getId());
        saveSelectedTag(player.getUniqueId(), tag.getId());
        refreshTag(player);
        return true;
    }

    public boolean setTag(UUID uuid, String tagNameOrId) {
        if (uuid == null || tagNameOrId == null) {
            return false;
        }

        Tag tag = resolveTag(tagNameOrId);
        if (tag == null) {
            return false;
        }

        selectedTags.put(uuid, tag.getId());
        saveSelectedTag(uuid, tag.getId());

        Player online = Bukkit.getPlayer(uuid);
        if (online != null && online.isOnline()) {
            refreshTag(online);
        }
        return true;
    }

    public boolean clearTag(UUID uuid) {
        if (uuid == null) {
            return false;
        }

        selectedTags.remove(uuid);
        if (playerTagRepository != null) {
            playerTagRepository.deleteAsync(uuid);
        }

        Player online = Bukkit.getPlayer(uuid);
        if (online != null && online.isOnline()) {
            refreshTag(online);
        }
        return true;
    }

    public Tag getTagByNameOrId(String tagNameOrId) {
        if (tagNameOrId == null) {
            return null;
        }
        return resolveTag(tagNameOrId);
    }

    public List<Tag> getAvailableTags(Player player) {
        if (player == null) {
            return Collections.emptyList();
        }

        List<Tag> list = new ArrayList<>();
        for (Tag tag : tagsById.values()) {
            if (tag != null && canUse(player, tag)) {
                list.add(tag);
            }
        }
        list.sort(Comparator.comparingInt(Tag::getPosition).thenComparing(Tag::getName, String.CASE_INSENSITIVE_ORDER));
        return list;
    }

    public boolean canUse(Player player, Tag tag) {
        if (player == null || tag == null) {
            return false;
        }

        String permission = tag.getPermission();
        if (permission == null || permission.trim().isEmpty()) {
            return true;
        }

        return player.hasPermission(permission);
    }

    private Tag resolveTag(String tagNameOrId) {
        if (tagNameOrId == null) {
            return null;
        }

        String normalized = normalize(tagNameOrId);
        if (normalized.isEmpty()) {
            return null;
        }

        Tag byId = tagsById.get(normalized);
        if (byId != null) {
            return byId;
        }

        return tagsByName.get(normalized);
    }

    public void refreshTag(Player player) {
        refreshTag(player, new String[0]);
    }

    public void refreshTag(Player player, String... staleEntries) {
        if (player == null || !player.isOnline() || plugin == null || plugin.getTagTeamManager() == null) {
            return;
        }

        if (staleEntries == null || staleEntries.length == 0) {
            plugin.getTagTeamManager().refreshPlayer(player);
            return;
        }

        plugin.getTagTeamManager().refreshPlayer(player, staleEntries);
    }

    public void refreshViewer(Player player) {
        if (player == null || !player.isOnline() || plugin == null || plugin.getTagTeamManager() == null) {
            return;
        }

        plugin.getTagTeamManager().refreshViewer(player);
    }

    public void refreshAll() {
        if (plugin == null || plugin.getTagTeamManager() == null) {
            return;
        }

        plugin.getTagTeamManager().refreshAll();
    }

    public void removeTagEntries(Player player, String... staleEntries) {
        if (player == null || plugin == null || plugin.getTagTeamManager() == null) {
            return;
        }

        if (staleEntries == null || staleEntries.length == 0) {
            plugin.getTagTeamManager().removePlayer(player);
            return;
        }

        plugin.getTagTeamManager().removePlayer(player, staleEntries);
    }

    private void saveSelectedTag(UUID playerId, String tagId) {
        if (playerId == null || tagId == null) {
            return;
        }
        if (playerTagRepository != null) {
            playerTagRepository.saveAsync(playerId, normalize(tagId));
        }
    }

    private String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.trim().toLowerCase(Locale.ROOT);
    }

    private String stripColors(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("(?i)&[0-9A-FK-OR]", "").replaceAll("(?i)§[0-9A-FK-OR]", "");
    }

    private String value(String text, String fallback) {
        if (text == null) {
            return fallback == null ? "" : fallback;
        }
        return text;
    }
}
