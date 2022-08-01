package com.gamersafer.minecraft.ablockalypse.leaderboard;

import com.gamersafer.minecraft.ablockalypse.AblockalypsePlugin;
import com.gamersafer.minecraft.ablockalypse.database.api.StoryStorage;
import com.gamersafer.minecraft.ablockalypse.story.Story;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class SurvivalTimeLeaderboard {

    private final AblockalypsePlugin plugin;
    private final StoryStorage storyStorage;
    private final List<Story> leaderboard;

    private BukkitTask refreshLeaderboardTask;
    private int leaderboardSize;

    public SurvivalTimeLeaderboard(AblockalypsePlugin plugin, StoryStorage storyStorage) {
        this.plugin = plugin;
        this.storyStorage = storyStorage;
        this.leaderboard = Collections.synchronizedList(new ArrayList<>());

        reload();
    }

    public void reload() {
        int leaderboardRefreshIntervalSeconds = plugin.getConfig().getInt("leaderboard.refresh-interval-seconds", 300);
        leaderboardSize = plugin.getConfig().getInt("leaderboard.max-entries", 10);

        if (refreshLeaderboardTask != null) {
            refreshLeaderboardTask.cancel();
        }

        refreshLeaderboardTask = Bukkit.getScheduler().runTaskTimer(plugin, this::refreshLeaderboard, 0, leaderboardRefreshIntervalSeconds * 20L);
    }

    private void refreshLeaderboard() {
        storyStorage.getTopSurvivalTimeStories(leaderboardSize).thenAccept(stories -> {
            leaderboard.clear();
            leaderboard.addAll(stories);
            leaderboard.sort(Comparator.comparingInt(Story::survivalTime).reversed());
        });
    }

    public Optional<Story> get(int index) {
        if (index < 0 || index >= leaderboard.size()) {
            throw new IllegalArgumentException(index + " is an invalid survival time leaderboard index. Must be between 0 and " + (leaderboard.size() - 1) + ".");
        }

        try {
            return Optional.of(leaderboard.get(index));
        } catch (IndexOutOfBoundsException ignore) {
            return Optional.empty();
        }
    }

}
