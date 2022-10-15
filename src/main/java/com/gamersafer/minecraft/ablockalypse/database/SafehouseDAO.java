package com.gamersafer.minecraft.ablockalypse.database;

import com.gamersafer.minecraft.ablockalypse.database.api.SafehouseStorage;
import com.gamersafer.minecraft.ablockalypse.safehouse.Safehouse;
import com.gamersafer.minecraft.ablockalypse.safehouse.SafehouseMemberRole;
import com.gamersafer.minecraft.ablockalypse.util.UUIDUtil;
import com.google.common.io.BaseEncoding;
import org.bukkit.Location;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SafehouseDAO implements SafehouseStorage {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final DataSource dataSource;

    public SafehouseDAO(DataSource dataSource) {
        this.dataSource = dataSource;

        // create the tables if they don't exist
        createTables();
    }

    private void createTables() {
        // todo copy queries
        String tableSafehouseQuery = "";
        String tableSafehouseMemberQuery = "";

        CompletableFuture.runAsync(() -> {
            try (Connection conn = dataSource.getConnection(); PreparedStatement safehouseStatement = conn.prepareStatement(tableSafehouseQuery);
                 PreparedStatement memberStatement = conn.prepareStatement(tableSafehouseMemberQuery)) {
                safehouseStatement.executeUpdate();
                memberStatement.executeUpdate();
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
                 // todo add on duplicate key stuff
                 PreparedStatement statement = conn.prepareStatement("INSERT INTO safehosue (regionName, doorLocation, spawnLocation, outsideLocation) VALUES (?, ?, ?, ?);", Statement.RETURN_GENERATED_KEYS)) {

                statement.setString(1, safehouse.getRegionName());
                statement.setString(2, serializeLocation(safehouse.getDoorLocation()));
                statement.setString(3, serializeLocation(safehouse.getSpawnLocation()));
                statement.setString(4, serializeLocation(safehouse.getOutsideLocation()));

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
            Map<Integer, Safehouse> safehouses = new HashMap<>();

            try (Connection conn = dataSource.getConnection(); PreparedStatement statement = conn.prepareStatement("SELECT * FROM safehouse S LEFT JOIN safehouse_members M ON S.id = M.safehouseId;")) {
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        int id = resultSet.getInt("id");

                        Safehouse safehouse;
                        if (safehouses.containsKey(id)) {
                            safehouse = safehouses.get(id);
                        } else {
                            safehouse = new Safehouse(
                                    id,
                                    resultSet.getString("regionName"),
                                    resultSet.getInt("doorLevel"),
                                    parseLocation(resultSet.getString("doorLocation")),
                                    parseLocation(resultSet.getString("spawnLocation")),
                                    parseLocation(resultSet.getString("outsideLocation")),
                                    null,
                                    new HashSet<>()
                            );
                            safehouses.put(id, safehouse);
                        }

                        byte[] playerUuidBytes = resultSet.getBytes("playerUuid");
                        if (playerUuidBytes != null) {
                            String uuidStr = BaseEncoding.base16().encode(playerUuidBytes);
                            UUID playerUuid = UUIDUtil.parseUUID(uuidStr).orElseThrow();
                            SafehouseMemberRole role = SafehouseMemberRole.valueOf(resultSet.getString("role"));

                            if (role == SafehouseMemberRole.OWNER) {
                                safehouse.setOwner(playerUuid);
                            } else if (role == SafehouseMemberRole.MEMBER) {
                                safehouse.getMembers().add(playerUuid);
                            } else {
                                throw new IllegalArgumentException();
                            }
                        }
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            // variable needed by the compiler
            //noinspection UnnecessaryLocalVariable
            Set<Safehouse> safehousesSet = new HashSet<>(safehouses.values());
            return safehousesSet;
        }, executor).exceptionally(throwable -> {
            throwable.printStackTrace();
            return Collections.emptySet();
        });
    }

    @Override
    public CompletableFuture<Boolean> addSafehousePlayer(UUID playerUuid, int safehouseId, SafehouseMemberRole role) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = dataSource.getConnection(); PreparedStatement statement = conn.prepareStatement("INSERT INTO safehouse_member (playerUuid, safehouseId, role) VALUES (UNHEX(?), ?, ?);")) {

                statement.setString(1, playerUuid.toString().replace("-", ""));
                statement.setInt(2, safehouseId);
                statement.setString(3, role.name());

                return statement.executeUpdate() == 1;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }, executor).exceptionally(throwable -> {
            throwable.printStackTrace();
            return false;
        });
    }

    @Override
    public CompletableFuture<Boolean> removeSafehousePlayer(UUID playerUuid, int safehouseId) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = dataSource.getConnection(); PreparedStatement statement = conn.prepareStatement("DELETE FROM safehouse_member WHERE playerUuid = UNHEX(?) AND safehouseId = ?;")) {
                statement.setString(1, playerUuid.toString().replace("-", ""));
                statement.setInt(2, safehouseId);

                return statement.executeUpdate() == 1;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }, executor).exceptionally(throwable -> {
            throwable.printStackTrace();
            return false;
        });
    }

    @Override
    public CompletableFuture<Void> updateSafehouses(Set<Safehouse> safehouses) {
        // todo batch update
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

    private String serializeLocation(Location location) {
        if (location == null) {
            return "";
        }
        // todo implement
        return "";
    }

    private Location parseLocation(String locationStr) {
        if (locationStr == null || locationStr.isEmpty()) {
            return null;
        }
        // todo implement
        return null;
    }
}
