package com.gamersafer.minecraft.ablockalypse.story;

import com.gamersafer.minecraft.ablockalypse.Character;
import org.bukkit.Location;
import org.bukkit.event.entity.EntityDamageEvent;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.Temporal;
import java.util.UUID;

public record Story(int id, UUID playerUuid, Character character, String characterName, LocalDateTime startTime,
                    @Nullable LocalDateTime endTime, @Nullable EntityDamageEvent.DamageCause deathCause,
                    @Nullable Location deathLocation) {


    public boolean isActive() {
        return endTime == null;
    }

    /**
     * Gets how long the story has lasted or is lasting.
     *
     * @return the story duration
     */
    public Duration survivalTime() {
        Temporal end = endTime != null ? endTime : LocalDateTime.now();
        return Duration.between(startTime, end);
    }

}
