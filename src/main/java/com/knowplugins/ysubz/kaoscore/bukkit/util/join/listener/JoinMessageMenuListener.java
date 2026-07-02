package com.knowplugins.ysubz.kaoscore.bukkit.util.join.listener;

import com.knowplugins.ysubz.kaoscore.bukkit.manager.MainManager;
import com.knowplugins.ysubz.kaoscore.bukkit.manager.player.JoinMessageManager;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class JoinMessageMenuListener implements Listener {

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
        if (title == null || !title.equals(JoinMessageManager.MENU_TITLE)) {
            return;
        }

        event.setCancelled(true);

        if (event.getCurrentItem() == null) {
            return;
        }

        JoinMessageManager manager = MainManager.getJoinMessageManager();
        if (manager == null) {
            return;
        }

        String id = manager.parseTemplateId(event.getCurrentItem());
        if (id.isEmpty()) {
            return;
        }

        if (manager.select(player, id)) {
            JoinMessageManager.Template template = manager.getTemplate(id);
            String name = template == null ? id : template.getName();
            player.sendMessage("§a§lWELCOME ➔ §aMensagem selecionada: §f" + name);
            player.playSound(player.getLocation(), Sound.NOTE_PLING, 1.0f, 1.0f);
            player.closeInventory();
        } else {
            player.sendMessage("§c§lWELCOME ➔ §cVocê não pode selecionar essa mensagem.");
            player.playSound(player.getLocation(), Sound.CLICK, 1.0f, 1.0f);
        }
    }
}
