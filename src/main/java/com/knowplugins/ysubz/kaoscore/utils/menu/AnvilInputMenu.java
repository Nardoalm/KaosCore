package com.knowplugins.ysubz.kaoscore.utils.menu;

import com.knowplugins.ysubz.kaoscore.KaosCore;
import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.ChatMessage;
import net.minecraft.server.v1_8_R3.ContainerAnvil;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import net.minecraft.server.v1_8_R3.IChatBaseComponent;
import net.minecraft.server.v1_8_R3.PacketPlayOutOpenWindow;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AnvilInputMenu implements Listener {

    public interface Handler {
        void onSubmit(Player player, String text);

        default void onClose(Player player) {
        }
    }

    private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();

    private AnvilInputMenu() {
    }

    public static boolean isOpen(Player player) {
        if (player == null) {
            return false;
        }
        return SESSIONS.containsKey(player.getUniqueId());
    }

    public static Session getSession(Player player) {
        if (player == null) {
            return null;
        }
        return SESSIONS.get(player.getUniqueId());
    }

    public static void closeSession(Player player) {
        if (player == null) {
            return;
        }
        SESSIONS.remove(player.getUniqueId());
    }

    public static void open(Player player, String title, String itemName, Handler handler) {
        ItemStack item = new ItemStack(Material.NAME_TAG);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(itemName == null ? " " : itemName);
            item.setItemMeta(meta);
        }
        open(player, title, item, handler);
    }

    public static void open(Player player, String title, ItemStack leftItem, Handler handler) {
        if (player == null || !player.isOnline()) {
            return;
        }

        KaosCore plugin = KaosCore.getInstance();
        if (plugin == null) {
            return;
        }

        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, () -> open(player, title, leftItem, handler));
            return;
        }

        EntityPlayer ep = ((CraftPlayer) player).getHandle();
        int containerId = ep.nextContainerCounter();

        ContainerAnvil container = new ContainerAnvil(ep.inventory, ep.world, new BlockPosition(0, 0, 0), ep);
        container.checkReachable = false;

        IChatBaseComponent component = new ChatMessage(title == null ? "" : title);
        PacketPlayOutOpenWindow packet = buildOpenWindowPacket(containerId, "minecraft:anvil", component);
        ep.playerConnection.sendPacket(packet);

        ep.activeContainer = container;
        ep.activeContainer.windowId = containerId;
        ep.activeContainer.addSlotListener(ep);

        Inventory inv = container.getBukkitView().getTopInventory();
        if (leftItem != null) {
            inv.setItem(0, leftItem);
        }

        SESSIONS.put(player.getUniqueId(), new Session(containerId, handler));
        player.updateInventory();
    }

    private static PacketPlayOutOpenWindow buildOpenWindowPacket(int windowId, String type, IChatBaseComponent title) {
        try {
            Constructor<PacketPlayOutOpenWindow> c = PacketPlayOutOpenWindow.class.getConstructor(int.class, String.class, IChatBaseComponent.class, int.class);
            return c.newInstance(windowId, type, title, 0);
        } catch (Exception ignored) {
        }

        try {
            Constructor<PacketPlayOutOpenWindow> c = PacketPlayOutOpenWindow.class.getConstructor(int.class, String.class, IChatBaseComponent.class);
            return c.newInstance(windowId, type, title);
        } catch (Exception ignored) {
        }

        return new PacketPlayOutOpenWindow(windowId, type, title, 0);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getPlayer();
        Session session = SESSIONS.remove(player.getUniqueId());

        if (session != null) {
            Inventory topInventory = event.getInventory();
            if (topInventory != null) {
                topInventory.clear();
            }
            session.handler.onClose(player);
        }
    }

    public static final class Session {
        public final int windowId;
        public final Handler handler;

        private Session(int windowId, Handler handler) {
            this.windowId = windowId;
            this.handler = handler;
        }
    }
}