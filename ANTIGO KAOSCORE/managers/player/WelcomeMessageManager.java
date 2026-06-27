package com.knowplugins.ysubz.kaoscore.bukkit.managers.player;

import com.knowplugins.ysubz.kaoscore.KaosCore;
import com.knowplugins.ysubz.kaoscore.bukkit.managers.Managers;
import com.knowplugins.ysubz.kaoscore.bukkit.managers.plugin.PlayerWelcomeMessageRepository;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class WelcomeMessageManager {

    public static final String MENU_TITLE = "§8Mensagens de Entrada";
    private static final String LORE_ID_PREFIX = "§0ID:";

    private final KaosCore plugin;
    private final File messagesFile;

    private final Map<String, Template> templates;
    private final ConcurrentMap<UUID, String> selections;
    private final PlayerWelcomeMessageRepository repository;

    public WelcomeMessageManager(KaosCore plugin) {
        this.plugin = plugin;
        this.messagesFile = plugin == null ? null : new File(plugin.getDataFolder(), "welcome.yml");
        this.templates = new LinkedHashMap<>();
        this.selections = new ConcurrentHashMap<>();
        this.repository = new PlayerWelcomeMessageRepository(plugin, Managers.getDatabaseManager());
        reload();
    }

    public void reload() {
        templates.clear();
        selections.clear();

        if (plugin == null) {
            return;
        }

        if (messagesFile != null && !messagesFile.exists()) {
            try {
                plugin.saveResource("welcome.yml", false);
            } catch (Exception ignored) {
            }
        }

        loadTemplates();
        loadSelectionsFromDb();
        migrateLegacySelections();
    }

    public Template getTemplate(String id) {
        if (id == null) {
            return null;
        }
        return templates.get(normalize(id));
    }

    public String resolveSelectedId(Player player) {
        if (player == null) {
            return "default";
        }

        String selected = selections.get(player.getUniqueId());
        if (selected != null) {
            Template template = getTemplate(selected);
            if (template != null && template.canUse(player)) {
                return template.getId();
            }
        }

        Template def = getTemplate("default");
        if (def != null && def.canUse(player)) {
            return def.getId();
        }

        for (Template template : templates.values()) {
            if (template != null && template.canUse(player)) {
                return template.getId();
            }
        }

        return "default";
    }

    public Template resolveSelected(Player player) {
        return getTemplate(resolveSelectedId(player));
    }

    public boolean select(Player player, String id) {
        if (player == null) {
            return false;
        }

        Template template = getTemplate(id);
        if (template == null || !template.canUse(player)) {
            return false;
        }

        selections.put(player.getUniqueId(), template.getId());
        if (repository != null) {
            repository.saveAsync(player.getUniqueId(), template.getId());
        }
        return true;
    }

    public void openMenu(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 54, MENU_TITLE);
        String selectedId = resolveSelectedId(player);

        int slot = 0;
        for (Template template : templates.values()) {
            if (template == null || !template.canUse(player)) {
                continue;
            }

            if (slot >= inv.getSize()) {
                break;
            }

            ItemStack item = new ItemStack(Material.PAPER);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("§e" + safe(template.getName()));

            String playerTag = PlaceholderAPI.setPlaceholders(player, "%kaoscore_tag_prefix%");
            String playerName = playerTag + player.getName();

            List<String> lore = new ArrayList<>();
            String preview = safe(template.getText()).replace("{player}", playerName);
            lore.add("§7Preview:");
            lore.add("§6" + shorten(ChatColor.stripColor(preview), 34));
            lore.add("");
            if (template.getId().equalsIgnoreCase(selectedId)) {
                lore.add("§aSelecionada");
                meta.addEnchant(Enchantment.DURABILITY, 1, true);
            } else {
                lore.add("§eClique para selecionar");
            }

            meta.setLore(lore);
            item.setItemMeta(meta);

            inv.setItem(slot++, item);
        }

        player.openInventory(inv);
    }

    public String parseTemplateId(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) {
            return "";
        }

        ItemMeta meta = stack.getItemMeta();
        if (meta == null || meta.getLore() == null) {
            return "";
        }

        for (String line : meta.getLore()) {
            if (line != null && line.startsWith(LORE_ID_PREFIX)) {
                return normalize(line.substring(LORE_ID_PREFIX.length()));
            }
        }

        return "";
    }

    private void loadTemplates() {
        if (messagesFile == null) {
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(messagesFile);
        ConfigurationSection root = config.getConfigurationSection("messages");
        if (root == null) {
            return;
        }

        for (String categoryKey : root.getKeys(false)) {
            ConfigurationSection category = root.getConfigurationSection(categoryKey);
            if (category == null) {
                continue;
            }

            for (String key : category.getKeys(false)) {
                ConfigurationSection section = category.getConfigurationSection(key);
                if (section == null) {
                    continue;
                }

                String id = normalize(key);
                String name = section.getString("name", key);
                String text = section.getString("text", "{player} entrou no lobby!");
                String permission = section.getString("permission", "");

                templates.put(id, new Template(id, name, color(text), permission));
            }
        }
    }

    private void loadSelectionsFromDb() {
        if (plugin == null || repository == null || Managers.getDatabaseManager() == null || !Managers.getDatabaseManager().isConnected()) {
            return;
        }

        Map<UUID, String> loaded = repository.loadAll();
        for (Map.Entry<UUID, String> entry : loaded.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                selections.put(entry.getKey(), normalize(entry.getValue()));
            }
        }
    }

    private void migrateLegacySelections() {
        if (plugin == null || repository == null || Managers.getDatabaseManager() == null || !Managers.getDatabaseManager().isConnected()) {
            return;
        }

        File legacyFile = new File(plugin.getDataFolder(), "welcomeSelected.yml");
        if (!legacyFile.exists()) {
            return;
        }

        try {
            FileConfiguration legacy = YamlConfiguration.loadConfiguration(legacyFile);
            ConfigurationSection section = legacy.getConfigurationSection("players");
            if (section != null) {
                for (String key : section.getKeys(false)) {
                    try {
                        UUID uuid = UUID.fromString(key);
                        String id = normalize(section.getString(key, ""));
                        if (id.isEmpty()) {
                            continue;
                        }
                        selections.putIfAbsent(uuid, id);
                        repository.saveAsync(uuid, id);
                    } catch (Exception ignored) {
                    }
                }
            }

            // Avoid re-migrating on every startup.
            File migrated = new File(plugin.getDataFolder(), "welcomeSelected.yml.migrated");
            //noinspection ResultOfMethodCallIgnored
            legacyFile.renameTo(migrated);
        } catch (Exception ignored) {
        }
    }

    private String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.trim().toLowerCase(Locale.ROOT);
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }

    private String safe(String text) {
        return text == null ? "" : text;
    }

    private String shorten(String text, int maxLen) {
        String value = text == null ? "" : text;
        if (value.length() <= maxLen) {
            return value;
        }
        return value.substring(0, Math.max(0, maxLen - 3)) + "...";
    }

    public static final class Template {
        private final String id;
        private final String name;
        private final String text;
        private final String permission;

        private Template(String id, String name, String text, String permission) {
            this.id = id == null ? "" : id;
            this.name = name == null ? "" : name;
            this.text = text == null ? "" : text;
            this.permission = permission == null ? "" : permission;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getText() {
            return text;
        }

        public String getPermission() {
            return permission;
        }

        public boolean canUse(Player player) {
            if (player == null) {
                return false;
            }
            return permission.trim().isEmpty() || player.hasPermission(permission);
        }
    }
}
