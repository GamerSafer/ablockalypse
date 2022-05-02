package com.gamersafer.minecraft.ablockalypse.story;

import com.gamersafer.minecraft.ablockalypse.Character;

import java.util.UUID;

public class OnboardingSessionData {

    private final UUID playerUuid;
    private final Character character;
    private boolean characterConfirmed;
    private String name;

    public OnboardingSessionData(UUID playerUuid, Character character) {
        this.playerUuid = playerUuid;
        this.character = character;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public Character getCharacter() {
        return character;
    }

    public boolean isCharacterConfirmed() {
        return characterConfirmed;
    }

    public void setCharacterConfirmed() {
        this.characterConfirmed = true;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isComplete() {
        return characterConfirmed && name != null;
    }

    @Override
    public String toString() {
        return "OnboardingSessionData{" +
                "playerUuid=" + playerUuid +
                ", character=" + character +
                ", characterConfirmed=" + characterConfirmed +
                ", name='" + name + '\'' +
                '}';
    }
}
