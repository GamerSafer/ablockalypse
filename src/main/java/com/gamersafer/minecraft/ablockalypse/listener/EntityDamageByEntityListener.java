package com.gamersafer.minecraft.ablockalypse.listener;

import com.gamersafer.minecraft.ablockalypse.Character;
import com.gamersafer.minecraft.ablockalypse.database.api.StoryStorage;
import com.gamersafer.minecraft.ablockalypse.util.ItemUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

@SuppressWarnings("ClassCanBeRecord")
public class EntityDamageByEntityListener implements Listener {

    private final StoryStorage storyStorage;

    public EntityDamageByEntityListener(StoryStorage storyStorage) {
        this.storyStorage = storyStorage;
    }

    @SuppressWarnings("unused")
    @EventHandler
    private void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            storyStorage.getActiveStory(player.getUniqueId()).thenAccept(story -> {
                // Police Officer - Does more weapon damage
                if (story.isPresent() && story.get().character() == Character.POLICE_OFFICER) {
                    ItemStack usedItem = player.getInventory().getItemInMainHand();
                    // make sure they used a weapon
                    if (ItemUtil.WEAPONS.contains(usedItem.getType())) {
                        double finalDamage = event.getFinalDamage() * 1.3d;
                        event.setDamage(finalDamage);
                    }
                }
            });
        }
    }

}
