package com.knowplugins.ysubz.kaoscore.bukkit.listeners.entity;

import com.knowplugins.ysubz.kaoscore.bukkit.KaosCore;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class MenuClickListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getName() != null && event.getInventory().getName().equals("§8Menu de Opções")) {

            event.setCancelled(true);

            if (event.getCurrentItem() == null || !event.getCurrentItem().hasItemMeta()) {
                return;
            }

            Player player = (Player) event.getWhoClicked();
            String itemName = event.getCurrentItem().getItemMeta().getDisplayName();

            if (itemName != null && itemName.equals("§aIr para o Spawn")) {

                player.closeInventory();

                if (KaosCore.getInstance().getConfig().contains("spawn.location")) {
                    try {
                        String raw = KaosCore.getInstance().getConfig().getString("spawn.location");
                        String[] parts = raw.split(";");

                        if (parts.length >= 6) {
                            World world = Bukkit.getWorld(parts[0]);
                            if (world != null) {
                                double x = Double.parseDouble(parts[1]);
                                double y = Double.parseDouble(parts[2]);
                                double z = Double.parseDouble(parts[3]);
                                float yaw = Float.parseFloat(parts[4]);
                                float pitch = Float.parseFloat(parts[5]);

                                Location loc = new Location(world, x, y, z, yaw, pitch);
                                player.teleport(loc);
                                player.sendMessage("§a§lMENU ➔ §aVocê foi teleportado para o spawn!");
                                player.playSound(player.getLocation(), Sound.NOTE_PLING, 1.0f, 1.0f);
                            }
                        }
                    } catch (Exception e) {
                        player.sendMessage("§c§lMENU ➔ §cOcorreu um erro ao teleportar.");
                    }
                } else {
                    player.sendMessage("§c§lMENU ➔ §cO spawn ainda não foi definido no servidor.");
                }
            }

            // Se quiser adicionar mais botões no futuro, é só colocar outro "else if" aqui:
            // else if (itemName.equals("§eOutro Botão")) { ... }
        }
    }
}