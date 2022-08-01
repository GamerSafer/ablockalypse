package com.gamersafer.minecraft.ablockalypse.papi;

import com.gamersafer.minecraft.ablockalypse.Character;
import com.gamersafer.minecraft.ablockalypse.database.api.StoryStorage;
import com.gamersafer.minecraft.ablockalypse.leaderboard.SurvivalTimeLeaderboard;
import com.gamersafer.minecraft.ablockalypse.story.Story;
import com.gamersafer.minecraft.ablockalypse.util.FormatUtil;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class AblockalypsePAPIExpansion extends PlaceholderExpansion {

    private final StoryStorage storyStorage;
    private final SurvivalTimeLeaderboard survivalTimeLeaderboard;

    public AblockalypsePAPIExpansion(StoryStorage storyStorage, SurvivalTimeLeaderboard survivalTimeLeaderboard) {
        this.storyStorage = storyStorage;
        this.survivalTimeLeaderboard = survivalTimeLeaderboard;
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
        String value = switch (params.toLowerCase()) {
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

            default -> null;
        };

        // leaderboard (e.g. alltop_survivaltime_1_name)
        if (value == null && params.toLowerCase().startsWith("alltop_survivaltime_")) {
            String[] split = params.toLowerCase().split("_");

            int position;
            try {
                position = Integer.parseInt(split[2]);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid leaderboard placeholder number: " + params);
            }

            Optional<Story> storyEntry = survivalTimeLeaderboard.get(position - 1);
            value = switch (split[3]) {
                case "name" -> storyEntry.map(Story::playerUuid)
                        .map(uuid -> Bukkit.getOfflinePlayer(uuid).getName()).orElse("None");
                case "playtime" -> FormatUtil.format(storyEntry.map(Story::survivalTime).orElse(0));
                case "character" -> storyEntry.map(Story::character).map(Character::getDisplayName).orElse("None");
                default -> throw new IllegalArgumentException("Invalid leaderboard placeholder: " + params);
            };
        }

        return value;
    }
}
