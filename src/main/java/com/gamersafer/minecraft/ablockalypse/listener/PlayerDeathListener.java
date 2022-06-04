package com.gamersafer.minecraft.ablockalypse.listener;

import com.gamersafer.minecraft.ablockalypse.AblockalypsePlugin;
import com.gamersafer.minecraft.ablockalypse.Character;
import com.gamersafer.minecraft.ablockalypse.database.api.StoryStorage;
import com.gamersafer.minecraft.ablockalypse.location.LocationManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.potion.PotionEffect;

import java.util.Objects;

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
                event.getDrops().clear();
                player.setExp(0);
                event.setShouldDropExperience(false);
                player.getActivePotionEffects().stream().map(PotionEffect::getType).forEach(player::removePotionEffect);
                player.setWalkSpeed(0.2f); // set default walking speed. it's changed for sprinters

                // end the story
                EntityDamageEvent.DamageCause deathCause = Objects.requireNonNull(player.getLastDamageCause()).getCause();
                Location deathLocation = player.getLocation();
                storyStorage.endStory(player.getUniqueId(), deathCause, deathLocation).thenRun(() -> plugin.getLogger().info("The player "
                        + player.getUniqueId() + " just completed a story as a " + story.get().character().name()));

                // try to remove tamed wolf
                if (story.get().character() == Character.DOG_WALKER) {
                    // only dog walkers can tame mobs and they can have max 1 wolf
                    for (World world : Bukkit.getWorlds()) {
                        for (Entity wolfEntity : world.getEntitiesByClasses(Wolf.class)) {
                            if (((Tameable) wolfEntity).isTamed() && player.getUniqueId().equals(((Tameable) wolfEntity).getOwner().getUniqueId())) {
                                wolfEntity.remove();
                                break;
                            }
                        }
                    }
                }

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
        event.setRespawnLocation(locationManager.getNextHospitalLoc().orElseThrow(() ->
                new IllegalStateException("Unable to make " + player.getUniqueId() + " respawn at the hospital. Its location is not set")));

    }

}
