package com.gamersafer.minecraft.ablockalypse.listener;

import com.gamersafer.minecraft.ablockalypse.AblockalypsePlugin;
import com.gamersafer.minecraft.ablockalypse.database.api.StoryStorage;
import com.gamersafer.minecraft.ablockalypse.location.LocationManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.potion.PotionEffect;

@SuppressWarnings("ClassCanBeRecord")
public class PlayerDeathListener implements Listener {

    private final AblockalypsePlugin plugin;
    private final StoryStorage storyStorage;
    private final LocationManager locationManager;

    public PlayerDeathListener(AblockalypsePlugin plugin, StoryStorage storyStorage, LocationManager locationManager) {
        this.plugin = plugin;
        this.storyStorage = storyStorage;
        this.locationManager = locationManager;
    }

    @SuppressWarnings("unused")
    @EventHandler
    private void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();

        storyStorage.getActiveStory(player.getUniqueId()).thenAccept(story -> {
            if (story.isEmpty()) {
                // this shouldn't happen
                plugin.getLogger().warning("The player " + player.getName() + "/" + player.getUniqueId() + " died while he didn't have an active story");
            } else {
                // when player dies, lose inventory, claims, character, and experience + create new character. Respawn at hospital
                player.getInventory().clear();
                player.setExp(0);
                player.getActivePotionEffects().stream().map(PotionEffect::getType).forEach(player::removePotionEffect);

                // end the story
                storyStorage.endStory(player.getUniqueId()).thenRun(() -> plugin.getLogger().info("The player "
                        + player.getUniqueId() + " just completed a story as a " + story.get().character().name()));

                // todo integrate with the claims plugin and remove all claims
            }
        });
    }

    @SuppressWarnings("unused")
    @EventHandler
    private void onPlayerRespawn(PlayerRespawnEvent event) {
        // todo use PlayerPostRespawnEvent is some changes don't persist

        Player player = event.getPlayer();

        // respawn at the hospital
        event.setRespawnLocation(locationManager.getHospital().orElseThrow(() ->
                new IllegalStateException("Unable to make " + player.getUniqueId() + " respawn at the hospital. Its location is not set")));

    }

}
