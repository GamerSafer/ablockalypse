package com.gamersafer.minecraft.ablockalypse.command;

import com.gamersafer.minecraft.ablockalypse.AblockalypsePlugin;
import com.gamersafer.minecraft.ablockalypse.location.LocationManager;
import com.gamersafer.minecraft.ablockalypse.location.LocationType;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

public class AblockalypseCommand implements CommandExecutor {

    private final AblockalypsePlugin plugin;
    private final LocationManager locationManager;

    public AblockalypseCommand(AblockalypsePlugin plugin) {
        this.plugin = plugin;
        this.locationManager = null; // todo get as argument
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String alias, @NotNull String[] args) {
        // reload command
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.reload();
            return true;
        }

        // only players will be allowed to run commands after this point
        if (!(sender instanceof Player player)) {
            plugin.sendMessage(sender, "player-only");
            return false;
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("location")) {
            LocationType locationType;
            // validate location type
            try {
                locationType = LocationType.valueOf(args[1]);
            } catch (IllegalArgumentException e) {
                player.sendMessage(plugin.getMessage("invalid-location-type")
                        .replace("{input}", args[1])
                        .replace("{valid}", Arrays.stream(LocationType.values())
                                .map(LocationType::name)
                                .map(String::toLowerCase)
                                .collect(Collectors.joining(", "))));
                return false;
            }

            if (args[2].equalsIgnoreCase("list")) {
                Collection<Location> locations = locationManager.getLocations(locationType);

                // check whether there are locations to display
                if (locations.isEmpty()) {
                    player.sendMessage(plugin.getMessage("location-list-none"));
                    return true;
                }

                player.sendMessage(plugin.getMessage("location-list-header")
                        .replace("{type}", locationType.name().toLowerCase()));

                for (Location loc : locations) {
                    // todo click to teleport
                    player.sendMessage(plugin.getMessage("location-list-entry")
                            .replace("{world}", loc.getWorld().getName())
                            .replace("{x}", Double.toString(loc.getX()))
                            .replace("{y}", Double.toString(loc.getY()))
                            .replace("{z}", Double.toString(loc.getZ()))
                            .replace("{pitch}", Float.toString(loc.getPitch()))
                            .replace("{yaw}", Float.toString(loc.getYaw()))
                    );
                }
                return true;
            } else if (args[2].equalsIgnoreCase("add")) {
                // try to add the location
                boolean added = locationManager.addLocation(locationType, player.getLocation());

                // send feedback message
                String messageId = added ? "location-add-success" : "location-add-already";
                player.sendMessage(plugin.getMessage(messageId));

                return true;
            } else if (args[2].equalsIgnoreCase("remove")) {
                Location location = player.getLocation();

                boolean removed = locationManager.addLocation(locationType, player.getLocation());
                String messageId = removed ? "location-remove-success" : "location-remove-none";
                player.sendMessage(plugin.getMessage(messageId));

                return true;
            }
        }

        sendHelpMenu(sender);
        return true;
    }

    private void sendHelpMenu(CommandSender sender) {

        /*
        /ablockalypse cinematic <type> set
         /ablockalypse location <type> list
         /ablockalypse location <type> set
         /ablockalypse location <type> remove
         */
    }

}
