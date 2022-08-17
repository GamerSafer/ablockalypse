package com.gamersafer.minecraft.ablockalypse.listener;

import com.gamersafer.minecraft.ablockalypse.Character;
import com.gamersafer.minecraft.ablockalypse.database.api.StoryStorage;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public class EntityDamageListener implements Listener {

    private final StoryStorage storyStorage;

    public EntityDamageListener(StoryStorage storyStorage) {
        this.storyStorage = storyStorage;
    }

    @SuppressWarnings("unused")
    @EventHandler
    private void onEntityDamage(EntityDamageEvent event) {

        if (event.getEntity() instanceof Player player && event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            storyStorage.getActiveStory(player.getUniqueId()).thenAccept(story -> {
                if (story.isPresent() && story.get().character() == Character.FREE_RUNNER) {
                    // disable fall damage for the free runner backstory
                    if (player.getFallDistance() < 12) {
                        event.setCancelled(true);
                    }
                }
            });
        }
    }

}
