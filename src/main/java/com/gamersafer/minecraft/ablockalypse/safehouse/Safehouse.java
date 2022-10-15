package com.gamersafer.minecraft.ablockalypse.safehouse;

import org.bukkit.Location;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Safehouse {

    private int id;
    private final String regionName;
    private final Set<UUID> members;
    private int doorLevel;
    private Location doorLocation;
    private Location spawnLocation;
    private Location outsideLocation;
    private UUID owner;

    public Safehouse(String regionName) {
        this(0, regionName, 0, null, null, null, null, new HashSet<>());
    }

    public Safehouse(int id, String regionName, int doorLevel, Location doorLocation, Location spawnLocation, Location outsideLocation, UUID owner, Set<UUID> members) {
        this.id = id;
        this.regionName = regionName;
        this.doorLevel = doorLevel;
        this.doorLocation = doorLocation;
        this.spawnLocation = spawnLocation;
        this.outsideLocation = outsideLocation;
        this.owner = owner;
        this.members = members;
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
        this.owner = owner;
    }

    public Set<UUID> getMembers() {
        return members;
    }

    public boolean canAccess(UUID playerUuid) {
        return playerUuid.equals(getOwner()) || (getMembers() != null && getMembers().contains(playerUuid));
    }

// todo implement equals and hashcode

}
