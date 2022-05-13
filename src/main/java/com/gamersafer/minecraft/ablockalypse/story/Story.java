package com.gamersafer.minecraft.ablockalypse.story;

import com.gamersafer.minecraft.ablockalypse.Character;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.Temporal;
import java.util.UUID;

public record Story(int id, UUID playerUuid, Character character, String characterName, LocalDateTime startTime,
                    @Nullable LocalDateTime endTime) {


    public boolean isActive() {
        return endTime == null;
    }

    /**
     * Gets how long the story has lasted or is lasting.
     *
     * @return the story duration
     */
    public Duration survivalTime() {
        Temporal end = endTime != null ? endTime : Instant.now();
        return Duration.between(startTime, end);
    }

}
