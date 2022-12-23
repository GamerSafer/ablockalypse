package com.gamersafer.minecraft.ablockalypse.listener;

import com.gamersafer.minecraft.ablockalypse.AblockalypsePlugin;
import com.gamersafer.minecraft.ablockalypse.safehouse.Safehouse;
import com.gamersafer.minecraft.ablockalypse.safehouse.SafehouseManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.Optional;

@SuppressWarnings("unused")
public class PlayerBuildListener implements Listener {

    private final AblockalypsePlugin plugin;
    private final SafehouseManager safehouseManager;

    public PlayerBuildListener(AblockalypsePlugin plugin, SafehouseManager safehouseManager) {
        this.plugin = plugin;
        this.safehouseManager = safehouseManager;
    }

    @EventHandler
    private void onBlockPlace(BlockPlaceEvent event) {
        Location blockLocation = event.getBlock().getLocation();
        Optional<Safehouse> safehouseOptional = safehouseManager.getSafehouseAt(blockLocation);

        if (safehouseOptional.isPresent()) {
            Player player = event.getPlayer();
            if (!safehouseOptional.get().isOwner(player)) {
                player.sendMessage(plugin.getMessage("safehouse-block-place"));
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    private void onBlockBreak(BlockBreakEvent event) {
        Location blockLocation = event.getBlock().getLocation();
        Optional<Safehouse> safehouseOptional = safehouseManager.getSafehouseAt(blockLocation);

        if (safehouseOptional.isPresent()) {
            Player player = event.getPlayer();
            if (!safehouseOptional.get().isOwner(player)) {
                player.sendMessage(plugin.getMessage("safehouse-block-break"));
                event.setCancelled(true);
            } else if (!SafehouseManager.WIPE_BLOCKS.contains(event.getBlock().getType())) {
                player.sendMessage(plugin.getMessage("safehouse-block-break-blocked"));
                event.setCancelled(true);
            }
        }

        // todo test whether this listener is invoked when breaking ItemsAdder furniture
    }

    // todo allow only safehouse owner to break furniture inside safehouses. furniture is made of
    //  armour stands and in some cases, barriers

}
