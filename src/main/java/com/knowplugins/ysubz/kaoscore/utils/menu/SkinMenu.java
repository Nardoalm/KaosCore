package com.knowplugins.ysubz.kaoscore.utils.menu;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class SkinMenu {

    public static final String TITLE = "§8Skins";
    private static final String LORE_ACTION_PREFIX = "§0ACTION:";

    private SkinMenu() {
    }

    public static void open(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 27, TITLE);

        inv.setItem(11, item(Material.NAME_TAG, "§aChoose Skin", lore(
                "§7Choose a skin by username.",
                "§7VIP Only.",
                "",
                "§eClick to choose."
        )));

        inv.setItem(15, item(Material.BARRIER, "§cReset Skin", lore(
                "§7Revert to your original skin.",
                "",
                "§eClick to reset."
        )));

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

    private static ItemStack item(Material material, String name, List<String> lore) {
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