package com.knowplugins.ysubz.kaoscore.bukkit.command.list;

import com.knowplugins.ysubz.kaoscore.bukkit.KaosCore;
import com.knowplugins.ysubz.kaoscore.bukkit.command.Commands;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

import static com.knowplugins.ysubz.kaoscore.bukkit.moderation.ModerationCommandUtil.play;

public class ReplyCommand extends Commands {
    public ReplyCommand(){
        super("reply", "r");
    }

    @Override
    public void perform(CommandSender sender, String label, String[] args) {
        if(!(sender instanceof Player)){
            sender.sendMessage("You must be a player to perform this command.");
            return;
        }

        if (args.length < 1) {
            sender.sendMessage("§cUsage: /reply <message>");
            play(sender, Sound.CLICK);
            return;
        }

        Player playerSender = (Player) sender;
        UUID senderUUID = playerSender.getUniqueId();

        UUID targetUUID = TellCommand.lastMessageSender.get(senderUUID);
        Long timeout = TellCommand.replyCooldown.get(senderUUID);

        if (targetUUID == null || timeout == null) {
            sender.sendMessage("§cYou have no one to reply to.");
            play(sender, Sound.CLICK);
            return;
        }

        if (System.currentTimeMillis() - timeout > TellCommand.REPLY_TIMEOUT) {
            TellCommand.lastMessageSender.remove(senderUUID);
            TellCommand.replyCooldown.remove(senderUUID);
            sender.sendMessage("§cThe reply time has expired.");
            play(sender, Sound.CLICK);
            return;
        }

        Player target = Bukkit.getPlayer(targetUUID);

        if (target == null || !target.isOnline()) {
            TellCommand.lastMessageSender.remove(senderUUID);
            TellCommand.replyCooldown.remove(senderUUID);
            sender.sendMessage("§cThe player is no longer online.");
            play(sender, Sound.CLICK);
            return;
        }

        StringBuilder messageBuilder = new StringBuilder();

        for (String arg : args) {
            messageBuilder.append(arg).append(" ");
        }

        String message = messageBuilder.toString().trim();

        target.sendMessage("§7[§eFrom " + KaosCore.getInstance().getTagManager().getTag(playerSender).getColor() + playerSender.getName() + "§7] §e" + message);

        playerSender.sendMessage("§7[§eTo " + KaosCore.getInstance().getTagManager().getTag(target).getColor() + target.getName() + "§7] §e" + message);

        play(target, Sound.NOTE_PLING);

        TellCommand.lastMessageSender.put(targetUUID, senderUUID);
        TellCommand.replyCooldown.put(targetUUID, System.currentTimeMillis());

        TellCommand.lastMessageSender.put(senderUUID, targetUUID);
        TellCommand.replyCooldown.put(senderUUID, System.currentTimeMillis());
    }
}