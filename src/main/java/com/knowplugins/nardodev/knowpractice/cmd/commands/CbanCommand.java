package com.knowplugins.nardodev.knowpractice.cmd.commands;

import com.knowplugins.nardodev.knowpractice.cmd.Commands;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class CbanCommand extends Commands {
    public CbanCommand(){
        super("cban", "cb");
    }

    @Override
    public void perform(CommandSender sender, String label, String[] args) {
        if(args.length < 1){
            sender.sendMessage("§c§lERRO ➔ §cUso correto: /cban <jogador>");
            Player senderPlayer = (Player) sender;
            senderPlayer.playSound(senderPlayer.getLocation(), Sound.VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        if (!sender.hasPermission("nardocore.staff.banir")){
            sender.sendMessage( "§c§lERRO ➔ §cVocê não tem permissão para usar esse comando.");
            Player senderPlayer = (Player) sender;
            senderPlayer.playSound(senderPlayer.getLocation(), Sound.VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        Player target = Bukkit.getPlayer(args[0]);

        if (target == null || !target.isOnline()){
            sender.sendMessage("§c§lERRO ➔ §cJogador não existe ou não está online.");
            Player senderPlayer = (Player) sender;
            senderPlayer.playSound(senderPlayer.getLocation(), Sound.VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        if (target.getName().equals(sender.getName())){
            sender.sendMessage("§c§lERRO ➔ §cVocê não pode se banir!");
            Player senderPlayer = (Player) sender;
            senderPlayer.playSound(senderPlayer.getLocation(), Sound.VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        String reason = "Uso de trapaças";

        Date today = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(today);
        calendar.add(Calendar.DAY_OF_MONTH, 30);
        Date expires = calendar.getTime();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

        String kickMessage =
                ChatColor.RED + "Você foi banido temporariamente.\n\n" +
                        ChatColor.RED +"Motivo: "+ ChatColor.GRAY  + reason + "\n" +
                        ChatColor.RED + "Expira em: " + ChatColor.GRAY + sdf.format(expires) + "\n" +
                        ChatColor.YELLOW + "Adquira seu unban em: " + ChatColor.AQUA + "www.fusionmc.com.br\n" +
                        ChatColor.RED + "Banido injustamente? Contate-nos via\n" +
                        ChatColor.BLUE + "discord.gg/fusionmc";

        Bukkit.getBanList(BanList.Type.NAME).addBan(target.getName(), reason, expires, sender.getName());

        Player senderPlayer = (Player) sender;
        senderPlayer.playSound(senderPlayer.getLocation(), Sound.EXPLODE, 1.0f, 1.0f);
        sender.sendMessage(ChatColor.GREEN + "§a§lSUCESSO ➔ §a" + target.getName() + " foi banido por 30 dias com sucesso!");

        Player[] playersOnline = Bukkit.getOnlinePlayers().toArray(new Player[0]);

        target.getWorld().strikeLightningEffect(target.getLocation());
        target.kickPlayer(kickMessage);

        for (Player p : playersOnline){
            p.sendMessage("§c§lBANIMENTO ➔ §c" + target.getName() + " §cfoi banido por uso de trapaças.");
            p.playSound(p.getLocation(), Sound.ARROW_HIT, 1.0f, 1.0f);
        }
    }
}
