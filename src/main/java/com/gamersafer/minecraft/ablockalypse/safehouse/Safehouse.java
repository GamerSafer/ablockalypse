package com.gamersafer.minecraft.ablockalypse.safehouse;

import com.gamersafer.minecraft.ablockalypse.AblockalypsePlugin;
import com.gamersafer.minecraft.ablockalypse.util.BlockUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class Safehouse {

    private final String regionName;
    private final Set<UUID> canClaim;
    private final Set<Booster> activeBoosters;
    private Type type;
    private int id;
    private int doorLevel;
    private Location doorLocation;
    private Location spawnLocation;
    private Location outsideLocation;
    private UUID owner;
    private UUID previousOwner;

    public Safehouse(String regionName) {
        this(0, regionName, Type.SAFEHOUSE, 1, null, null, null,
                null, EnumSet.noneOf(Booster.class));
    }

    public Safehouse(int id, String regionName, Type type, int doorLevel, Location doorLocation, Location spawnLocation,
                     Location outsideLocation, UUID owner, Set<Booster> activeBoosters) {
        this.id = id;
        this.regionName = regionName;
        this.type = type;
        this.doorLevel = doorLevel;
        this.doorLocation = doorLocation;
        this.spawnLocation = spawnLocation;
        this.outsideLocation = outsideLocation;
        this.owner = owner;
        this.canClaim = new HashSet<>();
        this.activeBoosters = activeBoosters;
    }

    /**
     * Gets the unique ID of this safehouse.
     *
     * @return the unique ID
     */
    public int getId() {
        return id;
    }

    /**
     * Sets the unique ID of this safehouse.
     * It should be set only when the safehouse is created in the database.
     *
     * @param id the id to set
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Gets the level of the door used to access the safehouse.
     *
     * @return the door level
     */
    public int getDoorLevel() {
        return doorLevel;
    }

    /**
     * Increases by 1 the door level.
     *
     * @return the increased level
     */
    public int increaseDoorLevel() {
        doorLevel++;

        if (getDoorLocation() == null) {
            Bukkit.getServer().getLogger().warning("Unable to upgrade the door of the safehouse #" + getId() + " since its door location is not set");
            return doorLevel;
        }

        if (doorLevel == 2) {
            BlockUtil.updateDoorMaterial(getDoorLocation(), Material.SPRUCE_DOOR);
        } else if (doorLevel >= 3) {
            BlockUtil.updateDoorMaterial(getDoorLocation(), Material.IRON_DOOR);
        }

        return doorLevel;
    }

    /**
     * Gets the name of the WorldGuard region associated with this safehouse.
     *
     * @return the WG region name
     */
    public String getRegionName() {
        return regionName;
    }

    /**
     * Gets the type of this safehouse.
     *
     * @return the safehouse type
     */
    public Type getType() {
        return type;
    }

    /**
     * Sets the type of the safehouse.
     *
     * @param type the new type
     */
    public void setType(Type type) {
        this.type = type;
    }

    /**
     * Gets the top half door location.
     *
     * @return the location or null if it is not configured
     */
    public Location getDoorLocation() {
        return doorLocation;
    }

    /**
     * Sets the location of the top half door.
     *
     * @param doorLocation the location to set
     */
    public void setDoorLocation(Location doorLocation) {
        this.doorLocation = doorLocation;
    }

    /**
     * Gest the location inside the safehouse were players will be teleported to when entering the house.
     *
     * @return the location of null if it is not set
     */
    public Location getSpawnLocation() {
        return spawnLocation;
    }

    /**
     * Sets the spawn location of the safehouse. It must be inside the safehouse.
     * Players will be teleported there when they'll enter the house.
     *
     * @param spawnLocation the location to set
     */
    public void setSpawnLocation(Location spawnLocation) {
        this.spawnLocation = spawnLocation;
    }

    /**
     * Gets the location outside the safehouse were players get teleported to when leaving the house.
     *
     * @return the location or null if it is not set
     */
    public Location getOutsideLocation() {
        return outsideLocation;
    }

    /**
     * Sets the location outside the safehouse were players get teleported to when leaving the house.
     *
     * @param outsideLocation the location to set
     */
    public void setOutsideLocation(Location outsideLocation) {
        this.outsideLocation = outsideLocation;
    }

    /**
     * Gets the UUID of the owner.
     *
     * @return the owner UUID or null if there is no owner
     */
    public UUID getOwner() {
        return owner;
    }

    /**
     * Sets the owner of the safehouse.
     *
     * @param owner their UUID
     */
    public void setOwner(UUID owner) {
        this.previousOwner = this.owner;
        this.owner = owner;
        this.canClaim.clear();
        this.activeBoosters.clear();
    }

    /**
     * Removes the safehouse owner.
     *
     * @return the previous owner UUID, or null if there was no owner
     */
    @Nullable
    public UUID removeOwner() {
        UUID result = this.owner;
        this.previousOwner = null;
        this.owner = null;
        this.canClaim.clear();
        this.activeBoosters.clear();
        return result;
    }

    /**
     * Gets the UUID of the player who was the owner of the safehouse before it was raided.
     * This data is not persisted since it's need for a short period of time only.
     *
     * @return the previous owner or null if the house wasn't recently broke into
     */
    public UUID getPreviousOwner() {
        return previousOwner;
    }

    /**
     * Gets the boosters active in this safehouse. Only the owner can activate them.
     *
     * @return the active boosters. it might be empty
     */
    public Set<Booster> getActiveBoosters() {
        return activeBoosters;
    }

    /**
     * Tries to activate the given booster.
     * There can be max {@link Type#getMaxBoostersAmount()} active boosters. See {@link #getType()}.
     * It's assumed the owner has the permission to activate it.
     *
     * @param booster the booster to activate
     * @return true if the booster was activated, false otherwise
     */
    public boolean activateBooster(Booster booster) {
        if (getActiveBoosters().size() + 1 > getType().getMaxBoostersAmount()) {
            return false;
        }
        return activeBoosters.add(booster);
    }

    /**
     * Deactivates the given booster.
     *
     * @param booster the booster to deactivate
     */
    public void deactivateBooster(Booster booster) {
        activeBoosters.remove(booster);
    }

    /**
     * Checks whether the given player is the owner of the safehouse.
     *
     * @param player the player to check
     * @return {@code true} if the player is the owner, {@code false} if they aren't or if there isn't an owner
     */
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
     * @see #getCanClaimPlayers()
     */
    public boolean canClaim(UUID uuid) {
        if (canClaim.isEmpty() && getOwner() == null) {
            return true;
        }
        return canClaim.contains(uuid);
    }

    /**
     * Gets the players who are allowed to claim the house.
     * This data is not persisted and is invalid after a restart.
     * If it is empty, all players can claim the house.
     *
     * @return the players who can claim the house
     * @see #canClaim(UUID)
     */
    public Set<UUID> getCanClaimPlayers() {
        return Collections.unmodifiableSet(canClaim);
    }

    /**
     * Marks the given player as the player who broke in a house.
     * This gives them the ability to claim the safehouse in the 10 minutes following the raid.
     *
     * @param uuid the UUID of the player
     */
    public void handleBreakIn(UUID uuid) {
        if (owner != null) {
            canClaim.add(uuid);
            canClaim.add(getOwner());

            previousOwner = owner;
            owner = null;
            activeBoosters.clear();

            String previousOwnerName = Bukkit.getOfflinePlayer(previousOwner).getName();
            // remove raider after 10 minutes. after that time, all players should be able to claim the house
            Bukkit.getScheduler().runTaskLater(AblockalypsePlugin.getInstance(), () -> {
                canClaim.clear();
                AblockalypsePlugin.getInstance().getSafehouseManager().dispatchSafehouseLossCommands(previousOwnerName);
            }, 20L * 60L * 10L);
        }
    }

    /**
     * Checks whether the house was recently raided.
     * If it was recently raided, only the previous owner and the player who broke in are allowed
     * to claim it. See {@link #canClaim(UUID)}.
     *
     * @return {@code true} if it was, {@code false} otherwise
     */
    public boolean wasRecentlyRaided() {
        return !canClaim.isEmpty();
    }

    /**
     * Tries to get the {@link Player} object of the owner of the safehouse.
     *
     * @return the player or an empty Optional if the owner is offline or there isn't an owner
     * @see #getOwner()
     */
    public Optional<Player> getOwnerPlayer() {
        if (getOwner() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(Bukkit.getPlayer(getOwner()));
    }

    /**
     * Tries to get the {@link Player} object of the previous safehouse owner.
     *
     * @return the player or an empty Optional if they are offline or if there isn't a previous owner
     * @see #getPreviousOwner()
     */
    public Optional<Player> getPreviousOwnerPlayer() {
        if (getPreviousOwner() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(Bukkit.getPlayer(getPreviousOwner()));
    }

    public enum Type {
        /**
         * The default safehouse
         */
        SAFEHOUSE(2),

        /**
         * A small safehouse where boosters can't be activated
         */
        BUNKER(0);

        /**
         * See {@link #getMaxBoostersAmount()}
         */
        private final int maxBoostersAmount;

        Type(int maxBoostersAmount) {
            this.maxBoostersAmount = maxBoostersAmount;
        }

        public static Optional<Type> fromString(String string) {
            for (Type type : values()) {
                if (type.name().equalsIgnoreCase(string)) {
                    return Optional.of(type);
                }
            }
            return Optional.empty();
        }

        /**
         * Gets the number of boosters that can be activated at the same time in houses of this type.
         *
         * @return the number of boosters
         */
        public int getMaxBoostersAmount() {
            return maxBoostersAmount;
        }
    }

// todo implement equals and hashcode

}
