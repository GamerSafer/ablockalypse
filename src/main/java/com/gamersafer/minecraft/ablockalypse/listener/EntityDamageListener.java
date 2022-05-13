package com.gamersafer.minecraft.ablockalypse.listener;

import com.gamersafer.minecraft.ablockalypse.Character;
import com.gamersafer.minecraft.ablockalypse.database.api.StoryStorage;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

@SuppressWarnings("ClassCanBeRecord")
public class EntityDamageListener implements Listener {

    private final StoryStorage storyStorage;

    public EntityDamageListener(StoryStorage storyStorage) {
        this.storyStorage = storyStorage;
    }

    @SuppressWarnings("unused")
    @EventHandler
    private void onEntityDamage(EntityDamageEvent event) {

        if (event.getEntity() instanceof Player player) {
            storyStorage.getActiveStory(player.getUniqueId()).thenAccept(story -> {
                if (story.isPresent()) {

                    if (event.getCause() == EntityDamageEvent.DamageCause.FALL && story.get().character() == Character.FREE_RUNNER) {
                        // disable fall damage for the free runner backstory
                        event.setCancelled(true);
                    } else if (story.get().character() == Character.CONSTRUCTION_WORKER) {
                        // reduce damage taken by construction worker
                        event.setDamage(event.getDamage() / 2);
                    }
                }
            });
        }
    }

}
