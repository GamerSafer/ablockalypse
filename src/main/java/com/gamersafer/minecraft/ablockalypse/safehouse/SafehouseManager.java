package com.gamersafer.minecraft.ablockalypse.safehouse;

import com.gamersafer.minecraft.ablockalypse.database.api.SafehouseStorage;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class SafehouseManager {

    private final SafehouseStorage safehouseStorage;
    private final RegionManager regionManager;

    public SafehouseManager(SafehouseStorage safehouseStorage, World safehouseWorld) {
        this.safehouseStorage = safehouseStorage;
        this.regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(safehouseWorld));
    }

    public CompletableFuture<Safehouse> createSafehouse(String regionName) {
        Safehouse safehouse = new Safehouse(regionName);
        return safehouseStorage.createSafehouse(safehouse);
    }

    public Optional<Safehouse> getSafehouseFromRegion(String regionName) {
        return safehouseStorage.getAllSafehouses().join().stream()
                .filter(safehouse -> safehouse.getRegionName().equals(regionName))
                .findAny();
    }

    public Optional<Safehouse> getSafehouseFromID(int safehouseID) {
        return safehouseStorage.getAllSafehouses().join().stream()
                .filter(safehouse -> safehouse.getId() == safehouseID)
                .findAny();
    }

    public Optional<Safehouse> getSafehouseFromDoor(Block door) {
        if (door == null || !door.getType().name().endsWith("_DOOR")) {
            return Optional.empty();
        }

        return safehouseStorage.getAllSafehouses().join().stream()
                .filter(safehouse -> safehouse.getDoorLocation() != null)
                .filter(safehouse -> safehouse.getDoorLocation().equals(door.getLocation()))
                .findAny();
    }

    public Optional<Safehouse> getSafehouseFromUuid(UUID playerUuid) {
        return safehouseStorage.getAllSafehouses().join().stream()
                .filter(safehouse -> safehouse.canAccess(playerUuid))
                .findAny();
    }

    public boolean isInsideSafehouse(Safehouse safehouse, Location location) {
        ApplicableRegionSet regionSet = regionManager.getApplicableRegions(BukkitAdapter.asBlockVector(location));
        return regionSet.getRegions().stream()
                .map(ProtectedRegion::getId)
                .anyMatch(regionName -> regionName.equals(safehouse.getRegionName()));
    }

    public boolean isSafehouseDoor(Block block) {
        return getSafehouseFromDoor(block).isPresent();
    }

    // todo handle break-ins and claiming

}
