package com.gamersafer.minecraft.ablockalypse.listener;

import com.gamersafer.minecraft.ablockalypse.Character;
import com.gamersafer.minecraft.ablockalypse.database.api.StoryStorage;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSneakEvent;

public class PlayerToggleSneakListener implements Listener {

    private final StoryStorage storyStorage;

    public PlayerToggleSneakListener(StoryStorage storyStorage) {
        this.storyStorage = storyStorage;
    }

    @SuppressWarnings("unused")
    @EventHandler
    private void onToggleSneak(PlayerToggleSneakEvent event) {
        // I.T Consultant - Moves faster while sneaking
        storyStorage.getActiveStory(event.getPlayer().getUniqueId()).thenAccept(story -> {
            if (story.isPresent() && story.get().character() == Character.IT_CONSULTANT) {
                if (event.isSneaking()) {
                    event.getPlayer().setWalkSpeed(0.3f);
                } else {
                    // set default walking speed
                    event.getPlayer().setWalkSpeed(0.2f);
                }
            }
        });
    }

}
