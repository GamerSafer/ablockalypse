package com.gamersafer.minecraft.ablockalypse.database;

import com.gamersafer.minecraft.ablockalypse.Character;
import com.gamersafer.minecraft.ablockalypse.database.api.StoryStorage;
import com.gamersafer.minecraft.ablockalypse.story.Story;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class StoryDAO implements StoryStorage {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final DataSource dataSource;

    public StoryDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public CompletableFuture<Optional<Story>> getActiveStory(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            Story result = null;

            try (PreparedStatement statement = dataSource.getConnection().prepareStatement("SELECT id, characterName, startTime FROM story WHERE playerUuid = UNHEX(?) AND endTime = NULL LIMIT 1;")) {
                statement.setString(1, playerUuid.toString().replaceAll("-", ""));
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        result = new Story(resultSet.getInt("id"),
                                playerUuid,
                                Character.valueOf(resultSet.getString("characterType")),
                                resultSet.getString("characterName"),
                                resultSet.getTimestamp("startTime").toLocalDateTime(),
                                null);
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            return Optional.ofNullable(result);
        }, executor);
    }

    public CompletableFuture<List<Story>> getAllStories(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            List<Story> stories = new ArrayList<>();

            try (PreparedStatement statement = dataSource.getConnection().prepareStatement("SELECT id, characterType, characterName, startTime, endTime FROM story WHERE playerUuid = UNHEX(?) ORDER BY id DESC;")) {
                statement.setString(1, playerUuid.toString().replaceAll("-", ""));
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        Story story = new Story(resultSet.getInt("id"),
                                playerUuid,
                                Character.valueOf(resultSet.getString("characterType")),
                                resultSet.getString("characterName"),
                                resultSet.getTimestamp("startTime").toLocalDateTime(),
                                resultSet.getTimestamp("endTime").toLocalDateTime());
                        stories.add(story);
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            return stories;
        }, executor);
    }

    public CompletableFuture<Story> startNewStory(UUID playerUuid, Character character, String characterName, LocalDateTime startTime) {
        return CompletableFuture.supplyAsync(() -> {
            try (PreparedStatement statement = dataSource.getConnection()
                    .prepareStatement("INSERT INTO story (playerUuid, characterType, characterName, startTime) VALUES (UNHEX(?), ?, ?, ?);", Statement.RETURN_GENERATED_KEYS)) {

                statement.setString(1, playerUuid.toString().replaceAll("-", ""));
                statement.setString(2, character.name());
                statement.setString(3, characterName);
                statement.setTimestamp(4, Timestamp.valueOf(startTime));

                statement.executeUpdate();

                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (!keys.next()) {
                        throw new AssertionError();
                    }

                    int id = keys.getInt(1);
                    return new Story(id, playerUuid, character, characterName, startTime, null);
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }, executor);
    }

    public CompletableFuture<Void> endStory(UUID playerUuid, LocalDateTime endTime) {
        try (PreparedStatement statement = dataSource.getConnection().prepareStatement("UPDATE story SET endTime = ? WHERE playerUuid = UNHEX(?) AND endTime = NULL LIMIT 1;")) {

            statement.setTimestamp(1, Timestamp.valueOf(endTime));
            statement.setString(2, playerUuid.toString().replaceAll("-", ""));

            int affectedRow = statement.executeUpdate();
            if (affectedRow != 1) {
                throw new RuntimeException("Couldn't end the story of " + playerUuid);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    @Override
    public void shutdown() {
        try {
            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (Exception ignored) {
        }
    }

}
