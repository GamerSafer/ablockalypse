package com.gamersafer.minecraft.ablockalypse.safehouse;

import com.gamersafer.minecraft.ablockalypse.AblockalypsePlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class Safehouse {

    private final String regionName;
    private final Set<UUID> canClaim;
    private Type type;
    private int id;
    private int doorLevel;
    private Location doorLocation;
    private Location spawnLocation;
    private Location outsideLocation;
    private UUID owner;
    private UUID previousOwner;

    public Safehouse(String regionName) {
        this(0, regionName, Type.SAFEHOUSE, 1, null, null, null, null);
    }

    public Safehouse(int id, String regionName, Type type, int doorLevel, Location doorLocation, Location spawnLocation, Location outsideLocation, UUID owner) {
        this.id = id;
        this.regionName = regionName;
        this.type = type;
        this.doorLevel = doorLevel;
        this.doorLocation = doorLocation;
        this.spawnLocation = spawnLocation;
        this.outsideLocation = outsideLocation;
        this.owner = owner;
        this.canClaim = new HashSet<>();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getDoorLevel() {
        return doorLevel;
    }

    public int increaseDoorLevel() {
        return ++doorLevel;
    }

    public String getRegionName() {
        return regionName;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Location getDoorLocation() {
        return doorLocation;
    }

    public void setDoorLocation(Location doorLocation) {
        this.doorLocation = doorLocation;
    }

    public Location getSpawnLocation() {
        return spawnLocation;
    }

    public void setSpawnLocation(Location spawnLocation) {
        this.spawnLocation = spawnLocation;
    }

    public Location getOutsideLocation() {
        return outsideLocation;
    }

    public void setOutsideLocation(Location outsideLocation) {
        this.outsideLocation = outsideLocation;
    }

    public UUID getOwner() {
        return owner;
    }

    public void setOwner(UUID owner) {
        this.previousOwner = this.owner;
        this.owner = owner;
        this.canClaim.clear();
    }

    public void removeOwner() {
        this.previousOwner = null;
        this.owner = null;
        this.canClaim.clear();
    }

    public UUID getPreviousOwner() {
        return previousOwner;
    }

    public boolean isOwner(Player player) {
        return owner != null && player.getUniqueId().equals(owner);
    }

    /**
     * Checks whether this safehouse has an owner.
     *
     * @return {@code true} if it is claimed, {@code false} otherwise
     */
    public boolean isClaimed() {
        return getOwner() != null;
    }

    /**
     * Checks whether the given player is allowed to claim the safehouse.
     * In the 10 minutes following a raid, only the previous owner and the player who broke in are allowed to claim it.
     *
     * @param uuid the UUID of the player to check
     * @return {@code true} if the player is allowed to claim it, {@code false} otherwise
     */
    public boolean canClaim(UUID uuid) {
        if (canClaim.isEmpty() && getOwner() == null) {
            return true;
        }
        return canClaim.contains(uuid);
    }

    /**
     * Marks the given player as the player who broke in a house.
     * This gives them the ability to claim the safehouse in the 10 minutes following the raid.
     *
     * @param uuid the UUID of the player
     */
    public void handleBreakIn(UUID uuid) {
        canClaim.add(uuid);
        canClaim.add(getOwner());

        previousOwner = owner;
        owner = null;

        // remove raider after 10 minutes. after that time, all players should be able to claim the house
        Bukkit.getScheduler().runTaskLater(AblockalypsePlugin.getInstance(), canClaim::clear, 20L * 60L * 10L);
    }

    /**
     * Checks whether the house was recently raided.
     * If it was recently raided, only the previous owner and the player who broke in are allowed
     * to claim it. See {@link #canClaim(UUID)}.
     *
     * @return {@code true} if it was, {@code false} otherwise
     */
    public boolean wasRecentlyRaided() {
        return canClaim.isEmpty();
    }

    public Optional<Player> getOwnerPlayer() {
        if (getOwner() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(Bukkit.getPlayer(getOwner()));
    }

    public Optional<Player> getPreviousOwnerPlayer() {
        if (getPreviousOwner() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(Bukkit.getPlayer(getPreviousOwner()));
    }

    public enum Type {
        SAFEHOUSE,
        BUNKER;

        public static Optional<Type> fromString(String string) {
            for (Type type : values()) {
                if (type.name().equalsIgnoreCase(string)) {
                    return Optional.of(type);
                }
            }
            return Optional.empty();
        }
    }

// todo implement equals and hashcode

}
