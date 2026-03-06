package com.knowplugins.ysubz.kaoscore.cmd;

import java.util.Arrays;

import com.knowplugins.ysubz.kaoscore.cmd.commands.*;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.SimpleCommandMap;

import com.knowplugins.ysubz.kaoscore.KaosCore;

public abstract class Commands extends Command {
   public Commands(String name, String... aliases) {
      super(name);
      this.setAliases(Arrays.asList(aliases));

      try {
         SimpleCommandMap simpleCommandMap = (SimpleCommandMap)Bukkit.getServer().getClass().getDeclaredMethod("getCommandMap").invoke(Bukkit.getServer());
         simpleCommandMap.register(this.getName(), "nardocore", this);
      } catch (ReflectiveOperationException var4) {
        Bukkit.getServer().getConsoleSender().sendMessage( "§c§lERRO ➔ §cNão foi possível iniciar os comandos.");
      }

   }

   public static void setupCommands(KaosCore plugin) {
      new GamemodeCommand();
      new ClearChatCommand();
      new VanishCommand();
      new DiscordCommand();
      new PingCommand();
      new CommandList();
      new TeleportCommand();
      new CbanCommand();
      new FlyCommand();
      new SetSpawnCommand();
      new UnbanCommand();
      new BuildCommand();
      // new PvpCommand();
      // new SetNpcCommand();
      // new MenuCommand();
      new DisguiseCommand();
      new SkinCommand();
      new ClanCommand(plugin);
      new CcCommand();
   }

   public abstract void perform(CommandSender sender, String label, String[] args);

   public boolean execute(CommandSender sender, String commandLabel, String[] args) {
      this.perform(sender, commandLabel, args);
      return true;
   }
}
