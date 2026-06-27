package com.knowplugins.ysubz.kaoscore.bukkit.cmd.commands.vip;

import com.knowplugins.ysubz.kaoscore.bukkit.cmd.Commands;
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

    // Guarda o nome original
    private static final ConcurrentMap<UUID, String> ORIGINAL_NAMES = new ConcurrentHashMap<>();

    public DinnerboneCommand() {
        super("dinnerbone");
    }

    @Override
    public void perform(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cApenas jogadores podem usar este comando!");
            return;
        }

        Player player = (Player) sender;

        if (args.length > 1) {
            player.sendMessage("§c§lDINNERBONE ➔ §cUso correto: /dinnerbone ou /dinnerbone <nick>");
            return;
        }

        if (args.length == 1) {
            if (!player.hasPermission(PERM_OTHERS)) {
                player.sendMessage("§c§lDINNERBONE ➔ §cVocê não possui permissão para executar este comando.");
                player.playSound(player.getLocation(), Sound.CLICK, 1.0f, 1.0f);
                return;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null || !target.isOnline()) {
                player.sendMessage("§c§lDINNERBONE ➔ §cJogador não encontrado.");
                return;
            }

            boolean enabled = toggle(target);
            player.sendMessage("§a§lDINNERBONE ➔ §a" + target.getName() + (enabled ? " agora está de cabeça pra baixo." : " voltou ao normal."));
            return;
        }

        if (!player.hasPermission(PERM_SELF)) {
            player.sendMessage("§c§lDINNERBONE ➔ §cVocê não possui permissão para executar este comando.");
            player.playSound(player.getLocation(), Sound.CLICK, 1.0f, 1.0f);
            return;
        }

        boolean enabled = toggle(player);
        player.sendMessage("§a§lDINNERBONE ➔ §a" + (enabled ? "Você agora está de cabeça pra baixo." : "Você voltou ao normal."));
        player.playSound(player.getLocation(), Sound.LEVEL_UP, 1.0f, 1.0f);
    }

    private boolean toggle(Player player) {
        UUID id = player.getUniqueId();
        boolean isDinnerbone = ORIGINAL_NAMES.containsKey(id);

        if (isDinnerbone) {
            String originalName = ORIGINAL_NAMES.remove(id);
            changeProfileNameAndRefresh(player, originalName, originalName);
            return false;
        } else {
            String realName = player.getName(); // Salva o nome antes de mudar
            ORIGINAL_NAMES.put(id, realName);
            changeProfileNameAndRefresh(player, "Dinnerbone", realName);
            return true;
        }
    }

    // O "Motor" NMS que separa a visão do jogador da visão dos outros
    private void changeProfileNameAndRefresh(Player player, String newProfileName, String realName) {
        try {
            EntityPlayer ep = ((CraftPlayer) player).getHandle();
            boolean turningDinnerbone = newProfileName.equals("Dinnerbone");

            String currentTabName = player.getPlayerListName();
            if (currentTabName == null) currentTabName = realName;

            PacketPlayOutPlayerInfo removeInfo = new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER, ep);
            PacketPlayOutEntityDestroy destroyEntity = new PacketPlayOutEntityDestroy(ep.getId());

            for (Player online : Bukkit.getOnlinePlayers()) {
                if (!online.equals(player)) {
                    EntityPlayer observer = ((CraftPlayer) online).getHandle();
                    observer.playerConnection.sendPacket(removeInfo);
                    observer.playerConnection.sendPacket(destroyEntity);
                }
            }

            GameProfile profile = ep.getProfile();
            Field nameField = GameProfile.class.getDeclaredField("name");
            nameField.setAccessible(true);
            nameField.set(profile, newProfileName);

            IChatBaseComponent originalList = ep.listName;
            ep.listName = CraftChatMessage.fromString(currentTabName)[0];

            PacketPlayOutPlayerInfo addInfo = new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, ep);
            PacketPlayOutNamedEntitySpawn spawnEntity = new PacketPlayOutNamedEntitySpawn(ep);

            for (Player online : Bukkit.getOnlinePlayers()) {
                if (!online.equals(player)) {
                    EntityPlayer observer = ((CraftPlayer) online).getHandle();
                    observer.playerConnection.sendPacket(addInfo);
                    observer.playerConnection.sendPacket(spawnEntity);
                }
            }

            if (turningDinnerbone) {
                ep.listName = CraftChatMessage.fromString("§f" + realName)[0];
            } else {
                ep.listName = CraftChatMessage.fromString(currentTabName)[0];
            }

            PacketPlayOutPlayerInfo personalTabPacket = new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.UPDATE_DISPLAY_NAME, ep);
            ep.playerConnection.sendPacket(personalTabPacket);

            ep.listName = originalList;

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
