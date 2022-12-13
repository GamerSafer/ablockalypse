package com.gamersafer.minecraft.ablockalypse.listener;

import com.gamersafer.minecraft.ablockalypse.AblockalypsePlugin;
import com.gamersafer.minecraft.ablockalypse.Character;
import com.gamersafer.minecraft.ablockalypse.database.api.StoryStorage;
import com.gamersafer.minecraft.ablockalypse.location.LocationManager;
import io.papermc.lib.PaperLib;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.potion.PotionEffectType;

import java.util.Arrays;

public class PlayerJoinListener implements Listener {

    private final AblockalypsePlugin plugin;
    private final StoryStorage storyStorage;
    private final LocationManager locationManager;

    public PlayerJoinListener(AblockalypsePlugin plugin, StoryStorage storyStorage, LocationManager locationManager) {
        this.plugin = plugin;
        this.storyStorage = storyStorage;
        this.locationManager = locationManager;
    }

    @SuppressWarnings("unused")
    @EventHandler
    private void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // teleport the player to the spawnpoint if they don't have an active story
        storyStorage.getActiveStory(player.getUniqueId()).thenAccept(story -> {
            if (story.isEmpty()) {
                // we can assume the spawnpoint is present
                //noinspection OptionalGetWithoutIsPresent
                plugin.sync(() -> PaperLib.teleportAsync(player, locationManager.getNextSpawnPoint().get()));

                // remove all potion effects, there may have been a reset
                Arrays.stream(PotionEffectType.values()).forEach(player::removePotionEffect);
            }
            if (story.isEmpty() || story.get().character() != Character.SPRINTER) {
                // update walking speed. there may have been a reset when the player was offline
                player.setWalkSpeed(0.2f);
            }
        });
    }

}
