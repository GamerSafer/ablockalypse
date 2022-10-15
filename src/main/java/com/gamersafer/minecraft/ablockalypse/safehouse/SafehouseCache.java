package com.gamersafer.minecraft.ablockalypse.safehouse;

import com.gamersafer.minecraft.ablockalypse.AblockalypsePlugin;
import com.gamersafer.minecraft.ablockalypse.database.api.SafehouseStorage;
import org.bukkit.Bukkit;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class SafehouseCache implements SafehouseStorage {

    private final SafehouseStorage base;
    private final Set<Safehouse> safehouses;

    public SafehouseCache(SafehouseStorage base) {
        this.base = base;
        // there will be only a few dozen safehouses, we can simply keep all of them in memory
        // and save them to the DB every N minutes and on shutdown
        safehouses = Collections.synchronizedSet(new HashSet<>());
        base.getAllSafehouses().thenApply(safehouses::addAll);

        // start saving repeating task. run every 30 min
        Bukkit.getScheduler().runTaskTimer(AblockalypsePlugin.getInstance(), () -> {
            AblockalypsePlugin.getInstance().getLogger().info("Saving safehouses to the database...");
            updateSafehouses(safehouses);
        }, 20 * 60 * 30, 20 * 60 * 30);
    }

    @Override
    public CompletableFuture<Safehouse> createSafehouse(Safehouse safehouse) {
        return base.createSafehouse(safehouse)
                .thenApply(safehouseWithId -> {
                    safehouses.add(safehouseWithId);
                    return safehouseWithId;
                }).exceptionally(throwable -> {
                    throwable.printStackTrace();
                    return null;
                });
    }

    @Override
    public CompletableFuture<Void> deleteSafehouse(Safehouse safehouse) {
        return base.deleteSafehouse(safehouse)
                .thenAccept(unused -> safehouses.remove(safehouse))
                .exceptionally(throwable -> {
                    throwable.printStackTrace();
                    return null;
                });
    }

    @Override
    public CompletableFuture<Set<Safehouse>> getAllSafehouses() {
        return CompletableFuture.completedFuture(safehouses);
    }

    @Override
    public CompletableFuture<Boolean> addSafehousePlayer(UUID playerUuid, int safehouseId, SafehouseMemberRole role) {
        // todo implement
        return null;
    }

    @Override
    public CompletableFuture<Boolean> removeSafehousePlayer(UUID playerUuid, int safehouseId) {
        // todo implement
        return null;
    }

    @Override
    public CompletableFuture<Void> updateSafehouses(Set<Safehouse> safehouses) {
        return base.updateSafehouses(safehouses);
    }

    @Override
    public void shutdown() {
        // save all safehouses to the DB
        updateSafehouses(safehouses).thenRun(base::shutdown);
    }

}
