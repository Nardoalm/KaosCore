package com.knowplugins.ysubz.kaoscore.bukkit.listeners.player;

import com.knowplugins.ysubz.kaoscore.bukkit.cmd.commands.vip.SkinCommand;
import com.knowplugins.ysubz.kaoscore.bukkit.utils.menu.AnvilInputMenu;
import com.knowplugins.ysubz.kaoscore.bukkit.utils.menu.SkinMenu;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public final class SkinMenuListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        if (player.getOpenInventory() == null || player.getOpenInventory().getTopInventory() == null) {
            return;
        }

        String title = player.getOpenInventory().getTopInventory().getName();
        if (title == null || !title.equals(SkinMenu.TITLE)) {
            return;
        }

        event.setCancelled(true);

        if (event.getCurrentItem() == null) {
            return;
        }

        String action = SkinMenu.parseAction(event.getCurrentItem());
        if (action.isEmpty()) {
            return;
        }

        if (action.equals("reset")) {
            player.closeInventory();
            SkinCommand.resetSkin(player);
            return;
        }

        if (action.equals("choose")) {
            if (!player.hasPermission("kaoscore.member.vip")) {
                player.sendMessage("§c§lSKIN ➔ §cEssa opção é apenas para VIP.");
                player.playSound(player.getLocation(), Sound.CLICK, 1.0f, 1.0f);
                return;
            }

            player.closeInventory();

            AnvilInputMenu.open(player, "§8Nick da Skin", "Digite o nick...", (p, text) -> {
                if (text == null || text.trim().isEmpty()) {
                    p.sendMessage("§c§lSKIN ➔ §cNick inválido.");
                    p.playSound(p.getLocation(), Sound.CLICK, 1.0f, 1.0f);
                    return;
                }
                if (text.length() > 16) {
                    p.sendMessage("§c§lSKIN ➔ §cO nick não pode ter mais de 16 caracteres.");
                    p.playSound(p.getLocation(), Sound.CLICK, 1.0f, 1.0f);
                    return;
                }
                SkinCommand.setSkin(p, text);
            });
        }
    }
}

