package com.knowplugins.ysubz.kaoscore.bukkit.utils.menu;

import com.knowplugins.ysubz.kaoscore.bukkit.managers.Managers;
import com.knowplugins.ysubz.kaoscore.bukkit.managers.player.FriendManager;
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

    public static final String TITLE = "§8Amigos";
    public static final String REQUESTS_TITLE = "§8Pedidos de Amizade";
    private static final String LORE_ACTION_PREFIX = "§0ACTION:";
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
        inv.setItem(45, button(Material.PAPER, "§ePedidos §7(" + requests + ")", lore(
                "§7Veja/aceite seus pedidos.",
                "",
                "§eClique para abrir.",
                LORE_ACTION_PREFIX + "requests"
        )));

        inv.setItem(49, button(Material.NAME_TAG, "§aAdicionar Amigo", lore(
                "§7Envie um pedido para um jogador online.",
                "",
                "§eClique para digitar o nick.",
                LORE_ACTION_PREFIX + "add"
        )));

        inv.setItem(53, button(Material.BARRIER, "§cFechar", lore(
                LORE_ACTION_PREFIX + "close"
        )));

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

        inv.setItem(49, button(Material.ARROW, "§aVoltar", lore(LORE_ACTION_PREFIX + "back")));
        inv.setItem(53, button(Material.BARRIER, "§cFechar", lore(LORE_ACTION_PREFIX + "close")));

        player.openInventory(inv);
    }

    public static String parseAction(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta() || stack.getItemMeta() == null || stack.getItemMeta().getLore() == null) {
            return "";
        }
        for (String line : stack.getItemMeta().getLore()) {
            if (line != null && line.startsWith(LORE_ACTION_PREFIX)) {
                return line.substring(LORE_ACTION_PREFIX.length()).trim().toLowerCase();
            }
        }
        return "";
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
        String name = friendManager == null ? "Desconhecido" : friendManager.getFriendAccountName(friendId);
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
        String name = request.getRequesterName() == null ? "Desconhecido" : request.getRequesterName();
        skull.setDisplayName("§e" + name);

        // try set owner for skin (only works if name is valid)
        String owner = name;
        OfflinePlayer offline = Bukkit.getOfflinePlayer(name);
        if (offline != null && offline.getName() != null) {
            owner = offline.getName();
        }
        skull.setOwner(owner);

        List<String> lore = new ArrayList<>();
        lore.add("§aClique para aceitar");
        lore.add("§cClique direito para recusar");
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
