package com.gamersafer.minecraft.ablockalypse.database.api;

import com.gamersafer.minecraft.ablockalypse.safehouse.Safehouse;

import java.util.Set;
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

    /**
     * Flushes the changes made to the given safehouses to the database.
     *
     * @param safehouses the safehouses to updated
     * @return a CompletableFuture that will complete once all safehouses has been updated
     */
    CompletableFuture<Void> updateSafehouses(Set<Safehouse> safehouses);

    /**
     * Sets after how many days of inactivity players will lose their safehouse.
     *
     * @param expirationDays the amount of days since their last logout
     */
    void setSafehouseExpirationForInactivity(int expirationDays);

    /**
     * Sets after how many days of inactivity players will lose their bunker.
     *
     * @param expirationDays the amount of days since the last logout
     */
    void setBunkerExpirationForInactivity(int expirationDays);

    void shutdown();

}
