package com.gamersafer.minecraft.ablockalypse.database.api;

import com.gamersafer.minecraft.ablockalypse.Character;
import com.gamersafer.minecraft.ablockalypse.story.OnboardingSessionData;
import com.gamersafer.minecraft.ablockalypse.story.Story;
import com.google.common.base.Preconditions;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface StoryStorage {

    // TODO javadocs

    /**
     * Tries to get active story of a player. See {@link Story#isActive()}.
     *
     * @param playerUuid The UUID of the player
     * @return his active story or an empty optional if he doesn't have a selected character
     */
    CompletableFuture<Optional<Story>> getActiveStory(UUID playerUuid);

    CompletableFuture<List<Story>> getAllStories(UUID playerUuid);

    default CompletableFuture<Story> startNewStory(OnboardingSessionData onboardingSessionData) {
        Preconditions.checkArgument(onboardingSessionData.isComplete());
        return startNewStory(onboardingSessionData.getPlayerUuid(),
                onboardingSessionData.getCharacter(),
                onboardingSessionData.getName(),
                LocalDateTime.now());
    }

    CompletableFuture<Story> startNewStory(UUID playerUuid, Character character, String characterName, LocalDateTime startTime);

    CompletableFuture<Void> endStory(UUID playerUuid, LocalDateTime endTime);

    void shutdown();

}
