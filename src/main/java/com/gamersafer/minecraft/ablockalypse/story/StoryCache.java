package com.gamersafer.minecraft.ablockalypse.story;

import com.gamersafer.minecraft.ablockalypse.Character;
import com.gamersafer.minecraft.ablockalypse.database.api.StoryStorage;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.time.LocalDateTime;
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
        return cacheAll.get(playerUuid);
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
        });
    }

    @Override
    public CompletableFuture<Void> endStory(UUID playerUuid, LocalDateTime endTime) {
        return base.endStory(playerUuid, endTime).thenRun(() -> {

            cacheActive.synchronous().invalidate(playerUuid);

            // todo update the endDate of the story or invalidate cache all

        });
    }

    @Override
    public void shutdown() {
        base.shutdown();
    }

}
