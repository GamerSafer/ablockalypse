package com.gamersafer.minecraft.ablockalypse.safehouse;

import com.gamersafer.minecraft.ablockalypse.AblockalypsePlugin;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Manges safehouse boosters.
 * See {@link Safehouse}, {@link Booster}, and {@link com.gamersafer.minecraft.ablockalypse.menu.SafehouseBoostersMenu}
 */
public class BoosterManager {

    private final AblockalypsePlugin plugin;
    private final SafehouseManager safehouseManager;
    private final Map<UUID, BukkitTask> activeBoostersTask;

    public BoosterManager(AblockalypsePlugin plugin, SafehouseManager safehouseManager) {
        this.plugin = plugin;
        this.safehouseManager = safehouseManager;
        this.activeBoostersTask = new HashMap<>();
    }

    /**
     * Tries give to the given player the boosters active in the safehouse they are in.
     *
     * @param player the player to give the boosters to
     * @return true if the player was given boosters, false otherwise
     */
    @SuppressWarnings("UnusedReturnValue")
    public boolean tryGiveBoosters(Player player) {
        Optional<Safehouse> safehouseOptional = safehouseManager.getSafehouseAt(player.getLocation());
        if (safehouseOptional.isPresent() && safehouseOptional.get().getOwner() != null && safehouseManager.canAccess(safehouseOptional.get(), player.getUniqueId())) {
            giveBoosters(player, safehouseOptional.get());
            return true;
        }
        return false;
    }

    /**
     * Gives the player the active boosters from the safehouse.
     *
     * @param player    the player to give the boosters to
     * @param safehouse the safehouse to get the boosters from. It's assumed the player is inside this house.
     */
    private void giveBoosters(Player player, Safehouse safehouse) {
        // remove current boosters if the user already has a running task
        boolean noActiveBoosters = safehouse.getActiveBoosters().isEmpty();
        if ((activeBoostersTask.containsKey(player.getUniqueId()) && !activeBoostersTask.get(player.getUniqueId()).isCancelled())
                || noActiveBoosters) {
            removeBoosters(player);
            if (noActiveBoosters) {
                return;
            }
        }
        BukkitTask giveBoosterTask = new BukkitRunnable() {
            @Override
            public void run() {
                // remove boosters if the player or the safehouse owner is not online
                Optional<Player> safehouseOwner = safehouse.getOwnerPlayer();
                if (!player.isOnline() || safehouseOwner.isEmpty()) {
                    this.cancel();
                    removeBoosters(player);
                    return;
                }

                safehouse.getActiveBoosters().forEach(booster -> booster.give(player));
            }
        }.runTaskTimer(plugin, 0L, 20 * 5L);
        activeBoostersTask.put(player.getUniqueId(), giveBoosterTask);
    }

    /**
     * Checks whether the safehouse where the player is standing has the passed booster.
     *
     * @param player  the player
     * @param booster the booster
     * @return true if the player is standing in a safehouse that has the passed booster active
     */
    public boolean hasBooster(Player player, Booster booster) {
        Optional<Safehouse> safehouseOpt = safehouseManager.getSafehouseAt(player.getLocation());
        return safehouseOpt.map(safehouse -> safehouse.getActiveBoosters().contains(booster)).orElse(false);
    }

    /**
     * Removes all boosters from the player.
     *
     * @param player the player
     */
    public void removeBoosters(Player player) {
        Optional.ofNullable(activeBoostersTask.remove(player.getUniqueId()))
                .filter(Predicate.not(BukkitTask::isCancelled))
                .ifPresent(BukkitTask::cancel);
    }

}
