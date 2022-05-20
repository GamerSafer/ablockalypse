package com.gamersafer.minecraft.ablockalypse.listener;

import com.gamersafer.minecraft.ablockalypse.AblockalypsePlugin;
import com.gamersafer.minecraft.ablockalypse.Character;
import com.gamersafer.minecraft.ablockalypse.database.api.StoryStorage;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTameEvent;

public class EntityTameListener implements Listener {

    private final AblockalypsePlugin plugin;
    private final StoryStorage storyStorage;

    public EntityTameListener(AblockalypsePlugin plugin, StoryStorage storyStorage) {
        this.plugin = plugin;
        this.storyStorage = storyStorage;
    }

    @SuppressWarnings("unused")
    @EventHandler
    private void onEntityTame(EntityTameEvent event) {
        // Dog Walker - Can tame/ starts with wolf (can re-tame wolves if they lose theirs)
        if (event.getOwner() instanceof Player player) {

            if (event.getEntityType() != EntityType.WOLF) {
                event.setCancelled(true);
                plugin.sendMessage(player, "tame-no");
                return;
            }

            // check whether the player already has a tamed wolf
            for (World world : Bukkit.getWorlds()) {
                for (Entity wolfEntity : world.getEntitiesByClasses(Wolf.class)) {
                    if (((Tameable) wolfEntity).isTamed() && player.getUniqueId().equals(((Tameable) wolfEntity).getOwner().getUniqueId())) {
                        // the player already has a tamed wolf. we can assume he is a Dog Walker.
                        // cancel the event since they can have only 1 tamed wolf at a time
                        event.setCancelled(true);
                        plugin.sendMessage(player, "tame-wolf-already");
                        break;
                    }
                }
            }

            storyStorage.getActiveStory(player.getUniqueId()).thenAccept(story -> {
                if (story.isEmpty() || story.get().character() != Character.DOG_WALKER) {
                    // the player is not a dog walker, he can't tame wolves
                    event.setCancelled(true);
                    plugin.sendMessage(player, "tame-wolf-only-dogwalker");
                }
            });
        }
    }


}
