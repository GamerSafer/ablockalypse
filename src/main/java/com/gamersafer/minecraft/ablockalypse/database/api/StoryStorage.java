package com.gamersafer.minecraft.ablockalypse.database.api;

import com.gamersafer.minecraft.ablockalypse.Character;
import com.gamersafer.minecraft.ablockalypse.story.OnboardingSessionData;
import com.gamersafer.minecraft.ablockalypse.story.Story;
import com.google.common.base.Preconditions;
import org.bukkit.Location;
import org.bukkit.event.entity.EntityDamageEvent;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface StoryStorage {

    /**
     * Tries to get active story of a player. See {@link Story#isActive()}.
     *
     * @param playerUuid the UUID of the player
     * @return their active story or an empty optional if they don't have a selected character
     */
    CompletableFuture<Optional<Story>> getActiveStory(UUID playerUuid);

    /**
     * Gets all stories started by the given player, including the active one ({@link #getActiveStory(UUID)})
     *
     * @param playerUuid the UUID of the player
     * @return all e stories started by the player
     */
    CompletableFuture<List<Story>> getAllStories(UUID playerUuid);

    /**
     * Starts a new story.
     *
     * @param onboardingSessionData the data of the story start
     * @return the newly created story
     */
    default CompletableFuture<Story> startNewStory(OnboardingSessionData onboardingSessionData) {
        Preconditions.checkArgument(onboardingSessionData.isComplete());
        return startNewStory(onboardingSessionData.getPlayerUuid(),
                onboardingSessionData.getCharacter(),
                onboardingSessionData.getName(),
                LocalDateTime.now());
    }

    /**
     * Starts a new story with the given data.
     *
     * @param playerUuid    the of the player who is starting the story
     * @param character     the character of the story
     * @param characterName the display name of the character
     * @param startTime     the story start time
     * @return the newly created story
     */
    CompletableFuture<Story> startNewStory(UUID playerUuid, Character character, String characterName, LocalDateTime startTime);

    default CompletableFuture<Void> endStory(UUID playerUuid, EntityDamageEvent.DamageCause deathCause, Location deathLocation) {
        return this.endStory(playerUuid, deathCause, deathLocation, LocalDateTime.now());
    }

    /**
     * Ends the active story of the given player.
     *
     * @param playerUuid    the UUID of the player whose active story to end
     * @param deathCause    the cause of death
     * @param deathLocation the location where the characted died
     * @param endTime       the story end time
     * @return a CompletableFuture that will complete once the story has been completed
     */
    CompletableFuture<Void> endStory(UUID playerUuid, EntityDamageEvent.DamageCause deathCause, Location deathLocation, LocalDateTime endTime);

    /**
     * Gets the playtime of the given player.
     * The playtime is the sum of the survival time of all the stories started by a player. See {@link Story#survivalTime()}
     *
     * @param playerUuid the UUID of the player
     * @return their playtime
     */
    CompletableFuture<Duration> getPlaytime(UUID playerUuid);

    /**
     * Attempts to update the survival time stored in the database.
     *
     * @param playerUuid the UUID of the player
     * @return a CompletableFuture that will complete once the update has been completed or if the given
     * player doesn't have an active story
     */
    default CompletableFuture<Void> updateSurvivalTime(UUID playerUuid) {
        return getActiveStory(playerUuid).thenAccept(story -> {
            // update the survival time in the database if present
            story.ifPresent(this::updateSurvivalTime);
        });
    }

    /**
     * Updates the survival time stored in the database. See {@link Story#survivalTime()}.
     *
     * @param story the story to save
     * @return a CompletableFuture that will complete once the update has been completed
     */
    CompletableFuture<Void> updateSurvivalTime(Story story);

    void shutdown();

}
