package com.gamersafer.minecraft.ablockalypse.database;

import com.gamersafer.minecraft.ablockalypse.Character;
import com.gamersafer.minecraft.ablockalypse.database.api.StoryStorage;
import com.gamersafer.minecraft.ablockalypse.story.Story;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.entity.EntityDamageEvent;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
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

        // create the story table if it doesn't exist
        createTable();
    }

    private void createTable() {
        String tableCreationQuery = "CREATE TABLE IF NOT EXISTS story (" +
                "  id            int(11) NOT NULL AUTO_INCREMENT," +
                "  playerUuid    binary(16) NOT NULL," +
                "  characterType varchar(48) NOT NULL," +
                "  characterName varchar(20) NOT NULL," +
                "  startTime     TIMESTAMP NOT NULL DEFAULT 0," +
                "  endTime       TIMESTAMP NULL DEFAULT NULL," +
                "  deathCause    varchar(48) NULL DEFAULT NULL," +
                "  deathLocWorld varchar(48) NULL DEFAULT NULL," +
                "  deathLocX     DOUBLE NULL DEFAULT NULL," +
                "  deathLocY     DOUBLE NULL DEFAULT NULL," +
                "  deathLocZ     DOUBLE NULL DEFAULT NULL," +
                "  PRIMARY KEY (`id`)" +
                ")ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;";

        CompletableFuture.runAsync(() -> {
            try (Connection conn = dataSource.getConnection(); PreparedStatement statement = conn.prepareStatement(tableCreationQuery)) {
                statement.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }, executor).exceptionally(throwable -> {
            throwable.printStackTrace();
            return null;
        });
    }

    @Override
    public CompletableFuture<Optional<Story>> getActiveStory(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            Story result = null;

            try (Connection conn = dataSource.getConnection(); PreparedStatement statement = conn.prepareStatement("SELECT id, characterType, characterName, startTime FROM story WHERE playerUuid = UNHEX(?) AND endTime IS NULL LIMIT 1;")) {
                statement.setString(1, playerUuid.toString().replace("-", ""));
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        result = new Story(resultSet.getInt("id"),
                                playerUuid,
                                Character.valueOf(resultSet.getString("characterType")),
                                resultSet.getString("characterName"),
                                resultSet.getTimestamp("startTime").toLocalDateTime(),
                                null,
                                null,
                                null);
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            return Optional.ofNullable(result);
        }, executor).exceptionally(throwable -> {
            throwable.printStackTrace();
            return Optional.empty();
        });
    }

    @Override
    public CompletableFuture<List<Story>> getAllStories(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            List<Story> stories = new ArrayList<>();

            try (Connection conn = dataSource.getConnection(); PreparedStatement statement = conn.prepareStatement("SELECT id, characterType, characterName, startTime, endTime FROM story WHERE playerUuid = UNHEX(?) ORDER BY id DESC;")) {
                statement.setString(1, playerUuid.toString().replace("-", ""));
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        LocalDateTime endTime = Optional.ofNullable(resultSet.getTimestamp("endTime"))
                                .map(Timestamp::toLocalDateTime).orElse(null);

                        EntityDamageEvent.DamageCause deathCause = null;
                        Location deathLocation = null;
                        if (endTime != null) {
                            deathCause = EntityDamageEvent.DamageCause.valueOf(resultSet.getString("deathCause"));
                            World deathLocationWorld = Bukkit.getWorld(resultSet.getString("deathLocWorld"));
                            deathLocation = new Location(deathLocationWorld, resultSet.getDouble("deathLocX"),
                                    resultSet.getDouble("deathLocY"), resultSet.getDouble("deathLocZ"));
                        }

                        Story story = new Story(resultSet.getInt("id"),
                                playerUuid,
                                Character.valueOf(resultSet.getString("characterType")),
                                resultSet.getString("characterName"),
                                resultSet.getTimestamp("startTime").toLocalDateTime(),
                                endTime,
                                deathCause,
                                deathLocation);
                        stories.add(story);
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            return stories;
        }, executor).exceptionally(throwable -> {
            throwable.printStackTrace();
            return Collections.emptyList();
        });
    }

    @Override
    public CompletableFuture<Story> startNewStory(UUID playerUuid, Character character, String characterName, LocalDateTime startTime) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement statement = conn.prepareStatement("INSERT INTO story (playerUuid, characterType, characterName, startTime) VALUES (UNHEX(?), ?, ?, ?);", Statement.RETURN_GENERATED_KEYS)) {

                statement.setString(1, playerUuid.toString().replace("-", ""));
                statement.setString(2, character.name());
                statement.setString(3, characterName);
                statement.setTimestamp(4, Timestamp.valueOf(startTime));

                statement.executeUpdate();

                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (!keys.next()) {
                        throw new AssertionError();
                    }

                    int id = keys.getInt(1);
                    return new Story(id, playerUuid, character, characterName, startTime, null, null, null);
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }, executor).exceptionally(throwable -> {
            throwable.printStackTrace();
            return null;
        });
    }

    @Override
    public CompletableFuture<Void> endStory(UUID playerUuid, EntityDamageEvent.DamageCause deathCause, Location deathLocation, LocalDateTime endTime) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = dataSource.getConnection(); PreparedStatement statement = conn.prepareStatement("UPDATE story SET endTime = ?, deathCause = ?, deathLocWorld = ?, deathLocX = ?, deathLocY = ?, deathLocZ = ? WHERE playerUuid = UNHEX(?) AND endTime IS NULL LIMIT 1;")) {

                statement.setTimestamp(1, Timestamp.valueOf(endTime));
                statement.setString(2, deathCause.name());
                statement.setString(3, deathLocation.getWorld().getName());
                statement.setDouble(4, deathLocation.getX());
                statement.setDouble(5, deathLocation.getY());
                statement.setDouble(6, deathLocation.getZ());
                statement.setString(7, playerUuid.toString().replace("-", ""));

                int affectedRow = statement.executeUpdate();
                if (affectedRow != 1) {
                    throw new RuntimeException("Couldn't end the story of " + playerUuid);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }, executor).exceptionally(throwable -> {
            throwable.printStackTrace();
            return null;
        });
    }

    @Override
    public CompletableFuture<Duration> getPlaytime(UUID playerUuid) {
        return getAllStories(playerUuid).thenApply(stories -> stories.stream()
                .map(Story::survivalTime)
                .reduce(Duration::plus)
                .orElse(Duration.ZERO)
        );
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
