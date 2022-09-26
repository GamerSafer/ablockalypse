package com.gamersafer.minecraft.ablockalypse.listener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkUnloadEvent;

import java.util.Arrays;
import java.util.UUID;

public class ChunkUnloadListener implements Listener {

    @SuppressWarnings("unused")
    @EventHandler
    private void onEntitiesUnload(ChunkUnloadEvent event) {
        // remove wolves tamed by dead players.
        // they should be removed in PlayerDeathListener, this is an extra check
        Arrays.stream(event.getChunk().getEntities())
                .filter(entity -> entity.getType() == EntityType.WOLF)
                .map(Wolf.class::cast)
                .filter(Wolf::isTamed)
                .filter(wolf -> {
                    UUID ownerUuid = wolf.getOwnerUniqueId();
                    if (ownerUuid != null) {
                        Player owner = Bukkit.getPlayer(ownerUuid);
                        return owner != null && owner.isDead();
                    }
                    return false;
                })
                .forEach(Entity::remove);
    }

}
