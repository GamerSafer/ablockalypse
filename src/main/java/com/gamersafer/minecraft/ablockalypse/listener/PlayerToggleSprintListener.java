package com.gamersafer.minecraft.ablockalypse.listener;

import com.gamersafer.minecraft.ablockalypse.Character;
import com.gamersafer.minecraft.ablockalypse.database.api.StoryStorage;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSprintEvent;

public class PlayerToggleSprintListener implements Listener {

    private final StoryStorage storyStorage;

    public PlayerToggleSprintListener(StoryStorage storyStorage) {
        this.storyStorage = storyStorage;
    }

    @SuppressWarnings("unused")
    @EventHandler
    private void onToggleSprint(PlayerToggleSprintEvent event) {
        // Sprinter - Can sprint faster (regular speed when walking and sneaking)
        storyStorage.getActiveStory(event.getPlayer().getUniqueId()).thenAccept(story -> {
            if (story.isPresent() && story.get().character() == Character.SPRINTER) {
                if (event.isSprinting()) {
                    event.getPlayer().setWalkSpeed(0.3f);
                } else {
                    // set default walking speed
                    event.getPlayer().setWalkSpeed(0.2f);
                }
            }
        });
    }

}
