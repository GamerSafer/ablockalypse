package com.gamersafer.minecraft.ablockalypse.story;

import com.gamersafer.minecraft.ablockalypse.Character;
import org.bukkit.Location;
import org.bukkit.event.entity.EntityDamageEvent;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

public record Story(int id, UUID playerUuid, Character character, String characterName, LocalDateTime startTime,
                    @Nullable LocalDateTime endTime, @Nullable LocalDateTime sessionStartTime, int survivalTime,
                    @Nullable EntityDamageEvent.DamageCause deathCause, @Nullable Location deathLocation) {


    public boolean isActive() {
        return endTime == null;
    }

    /**
     * Gets how many seconds the character has survived.
     *
     * @return the story duration
     */
    public int survivalTime() {
        if (sessionStartTime() == null) {
            return this.survivalTime;
        }
        int sessionSurvivalTime = (int) (LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) - sessionStartTime().toEpochSecond(ZoneOffset.UTC));
        return this.survivalTime + sessionSurvivalTime;
    }

}
