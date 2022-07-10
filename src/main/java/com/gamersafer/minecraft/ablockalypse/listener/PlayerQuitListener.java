package com.gamersafer.minecraft.ablockalypse.listener;

import com.gamersafer.minecraft.ablockalypse.database.api.StoryStorage;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitListener implements Listener {

    private final StoryStorage storyStorage;

    public PlayerQuitListener(StoryStorage storyStorage) {
        this.storyStorage = storyStorage;
    }

    @SuppressWarnings("unused")
    @EventHandler
    private void onPlayerQuit(PlayerQuitEvent event) {
        // try to update the survival time in the database
        storyStorage.updateSurvivalTime(event.getPlayer().getUniqueId());
    }

}
