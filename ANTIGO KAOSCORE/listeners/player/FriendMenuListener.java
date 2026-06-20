package com.knowplugins.ysubz.kaoscore.listeners.player;

import com.knowplugins.ysubz.kaoscore.friend.FriendRequest;
import com.knowplugins.ysubz.kaoscore.managers.Managers;
import com.knowplugins.ysubz.kaoscore.managers.player.FriendManager;
import com.knowplugins.ysubz.kaoscore.managers.player.DisguiseManager;
import com.knowplugins.ysubz.kaoscore.utils.menu.AnvilInputMenu;
import com.knowplugins.ysubz.kaoscore.utils.menu.FriendMenu;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.UUID;

public final class FriendMenuListener implements Listener {

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
        if (title == null) {
            return;
        }

        if (!title.equals(FriendMenu.TITLE) && !title.equals(FriendMenu.REQUESTS_TITLE)) {
            return;
        }

        event.setCancelled(true);

        if (event.getCurrentItem() == null) {
            return;
        }

        FriendManager friendManager = Managers.getFriendManager();
        if (friendManager == null) {
            player.sendMessage("§cSistema de amizade indisponível.");
            return;
        }

        String action = FriendMenu.parseAction(event.getCurrentItem());
        if (!action.isEmpty()) {
            switch (action) {
                case "close":
                    player.closeInventory();
                    return;
                case "requests":
                    FriendMenu.openRequests(player);
                    return;
                case "back":
                    FriendMenu.open(player);
                    return;
                case "add":
                    player.closeInventory();
                    AnvilInputMenu.open(player, "§8Adicionar Amigo", "Digite o nick...", (p, text) -> {
                        if (text == null || text.trim().isEmpty()) {
                            p.sendMessage("§cNick inválido.");
                            p.playSound(p.getLocation(), Sound.VILLAGER_NO, 1.0f, 1.0f);
                            return;
                        }

                        Player target = Bukkit.getPlayerExact(text);
                        if (target == null) {
                            target = DisguiseManager.getOnlinePlayerByName(text);
                        }

                        if (target == null || !target.isOnline()) {
                            p.sendMessage("§cJogador não encontrado (precisa estar online).");
                            p.playSound(p.getLocation(), Sound.VILLAGER_NO, 1.0f, 1.0f);
                            return;
                        }

                        FriendManager.RequestResult result = friendManager.createRequest(p, target);
                        switch (result) {
                            case SENT: {
                                p.sendMessage("§aPedido de amizade enviado para §f" + target.getDisplayName() + "§a.");
                                FriendRequest req = friendManager.getRequest(target.getUniqueId(), p.getUniqueId());
                                if (req != null) {
                                    friendManager.sendRequestNotification(target, req);
                                }
                                p.playSound(p.getLocation(), Sound.LEVEL_UP, 1.0f, 1.0f);
                                return;
                            }
                            case AUTO_ACCEPTED:
                                p.sendMessage("§aPedido mútuo detectado. Vocês agora são amigos!");
                                target.sendMessage("§aPedido mútuo detectado. Vocês agora são amigos!");
                                p.playSound(p.getLocation(), Sound.LEVEL_UP, 1.0f, 1.0f);
                                return;
                            case ALREADY_FRIENDS:
                                p.sendMessage("§cVocês já são amigos.");
                                break;
                            case ALREADY_REQUESTED:
                                p.sendMessage("§cVocê já enviou um pedido para este jogador.");
                                break;
                            case LIMIT_REACHED:
                                p.sendMessage("§cUma das contas atingiu o limite de amizades.");
                                break;
                            case SELF:
                                p.sendMessage("§cVocê não pode enviar amizade para si mesmo.");
                                break;
                            default:
                                p.sendMessage("§cNão foi possível enviar o pedido de amizade.");
                                break;
                        }
                        p.playSound(p.getLocation(), Sound.VILLAGER_NO, 1.0f, 1.0f);
                    });
                    return;
                default:
                    return;
            }
        }

        // Requests menu: accept/deny
        if (title.equals(FriendMenu.REQUESTS_TITLE)) {
            UUID requesterId = FriendMenu.parseUuid(event.getCurrentItem());
            if (requesterId == null) {
                return;
            }

            if (event.isRightClick()) {
                friendManager.denyRequest(player.getUniqueId(), requesterId);
                player.sendMessage("§cPedido de amizade recusado.");
                player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1.0f, 1.0f);
                FriendMenu.openRequests(player);
                return;
            }

            boolean ok = friendManager.acceptRequest(player.getUniqueId(), requesterId);
            if (!ok) {
                player.sendMessage("§cNão foi possível aceitar o pedido de amizade.");
                player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1.0f, 1.0f);
                FriendMenu.openRequests(player);
                return;
            }

            player.sendMessage("§aPedido de amizade aceito!");
            player.playSound(player.getLocation(), Sound.LEVEL_UP, 1.0f, 1.0f);

            Player requester = Bukkit.getPlayer(requesterId);
            if (requester != null && requester.isOnline()) {
                requester.sendMessage("§a" + player.getDisplayName() + " aceitou seu pedido de amizade!");
            }

            FriendMenu.openRequests(player);
        }
    }
}

