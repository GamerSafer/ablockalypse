package com.gamersafer.minecraft.ablockalypse.listener;

import com.gamersafer.minecraft.ablockalypse.AblockalypsePlugin;
import com.gamersafer.minecraft.ablockalypse.Character;
import com.gamersafer.minecraft.ablockalypse.database.api.StoryStorage;
import com.gamersafer.minecraft.ablockalypse.location.LocationManager;
import com.gamersafer.minecraft.ablockalypse.menu.CharacterSelectionMenu;
import com.gamersafer.minecraft.ablockalypse.menu.PastStoriesMenu;
import com.gamersafer.minecraft.ablockalypse.menu.SafehouseBoostersMenu;
import com.gamersafer.minecraft.ablockalypse.menu.SafehouseDoorMenu;
import com.gamersafer.minecraft.ablockalypse.safehouse.BoosterManager;
import com.gamersafer.minecraft.ablockalypse.safehouse.Safehouse;
import com.gamersafer.minecraft.ablockalypse.safehouse.SafehouseManager;
import com.gamersafer.minecraft.ablockalypse.story.OnboardingSessionData;
import io.papermc.lib.PaperLib;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.conversations.Conversation;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.ExactMatchConversationCanceller;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.ValidatingPrompt;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MenuListener implements Listener {


    private static Map<Integer, Character> slotToCharacter;
    private final AblockalypsePlugin plugin;
    private final StoryStorage storyStorage;
    private final LocationManager locationManager;
    private final SafehouseManager safehouseManager;
    private final BoosterManager boosterManager;

    public MenuListener(AblockalypsePlugin plugin, StoryStorage storyStorage, LocationManager locationManager,
                        SafehouseManager safehouseManager, BoosterManager boosterManager) {
        this.plugin = plugin;
        this.storyStorage = storyStorage;
        this.locationManager = locationManager;
        this.safehouseManager = safehouseManager;
        this.boosterManager = boosterManager;

        reload();
    }

    public static void reload() {
        slotToCharacter = Arrays.stream(Character.values())
                .collect(Collectors.toMap(Character::getMenuIndex, Function.identity()));
    }


    @SuppressWarnings("unused")
    @EventHandler
    private void onInventoryClick(InventoryClickEvent event) {

        // character selection menu
        if (event.getInventory().getHolder() instanceof CharacterSelectionMenu characterSelectionMenu) {
            event.setCancelled(true);
            Character clickedCharacter = slotToCharacter.get(event.getRawSlot());

            if (clickedCharacter != null) {
                characterSelectionMenu.pickCharacter((Player) event.getWhoClicked(), storyStorage, locationManager, plugin, clickedCharacter);
            }
        } else if (event.getInventory().getHolder() instanceof PastStoriesMenu pastStoriesMenu) {
            // past stories menu
            event.setCancelled(true);

            if (event.getCurrentItem() != null && !event.getCurrentItem().equals(PastStoriesMenu.GLASS_PANE)) {
                // handle previous/next page navigation buttons
                if (event.getRawSlot() == PastStoriesMenu.PREVIOUS_PAGE_SLOT) {
                    pastStoriesMenu.openPreviousPage((Player) event.getWhoClicked());
                } else if (event.getRawSlot() == PastStoriesMenu.PREVIOUS_PAGE_SLOT) {
                    pastStoriesMenu.openNextPage((Player) event.getWhoClicked());
                }
            }
        } else if (event.getInventory().getHolder() instanceof SafehouseDoorMenu doorMenu) {
            // door upgrades menu
            event.setCancelled(true);

            if (event.getCurrentItem() != null) {
                Safehouse safehouse = doorMenu.getSafehouse();
                int currentDoorLevel = safehouse.getDoorLevel();
                Player player = (Player) event.getWhoClicked();
                if ((event.getRawSlot() == SafehouseDoorMenu.SLOT_LEVEL_2 && currentDoorLevel == 1)
                        || (event.getRawSlot() == SafehouseDoorMenu.SLOT_LEVEL_3 && currentDoorLevel == 2)) {
                    boolean success = safehouseManager.tryUpgradeDoor(safehouse, player);
                    if (success) {
                        player.sendMessage(plugin.getMessage("safehouse-upgrade-door-success")
                                .replace("{level}", Integer.toString(safehouse.getDoorLevel() + 1)));
                        player.closeInventory();
                    } else {
                        player.sendMessage(plugin.getMessage("safehouse-upgrade-door-failure"));
                    }
                } else if (event.getRawSlot() == SafehouseDoorMenu.SLOT_LEVEL_3 && currentDoorLevel < 2) {
                    player.sendMessage(plugin.getMessage("safehouse-upgrade-door-level-2-first"));
                }
            }
        } else if (event.getInventory().getHolder() instanceof SafehouseBoostersMenu boostersMenu) {
            // boosters menu
            event.setCancelled(true);

            if (event.getCurrentItem() != null) {
                boostersMenu.handleClick(event.getRawSlot(), safehouseManager, boosterManager);
            }
        }
    }


}
