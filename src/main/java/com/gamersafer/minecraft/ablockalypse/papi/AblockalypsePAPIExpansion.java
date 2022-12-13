package com.gamersafer.minecraft.ablockalypse.papi;

import com.gamersafer.minecraft.ablockalypse.Character;
import com.gamersafer.minecraft.ablockalypse.database.api.StoryStorage;
import com.gamersafer.minecraft.ablockalypse.leaderboard.SurvivalTimeLeaderboard;
import com.gamersafer.minecraft.ablockalypse.safehouse.Safehouse;
import com.gamersafer.minecraft.ablockalypse.safehouse.SafehouseManager;
import com.gamersafer.minecraft.ablockalypse.story.Story;
import com.gamersafer.minecraft.ablockalypse.util.FormatUtil;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class AblockalypsePAPIExpansion extends PlaceholderExpansion {

    private static final List<String> LEADERBOARD_PLACEHOLDERS_POSSIBLE_ARGS = List.of("name", "playtime", "character");

    private final StoryStorage storyStorage;
    private final SurvivalTimeLeaderboard survivalTimeLeaderboard;
    private final SafehouseManager safehouseManager;

    public AblockalypsePAPIExpansion(StoryStorage storyStorage, SurvivalTimeLeaderboard survivalTimeLeaderboard, SafehouseManager safehouseManager) {
        this.storyStorage = storyStorage;
        this.survivalTimeLeaderboard = survivalTimeLeaderboard;
        this.safehouseManager = safehouseManager;
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

            case "level" -> storyStorage.getActiveStory(player.getUniqueId()).join()
                    .map(Story::level)
                    .map(String::valueOf)
                    .orElse("0");

            case "safehouse_door_lvl" -> safehouseManager.getSafehouseFromOwnerUuid(player.getUniqueId())
                    .map(Safehouse::getDoorLevel)
                    .map(String::valueOf)
                    .orElse("0");

            default -> null;
        };

        // global leaderboard (e.g. alltop_survivaltime_1_name)
        if (value == null)
            if (params.toLowerCase().startsWith("ptop_survivaltime_")) {
                String[] split = params.toLowerCase().split("_");
                String secondArg = split[2];
                if (LEADERBOARD_PLACEHOLDERS_POSSIBLE_ARGS.contains(secondArg)) {
                    Optional<Story> storyEntry = storyStorage.getTopSurvivalTimePersonal(player.getUniqueId()).join();
                    value = parseTop(storyEntry, secondArg);
                } else {
                    Character character;
                    try {
                        character = Character.valueOf(secondArg.toUpperCase());
                    } catch (IllegalArgumentException invalidCharacterType) {
                        throw new IllegalArgumentException("Invalid personal top placeholder character type: " + params);
                    }
                    Optional<Story> storyEntry = storyStorage.getTopSurvivalTimePersonalByCharacter(character, player.getUniqueId()).join();
                    value = parseTop(storyEntry, split[4]);
                }
            } else if (params.toLowerCase().startsWith("alltop_survivaltime_")) {
                String[] split = params.toLowerCase().split("_");

                if (LEADERBOARD_PLACEHOLDERS_POSSIBLE_ARGS.contains(split[3])) {
                    int position;
                    try {
                        position = Integer.parseInt(split[2]);
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Invalid leaderboard placeholder number: " + params);
                    }

                    Optional<Story> storyEntry = survivalTimeLeaderboard.get(position - 1);
                    value = parseTop(storyEntry, split[3]);
                } else {
                    Character character;
                    try {
                        character = Character.valueOf(split[2].toUpperCase());
                    } catch (IllegalArgumentException invalidCharacterType) {
                        throw new IllegalArgumentException("Invalid personal top placeholder character type: " + params);
                    }
                    int position;
                    try {
                        position = Integer.parseInt(split[3]);
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Invalid leaderboard placeholder number: " + params);
                    }
                    Optional<Story> storyEntry = survivalTimeLeaderboard.get(character, position - 1);
                    value = parseTop(storyEntry, split[4]);
                }
            }

        return value;
    }

    private String parseTop(@SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<Story> story, String argument) {
        return switch (argument) {
            case "name" -> story.map(Story::playerUuid)
                    .map(uuid -> Bukkit.getOfflinePlayer(uuid).getName()).orElse("None");
            case "playtime" -> FormatUtil.format(story.map(Story::survivalTime).orElse(0));
            case "character" -> story.map(Story::character).map(Character::getDisplayName).orElse("None");
            default -> throw new IllegalArgumentException("Invalid leaderboard placeholder: " + argument);
        };
    }
}
