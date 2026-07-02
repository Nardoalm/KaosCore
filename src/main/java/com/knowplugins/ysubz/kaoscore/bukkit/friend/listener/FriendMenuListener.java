package com.knowplugins.ysubz.kaoscore.bukkit.friend.listener;

import com.knowplugins.ysubz.kaoscore.bukkit.friend.FriendRequest;
import com.knowplugins.ysubz.kaoscore.bukkit.manager.MainManager;
import com.knowplugins.ysubz.kaoscore.bukkit.friend.manager.FriendManager;
import com.knowplugins.ysubz.kaoscore.bukkit.disguise.DisguiseManager;
import com.knowplugins.ysubz.kaoscore.bukkit.util.menu.AnvilInputMenu;
import com.knowplugins.ysubz.kaoscore.bukkit.util.menu.FriendMenu;
import org.bukkit.Bukkit;
import org.bukkit.Material;
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

        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) {
            return;
        }

        FriendManager friendManager = MainManager.getFriendManager();
        if (friendManager == null) {
            player.sendMessage("§cFriend system is currently unavailable.");
            return;
        }

        int slot = event.getRawSlot();
        Material type = event.getCurrentItem().getType();

        if (title.equals(FriendMenu.TITLE)) {
            if (slot == 45 && type == Material.PAPER) {
                FriendMenu.openRequests(player);
                return;
            }

            if (slot == 49 && type == Material.NAME_TAG) {
                player.closeInventory();
                AnvilInputMenu.open(player, "§8Add Friend", "Type username...", (p, text) -> {
                    if (text == null || text.trim().isEmpty()) {
                        p.sendMessage("§cInvalid username.");
                        p.playSound(p.getLocation(), Sound.CLICK, 1.0f, 1.0f);
                        return;
                    }

                    Player target = Bukkit.getPlayerExact(text);
                    if (target == null) {
                        target = DisguiseManager.getOnlinePlayerByName(text);
                    }

                    if (target == null || !target.isOnline()) {
                        p.sendMessage("§cPlayer not found (must be online).");
                        p.playSound(p.getLocation(), Sound.CLICK, 1.0f, 1.0f);
                        return;
                    }

                    FriendManager.RequestResult result = friendManager.createRequest(p, target);
                    switch (result) {
                        case SENT: {
                            p.sendMessage("§aFriend request sent to §f" + target.getDisplayName() + "§a.");
                            FriendRequest req = friendManager.getRequest(target.getUniqueId(), p.getUniqueId());
                            if (req != null) {
                                friendManager.sendRequestNotification(target, req);
                            }
                            p.playSound(p.getLocation(), Sound.NOTE_PLING, 1.0f, 1.0f);
                            return;
                        }
                        case AUTO_ACCEPTED:
                            p.sendMessage("§aMutual request detected. You are now friends!");
                            target.sendMessage("§aMutual request detected. You are now friends!");
                            p.playSound(p.getLocation(), Sound.NOTE_PLING, 1.0f, 1.0f);
                            return;
                        case ALREADY_FRIENDS:
                            p.sendMessage("§cYou are already friends.");
                            break;
                        case ALREADY_REQUESTED:
                            p.sendMessage("§cYou have already sent a request to this player.");
                            break;
                        case LIMIT_REACHED:
                            p.sendMessage("§cOne of the accounts has reached the friend limit.");
                            break;
                        case SELF:
                            p.sendMessage("§cYou cannot add yourself.");
                            break;
                        default:
                            p.sendMessage("§cCould not send friend request.");
                            break;
                    }
                    p.playSound(p.getLocation(), Sound.CLICK, 1.0f, 1.0f);
                });
                return;
            }

            if (slot == 53 && type == Material.BARRIER) {
                player.closeInventory();
                return;
            }
        }

        if (title.equals(FriendMenu.REQUESTS_TITLE)) {
            if (slot == 49 && type == Material.ARROW) {
                FriendMenu.open(player);
                return;
            }

            if (slot == 53 && type == Material.BARRIER) {
                player.closeInventory();
                return;
            }

            UUID requesterId = FriendMenu.parseUuid(event.getCurrentItem());
            if (requesterId == null) {
                return;
            }

            if (event.isRightClick()) {
                friendManager.denyRequest(player.getUniqueId(), requesterId);
                player.sendMessage("§cFriend request denied.");
                player.playSound(player.getLocation(), Sound.CLICK, 1.0f, 1.0f);
                FriendMenu.openRequests(player);
                return;
            }

            boolean ok = friendManager.acceptRequest(player.getUniqueId(), requesterId);
            if (!ok) {
                player.sendMessage("§cCould not accept friend request.");
                player.playSound(player.getLocation(), Sound.CLICK, 1.0f, 1.0f);
                FriendMenu.openRequests(player);
                return;
            }

            player.sendMessage("§aFriend request accepted!");
            player.playSound(player.getLocation(), Sound.NOTE_PLING, 1.0f, 1.0f);

            Player requester = Bukkit.getPlayer(requesterId);
            if (requester != null && requester.isOnline()) {
                requester.sendMessage("§a" + player.getDisplayName() + " accepted your friend request!");
            }

            FriendMenu.openRequests(player);
        }
    }
}