package com.knowplugins.ysubz.kaoscore.bukkit.command;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.knowplugins.ysubz.kaoscore.bukkit.clan.command.ClanChatCommand;
import com.knowplugins.ysubz.kaoscore.bukkit.clan.command.ClanCommand;
import com.knowplugins.ysubz.kaoscore.bukkit.command.list.*;
import com.knowplugins.ysubz.kaoscore.bukkit.auth.command.LoginCommand;
import com.knowplugins.ysubz.kaoscore.bukkit.auth.command.RegisterCommand;
import com.knowplugins.ysubz.kaoscore.bukkit.auth.command.SetAuthLocationCommand;
import com.knowplugins.ysubz.kaoscore.bukkit.command.list.vip.*;
import com.knowplugins.ysubz.kaoscore.bukkit.disguise.command.DisguiseCommand;
import com.knowplugins.ysubz.kaoscore.bukkit.disguise.command.UnDisguiseCommand;
import com.knowplugins.ysubz.kaoscore.bukkit.friend.command.FriendCommand;
import com.knowplugins.ysubz.kaoscore.bukkit.moderation.admin.command.*;
import com.knowplugins.ysubz.kaoscore.bukkit.moderation.staff.command.*;
import com.knowplugins.ysubz.kaoscore.bukkit.sit.command.SitCommand;
import com.knowplugins.ysubz.kaoscore.bukkit.util.medal.command.MedalCommand;
import com.knowplugins.ysubz.kaoscore.bukkit.util.skin.command.SkinCommand;
import com.knowplugins.ysubz.kaoscore.bukkit.util.tag.command.SetTagCommand;
import com.knowplugins.ysubz.kaoscore.bukkit.util.tag.command.TagCommand;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.SimpleCommandMap;

import com.knowplugins.ysubz.kaoscore.bukkit.KaosCore;

public abstract class Commands extends Command {
   private static final String FALLBACK_PREFIX = "kaoscore";

   public Commands(String name, String... aliases) {
      super(name);
      List<String> aliasList = new ArrayList<>();
      if (aliases != null) {
         aliasList.addAll(Arrays.asList(aliases));
      }
      this.setAliases(aliasList);

      try {
         SimpleCommandMap simpleCommandMap = resolveCommandMap();
         if (simpleCommandMap == null) {
            throw new IllegalStateException("CommandMap not found");
         }

         forceUnregister(simpleCommandMap, this.getName());
         for (String alias : this.getAliases()) {
            forceUnregister(simpleCommandMap, alias);
         }

         simpleCommandMap.register(this.getName(), FALLBACK_PREFIX, this);
      } catch (Throwable var4) {
        Bukkit.getServer().getConsoleSender().sendMessage( "§c§lCOMANDOS ➔ §cNão foi possível iniciar os comandos.");
        var4.printStackTrace();
      }

   }

   public static void setupCommands(KaosCore plugin) {
      new TellCommand();
      new ReplyCommand();
      new GoCommand();
      new GamemodeCommand();
      new ClearChatCommand();
      new VanishCommand();
      new DiscordCommand();
      new PingCommand();
      new RegisterCommand(plugin);
      new SetAuthLocationCommand(plugin);
      new LoginCommand(plugin);
      // new CommandList();
      new UnDisguiseCommand();
      new TeleportCommand();
      new TpallCommand();
      new CheatBanCommand();
      new BanCommand();
      new MuteCommand();
      new UnmuteCommand();
      new WarnCommand();
      new HistoryCommand();
      new KickCommand();
      new AltsCommand();
      new GroupCommand();
      new GrantCommand();
      new UngrantCommand();
      new FlyCommand();
      new SetSpawnCommand();
      new UnbanCommand();
      // new BuildCommand();
      // new PvpCommand();
      new SetNpcCommand();
      new TagCommand();
      new RemoveNpcCommand();
      // new MenuCommand();
      new DisguiseCommand();
      new SkinCommand();
      new SitCommand();
      new DinnerboneCommand();
      new WhitelistCommand();
      // new WelcomeMessageCommand();
      new DiscloseCommand();
      new ReportCommand();
      new MedalCommand(plugin);
      new SetJumpPadCommand();
      new StaffChatCommand();
      new FriendCommand();
      new AdminChatCommand();
      new ClanCommand(plugin);
      new ClanChatCommand();
      new SetTagCommand();
      new TrollCommand();
      new OpCommand();
      new KaosCoreCommand(plugin);
   }

   public abstract void perform(CommandSender sender, String label, String[] args);

   public boolean execute(CommandSender sender, String commandLabel, String[] args) {
      this.perform(sender, commandLabel, args);
      return true;
   }

   private SimpleCommandMap resolveCommandMap() {
      try {
         return (SimpleCommandMap) Bukkit.getServer().getClass().getDeclaredMethod("getCommandMap").invoke(Bukkit.getServer());
      } catch (Throwable ignored) {
         return null;
      }
   }

   @SuppressWarnings("unchecked")
   private void forceUnregister(SimpleCommandMap commandMap, String label) throws Exception {
      if (commandMap == null || label == null || label.trim().isEmpty()) {
         return;
      }

      String key = label.trim().toLowerCase(Locale.ROOT);
      Field field = SimpleCommandMap.class.getDeclaredField("knownCommands");
      field.setAccessible(true);
      Map<String, Command> known = (Map<String, Command>) field.get(commandMap);
      if (known == null) {
         return;
      }

      known.remove(key);
      known.remove(FALLBACK_PREFIX + ":" + key);
   }
}

