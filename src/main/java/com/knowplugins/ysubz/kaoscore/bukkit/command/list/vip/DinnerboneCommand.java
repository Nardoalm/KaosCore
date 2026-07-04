package com.knowplugins.ysubz.kaoscore.bukkit.command.list.vip;

import com.knowplugins.ysubz.kaoscore.bukkit.command.Commands;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import net.minecraft.server.v1_8_R3.IChatBaseComponent;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityDestroy;
import net.minecraft.server.v1_8_R3.PacketPlayOutNamedEntitySpawn;
import net.minecraft.server.v1_8_R3.PacketPlayOutPlayerInfo;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_8_R3.util.CraftChatMessage;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class DinnerboneCommand extends Commands {

    private static final String PERM_SELF = "kaoscore.member.dinnerbone";
    private static final String PERM_OTHERS = "kaoscore.staff.dinnerbone";

    private static final ConcurrentMap<UUID, String> ORIGINAL_NAMES = new ConcurrentHashMap<>();

    public DinnerboneCommand() {
        super("dinnerbone");
    }

    @Override
    public void perform(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return;
        }

        Player player = (Player) sender;

        if (args.length > 1) {
            player.sendMessage("§cCorrect usage: /dinnerbone or /dinnerbone <nick>");
            return;
        }

        if (args.length == 1) {
            if (!player.hasPermission(PERM_OTHERS)) {
                player.sendMessage("§cYou do not have permission to execute this command.");
                player.playSound(player.getLocation(), Sound.CLICK, 1.0f, 1.0f);
                return;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null || !target.isOnline()) {
                player.sendMessage("§cPlayer not found.");
                return;
            }

            boolean enabled = toggle(target);
            player.sendMessage("§a" + target.getName() + (enabled ? " is now upside down." : " is back to normal."));
            return;
        }

        if (!player.hasPermission(PERM_SELF)) {
            player.sendMessage("§cYou do not have permission to execute this command.");
            player.playSound(player.getLocation(), Sound.CLICK, 1.0f, 1.0f);
            return;
        }

        boolean enabled = toggle(player);
        player.sendMessage("§a" + (enabled ? "You are now upside down." : "You are back to normal."));
        player.playSound(player.getLocation(), Sound.NOTE_PLING, 1.0f, 1.0f);
    }

    private boolean toggle(Player player) {
        UUID id = player.getUniqueId();
        boolean isDinnerbone = ORIGINAL_NAMES.containsKey(id);

        if (isDinnerbone) {
            String originalName = ORIGINAL_NAMES.remove(id);
            changeProfileNameAndRefresh(player, originalName, originalName);
            return false;
        } else {
            String realName = player.getName();
            ORIGINAL_NAMES.put(id, realName);
            changeProfileNameAndRefresh(player, "Dinnerbone", realName);
            return true;
        }
    }

    private void changeProfileNameAndRefresh(Player player, String newProfileName, String realName) {
        try {
            EntityPlayer entityPlayer = ((CraftPlayer) player).getHandle();
            boolean turningDinnerbone = newProfileName.equals("Dinnerbone");

            String currentTabName = player.getPlayerListName();
            if (currentTabName == null) currentTabName = realName;

            PacketPlayOutPlayerInfo removeInfo = new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER, entityPlayer);
            PacketPlayOutEntityDestroy destroyEntity = new PacketPlayOutEntityDestroy(entityPlayer.getId());

            for (Player online : Bukkit.getOnlinePlayers()) {
                if (!online.equals(player)) {
                    EntityPlayer observer = ((CraftPlayer) online).getHandle();
                    observer.playerConnection.sendPacket(removeInfo);
                    observer.playerConnection.sendPacket(destroyEntity);
                }
            }

            GameProfile profile = entityPlayer.getProfile();
            Field nameField = GameProfile.class.getDeclaredField("name");
            nameField.setAccessible(true);
            nameField.set(profile, newProfileName);

            IChatBaseComponent originalList = entityPlayer.listName;
            entityPlayer.listName = CraftChatMessage.fromString(currentTabName)[0];

            PacketPlayOutPlayerInfo addInfo = new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, entityPlayer);
            PacketPlayOutNamedEntitySpawn spawnEntity = new PacketPlayOutNamedEntitySpawn(entityPlayer);

            for (Player online : Bukkit.getOnlinePlayers()) {
                if (!online.equals(player)) {
                    EntityPlayer observer = ((CraftPlayer) online).getHandle();
                    observer.playerConnection.sendPacket(addInfo);
                    observer.playerConnection.sendPacket(spawnEntity);
                }
            }

            if (turningDinnerbone) {
                entityPlayer.listName = CraftChatMessage.fromString("§f" + realName)[0];
            } else {
                entityPlayer.listName = CraftChatMessage.fromString(currentTabName)[0];
            }

            PacketPlayOutPlayerInfo personalTabPacket = new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.UPDATE_DISPLAY_NAME, entityPlayer);
            entityPlayer.playerConnection.sendPacket(personalTabPacket);

            entityPlayer.listName = originalList;

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}