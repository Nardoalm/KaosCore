package com.knowplugins.ysubz.kaoscore.bukkit.util.menu;

import com.knowplugins.ysubz.kaoscore.bukkit.managers.Managers;
import com.knowplugins.ysubz.kaoscore.bukkit.friend.manager.FriendManager;
import com.knowplugins.ysubz.kaoscore.bukkit.friend.FriendRequest;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class FriendMenu {

    public static final String TITLE = "§8Friends";
    public static final String REQUESTS_TITLE = "§8Friend Requests";
    private static final String LORE_UUID_PREFIX = "§0UUID:";

    private FriendMenu() {
    }

    public static void open(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }

        FriendManager friendManager = Managers.getFriendManager();
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);

        if (friendManager != null) {
            List<UUID> friends = new ArrayList<>(friendManager.getFriends(player.getUniqueId()));
            int slot = 0;
            for (UUID friendId : friends) {
                if (slot >= 45) {
                    break;
                }
                inv.setItem(slot++, friendHead(player.getUniqueId(), friendManager, friendId));
            }
        }

        int requests = friendManager == null ? 0 : friendManager.getRequests(player.getUniqueId()).size();
        inv.setItem(45, button(Material.PAPER, "§eRequests §7(" + requests + ")", lore(
                "§7View/accept your requests.",
                "",
                "§eClick to open."
        )));

        inv.setItem(49, button(Material.NAME_TAG, "§aAdd Friend", lore(
                "§7Send a request to an online player.",
                "",
                "§eClick to type username."
        )));

        inv.setItem(53, button(Material.BARRIER, "§cClose", lore()));

        player.openInventory(inv);
    }

    public static void openRequests(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }

        FriendManager friendManager = Managers.getFriendManager();
        Inventory inv = Bukkit.createInventory(null, 54, REQUESTS_TITLE);

        if (friendManager != null) {
            List<FriendRequest> requests = new ArrayList<>(friendManager.getRequests(player.getUniqueId()));
            int slot = 0;
            for (FriendRequest request : requests) {
                if (request == null || request.getRequesterId() == null) {
                    continue;
                }
                if (slot >= 45) {
                    break;
                }
                inv.setItem(slot++, requestHead(request));
            }
        }

        inv.setItem(49, button(Material.ARROW, "§aBack", lore()));
        inv.setItem(53, button(Material.BARRIER, "§cClose", lore()));

        player.openInventory(inv);
    }

    public static UUID parseUuid(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta() || stack.getItemMeta() == null || stack.getItemMeta().getLore() == null) {
            return null;
        }
        for (String line : stack.getItemMeta().getLore()) {
            if (line != null && line.startsWith(LORE_UUID_PREFIX)) {
                try {
                    return UUID.fromString(line.substring(LORE_UUID_PREFIX.length()).trim());
                } catch (Exception ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    private static ItemStack friendHead(UUID viewerId, FriendManager friendManager, UUID friendId) {
        ItemStack head = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        ItemMeta meta = head.getItemMeta();
        if (!(meta instanceof SkullMeta)) {
            return head;
        }

        SkullMeta skull = (SkullMeta) meta;
        String name = friendManager == null ? "Unknown" : friendManager.getFriendAccountName(friendId);
        skull.setDisplayName("§f" + name);
        skull.setOwner(name);

        List<String> lore = new ArrayList<>();
        boolean online = friendManager != null && viewerId != null && friendId != null && friendManager.isFriendOnlineFor(viewerId, friendId);
        lore.add(online ? "§aOnline" : "§cOffline");
        lore.add(LORE_UUID_PREFIX + (friendId == null ? "" : friendId.toString()));
        skull.setLore(lore);
        head.setItemMeta(skull);
        return head;
    }

    private static ItemStack requestHead(FriendRequest request) {
        ItemStack head = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        ItemMeta meta = head.getItemMeta();
        if (!(meta instanceof SkullMeta)) {
            return head;
        }

        SkullMeta skull = (SkullMeta) meta;
        String name = request.getRequesterName() == null ? "Unknown" : request.getRequesterName();
        skull.setDisplayName("§e" + name);

        String owner = name;
        OfflinePlayer offline = Bukkit.getOfflinePlayer(name);
        if (offline != null && offline.getName() != null) {
            owner = offline.getName();
        }
        skull.setOwner(owner);

        List<String> lore = new ArrayList<>();
        lore.add("§aLeft-click to accept");
        lore.add("§cRight-click to deny");
        lore.add(LORE_UUID_PREFIX + request.getRequesterId().toString());
        skull.setLore(lore);
        head.setItemMeta(skull);
        return head;
    }

    private static ItemStack button(Material material, String name, List<String> lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private static List<String> lore(String... lines) {
        List<String> list = new ArrayList<>();
        if (lines != null) {
            for (String line : lines) {
                list.add(line == null ? "" : line);
            }
        }
        return list;
    }
}