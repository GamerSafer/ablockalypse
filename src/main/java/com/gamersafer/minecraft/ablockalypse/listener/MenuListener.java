package com.gamersafer.minecraft.ablockalypse.listener;

import com.gamersafer.minecraft.ablockalypse.AblockalypsePlugin;
import com.gamersafer.minecraft.ablockalypse.Character;
import com.gamersafer.minecraft.ablockalypse.database.api.StoryStorage;
import com.gamersafer.minecraft.ablockalypse.menu.CharacterSelectionMenu;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MenuListener implements Listener {

    private static Map<Integer, Character> slotToCharacter;
    private final AblockalypsePlugin plugin;
    private final StoryStorage storyStorage;

    public MenuListener(AblockalypsePlugin plugin, StoryStorage storyStorage) {
        this.plugin = plugin;
        this.storyStorage = storyStorage;

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
        if (CharacterSelectionMenu.isEquals(event.getInventory())) {
            Character clickedCharacter = slotToCharacter.get(event.getRawSlot());

            if (clickedCharacter != null) {
                Player player = (Player) event.getWhoClicked();

                // the player clicked a character. make sure he doesn't have an active story
                storyStorage.getActiveStory(player.getUniqueId())
                        .thenAccept(story -> {
                            if (story.isPresent()) {
                                // this should never happen to regular players
                                // only staff members are able to open the menu even if they have already started a story
                                player.sendMessage(plugin.getMessage("character-selector-already"));
                                return;
                            }

                            // todo start onboarding process
                        });

            }
        }
    }

}
