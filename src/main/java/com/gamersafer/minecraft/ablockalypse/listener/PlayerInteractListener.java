package com.gamersafer.minecraft.ablockalypse.listener;

import com.gamersafer.minecraft.ablockalypse.safehouse.Safehouse;
import com.gamersafer.minecraft.ablockalypse.safehouse.SafehouseManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public class PlayerInteractListener implements Listener {

    private final SafehouseManager safehouseManager;

    public PlayerInteractListener(SafehouseManager safehouseManager) {
        this.safehouseManager = safehouseManager;
    }

    @SuppressWarnings("unused")
    @EventHandler
    private void onPlayerInteract(PlayerInteractEvent event) {
        // try to get the safehouse associated with the potentially clicked door
        Optional<Safehouse> clickedSafehouse = safehouseManager.getSafehouseFromDoor(event.getClickedBlock());
        if (clickedSafehouse.isPresent()) {
            Safehouse safehouse = clickedSafehouse.get();
            Player player = event.getPlayer();

            if (safehouse.canAccess(player.getUniqueId())) {
                // teleport player in/out the safehouse
                Location tpLocation;
                if (safehouseManager.isInsideSafehouse(safehouse, player.getLocation())) {
                    tpLocation = safehouse.getOutsideLocation();
                } else {
                    tpLocation = safehouse.getSpawnLocation();
                }
                if (tpLocation != null) {
                    player.teleport(tpLocation);
                }
            } else {
                ItemStack item = event.getItem();
                if (safehouse.getOwner() == null && player.isSneaking()) {
                    // todo check whether they are trying to break in and make sure they are allowed to do so
                    //  integrate with mmoitem

                    // todo check whether they have a key in their hand to claim the house
                    if (safehouseManager.getSafehouseFromUuid(player.getUniqueId()).isPresent()) {
                        // todo the player already has a safehouse, can't claim/break in another one
                    }
                }
            }
        }
    }

}
