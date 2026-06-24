package com.knowplugins.ysubz.kaoscore.cmd.commands.vip;

import com.knowplugins.ysubz.kaoscore.cmd.Commands;
import net.minecraft.server.v1_8_R3.PacketPlayOutGameStateChange;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

public class TrollCommand extends Commands {

    private static final String PERMISSION = "kaoscore.admin.troll";

    public TrollCommand() {
        super("troll", "demo");
    }

    @Override
    public void perform(CommandSender sender, String label, String[] args) {

        if (sender instanceof Player && !sender.hasPermission(PERMISSION)) {
            sender.sendMessage("§c§lTROLL ➔ §cVocê não possui permissão para executar este comando.");
            if (sender instanceof Player) {
                ((Player) sender).playSound(((Player) sender).getLocation(), Sound.CLICK, 1.0f, 1.0f);
            }
            return;
        }

        if (args.length < 1) {
            sender.sendMessage("§c§lTROLL ➔ §cUso correto: /troll <jogador>");
            if (sender instanceof Player) {
                ((Player) sender).playSound(((Player) sender).getLocation(), Sound.CLICK, 1.0f, 1.0f);
            }
            return;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            sender.sendMessage("§c§lTROLL ➔ §cJogador não encontrado.");
            if (sender instanceof Player) {
                ((Player) sender).playSound(((Player) sender).getLocation(), Sound.CLICK, 1.0f, 1.0f);
            }
            return;
        }

        try {
            // GameStateChange id=5 costuma abrir a tela de demo (depende do client).
            ((CraftPlayer) target).getHandle().playerConnection.sendPacket(new PacketPlayOutGameStateChange(5, 0.0F));

            sender.sendMessage("§a§lTROLL ➔ §aA tela de demonstração foi aberta para " + target.getName() + ".");
            if (sender instanceof Player) {
                ((Player) sender).playSound(((Player) sender).getLocation(), Sound.LEVEL_UP, 1.0f, 1.0f);
            }

        } catch (Exception ex) {
            sender.sendMessage("§c§lTROLL ➔ §cOcorreu um erro interno ao tentar trollar o jogador.");
            ex.printStackTrace();
        }
    }
}

