package com.knowplugins.ysubz.kaoscore.bukkit.managers.player;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;

public class GameModeManager {
   public static void setGamemode(Player player, String type) {
      if (type.equals("0")) {
         type = "SURVIVAL";
      }

      if (type.equals("1")) {
         type = "CREATIVE";
      }

      if (type.equals("2")) {
         type = "ADVENTURE";
      }

      if (type.equals("3")) {
         type = "SPECTATOR";
      }

      player.setGameMode(GameMode.valueOf(type));
   }

   public static boolean isValidMode(String type) {
      try {
         return Integer.parseInt(type) == 0 || Integer.parseInt(type) == 3 || Integer.parseInt(type) == 1 || Integer.parseInt(type) == 2;
      } catch (NumberFormatException var2) {
         return type.equalsIgnoreCase("spectator") || type.equalsIgnoreCase("survival") || type.equalsIgnoreCase("creative") || type.equalsIgnoreCase("adventure");
      }
   }

   public static String getFrom(String type) {
      try {
         if (Integer.parseInt(type) == 0) {
            return "Survival";
         } else if (Integer.parseInt(type) == 1) {
            return "Creative";
         } else if (Integer.parseInt(type) == 2) {
            return "Adventure";
         } else {
            return Integer.parseInt(type) == 3 ? "Spectador" : "Survival";
         }
      } catch (NumberFormatException var2) {
         if (type.equalsIgnoreCase("Survival")) {
            return "Sobrevivência";
         } else if (type.equalsIgnoreCase("Creative")) {
            return "Criativo";
         } else if (type.equalsIgnoreCase("Adventure")) {
            return "Aventura";
         } else {
            return type.equalsIgnoreCase("Spectator") ? "Espectador" : "Aventura";
         }
      }
   }
}
