package com.gamersafer.minecraft.ablockalypse.listener;

import com.gamersafer.minecraft.ablockalypse.AblockalypsePlugin;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;

public class PlayerItemConsumeListener implements Listener {

    private final AblockalypsePlugin plugin;

    public PlayerItemConsumeListener(AblockalypsePlugin plugin) {
        this.plugin = plugin;
    }

    @SuppressWarnings("unused")
    @EventHandler
    private void onItemConsume(PlayerItemConsumeEvent event) {
        if (event.getItem().getType() == Material.MILK_BUCKET) {
            Player player = event.getPlayer();

            // try to give back the permanent backstory effects
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                plugin.giveCharacterPermanentPotionEffect(player);
            }, 1L);
        }
    }

}
