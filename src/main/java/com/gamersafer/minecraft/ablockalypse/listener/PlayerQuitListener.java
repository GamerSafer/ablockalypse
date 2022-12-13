package com.gamersafer.minecraft.ablockalypse.listener;

import com.gamersafer.minecraft.ablockalypse.database.api.StoryStorage;
import com.gamersafer.minecraft.ablockalypse.safehouse.BoosterManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitListener implements Listener {

    private final StoryStorage storyStorage;
    private final BoosterManager boosterManager;

    public PlayerQuitListener(StoryStorage storyStorage, BoosterManager boosterManager) {
        this.storyStorage = storyStorage;
        this.boosterManager = boosterManager;
    }

    @SuppressWarnings("unused")
    @EventHandler
    private void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // remove boosters
        boosterManager.removeBoosters(player);

        // try to update the survival time in the database
        storyStorage.updateSurvivalTime(player.getUniqueId());
    }

}
