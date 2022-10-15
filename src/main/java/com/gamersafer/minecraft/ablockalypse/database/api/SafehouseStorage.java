package com.gamersafer.minecraft.ablockalypse.database.api;

import com.gamersafer.minecraft.ablockalypse.safehouse.Safehouse;
import com.gamersafer.minecraft.ablockalypse.safehouse.SafehouseMemberRole;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface SafehouseStorage {

    /**
     * Creates a new safehouse and updates its ID. See {@link Safehouse#getId()}.
     *
     * @param safehouse the safehouse to create
     * @return the given safehouse instance with the updated id
     */
    CompletableFuture<Safehouse> createSafehouse(Safehouse safehouse);

    /**
     * Deletes the given safehouse.
     *
     * @param safehouse the safehouse to delete
     * @return a CompletableFuture that will complete once the safehouse has been deleted
     */
    CompletableFuture<Void> deleteSafehouse(Safehouse safehouse);

    /**
     * Gets all the safehouses.
     *
     * @return all the existing safehouses
     */
    CompletableFuture<Set<Safehouse>> getAllSafehouses();

    // todo use party plugin api instead of persisting members here (?)

    /**
     * Gives a player access to a safehouse.
     *
     * @param playerUuid  the UUID of the player to add to a safehouse
     * @param safehouseId the id of the safehouse where to add the player
     * @param role        the role to give to the player
     * @return whether the updated succeeded
     */
    CompletableFuture<Boolean> addSafehousePlayer(UUID playerUuid, int safehouseId, SafehouseMemberRole role);

    /**
     * Removes access to a safehouse from a player.
     *
     * @param playerUuid  the UUID of the player to remove from the safehouse
     * @param safehouseId teh id of the safehouse where to remove the player.
     * @return whether the updated succeeded
     */
    CompletableFuture<Boolean> removeSafehousePlayer(UUID playerUuid, int safehouseId);

    /**
     * Flushes the changes made to the given safehouses to the database.
     *
     * @param safehouses the safehouses to updated
     * @return a CompletableFuture that will complete once all safehosues has been updated
     */
    CompletableFuture<Void> updateSafehouses(Set<Safehouse> safehouses);

    void shutdown();

}
