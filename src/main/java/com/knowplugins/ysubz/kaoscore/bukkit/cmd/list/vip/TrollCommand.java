package com.knowplugins.ysubz.kaoscore.bukkit.cmd.list.vip;

import com.knowplugins.ysubz.kaoscore.bukkit.cmd.Commands;
import net.minecraft.server.v1_8_R3.PacketPlayOutGameStateChange;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TrollCommand extends Commands {

    private static final String PERMISSION = "kaoscore.admin.troll";
    private static final Map<UUID, Long> COOLDOWNS = new HashMap<>();
    private static final long COOLDOWN_TIME = 10000;

    public TrollCommand() {
        super("troll", "demo");
    }

    @Override
    public void perform(CommandSender sender, String label, String[] args) {

        if (sender instanceof Player && !sender.hasPermission(PERMISSION)) {
            sender.sendMessage("§cYou do not have permission to execute this command.");
            if (sender instanceof Player) {
                ((Player) sender).playSound(((Player) sender).getLocation(), Sound.CLICK, 1.0f, 1.0f);
            }
            return;
        }

        if (sender instanceof Player) {
            Player playerSender = (Player) sender;
            UUID senderUUID = playerSender.getUniqueId();

            if (COOLDOWNS.containsKey(senderUUID)) {
                long timeLeft = (COOLDOWNS.get(senderUUID) + COOLDOWN_TIME) - System.currentTimeMillis();
                if (timeLeft > 0) {
                    playerSender.sendMessage("§cYou must wait " + String.format("%.1f", (timeLeft / 1000.0)) + "s to use this command again.");
                    playerSender.playSound(playerSender.getLocation(), Sound.CLICK, 1.0f, 1.0f);
                    return;
                }
            }
        }

        if (args.length < 1) {
            sender.sendMessage("§cUsage: /troll <player>");
            if (sender instanceof Player) {
                ((Player) sender).playSound(((Player) sender).getLocation(), Sound.CLICK, 1.0f, 1.0f);
            }
            return;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            sender.sendMessage("§cPlayer not found.");
            if (sender instanceof Player) {
                ((Player) sender).playSound(((Player) sender).getLocation(), Sound.CLICK, 1.0f, 1.0f);
            }
            return;
        }

        try {
            ((CraftPlayer) target).getHandle().playerConnection.sendPacket(new PacketPlayOutGameStateChange(5, 0.0F));

            sender.sendMessage("§aThe demo screen has been opened for " + target.getName() + ".");
            if (sender instanceof Player) {
                Player playerSender = (Player) sender;
                COOLDOWNS.put(playerSender.getUniqueId(), System.currentTimeMillis());
                playerSender.playSound(playerSender.getLocation(), Sound.NOTE_PLING, 1.0f, 1.0f);
            }

        } catch (Exception ex) {
            sender.sendMessage("§cAn internal error occurred while trying to troll the player.");
            ex.printStackTrace();
        }
    }
}