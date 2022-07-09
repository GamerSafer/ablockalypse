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
        // try to get the active story
        storyStorage.getActiveStory(event.getPlayer().getUniqueId()).thenAccept(story -> {
            // update the survival time in the database if present
            story.ifPresent(storyStorage::updateSurvivalTime);
        });
    }

}
