package com.knowplugins.ysubz.kaoscore.bukkit.listeners.world;

import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;

public class TimeListener implements Listener {
    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        World world = event.getWorld();
        world.setGameRuleValue("doDaylightCycle", "false");
        world.setTime(6000L);
    }
}
