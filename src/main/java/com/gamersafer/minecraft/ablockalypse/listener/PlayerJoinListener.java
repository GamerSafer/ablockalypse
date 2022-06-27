package com.gamersafer.minecraft.ablockalypse.listener;

import com.gamersafer.minecraft.ablockalypse.AblockalypsePlugin;
import com.gamersafer.minecraft.ablockalypse.CharacterNametagManager;
import com.gamersafer.minecraft.ablockalypse.database.api.StoryStorage;
import com.gamersafer.minecraft.ablockalypse.location.LocationManager;
import io.papermc.lib.PaperLib;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {

    private final AblockalypsePlugin plugin;
    private final StoryStorage storyStorage;
    private final LocationManager locationManager;
    private final CharacterNametagManager nametagManager;

    public PlayerJoinListener(AblockalypsePlugin plugin, StoryStorage storyStorage, LocationManager locationManager,
                              CharacterNametagManager nametagManager) {
        this.plugin = plugin;
        this.storyStorage = storyStorage;
        this.locationManager = locationManager;
        this.nametagManager = nametagManager;
    }

    @SuppressWarnings("unused")
    @EventHandler
    private void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // teleport the player to the spawnpoint if he doesn't have an active story
        storyStorage.getActiveStory(player.getUniqueId()).thenAccept(story -> {
            if (story.isEmpty()) {
                // we can assume the spawnpoint is present
                //noinspection OptionalGetWithoutIsPresent
                plugin.sync(() -> PaperLib.teleportAsync(player, locationManager.getNextHospitalLoc().get()));
            }

            // update nametag
            nametagManager.updateTag(player, story.orElse(null));
        });
    }

}
