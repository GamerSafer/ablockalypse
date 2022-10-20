package com.gamersafer.minecraft.ablockalypse.story;

import com.gamersafer.minecraft.ablockalypse.Character;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.entity.EntityDamageEvent;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;

public final class Story {
    private final int id;
    private final UUID playerUuid;
    private final Character character;
    private final String characterName;
    private final LocalDateTime startTime;
    private final @Nullable LocalDateTime endTime;
    private final @Nullable LocalDateTime sessionStartTime;
    private final int survivalTime;
    private final EntityDamageEvent.@Nullable DamageCause deathCause;
    private final @Nullable Location deathLocation;
    private int level;

    public Story(int id, UUID playerUuid, Character character, String characterName, int level, LocalDateTime startTime,
                 @Nullable LocalDateTime endTime, @Nullable LocalDateTime sessionStartTime, int survivalTime,
                 @Nullable EntityDamageEvent.DamageCause deathCause, @Nullable Location deathLocation) {
        this.id = id;
        this.playerUuid = playerUuid;
        this.character = character;
        this.characterName = characterName;
        this.level = level;
        this.startTime = startTime;
        this.endTime = endTime;
        this.sessionStartTime = sessionStartTime;
        this.survivalTime = survivalTime;
        this.deathCause = deathCause;
        this.deathLocation = deathLocation;
    }

    public boolean isActive() {
        return endTime == null;
    }

    /**
     * Gets how many seconds the character has survived.
     *
     * @return the story duration
     */
    public int survivalTime() {
        if (sessionStartTime() == null || Bukkit.getPlayer(playerUuid) == null) {
            return this.survivalTime;
        }
        int sessionSurvivalTime = (int) (LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) - sessionStartTime().toEpochSecond(ZoneOffset.UTC));
        return this.survivalTime + sessionSurvivalTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Story story = (Story) o;
        return id == story.id && playerUuid.equals(story.playerUuid) && character == story.character && characterName.equals(story.characterName) && level == story.level && startTime.equals(story.startTime) && Objects.equals(endTime, story.endTime) && deathCause == story.deathCause && Objects.equals(deathLocation, story.deathLocation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, playerUuid, character, characterName, level, startTime, endTime, deathCause, deathLocation);
    }

    public int id() {
        return id;
    }

    public UUID playerUuid() {
        return playerUuid;
    }

    public Character character() {
        return character;
    }

    public String characterName() {
        return characterName;
    }

    public int level() {
        return level;
    }

    public int increaseLevel() {
        return ++level;
    }

    public LocalDateTime startTime() {
        return startTime;
    }

    public @Nullable LocalDateTime endTime() {
        return endTime;
    }

    public @Nullable LocalDateTime sessionStartTime() {
        return sessionStartTime;
    }

    public EntityDamageEvent.@Nullable DamageCause deathCause() {
        return deathCause;
    }

    public @Nullable Location deathLocation() {
        return deathLocation;
    }

    @Override
    public String toString() {
        return "Story[" +
                "id=" + id + ", " +
                "playerUuid=" + playerUuid + ", " +
                "character=" + character + ", " +
                "characterName=" + characterName + ", " +
                "level=" + level + ", " +
                "startTime=" + startTime + ", " +
                "endTime=" + endTime + ", " +
                "sessionStartTime=" + sessionStartTime + ", " +
                "survivalTime=" + survivalTime + ", " +
                "deathCause=" + deathCause + ", " +
                "deathLocation=" + deathLocation + ']';
    }

}
