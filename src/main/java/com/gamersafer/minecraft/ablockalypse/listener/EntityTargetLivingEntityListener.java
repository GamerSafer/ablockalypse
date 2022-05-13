package com.gamersafer.minecraft.ablockalypse.listener;

import com.gamersafer.minecraft.ablockalypse.Character;
import com.gamersafer.minecraft.ablockalypse.database.api.StoryStorage;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;

@SuppressWarnings("ClassCanBeRecord")
public class EntityTargetLivingEntityListener implements Listener {

    private final StoryStorage storyStorage;

    public EntityTargetLivingEntityListener(StoryStorage storyStorage) {
        this.storyStorage = storyStorage;
    }

    @SuppressWarnings("unused")
    @EventHandler
    private void onEntityTargetLivingEntity(EntityTargetLivingEntityEvent event) {
        // I.T Consultant - Quiet (zombies can only hear them if within 5 block radius)
        if (event.getTarget() instanceof Player targetPlayer
                && (event.getEntityType() == EntityType.ZOMBIE || event.getEntityType() == EntityType.ZOMBIE_VILLAGER)) {

            if (targetPlayer.getLocation().distance(event.getEntity().getLocation()) > 5) {

                storyStorage.getActiveStory(targetPlayer.getUniqueId()).thenAccept(story -> {
                    if (story.isPresent() && story.get().character() == Character.IT_CONSULTANT) {
                        event.setTarget(null);
                        event.setCancelled(true);
                    }
                });
            }
        }
    }

}
