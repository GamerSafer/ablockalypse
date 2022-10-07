package com.gamersafer.minecraft.ablockalypse.leaderboard;

import com.gamersafer.minecraft.ablockalypse.AblockalypsePlugin;
import com.gamersafer.minecraft.ablockalypse.Character;
import com.gamersafer.minecraft.ablockalypse.database.api.StoryStorage;
import com.gamersafer.minecraft.ablockalypse.story.Story;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class SurvivalTimeLeaderboard {

    private final AblockalypsePlugin plugin;
    private final StoryStorage storyStorage;
    private final List<Story> survivalTimeGlobalLeaderboard;
    private final Map<Character, List<Story>> survivalTimeGlobalLeaderboardByCharacter;

    private BukkitTask refreshLeaderboardTask;
    private int leaderboardSize;

    public SurvivalTimeLeaderboard(AblockalypsePlugin plugin, StoryStorage storyStorage) {
        this.plugin = plugin;
        this.storyStorage = storyStorage;
        this.survivalTimeGlobalLeaderboard = Collections.synchronizedList(new ArrayList<>());
        this.survivalTimeGlobalLeaderboardByCharacter = new ConcurrentHashMap<>();

        reload();
    }

    public void reload() {
        int leaderboardRefreshIntervalSeconds = plugin.getConfig().getInt("leaderboard.refresh-interval-seconds", 300);
        leaderboardSize = plugin.getConfig().getInt("leaderboard.max-entries", 10);

        if (refreshLeaderboardTask != null) {
            refreshLeaderboardTask.cancel();
        }

        refreshLeaderboardTask = Bukkit.getScheduler().runTaskTimer(plugin, this::refreshGlobalLeaderboard, 0, leaderboardRefreshIntervalSeconds * 20L);
        refreshLeaderboardTask = Bukkit.getScheduler().runTaskTimer(plugin, this::refreshGlobalLeaderboardByCharacter, 5, leaderboardRefreshIntervalSeconds * 20L);
    }

    /**
     * Loads the global survival time leaderboard from the database.
     */
    private void refreshGlobalLeaderboard() {
        storyStorage.getTopSurvivalTimeStories(leaderboardSize).thenAccept(stories -> {
            survivalTimeGlobalLeaderboard.clear();
            survivalTimeGlobalLeaderboard.addAll(stories);
            survivalTimeGlobalLeaderboard.sort(Comparator.comparingInt(Story::survivalTime).reversed());
        });
    }

    /**
     * Loads the global survival time leaderboard by character from the database.
     */
    private void refreshGlobalLeaderboardByCharacter() {
        for (Character character : Character.values()) {
            storyStorage.getTopSurvivalTimeStoriesByCharacter(character, leaderboardSize).thenAccept(stories -> {
                stories.sort(Comparator.comparingInt(Story::survivalTime).reversed());
                survivalTimeGlobalLeaderboardByCharacter.put(character, stories);
            });
        }
    }

    public Optional<Story> get(int index) {
        if (index < 0 || index >= survivalTimeGlobalLeaderboard.size()) {
            throw new IllegalArgumentException(index + " is an invalid global survival time leaderboard index. Must be between 0 and " + (survivalTimeGlobalLeaderboard.size() - 1) + ".");
        }

        try {
            return Optional.of(survivalTimeGlobalLeaderboard.get(index));
        } catch (IndexOutOfBoundsException ignore) {
            return Optional.empty();
        }
    }

    public Optional<Story> get(Character character, int index) {
        int leaderboardSize = survivalTimeGlobalLeaderboardByCharacter.getOrDefault(character, Collections.emptyList()).size();
        if (index < 0 || index >= leaderboardSize) {
            throw new IllegalArgumentException(index + " is an invalid global character survival time leaderboard index. Must be between 0 and " + leaderboardSize + ".");
        }

        try {
            return Optional.of(survivalTimeGlobalLeaderboardByCharacter.get(character).get(index));
        } catch (IndexOutOfBoundsException | NullPointerException ignore) {
            return Optional.empty();
        }
    }

}
