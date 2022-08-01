package com.gamersafer.minecraft.ablockalypse.story;

import com.gamersafer.minecraft.ablockalypse.Character;
import com.gamersafer.minecraft.ablockalypse.database.api.StoryStorage;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.bukkit.Location;
import org.bukkit.event.entity.EntityDamageEvent;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class StoryCache implements StoryStorage {

    private final StoryStorage base;

    private final AsyncLoadingCache<UUID, Optional<Story>> cacheActive;

    private final AsyncLoadingCache<UUID, List<Story>> cacheAll;

    public StoryCache(StoryStorage base) {
        this.base = base;
        cacheActive = Caffeine.newBuilder().buildAsync((uuid, executor) -> base.getActiveStory(uuid));
        cacheAll = Caffeine.newBuilder().buildAsync((uuid, executor) -> base.getAllStories(uuid));
    }

    @Override
    public CompletableFuture<Optional<Story>> getActiveStory(UUID playerUuid) {
        return cacheActive.get(playerUuid);
    }

    @Override
    public CompletableFuture<List<Story>> getAllStories(UUID playerUuid) {
        return cacheAll.get(playerUuid).thenCompose(stories -> {
            CompletableFuture<Optional<Story>> activeStoryFuture = cacheActive.getIfPresent(playerUuid);
            if (activeStoryFuture == null) {
                return CompletableFuture.completedFuture(stories);
            } else {
                return activeStoryFuture.thenApply(activeStory -> {
                    if (activeStory.isPresent()) {
                        // replace with the active story containing the updated session start time
                        stories.removeIf(story -> story.id() == activeStory.get().id());
                        stories.add(activeStory.get());
                    }
                    return stories;
                });
            }
        });
    }

    @Override
    public CompletableFuture<Story> startNewStory(UUID playerUuid, Character character, String characterName, LocalDateTime startTime) {
        return base.startNewStory(playerUuid, character, characterName, startTime).thenApply(story -> {
            cacheActive.synchronous().put(playerUuid, Optional.ofNullable(story));

            List<Story> list = cacheAll.synchronous().getIfPresent(playerUuid);
            if (list != null) {
                //data has been loaded by base once, so we can update
                synchronized (list) { //yes this is intentional
                    list.add(story);
                }
            }
            return story;
        }).exceptionally(throwable -> {
            throwable.printStackTrace();
            return null;
        });
    }

    @Override
    public CompletableFuture<Void> endStory(UUID playerUuid, EntityDamageEvent.DamageCause deathCause, Location deathLocation, LocalDateTime endTime) {
        return updateSurvivalTime(playerUuid).thenRun(() -> base.endStory(playerUuid, deathCause, deathLocation, endTime).thenRun(() -> {

            // invalidate cache
            cacheActive.synchronous().invalidate(playerUuid);
            cacheAll.synchronous().invalidate(playerUuid);
        }).exceptionally(throwable -> {
            throwable.printStackTrace();
            return null;
        }));
    }

    @Override
    public CompletableFuture<Duration> getPlaytime(UUID playerUuid) {
        return cacheAll.get(playerUuid).thenApply(stories -> stories.stream()
                        .mapToInt(Story::survivalTime)
                        .sum()
                ).thenApply(Duration::ofSeconds)
                .exceptionally(throwable -> {
                    throwable.printStackTrace();
                    return Duration.ZERO;
                });
    }

    @Override
    public CompletableFuture<Void> updateSurvivalTime(Story story) {
        return base.updateSurvivalTime(story).thenRun(() -> {
            cacheActive.synchronous().invalidate(story.playerUuid());
            cacheAll.synchronous().invalidate(story.playerUuid());
        });
    }

    @Override
    public CompletableFuture<List<Story>> getTopSurvivalTimeStories(int count) {
        return base.getTopSurvivalTimeStories(count).thenApply(stories -> {
            List<Story> topSurvivalTimeStories = new ArrayList<>(stories);

            cacheActive.synchronous().asMap().values().forEach(story -> {
                if (story.isPresent()) {
                    topSurvivalTimeStories.removeIf(s -> s.id() == story.get().id());
                    topSurvivalTimeStories.add(story.get());
                }
            });

            topSurvivalTimeStories.sort(Comparator.comparingInt(Story::survivalTime).reversed());
            return topSurvivalTimeStories;
        });
    }

    @Override
    public void shutdown() {
        base.shutdown();
    }

}
