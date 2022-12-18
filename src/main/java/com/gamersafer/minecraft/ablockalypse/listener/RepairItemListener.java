package com.gamersafer.minecraft.ablockalypse.listener;

import com.gamersafer.minecraft.ablockalypse.AblockalypsePlugin;
import com.gamersafer.minecraft.ablockalypse.Character;
import com.gamersafer.minecraft.ablockalypse.database.api.StoryStorage;
import com.gamersafer.minecraft.ablockalypse.util.ItemUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class RepairItemListener implements Listener {

    private final AblockalypsePlugin plugin;
    private final StoryStorage storyStorage;

    public RepairItemListener(AblockalypsePlugin plugin, StoryStorage storyStorage) {
        this.plugin = plugin;
        this.storyStorage = storyStorage;
    }

    @SuppressWarnings("unused")
    @EventHandler
    private void onItemRepair(PrepareItemCraftEvent event) {
        Player player;
        try {
            player = (Player) event.getInventory().getViewers().get(0);
        } catch (IndexOutOfBoundsException ignore) {
            return;
        }
        List<Material> materials = Arrays.stream(event.getInventory().getContents())
                .filter(Objects::nonNull)
                .map(ItemStack::getType)
                .toList();

        boolean containsArmor = materials.stream().anyMatch(ItemUtil.ARMOR::contains);
        boolean containsWeapon = materials.stream().anyMatch(ItemUtil.WEAPONS::contains);

        if (containsArmor) {
            // check whether the player is a farmer. that's the only character allowed to repair armor
            storyStorage.getActiveStory(player.getUniqueId()).thenAccept(story -> {
                if (story.isEmpty() || story.get().character() != Character.TAILOR) {
                    // cancel the event and send feedback message
                    event.getInventory().setResult(null);
                    plugin.sendMessage(player, "anvil-armor-no");
                }
            });
        } else if (containsWeapon) {
            // check whether the player is a mechanic. that's the only character allowed to repair weapons
            storyStorage.getActiveStory(player.getUniqueId()).thenAccept(story -> {
                if (story.isEmpty() || story.get().character() != Character.REPAIR_TECH) {
                    // cancel the event and send feedback message
                    event.getInventory().setResult(null);
                    plugin.sendMessage(player, "anvil-weapon-no");
                }
            });
        }
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
                if (story.isEmpty() || story.get().character() != Character.TAILOR) {
                    // cancel the event and send feedback message
                    event.getInventory().setRepairCost(0);
                    event.setResult(null);
                    plugin.sendMessage(player, "anvil-armor-no");
                }
            });
        } else if (ItemUtil.WEAPONS.contains(firstItem.getType())) {
            // check whether the player is a mechanic. that's the only character allowed to repair weapons
            storyStorage.getActiveStory(player.getUniqueId()).thenAccept(story -> {
                if (story.isEmpty() || story.get().character() != Character.REPAIR_TECH) {
                    // cancel the event and send feedback message
                    event.getInventory().setRepairCost(0);
                    event.setResult(null);
                    plugin.sendMessage(player, "anvil-weapon-no");
                }
            });
        }
    }

}
