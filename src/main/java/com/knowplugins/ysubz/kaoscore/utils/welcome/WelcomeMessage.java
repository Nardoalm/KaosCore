package com.knowplugins.ysubz.kaoscore.utils.welcome;

import com.knowplugins.ysubz.kaoscore.KaosCore;
import com.knowplugins.ysubz.kaoscore.integration.practice.PracticeModeBlocker;
import com.knowplugins.ysubz.kaoscore.managers.Managers;
import com.knowplugins.ysubz.kaoscore.managers.player.DisguiseManager;
import com.knowplugins.ysubz.kaoscore.managers.player.TagManager;
import com.knowplugins.ysubz.kaoscore.managers.player.VanishManager;
import com.knowplugins.ysubz.kaoscore.managers.player.WelcomeMessageManager;
import com.knowplugins.ysubz.kaoscore.utils.disguise.DisguiseApiService;
import org.bukkit.entity.Player;

public class WelcomeMessage {
    public static void sendWelcomeMessage(Player player) {
        if (player == null || !player.hasPermission("kaoscore.member.welcomeMessage")) {
            return;
        }

        if (PracticeModeBlocker.check(player, false)) {
            return;
        }

        if(VanishManager.isVanished(player)) {
            return;
        }

        KaosCore plugin = KaosCore.getInstance();

        if (plugin == null || !plugin.getConfig().getBoolean("features.welcome-messages.enabled", true)) {
            return;
        }


        TagManager tagManager = KaosCore.getInstance().getTagManager();
        String tag = tagManager.getTag(player).getPrefix();

        if(tag.startsWith("§7")){
            return;
        }

        String playerText;
        if (DisguiseManager.isDisguised(player)) {
            playerText = "§6§l⭐ " + tag + DisguiseApiService.getDisguiseNick(player);
        } else {
            playerText = "§6§l⭐ " + tag + player.getName();
        }

        WelcomeMessageManager manager = Managers.getWelcomeMessageManager();
        WelcomeMessageManager.Template template = manager == null ? null : manager.resolveSelected(player);
        String raw = template == null ? "{player} §6joined the lobby!" : template.getText();
        String message = raw.contains("{player}") ? raw.replace("{player}", playerText) : (playerText + " " + raw);

        player.getServer().broadcastMessage(message);
    }
}

