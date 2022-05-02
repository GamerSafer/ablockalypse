package com.gamersafer.minecraft.ablockalypse.listener;

import com.gamersafer.minecraft.ablockalypse.AblockalypsePlugin;
import com.gamersafer.minecraft.ablockalypse.Character;
import com.gamersafer.minecraft.ablockalypse.database.api.StoryStorage;
import com.gamersafer.minecraft.ablockalypse.util.ItemUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.ItemStack;

@SuppressWarnings("ClassCanBeRecord")
public class PrepareAnvilListener implements Listener {

    private final AblockalypsePlugin plugin;
    private final StoryStorage storyStorage;

    public PrepareAnvilListener(AblockalypsePlugin plugin, StoryStorage storyStorage) {
        this.plugin = plugin;
        this.storyStorage = storyStorage;
    }

    @SuppressWarnings("unused")
    @EventHandler
    private void onAnvilPrepare(PrepareAnvilEvent event) {

        // Farmer - Can repair armour
        // Mechanic - Can repair weapons

        ItemStack firstItem = event.getInventory().getFirstItem();
        if (firstItem == null) {
            return;
        }
        Player player = (Player) event.getView().getPlayer();

        if (ItemUtil.ARMOR.contains(firstItem.getType())) {
            // check whether the player is a farmer. that's the only character allowed to repair armor
            storyStorage.getActiveStory(player.getUniqueId()).thenAccept(story -> {
                if (story.isEmpty() || story.get().character() != Character.FARMER) {

                    // todo test does this block the anvil usage? do we need to block the thread until the future is complete?
                    event.getInventory().setRepairCost(0);
                }
            });
        } else if (ItemUtil.WEAPONS.contains(firstItem.getType())) {
            // check whether the player is a mechanic. that's the only character allowed to repair weapons

            // todo copy from above if it works
        }
    }

}
