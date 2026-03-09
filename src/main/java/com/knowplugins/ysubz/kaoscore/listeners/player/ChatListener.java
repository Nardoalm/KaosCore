package com.knowplugins.ysubz.kaoscore.listeners.player;

import com.knowplugins.ysubz.kaoscore.managers.Managers;
import com.knowplugins.ysubz.kaoscore.managers.player.PunishmentManager;
import com.knowplugins.ysubz.kaoscore.punishment.PunishmentEntry;
import com.knowplugins.ysubz.kaoscore.punishment.PunishmentTime;
import com.knowplugins.ysubz.kaoscore.utils.placeholder.PlaceholderManager;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;


public class ChatListener implements Listener {

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        event.setCancelled(true);

        Player p = event.getPlayer();
        String msg = event.getMessage();

        PunishmentManager punishmentManager = Managers.getPunishmentManager();
        if (punishmentManager != null) {
            PunishmentEntry mute = punishmentManager.getActiveMute(p);
            if (mute != null) {
                p.sendMessage("§c§lMUTE ➔ §cVocê está mutado.");
                p.sendMessage("§cMotivo: §7" + mute.getReason());
                p.sendMessage("§cDuração: §7" + PunishmentTime.formatRemaining(mute.getExpiresAt()));
                return;
            }
        }

        String level = PlaceholderAPI.setPlaceholders(p, "%kaos_level%");
        String medal = PlaceholderAPI.setPlaceholders(p, "%kaoscore_medal_chat%");
        String tag = PlaceholderAPI.setPlaceholders(p, "%kaoscore_tag_prefix%");
        String clan = PlaceholderAPI.setPlaceholders(p, "%kaoscore_clan_tag%");
        String displayName = PlaceholderManager.getDisplayName(p);

        StringBuilder prefix = new StringBuilder();
        appendPart(prefix, level);
        appendPart(prefix, medal);
        appendPart(prefix, tag);
        appendPart(prefix, clan);

        String format = prefix + displayName + " §8» §7" + msg;

        if(event.getPlayer().hasPermission("nardocore.member.vip")) {
            format = prefix + displayName + " §8» §f" + msg;
        }
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.sendMessage(format);
        }
    }

    private void appendPart(StringBuilder builder, String value) {
        if (value == null || value.trim().isEmpty()) {
            return;
        }
        builder.append(value);
        if (!value.endsWith(" ")) {
            builder.append(' ');
        }
    }
}
