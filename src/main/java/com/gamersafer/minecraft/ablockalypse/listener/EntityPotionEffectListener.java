package com.gamersafer.minecraft.ablockalypse.listener;

import com.gamersafer.minecraft.ablockalypse.AblockalypsePlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPotionEffectEvent;

public class EntityPotionEffectListener implements Listener {

    private final AblockalypsePlugin plugin;

    public EntityPotionEffectListener(AblockalypsePlugin plugin) {
        this.plugin = plugin;
    }

    @SuppressWarnings("unused")
    @EventHandler
    private void onEntityPotionEffect(EntityPotionEffectEvent event) {
        if (event.getEntity() instanceof Player player && event.getNewEffect() == null) {
            // try to give back the permanent backstory effects
            plugin.giveCharacterPermanentPotionEffect(player);
        }
    }

}
