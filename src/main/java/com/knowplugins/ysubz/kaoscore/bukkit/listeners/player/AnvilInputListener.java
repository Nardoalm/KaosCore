package com.knowplugins.ysubz.kaoscore.bukkit.listeners.player;

import com.knowplugins.ysubz.kaoscore.bukkit.util.menu.AnvilInputMenu;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import org.bukkit.ChatColor;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class AnvilInputListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        AnvilInputMenu.Session session = AnvilInputMenu.getSession(player);
        if (session == null) {
            return;
        }

        if (event.getView() == null || event.getView().getTopInventory() == null || event.getView().getTopInventory().getType() != InventoryType.ANVIL) {
            return;
        }

        EntityPlayer ep = ((CraftPlayer) player).getHandle();
        if (ep == null || ep.activeContainer == null || ep.activeContainer.windowId != session.windowId) {
            return;
        }

        event.setCancelled(true);

        // Output slot do anvil
        if (event.getRawSlot() != 2) {
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null) {
            return;
        }

        String text = "";
        try {
            ItemMeta meta = clicked.getItemMeta();
            if (meta != null && meta.getDisplayName() != null) {
                text = meta.getDisplayName();
            }
        } catch (Exception ignored) {
        }

        text = ChatColor.stripColor(text);
        if (text == null) {
            text = "";
        }
        text = text.trim();

        AnvilInputMenu.closeSession(player);
        player.closeInventory();

        try {
            if (session.handler != null) {
                session.handler.onSubmit(player, text);
            }
        } catch (Exception ignored) {
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getPlayer();
        AnvilInputMenu.Session session = AnvilInputMenu.getSession(player);
        if (session == null) {
            return;
        }

        // Se fechou qualquer inventario com sessao aberta, encerra.
        AnvilInputMenu.closeSession(player);
        try {
            if (session.handler != null) {
                session.handler.onClose(player);
            }
        } catch (Exception ignored) {
        }
    }
}

