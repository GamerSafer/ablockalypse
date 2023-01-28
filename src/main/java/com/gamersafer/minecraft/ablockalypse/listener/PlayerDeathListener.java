package com.gamersafer.minecraft.ablockalypse.listener;

import com.gamersafer.minecraft.ablockalypse.AblockalypsePlugin;
import com.gamersafer.minecraft.ablockalypse.Character;
import com.gamersafer.minecraft.ablockalypse.database.api.StoryStorage;
import com.gamersafer.minecraft.ablockalypse.location.LocationManager;
import com.gamersafer.minecraft.ablockalypse.safehouse.BoosterManager;
import com.gamersafer.minecraft.ablockalypse.safehouse.Safehouse;
import com.gamersafer.minecraft.ablockalypse.safehouse.SafehouseManager;
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
import java.util.Optional;

public class PlayerDeathListener implements Listener {

    private final AblockalypsePlugin plugin;
    private final StoryStorage storyStorage;
    private final LocationManager locationManager;
    private final BoosterManager boosterManager;
    private final SafehouseManager safehouseManager;

    public PlayerDeathListener(AblockalypsePlugin plugin, StoryStorage storyStorage, LocationManager locationManager,
                               BoosterManager boosterManager, SafehouseManager safehouseManager) {
        this.plugin = plugin;
        this.storyStorage = storyStorage;
        this.locationManager = locationManager;
        this.boosterManager = boosterManager;
        this.safehouseManager = safehouseManager;
    }

    @SuppressWarnings("unused")
    @EventHandler
    private void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();

        // try to remove boosters
        boosterManager.removeBoosters(player);

        storyStorage.getActiveStory(player.getUniqueId()).thenAccept(story -> {
            if (story.isEmpty()) {
                // this shouldn't happen
                plugin.getLogger().warning("The player " + player.getName() + "/" + player.getUniqueId() + " died while they didn't have an active story");
            } else {
                // when player dies, lose inventory, claims, character, and experience + create new character. Respawn at hospital
                player.setExp(0);
                event.setShouldDropExperience(false);
                player.getActivePotionEffects().stream().map(PotionEffect::getType).forEach(player::removePotionEffect);
                player.setWalkSpeed(0.2f); // set default walking speed. it's changed for sprinters

                // end the story
                EntityDamageEvent.DamageCause deathCause = Objects.requireNonNull(player.getLastDamageCause()).getCause();
                Location deathLocation = player.getLocation();
                storyStorage.endStory(player.getUniqueId(), deathCause, deathLocation).thenRun(() -> plugin.getLogger().info("The player "
                                                                                                                             + player.getUniqueId() + " just completed a story as a " + story.get().character().name()));

                // dispatch story-end commands
                story.get().character().getCommandsOnStoryEnd().stream()
                        .map(cmd -> cmd.replace("{name}", player.getName())
                                .replace("{uuid}", player.getUniqueId().toString()))
                        .forEach(cmd -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd));

                // try to remove tamed wolf
                if (story.get().character() == Character.VETERINARIAN) {
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

                // remove the safehouse owned by the player if it exists. we can safely assume that if the player
                // doesn't have an active story, they don't have a safehouse
                Optional<Safehouse> safehouseOptional = safehouseManager.getSafehouseFromOwnerUuid(player.getUniqueId());
                safehouseOptional.map(Safehouse::removeOwnerKeepPrevious)
                        .map(Bukkit::getPlayer)
                        .map(Player::getName)
                        .ifPresent(safehouseManager::dispatchSafehouseLossCommands);
            }
        }).exceptionally(throwable -> {
            throwable.printStackTrace();
            return null;
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
