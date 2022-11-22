package com.gamersafer.minecraft.ablockalypse.safehouse;

import com.gamersafer.minecraft.ablockalypse.AblockalypsePlugin;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class BoosterManager {

    private final AblockalypsePlugin plugin;
    private final SafehouseManager safehouseManager;
    private final Multimap<UUID, Booster> activeBoosters;
    private final Map<UUID, BukkitTask> activeBoostersTask;

    public BoosterManager(AblockalypsePlugin plugin, SafehouseManager safehouseManager) {
        this.plugin = plugin;
        this.safehouseManager = safehouseManager;
        this.activeBoosters = HashMultimap.create();
        this.activeBoostersTask = new HashMap<>();
    }

    @SuppressWarnings("UnusedReturnValue")
    public boolean tryGiveBoosters(Player player) {
        Optional<Safehouse> safehouseOptional = safehouseManager.getSafehouseAt(player.getLocation());
        if (safehouseOptional.isPresent() && safehouseManager.canAccess(safehouseOptional.get(), player.getUniqueId())) {
            giveBoosters(player, safehouseOptional.get());
            return true;
        }
        return false;
    }

    private void giveBoosters(Player player, Safehouse safehouse) {
        // remove current boosters if the user already has a running task
        if (activeBoostersTask.containsKey(player.getUniqueId()) && !activeBoostersTask.get(player.getUniqueId()).isCancelled()) {
            removeBoosters(player);
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

                Set<Booster> boosters = Arrays.stream(Booster.values())
                        .filter(booster -> safehouseOwner.get().hasPermission(booster.getPermission()))
                        .collect(Collectors.toSet());
                activeBoosters.putAll(player.getUniqueId(), boosters);
                boosters.forEach(booster -> booster.give(player));
            }
        }.runTaskTimer(plugin, 0L, 20 * 5L);
        activeBoostersTask.put(player.getUniqueId(), giveBoosterTask);
    }

    public boolean hasBooster(Player player, Booster booster) {
        return activeBoosters.containsEntry(player.getUniqueId(), booster);
    }

    public void removeBoosters(Player player) {
        activeBoosters.removeAll(player.getUniqueId());
        Optional.ofNullable(activeBoostersTask.remove(player.getUniqueId()))
                .filter(Predicate.not(BukkitTask::isCancelled))
                .ifPresent(BukkitTask::cancel);
    }

}
