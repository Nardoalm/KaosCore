package com.knowplugins.ysubz.kaoscore.bukkit.managers.world;

import org.bukkit.Bukkit;
import org.bukkit.World;

public class ClimateManager {
    public void forceClearWeather(){
        for (World world : Bukkit.getWorlds()){
            world.setStorm(false);
            world.setThundering(false);
            world.setWeatherDuration(Integer.MAX_VALUE);
        }
    }
}
