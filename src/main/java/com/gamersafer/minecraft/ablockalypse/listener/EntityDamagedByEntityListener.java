package com.gamersafer.minecraft.ablockalypse.listener;

import com.gamersafer.minecraft.ablockalypse.safehouse.Booster;
import com.gamersafer.minecraft.ablockalypse.safehouse.BoosterManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class EntityDamagedByEntityListener implements Listener {

    private final BoosterManager boosterManager;

    public EntityDamagedByEntityListener(BoosterManager boosterManager) {
        this.boosterManager = boosterManager;
    }

    @SuppressWarnings("unused")
    @EventHandler
    private void onEntityDamageEntity(EntityDamageByEntityEvent event) {
        // simulate knockback effect if the player has the KNOCKBACK booster
        if (event.getDamager() instanceof Player player) {
            if (boosterManager.hasBooster(player, Booster.KNOCKBACK)) {
                double factor = 2;
                if (player.isSprinting()) {
                    factor += .5;
                }
                event.getEntity().setVelocity(player.getLocation().getDirection().multiply(factor));
            }
        }
    }

}
