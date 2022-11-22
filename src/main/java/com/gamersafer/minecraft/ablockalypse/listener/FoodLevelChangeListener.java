package com.gamersafer.minecraft.ablockalypse.listener;

import com.gamersafer.minecraft.ablockalypse.Character;
import com.gamersafer.minecraft.ablockalypse.database.api.StoryStorage;
import com.gamersafer.minecraft.ablockalypse.safehouse.Booster;
import com.gamersafer.minecraft.ablockalypse.safehouse.BoosterManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.FoodLevelChangeEvent;

public class FoodLevelChangeListener implements Listener {

    private final StoryStorage storyStorage;
    private final BoosterManager boosterManager;

    public FoodLevelChangeListener(StoryStorage storyStorage, BoosterManager boosterManager) {
        this.storyStorage = storyStorage;
        this.boosterManager = boosterManager;
    }

    @SuppressWarnings("unused")
    @EventHandler
    private void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player player) {
            storyStorage.getActiveStory(player.getUniqueId()).thenAccept(story -> {
                if (story.isPresent()) {
                    if (story.get().character() == Character.SURVIVALIST) {
                        // Survivalist - Hunger decreases slower
                        if (player.getFoodLevel() > event.getFoodLevel()) {
                            int average = (player.getFoodLevel() + event.getFoodLevel()) / 2;
                            event.setFoodLevel(average);
                        }
                    } else if (story.get().character() == Character.CHEF) {
                        // Chef - Raises effectiveness of all foods
                        int foodLevelIncrease = event.getFoodLevel() - player.getFoodLevel();
                        if (foodLevelIncrease > 0) {
                            int newFoodLevel = Math.min(20, Math.round(event.getFoodLevel() + 2));
                            event.setFoodLevel(newFoodLevel);
                        }
                    }
                }
            });

            // make hunger decrease slower if the user has LESS_HUNGER booster
            if (boosterManager.hasBooster(player, Booster.LESS_HUNGER)) {
                if (player.getFoodLevel() > event.getFoodLevel()) {
                    int average = (player.getFoodLevel() + event.getFoodLevel()) / 2;
                    event.setFoodLevel(average);
                }
            }
        }
    }

}
