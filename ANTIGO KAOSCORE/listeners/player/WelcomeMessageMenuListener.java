package com.knowplugins.ysubz.kaoscore.listeners.player;

import com.knowplugins.ysubz.kaoscore.managers.Managers;
import com.knowplugins.ysubz.kaoscore.managers.player.WelcomeMessageManager;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class WelcomeMessageMenuListener implements Listener {

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
        if (title == null || !title.equals(WelcomeMessageManager.MENU_TITLE)) {
            return;
        }

        event.setCancelled(true);

        if (event.getCurrentItem() == null) {
            return;
        }

        WelcomeMessageManager manager = Managers.getWelcomeMessageManager();
        if (manager == null) {
            return;
        }

        String id = manager.parseTemplateId(event.getCurrentItem());
        if (id.isEmpty()) {
            return;
        }

        if (manager.select(player, id)) {
            WelcomeMessageManager.Template template = manager.getTemplate(id);
            String name = template == null ? id : template.getName();
            player.sendMessage("§a§lWELCOME ➔ §aMensagem selecionada: §f" + name);
            player.playSound(player.getLocation(), Sound.LEVEL_UP, 1.0f, 1.0f);
            player.closeInventory();
        } else {
            player.sendMessage("§c§lWELCOME ➔ §cVocê não pode selecionar essa mensagem.");
            player.playSound(player.getLocation(), Sound.CLICK, 1.0f, 1.0f);
        }
    }
}
