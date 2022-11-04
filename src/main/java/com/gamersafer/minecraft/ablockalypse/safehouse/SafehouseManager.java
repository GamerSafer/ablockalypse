package com.gamersafer.minecraft.ablockalypse.safehouse;

import com.alessiodp.parties.api.Parties;
import com.alessiodp.parties.api.interfaces.PartiesAPI;
import com.alessiodp.parties.api.interfaces.Party;
import com.gamersafer.minecraft.ablockalypse.AblockalypsePlugin;
import com.gamersafer.minecraft.ablockalypse.Character;
import com.gamersafer.minecraft.ablockalypse.database.api.SafehouseStorage;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class SafehouseManager {

    private final World safehouseWorld;
    private final SafehouseStorage safehouseStorage;
    private final RegionManager regionManager;
    private AblockalypsePlugin plugin;

    public SafehouseManager(AblockalypsePlugin plugin, SafehouseStorage safehouseStorage, World safehouseWorld) {
        this.plugin = plugin;
        this.safehouseStorage = safehouseStorage;
        this.safehouseWorld = safehouseWorld;
        this.regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(safehouseWorld));
    }

    public CompletableFuture<Safehouse> createSafehouse(String regionName) {
        Safehouse safehouse = new Safehouse(regionName);
        return safehouseStorage.createSafehouse(safehouse);
    }

    public CompletableFuture<Void> deleteSafehouse(Safehouse safehouse) {
        return safehouseStorage.deleteSafehouse(safehouse).thenAccept(unused -> {
            // remove house furniture
            plugin.sync(() -> {
                clearSafehouseContent(safehouse);
            });
        });
    }

    /**
     * Removes all furniture and chests from the safehouse.
     *
     * @param safehouse the safehouse to clear
     */
    public void clearSafehouseContent(Safehouse safehouse) {
        ProtectedRegion protectedRegion = regionManager.getRegion(safehouse.getRegionName());
        if (protectedRegion != null) {
            Region region = new CuboidRegion(protectedRegion.getMaximumPoint(), protectedRegion.getMinimumPoint());
            Location centerLoc = new Location(safehouseWorld, region.getCenter().getX(), region.getCenter().getY(), region.getCenter().getZ());
            Collection<Entity> entities = safehouseWorld.getNearbyEntities(centerLoc, region.getWidth() >> 1, region.getHeight() >> 1, region.getLength() >> 1);
            entities.stream()
                    .filter(Objects::nonNull)
                    .filter(entity -> entity.getType() == EntityType.ARMOR_STAND)
                    .forEach(Entity::remove);

            // remove chests
            for (BlockVector3 blockVector : region.getBoundingBox()) {
                Location bukkitLocation = BukkitAdapter.adapt(safehouseWorld, blockVector);
                Block block = bukkitLocation.getBlock();
                if (block.getType() == Material.CHEST || block.getType() == Material.TRAPPED_CHEST) {
                    block.setType(Material.AIR);
                }
            }
        }
    }

    public Optional<Safehouse> getSafehouseFromRegion(String regionName) {
        return safehouseStorage.getAllSafehouses().join().stream()
                .filter(safehouse -> safehouse.getRegionName().equals(regionName))
                .findAny();
    }

    public Optional<Safehouse> getSafehouseAt(Location location) {
        ApplicableRegionSet regionSet = regionManager.getApplicableRegions(BukkitAdapter.asBlockVector(location));
        List<String> regionsAtLocation = regionSet.getRegions().stream()
                .map(ProtectedRegion::getId)
                .toList();

        return safehouseStorage.getAllSafehouses().join().stream()
                .filter(safehouse -> regionsAtLocation.contains(safehouse.getRegionName()))
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
                .filter(safehouse -> safehouse.getDoorLocation().equals(door.getLocation()) || safehouse.getDoorLocation().clone().subtract(0, 1, 0).equals(door.getLocation()))
                .findAny();
    }

    /**
     * Tries ot get the safehouse owned by the passed player.
     *
     * @param playerUuid the UUID of the player
     * @return an optional containing the safehouse owned by the player, or an empty optional if the player
     * doesn't own a safehouse
     */
    public Optional<Safehouse> getSafehouseFromOwnerUuid(UUID playerUuid) {
        return safehouseStorage.getAllSafehouses().join().stream()
                .filter(safehouse -> safehouse.getOwner() != null)
                .filter(safehouse -> safehouse.getOwner().equals(playerUuid))
                .findAny();
    }

    /**
     * Checks whether a location is inside a specific safehouse.
     *
     * @param safehouse the safehouse
     * @param location  the location to check
     * @return {@code true} if the location is inside, {@code false} otherwise
     */
    public boolean isInsideSafehouse(Safehouse safehouse, Location location) {
        ApplicableRegionSet regionSet = regionManager.getApplicableRegions(BukkitAdapter.asBlockVector(location));
        return regionSet.getRegions().stream()
                .map(ProtectedRegion::getId)
                .anyMatch(regionName -> regionName.equals(safehouse.getRegionName()));
    }

    /**
     * Checks whether the given location is inside a safehouse.
     *
     * @param location the location to check
     * @return {@code true} if the location is inside a safehouse, {@code false} otherwise
     */
    public boolean isInsideSafehouse(Location location) {
        return getSafehouseAt(location).isPresent();
    }

    /**
     * Checks whether the given block is part of a safehosue door.
     *
     * @param block the block to check
     * @return {@code true} if the block is a door, {@code false} otherwise
     */
    public boolean isSafehouseDoor(Block block) {
        return getSafehouseFromDoor(block).isPresent();
    }

    /**
     * Checks whether the given player is allowed to access the passed safehouse.
     * Players are allowed to enter houses they own and houses owned by party members.
     *
     * @param safehouse  the safehouse
     * @param playerUuid the player UUId
     * @return {@code true} if they can access, {@code false} otherwise
     */
    public boolean canAccess(Safehouse safehouse, UUID playerUuid) {
        if (playerUuid.equals(safehouse.getOwner())) {
            return true;
        }

        PartiesAPI api = Parties.getApi();
        Party playerParty = api.getPartyOfPlayer(playerUuid);
        Party ownerParty = api.getPartyOfPlayer(safehouse.getOwner());

        return playerParty != null && ownerParty != null && playerParty.getId().equals(ownerParty.getId());
    }

    public Optional<Safehouse> getNearestUnclaimedSafehouse(Location from) {
        return safehouseStorage.getAllSafehouses().join().stream()
                .filter(safehouse -> safehouse.getOwner() == null)
                .filter(safehouse -> safehouse.getDoorLocation() != null)
                .filter(safehouse -> safehouse.getDoorLocation().getWorld().equals(from.getWorld()))
                .min(Comparator.comparingDouble(safehouse -> safehouse.getDoorLocation().distanceSquared(from)));
        // todo test the comparator above
    }

    public List<String> getSafehouseRegionNamesIds() {
        return safehouseStorage.getAllSafehouses().join().stream()
                .<String>mapMulti((safehouse, objectConsumer) -> {
                    objectConsumer.accept(Integer.toString(safehouse.getId()));
                    objectConsumer.accept(safehouse.getRegionName());
                })
                .toList();
    }

    public boolean canBreakIn(Player player, Safehouse safehouse) {
        // todo add scheduled raid times in the config
        //  make sure the owner is online


        return true;
    }

    /**
     * Gets for how many seconds the given player needs to right-click the safehouse door to break in.
     *
     * @param player    the player
     * @param safehouse the safehouse they are trying to break into
     * @return the duration in seconds
     */
    public int getBreakInDuration(Player player, Safehouse safehouse) {
        int result =  safehouse.getDoorLevel() * 5; // todo ask tim the duration

        // we can assume that when this method is called, the active story exists and is in the cache
        //noinspection OptionalGetWithoutIsPresent
        Character playerCharacter = plugin.getStoryStorage().getActiveStory(player.getUniqueId()).join()
                .get().character();

        // todo uncomment when we'll have a LOCKSMITH character
        if (playerCharacter == Character.THIEF /*|| playerCharacter == Character.LOCKSMITH*/) {
            result *= 0.75;
        }
        return result;
    }

    /**
     * Gets for how many seconds the given player needs to right-click the safehouse door to claim in.
     *
     * @param player    the player
     * @param safehouse the safehouse they are trying to claim
     * @return the duration in seconds
     */
    public int getClaimingDuration(Player player, Safehouse safehouse) {
        return safehouse.getDoorLevel() * 5; // todo ask tim the duration
    }

    /**
     * Checks whether the given item is a key.
     * Keys can be used to claim safehouses.
     *
     * @param item the item to check
     * @return {@code true} if the item is a key, {@code false} otherwise
     */
    public boolean isKey(ItemStack item) {
        if (item == null) {
            return false;
        }
        // todo implement MMOItem integration
        return item.getType() == Material.SHEARS; // for testing
    }

    /**
     * Checks whether the given item is a lockpick or a crowbar.
     * These items can be used to break in houses.
     *
     * @param item the item to check
     * @return {@code true} if the item is a lockpick or crowbar, {@code false} otherwise
     */
    public boolean isLockpickOrCrowbar(ItemStack item) {
        if (item == null) {
            return false;
        }
        // todo implement MMOItem integration
        return item.getType() == Material.STICK; // for testing
    }


    // todo handle break-ins and claiming

}
