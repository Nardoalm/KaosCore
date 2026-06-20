package com.knowplugins.ysubz.kaoscore.utils.menu;

import com.knowplugins.ysubz.kaoscore.integration.practice.PracticeModeBlocker;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class MenuUtils {

    public static void openMenu(Player player) {
        if (player == null || PracticeModeBlocker.check(player, false)) {
            return;
        }

        Inventory menu = Bukkit.createInventory(null, 27, "§8Menu de Opções");

        ItemStack botao = new ItemStack(Material.COMPASS);
        ItemMeta meta = botao.getItemMeta();
        meta.setDisplayName("§aIr para o Spawn");
        botao.setItemMeta(meta);

        menu.setItem(13, botao);
        player.openInventory(menu);
    }
}
