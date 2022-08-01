package com.gamersafer.minecraft.ablockalypse.listener;

import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public class EntityInteractEntityListener implements Listener {

    @SuppressWarnings("unused")
    @EventHandler
    private void onWolfInteract(PlayerInteractEntityEvent event) {
        if (event.getRightClicked().getType() == org.bukkit.entity.EntityType.WOLF) {
            Wolf wolf = (Wolf) event.getRightClicked();

            if (wolf.isTamed() && !wolf.isSitting()) {
                event.setCancelled(true);
            }
        }
    }

}
