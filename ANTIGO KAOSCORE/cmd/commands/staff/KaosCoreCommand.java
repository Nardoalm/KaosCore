package com.knowplugins.ysubz.kaoscore.cmd.commands.staff;

import com.knowplugins.ysubz.kaoscore.KaosCore;
import com.knowplugins.ysubz.kaoscore.cmd.Commands;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;

public class KaosCoreCommand extends Commands {

    private final KaosCore plugin;

    public KaosCoreCommand(KaosCore plugin) {
        super("kaoscore");
        this.plugin = plugin;
    }

    @Override
    public void perform(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission("kaoscore.staff.reload")) {
            sender.sendMessage("§c§lKAOSCORE ➔ §cVocê não tem permissão para usar esse comando.");
            if (sender instanceof org.bukkit.entity.Player) {
                ((org.bukkit.entity.Player) sender).playSound(((org.bukkit.entity.Player) sender).getLocation(), Sound.CLICK, 1.0f, 1.0f);
            }
            return;
        }

        if (args.length != 1 || !args[0].equalsIgnoreCase("reload")) {
            sender.sendMessage("§e§lKAOSCORE ➔ §eUse: /kaoscore reload");
            return;
        }

        try {
            plugin.reloadSystems();
            sender.sendMessage("§a§lKAOSCORE ➔ §aAs configurações foram recarregadas com sucesso.");
            if (sender instanceof org.bukkit.entity.Player) {
                ((org.bukkit.entity.Player) sender).playSound(((org.bukkit.entity.Player) sender).getLocation(), Sound.LEVEL_UP, 1.0f, 1.0f);
            }
        } catch (Exception ex) {
            sender.sendMessage("§c§lKAOSCORE ➔ §cFalha ao recarregar as configurações. Veja o console.");
            plugin.getLogger().severe("Falha ao executar /kaoscore reload: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}
