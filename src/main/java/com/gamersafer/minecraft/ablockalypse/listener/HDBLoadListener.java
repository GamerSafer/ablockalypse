package com.gamersafer.minecraft.ablockalypse.listener;

import com.gamersafer.minecraft.ablockalypse.AblockalypsePlugin;
import com.gamersafer.minecraft.ablockalypse.safehouse.Booster;
import me.arcaniax.hdb.api.DatabaseLoadEvent;
import me.arcaniax.hdb.api.HeadDatabaseAPI;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

public class HDBLoadListener implements Listener {

    @SuppressWarnings("unused")
    @EventHandler
    private void onHeadDatabaseLoad(DatabaseLoadEvent event) {
        HeadDatabaseAPI api = new HeadDatabaseAPI();
        for (Booster booster : Booster.values())
            try {
                ItemStack item = api.getItemHead(Integer.toString(booster.getHdbId()));
                booster.setHead(item);
            } catch (NullPointerException nullPointerException) {
                AblockalypsePlugin.getInstance().getLogger().warning("Could not load head for booster " + booster.name());
            }
    }

}
