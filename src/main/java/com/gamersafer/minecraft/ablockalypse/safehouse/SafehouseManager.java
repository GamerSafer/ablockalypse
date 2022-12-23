package com.gamersafer.minecraft.ablockalypse.safehouse;

import com.alessiodp.parties.api.Parties;
import com.alessiodp.parties.api.interfaces.PartiesAPI;
import com.alessiodp.parties.api.interfaces.Party;
import com.gamersafer.minecraft.ablockalypse.AblockalypsePlugin;
import com.gamersafer.minecraft.ablockalypse.Character;
import com.gamersafer.minecraft.ablockalypse.database.api.SafehouseStorage;
import com.gamersafer.minecraft.ablockalypse.util.BlockUtil;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import io.lumine.mythic.lib.api.item.NBTItem;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

public class SafehouseManager {

    /**
     * The type of blocks to remove when clearing safehouses.
     */
    public final static Set<Material> WIPE_BLOCKS = Set.of(
            Material.CHEST,
            Material.TRAPPED_CHEST,
            Material.BARRIER,
            Material.BROWN_SHULKER_BOX,
            Material.YELLOW_SHULKER_BOX,
            Material.PLAYER_HEAD,
            Material.PLAYER_WALL_HEAD
    );

    /**
     * The cost of upgrading a safehouse door.
     * <p>
     * The key is the door level, the value is the cost in planks and sheet metal.
     */
    private final static Map<Integer, Integer> DOOR_COST = Map.of(2, 5, 3, 8);

    private final World safehouseWorld;
    private final SafehouseStorage safehouseStorage;
    private final RegionManager regionManager;
    private final AblockalypsePlugin plugin;
    private ScheduledExecutorService raidTimeScheduler;
    private List<String> safehouseClaimCommands;
    private List<String> safehouseLossCommands;

    private boolean raidEnabled;


    public SafehouseManager(AblockalypsePlugin plugin, SafehouseStorage safehouseStorage, World safehouseWorld) {
        this.plugin = plugin;
        this.safehouseStorage = safehouseStorage;
        this.safehouseWorld = safehouseWorld;
        this.regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(safehouseWorld));
        this.raidTimeScheduler = Executors.newScheduledThreadPool(1);

        reload();
    }

    /**
     * Gets how much it costs to upgrade a safehouse door at the given level.
     *
     * @param level the level
     * @return the cost in scrap metala
     */
    public static int getDoorLevelCost(int level) {
        if (!DOOR_COST.containsKey(level)) {
            throw new IllegalArgumentException("No door level " + level);
        }
        return DOOR_COST.get(level);
    }

    /**
     * Tries to upgrade the door of the given safehouse.
     *
     * @param safehouse the safehouse whose door to upgrade
     * @param player    the player who is trying to upgrade the door. they must be the safehouse owner.
     * @return true if the door was upgraded, false if the player does not have enough scrap metal
     */
    public boolean tryUpgradeDoor(Safehouse safehouse, Player player) {
        if (!player.getUniqueId().equals(safehouse.getOwner())) {
            throw new IllegalArgumentException("Only owners can upgrade doors");
        }

        // make sure the player has enough resources to upgrade the door
        int plankAmount = 0;
        int sheetMetalAmount = 0;
        ItemStack plankItem = null;
        ItemStack sheetMetalItem = null;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null) {
                continue;
            }
            NBTItem nbtItem = NBTItem.get(item);
            if (nbtItem.hasType()) {
                if ("PLANK".equals(nbtItem.getString("MMOITEMS_ITEM_ID"))) {
                    if (plankItem == null) {
                        plankItem = item.clone();
                    }
                    plankAmount += item.getAmount();
                } else if ("SHEETMETAL".equals(nbtItem.getString("MMOITEMS_ITEM_ID"))) {
                    sheetMetalAmount += item.getAmount();
                    if (sheetMetalItem == null) {
                        sheetMetalItem = item.clone();
                    }
                }
            }
        }
        int cost = getDoorLevelCost(safehouse.getDoorLevel() + 1);
        if (cost < 0 || plankAmount < cost || sheetMetalAmount < cost) {
            return false;
        }

        // remove resources
//        int removedPlans = 0;
//        int removedSheetMetal = 0;
//        for (ItemStack item : player.getInventory().getContents()) {
//            if (removedPlans >= cost && removedSheetMetal >= cost) {
//                break;
//            }
//            if (item == null) {
//                continue;
//            }
//            NBTItem nbtItem = NBTItem.get(item);
//            if (nbtItem.hasType()) {
//                if ("PLANK".equals(nbtItem.getString("MMOITEMS_ITEM_ID"))) {
//                    if (item.)
//                    removedPlans += item.getAmount();
//                } else if ("SHEETMETAL".equals(nbtItem.getString("MMOITEMS_ITEM_ID"))) {
//                    removedSheetMetal += item.getAmount();
//                }
//            }
//        }
        //noinspection DataFlowIssue
        plankItem.setAmount(cost);
        player.getInventory().removeItem(plankItem);
        //noinspection DataFlowIssue
        sheetMetalItem.setAmount(cost);
        player.getInventory().removeItem(sheetMetalItem);

        // finally, increase the door level
        safehouse.increaseDoorLevel();
        return true;
    }

    public void reload() {
        raidEnabled = false;

        if (raidTimeScheduler != null) {
            // todo make sure all scheduled tasks are cancelled
            raidTimeScheduler.shutdownNow();
        }
        raidTimeScheduler = Executors.newScheduledThreadPool(1);

        ConfigurationSection raidWindowsSection = plugin.getConfig().getConfigurationSection("safehouse-raid-windows");
        for (String key : Objects.requireNonNull(raidWindowsSection).getKeys(false)) {
            LocalTime start = LocalTime.parse(Objects.requireNonNull(raidWindowsSection.getString(key + ".start")));
            LocalTime end = LocalTime.parse(Objects.requireNonNull(raidWindowsSection.getString(key + ".end")));
            LocalTime now = LocalTime.now();

            if (now.isAfter(start)) {
                if (now.isBefore(end)) {
                    raidEnabled = true;
                }
            } else {
                long delay = ChronoUnit.MILLIS.between(now, start);
                raidTimeScheduler.schedule(() -> {
                    raidEnabled = true;
                }, delay, TimeUnit.MILLISECONDS);
            }

            if (now.isBefore(end)) {
                long delay = ChronoUnit.MILLIS.between(now, end);
                raidTimeScheduler.schedule(() -> {
                    raidEnabled = false;
                }, delay, TimeUnit.MILLISECONDS);
            }
        }

        this.safehouseClaimCommands = plugin.getConfig().getStringList("safehouse-commands.claim");
        this.safehouseLossCommands = plugin.getConfig().getStringList("safehouse-commands.loss");
    }

    /**
     * Create a safehouse at the given WG region name.
     *
     * @param regionName the WG region name
     * @return the created safehouse
     */
    public CompletableFuture<Safehouse> createSafehouse(String regionName) {
        Safehouse safehouse = new Safehouse(regionName);
        return safehouseStorage.createSafehouse(safehouse);
    }

    /**
     * Deletes a safehouse from the database and removes all its contents.
     *
     * @param safehouse the safehouse to delete
     * @return a future that completes when the safehouse has been deleted
     */
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

            // remove chests, barriers, and shulker boxes
            for (BlockVector3 blockVector : region.getBoundingBox()) {
                Location bukkitLocation = BukkitAdapter.adapt(safehouseWorld, blockVector);
                Block block = bukkitLocation.getBlock();
                if (WIPE_BLOCKS.contains(block.getType())) {
                    block.setType(Material.AIR);
                }
            }
        }

        // reset the door material
        BlockUtil.updateDoorMaterial(safehouse.getDoorLocation(), Material.WARPED_DOOR);
    }

    /**
     * Gets all the entities inside the given safehouse.
     *
     * @param safehouse the safehouse
     */
    public Collection<Entity> getEntitiesInSafehouse(Safehouse safehouse) {
        ProtectedRegion protectedRegion = regionManager.getRegion(safehouse.getRegionName());
        if (protectedRegion != null) {
            Region region = new CuboidRegion(protectedRegion.getMaximumPoint(), protectedRegion.getMinimumPoint());
            Location centerLoc = new Location(safehouseWorld, region.getCenter().getX(), region.getCenter().getY(), region.getCenter().getZ());
            return safehouseWorld.getNearbyEntities(centerLoc, region.getWidth() >> 1, region.getHeight() >> 1, region.getLength() >> 1);
        }
        return Collections.emptyList();
    }

    /**
     * Gets the safehouse configured at the given WG region.
     *
     * @param regionName the name of the WG region
     * @return the safehouse or an empty optional if no safehouse is configured at the given region
     */
    public Optional<Safehouse> getSafehouseFromRegion(String regionName) {
        return safehouseStorage.getAllSafehouses().join().stream()
                .filter(safehouse -> safehouse.getRegionName().equals(regionName))
                .findAny();
    }

    /**
     * Gets the safehouse where the given location is in.
     *
     * @param location the inside location
     * @return the safehouse or an empty optional if the location is not in a safehouse
     */
    public Optional<Safehouse> getSafehouseAt(Location location) {
        ApplicableRegionSet regionSet = regionManager.getApplicableRegions(BukkitAdapter.asBlockVector(location));
        List<String> regionsAtLocation = regionSet.getRegions().stream()
                .map(ProtectedRegion::getId)
                .toList();

        return safehouseStorage.getAllSafehouses().join().stream()
                .filter(safehouse -> regionsAtLocation.contains(safehouse.getRegionName()))
                .findAny();
    }

    /**
     * Gets the safehouse with the given id.
     *
     * @param safehouseID the id of the safehouse
     * @return the safehouse with the given id or an empty optional if no safehouse with the given id exists
     */
    public Optional<Safehouse> getSafehouseFromID(int safehouseID) {
        return safehouseStorage.getAllSafehouses().join().stream()
                .filter(safehouse -> safehouse.getId() == safehouseID)
                .findAny();
    }

    /**
     * Tries to get the safehouse associated with the given door.
     *
     * @param door the entrance door
     * @return the safehouse or an empty optional if the door is not a safehouse door
     */
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
                .filter(Safehouse::isClaimed)
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
     * <p>
     * If a player raids a house, them, their party, the previous owner, and their party
     * members are allowed to enter the house for 10 minutes after the raid.
     *
     * @param safehouse  the safehouse
     * @param playerUuid the player UUId
     * @return {@code true} if they can access, {@code false} otherwise
     */
    public boolean canAccess(Safehouse safehouse, UUID playerUuid) {
        if (playerUuid.equals(safehouse.getOwner()) || safehouse.getCanClaimPlayers().contains(playerUuid)) {
            return true;
        }

        PartiesAPI api = Parties.getApi();
        Party playerParty = api.getPartyOfPlayer(playerUuid);
        if (playerParty != null) {
            for (UUID canClaimPlayerUUID : safehouse.getCanClaimPlayers()) {
                Party canClaimPlayerParty = api.getPartyOfPlayer(canClaimPlayerUUID);
                if (canClaimPlayerParty != null && playerParty.getId().equals(canClaimPlayerParty.getId())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Gets the nearest safehouse unclaimed safehouse of type {@link Safehouse.Type#SAFEHOUSE} to the passed location.
     *
     * @param from the starting location
     * @return the closest safehouse, or an empty optional if no safehouse was found
     */
    public Optional<Safehouse> getNearestUnclaimedSafehouse(Location from) {
        return safehouseStorage.getAllSafehouses().join().stream()
                .filter(Predicate.not(Safehouse::isClaimed))
                .filter(safehouse -> safehouse.getDoorLocation() != null)
                .filter(safehouse -> safehouse.getType() == Safehouse.Type.SAFEHOUSE)
                .filter(safehouse -> safehouse.getDoorLocation().getWorld().equals(from.getWorld()))
                .min(Comparator.comparingDouble(safehouse -> safehouse.getDoorLocation().distanceSquared(from)));
        // todo test the comparator above
    }

    /**
     * Gets a list containing all the names of the WG regions where a safehouse is configured.
     *
     * @return all WG region names
     */
    public List<String> getSafehouseRegionNamesIds() {
        return safehouseStorage.getAllSafehouses().join().stream()
                .<String>mapMulti((safehouse, objectConsumer) -> {
                    objectConsumer.accept(Integer.toString(safehouse.getId()));
                    objectConsumer.accept(safehouse.getRegionName());
                })
                .toList();
    }

    /**
     * Gets a list containing all the claimed safehouses.
     *
     * @return all claimed safehouses
     */
    public List<Safehouse> getClaimedSafehouses() {
        return safehouseStorage.getAllSafehouses().join().stream()
                .filter(Safehouse::isClaimed)
                .toList();
    }

    /**
     * Checks whether raids are enabled at the current time.
     * Players can break into houses owned by online players regardless of whether raids are enabled.
     *
     * @return {@code true} if raid are enabled, {@code false} otherwise
     */
    public boolean areRaidsEnabled() {
        return raidEnabled;
    }

    /**
     * Gets for how many seconds the given player needs to right-click the safehouse door to break in.
     *
     * @param player    the player
     * @param safehouse the safehouse they are trying to break into
     * @return the duration in seconds
     */
    public int getBreakInDuration(Player player, Safehouse safehouse) {
        int result = 5 + safehouse.getDoorLevel() * 5;

        // we can assume that when this method is called, the active story exists and is in the cache
        //noinspection OptionalGetWithoutIsPresent
        Character playerCharacter = plugin.getStoryStorage().getActiveStory(player.getUniqueId()).join()
                .get().character();

        if (playerCharacter == Character.LOCKSMITH) {
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
    @SuppressWarnings("unused")
    public int getClaimingDuration(Player player, Safehouse safehouse) {
        return 3;
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
        NBTItem nbtItem = NBTItem.get(item);
        return nbtItem.hasType() && "KEY".equals(nbtItem.getString("MMOITEMS_ITEM_ID"));
    }

    /**
     * Checks whether the given item is a lockpick
     * These items can be used to break in houses.
     *
     * @param item the item to check
     * @return {@code true} if the item is a lockpick, {@code false} otherwise
     */
    public boolean isLockpick(ItemStack item) {
        if (item == null) {
            return false;
        }
        NBTItem nbtItem = NBTItem.get(item);
        return nbtItem.hasType() && "LOCKPICK".equals(nbtItem.getString("MMOITEMS_ITEM_ID"));
    }

    /**
     * Checks whether the given item is a crowbar
     * These items can be used to break in houses.
     *
     * @param item the item to check
     * @return {@code true} if the item is a crowbar, {@code false} otherwise
     */
    public boolean isCrowbar(ItemStack item) {
        if (item == null) {
            return false;
        }
        NBTItem nbtItem = NBTItem.get(item);
        return nbtItem.hasType() && "CROWBAR".equals(nbtItem.getString("MMOITEMS_ITEM_ID"));
    }

    /**
     * Dispatches the commands that should be executed when a player claims a safehouse.
     *
     * @param playerName the name of the player who lost their safehouse
     */
    public void dispatchSafehouseClaimCommands(String playerName) {
        safehouseClaimCommands.stream()
                .map(cmd -> cmd.replace("{name}", playerName))
                .forEach(cmd -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd));
    }

    /**
     * Dispatches the commands that should be executed when a player loses their safehouse.
     *
     * @param playerName the name of the player who lost their safehouse
     */
    public void dispatchSafehouseLossCommands(String playerName) {
        safehouseLossCommands.stream()
                .map(cmd -> cmd.replace("{name}", playerName))
                .forEach(cmd -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd));
    }

    public void shutdown() {
        try {
            raidTimeScheduler.shutdown();
            raidTimeScheduler.awaitTermination(10, TimeUnit.SECONDS);
        } catch (Exception ignored) {
        }
    }

}
