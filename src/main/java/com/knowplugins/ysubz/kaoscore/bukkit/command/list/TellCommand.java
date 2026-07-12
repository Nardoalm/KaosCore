package com.knowplugins.ysubz.kaoscore.bukkit.command.list;

import com.knowplugins.ysubz.kaoscore.bukkit.KaosCore;
import com.knowplugins.ysubz.kaoscore.bukkit.command.Commands;
import com.knowplugins.ysubz.kaoscore.bukkit.util.disguise.DisguiseManager;
import com.knowplugins.ysubz.kaoscore.bukkit.manager.MainManager;
import com.knowplugins.ysubz.kaoscore.bukkit.manager.player.PunishmentManager;
import com.knowplugins.ysubz.kaoscore.bukkit.manager.plugin.PunishmentMessageManager;
import com.knowplugins.ysubz.kaoscore.bukkit.util.punishment.PunishmentEntry;
import com.knowplugins.ysubz.kaoscore.bukkit.util.punishment.PunishmentTime;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.knowplugins.ysubz.kaoscore.bukkit.moderation.ModerationCommandUtil.play;

public class TellCommand extends Commands {

    public static final Map<UUID, UUID> lastMessageSender = new HashMap<>();
    public static final Map<UUID, Long> replyCooldown = new HashMap<>();
    public static final long REPLY_TIMEOUT = 30000;

    public TellCommand(){
        super("tell", "falar");
    }

    @Override
    public void perform(CommandSender sender, String label, String[] args) {
        if(!(sender instanceof Player)){
            sender.sendMessage("You must be a player to perform this command.");
            return;
        }

        if (args.length < 2) {
            sender.sendMessage("§cUsage: /tell <player> <message>");
            play(sender, Sound.CLICK);
            return;
        }

        Player playerSender = (Player) sender;

        PunishmentManager punishmentManager = MainManager.getPunishmentManager();
        if (punishmentManager != null) {
            PunishmentEntry mute = punishmentManager.getActiveMute(playerSender);
            if (mute != null) {
                KaosCore plugin = KaosCore.getInstance();
                PunishmentMessageManager messages = plugin == null ? null : plugin.getPunishmentMessageManager();
                if (messages != null) {
                    for (String line : messages.buildMuteChatLines(mute)) {
                        playerSender.sendMessage(line);
                    }
                } else {
                    playerSender.sendMessage("§cYou are muted.");
                    playerSender.sendMessage("§cReason: §7" + mute.getReason());
                    playerSender.sendMessage("§cDuration: §7" + PunishmentTime.formatRemaining(mute.getExpiresAt()));
                }
                return;
            }
        }

        Player target = Bukkit.getPlayer(args[0]);

        if (target == null) {
            target = DisguiseManager.getOnlinePlayerByName(args[0]);
        }

        if (target == null) {
            sender.sendMessage("§cPlayer not found.");
            return;
        }

        if (target.getName().isEmpty()) {
            sender.sendMessage("§cInvalid player.");
            play(sender, Sound.CLICK);
            return;
        }

        StringBuilder messageBuilder = new StringBuilder();

        for (int i = 1; i < args.length; i++) {
            messageBuilder.append(args[i]);
            if (i < args.length - 1) {
                messageBuilder.append(" ");
            }
        }

        String message = messageBuilder.toString();


        target.sendMessage("§7[§eFrom " + KaosCore.getInstance().getTagManager().getTag(playerSender).getColor() + playerSender.getName() + "§7] §e" + message);

        playerSender.sendMessage("§7[§eTo " +  KaosCore.getInstance().getTagManager().getTag(target).getColor() + target.getName() + "§7] §e" + message);

        play(target, Sound.NOTE_PLING);

        lastMessageSender.put(target.getUniqueId(), playerSender.getUniqueId());
        replyCooldown.put(target.getUniqueId(), System.currentTimeMillis());

        lastMessageSender.put(playerSender.getUniqueId(), target.getUniqueId());
        replyCooldown.put(playerSender.getUniqueId(), System.currentTimeMillis());
    }
}