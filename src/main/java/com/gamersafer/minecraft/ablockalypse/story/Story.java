package com.gamersafer.minecraft.ablockalypse.story;

import com.gamersafer.minecraft.ablockalypse.Character;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.UUID;

public record Story(int id, UUID playerUuid, Character character, String characterName, LocalDateTime startTime,
                    @Nullable LocalDateTime endTime) {


    public boolean isActive() {
        return endTime == null;
    }


}
