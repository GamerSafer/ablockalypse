package com.gamersafer.minecraft.ablockalypse.papi;

import com.gamersafer.minecraft.ablockalypse.Character;
import com.gamersafer.minecraft.ablockalypse.database.api.StoryStorage;
import com.gamersafer.minecraft.ablockalypse.story.Story;
import com.gamersafer.minecraft.ablockalypse.util.FormatUtil;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AblockalypsePAPIExpansion extends PlaceholderExpansion {

    private final StoryStorage storyStorage;

    public AblockalypsePAPIExpansion(StoryStorage storyStorage) {
        this.storyStorage = storyStorage;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "ablockalypse";
    }

    @Override
    public @NotNull String getAuthor() {
        return "GamerSafer";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        return switch (params.toLowerCase()) {
            case "playtime" -> FormatUtil.format(storyStorage.getPlaytime(player.getUniqueId()).join());

            case "survivaltime" -> storyStorage.getActiveStory(player.getUniqueId()).join()
                    .map(Story::survivalTime)
                    .map(FormatUtil::format)
                    .orElse("None");

            case "character" -> storyStorage.getActiveStory(player.getUniqueId()).join()
                    .map(Story::character)
                    .map(Character::getDisplayName)
                    .orElse("None");

            case "displayname" -> storyStorage.getActiveStory(player.getUniqueId()).join()
                    .map(Story::characterName)
                    .orElse("Player");
            default -> null; // unknown placeholder
        };
    }
}
