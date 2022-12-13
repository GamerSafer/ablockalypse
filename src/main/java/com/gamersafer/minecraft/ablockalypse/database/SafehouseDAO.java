package com.gamersafer.minecraft.ablockalypse.database;

import com.gamersafer.minecraft.ablockalypse.database.api.SafehouseStorage;
import com.gamersafer.minecraft.ablockalypse.safehouse.Booster;
import com.gamersafer.minecraft.ablockalypse.safehouse.Safehouse;
import com.gamersafer.minecraft.ablockalypse.util.UUIDUtil;
import com.google.common.io.BaseEncoding;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class SafehouseDAO implements SafehouseStorage {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final DataSource dataSource;

    public SafehouseDAO(DataSource dataSource) {
        this.dataSource = dataSource;

        // create the tables if they don't exist
        createTables();
    }

    private void createTables() {
        String tableSafehouseQuery = """
                CREATE TABLE IF NOT EXISTS safehouse
                (
                    id              int(11) NOT NULL AUTO_INCREMENT,
                    regionName      varchar(48) NOT NULL UNIQUE,
                    houseType       varchar(48) NOT NULL DEFAULT 'SAFEHOUSE',
                    ownerUuid       binary(16),
                    doorLevel       INT UNSIGNED NOT NULL DEFAULT 1,
                    doorLocation    varchar(48),
                    spawnLocation   varchar(48),
                    outsideLocation varchar(48),
                    activeBoosters  varchar(256),
                    PRIMARY KEY (`id`)
                );
                """;
        CompletableFuture.runAsync(() -> {
            try (Connection conn = dataSource.getConnection(); PreparedStatement safehouseStatement = conn.prepareStatement(tableSafehouseQuery)) {
                safehouseStatement.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }, executor).exceptionally(throwable -> {
            throwable.printStackTrace();
            return null;
        });

        // todo remove once updated on production
        CompletableFuture.runAsync(() -> {
            try (Connection conn = dataSource.getConnection(); PreparedStatement updateSchemaStatement = conn.prepareStatement("ALTER TABLE safehouse ADD COLUMN IF NOT EXISTS activeBoosters varchar(256)")) {
                updateSchemaStatement.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }, executor).exceptionally(throwable -> {
            throwable.printStackTrace();
            return null;
        });
    }

    @Override
    public CompletableFuture<Safehouse> createSafehouse(Safehouse safehouse) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement statement = conn.prepareStatement("INSERT INTO safehouse (regionName, houseType, doorLocation, spawnLocation, outsideLocation) VALUES (?, ?, ?, ?, ?);", Statement.RETURN_GENERATED_KEYS)) {

                statement.setString(1, safehouse.getRegionName());
                statement.setString(2, safehouse.getType().name());
                statement.setString(3, serializeLocation(safehouse.getDoorLocation()));
                statement.setString(4, serializeLocation(safehouse.getSpawnLocation()));
                statement.setString(5, serializeLocation(safehouse.getOutsideLocation()));

                statement.executeUpdate();

                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (!keys.next()) {
                        throw new AssertionError();
                    }

                    int id = keys.getInt(1);
                    safehouse.setId(id);
                    return safehouse;
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
    public CompletableFuture<Void> deleteSafehouse(Safehouse safehouse) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = dataSource.getConnection(); PreparedStatement statement = conn.prepareStatement("DELETE FROM safehouse WHERE id = ?;")) {
                statement.setInt(1, safehouse.getId());

                statement.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }, executor).exceptionally(throwable -> {
            throwable.printStackTrace();
            return null;
        });
    }

    @Override
    public CompletableFuture<Set<Safehouse>> getAllSafehouses() {
        return CompletableFuture.supplyAsync(() -> {
            Set<Safehouse> safehouses = new HashSet<>();

            try (Connection conn = dataSource.getConnection(); PreparedStatement statement = conn.prepareStatement("SELECT * FROM safehouse;")) {
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        UUID ownerUuid = null;
                        byte[] playerUuidBytes = resultSet.getBytes("ownerUuid");
                        if (playerUuidBytes != null) {
                            String uuidStr = BaseEncoding.base16().encode(playerUuidBytes);
                            ownerUuid = UUIDUtil.parseUUID(uuidStr).orElseThrow();
                        }

                        Safehouse safehouse = new Safehouse(
                                resultSet.getInt("id"),
                                resultSet.getString("regionName"),
                                Safehouse.Type.fromString(resultSet.getString("houseType")).orElseThrow(),
                                resultSet.getInt("doorLevel"),
                                parseLocation(resultSet.getString("doorLocation")),
                                parseLocation(resultSet.getString("spawnLocation")),
                                parseLocation(resultSet.getString("outsideLocation")),
                                ownerUuid,
                                parseSet(resultSet.getString("activeBoosters"), Booster.class)
                        );

                        safehouses.add(safehouse);
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            return safehouses;
        }, executor).exceptionally(throwable -> {
            throwable.printStackTrace();
            return Collections.emptySet();
        });
    }

    @Override
    public CompletableFuture<Void> updateSafehouses(Set<Safehouse> safehouses) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = dataSource.getConnection(); PreparedStatement statement = conn.prepareStatement("UPDATE safehouse SET houseType = ?, ownerUuid = UNHEX(?), doorLevel = ?, doorLocation = ?, spawnLocation = ?, outsideLocation = ?, activeBoosters = ? WHERE id = ?;")) {
                for (Safehouse safehouse : safehouses) {
                    String uuidStr = null;
                    if (safehouse.getOwner() != null) {
                        uuidStr = safehouse.getOwner().toString().replace("-", "");
                    }
                    statement.setString(1, safehouse.getType().name());
                    statement.setString(2, uuidStr);
                    statement.setInt(3, safehouse.getDoorLevel());
                    statement.setString(4, serializeLocation(safehouse.getDoorLocation()));
                    statement.setString(5, serializeLocation(safehouse.getSpawnLocation()));
                    statement.setString(6, serializeLocation(safehouse.getOutsideLocation()));
                    statement.setString(7, serializeSet(safehouse.getActiveBoosters()));
                    statement.setInt(8, safehouse.getId());

                    statement.addBatch();
                }
                statement.executeBatch();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }, executor).exceptionally(throwable -> {
            throwable.printStackTrace();
            return null;
        });
    }

    @Override
    public void shutdown() {
        try {
            executor.shutdown();
            int attempt = 0;
            while (!executor.awaitTermination(10, TimeUnit.SECONDS) && ++attempt < 5) {
                System.out.println("Waiting for the SafehouseDAO to terminate...");
            }
        } catch (Exception ignored) {
        }
    }

    private String serializeLocation(Location location) {
        if (location == null) {
            return null;
        }
        return location.getWorld().getName() + " " + truncateDouble(location.getX()) + " " + truncateDouble(location.getY())
                + " " + truncateDouble(location.getZ()) + " " + truncateDouble(location.getYaw()) + " " + truncateDouble(location.getPitch());
    }

    private Location parseLocation(String locationStr) {
        if (locationStr == null || locationStr.isEmpty()) {
            return null;
        }
        String[] split = locationStr.split(" ");
        return new Location(Bukkit.getWorld(split[0]),
                Double.parseDouble(split[1]),
                Double.parseDouble(split[2]),
                Double.parseDouble(split[3]),
                Float.parseFloat(split[4]),
                Float.parseFloat(split[5])
        );
    }

    private <T extends Enum<T>> String serializeSet(Set<T> set) {
        if (set == null) {
            return null;
        }
        return set.stream().map(Enum::name).collect(Collectors.joining(","));
    }

    @SuppressWarnings("SameParameterValue")
    private <T extends Enum<T>> Set<T> parseSet(String setStr, Class<T> enumClass) {
        Set<T> set = new HashSet<>();
        if (setStr != null && !setStr.isEmpty()) {
            for (String str : setStr.split(",")) {
                set.add(Enum.valueOf(enumClass, str));
            }
        }
        return set;
    }

    private double truncateDouble(double value) {
        return Math.floor(value * 100) / 100;
    }

}
