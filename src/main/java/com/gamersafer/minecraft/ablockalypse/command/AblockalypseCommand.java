package com.gamersafer.minecraft.ablockalypse.command;

import com.gamersafer.minecraft.ablockalypse.AblockalypsePlugin;
import com.gamersafer.minecraft.ablockalypse.Character;
import com.gamersafer.minecraft.ablockalypse.location.LocationManager;
import com.gamersafer.minecraft.ablockalypse.menu.CharacterSelectionMenu;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class AblockalypseCommand implements CommandExecutor {

    private final AblockalypsePlugin plugin;
    private final LocationManager locationManager;

    public AblockalypseCommand(AblockalypsePlugin plugin, LocationManager locationManager) {
        this.plugin = plugin;
        this.locationManager = locationManager;
    }

    // todo permissions and autocompletion

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

        if (args.length == 1) {
            if (args[0].equalsIgnoreCase("backstory")) {
                // display the character selection menu. this subcommand is supposed to be used only by staff members for debug purposes
                CharacterSelectionMenu.open(player);
                return true;
            }
        } else if (args.length == 2) {

            if (args[0].equalsIgnoreCase("hospital")) {
                if (args[1].equalsIgnoreCase("tp")) {
                    Optional<Location> hospitalLoc = locationManager.getHospital();

                    if (hospitalLoc.isPresent()) {
                        // teleport to the hospital
                        player.sendMessage(plugin.getMessage("hospital-location-tp"));
                        player.teleport(hospitalLoc.get());
                    } else {
                        // the location has not been set yet
                        player.sendMessage(plugin.getMessage("hospital-location-none"));
                    }

                    return true;
                } else if (args[1].equalsIgnoreCase("set")) {
                    // set location and send feedback message
                    locationManager.setHospital(player.getLocation());
                    player.sendMessage(plugin.getMessage("hospital-location-set"));
                    return true;
                }
            } else if (args[0].equalsIgnoreCase("spawnpoint")) {
                if (args[1].equalsIgnoreCase("list")) {
                    List<Location> spawnPoints = locationManager.getSpawnPoints();
                    if (spawnPoints.isEmpty()) {
                        player.sendMessage(plugin.getMessage("spawn-list-none"));
                    } else {
                        player.sendMessage(plugin.getMessage("spawn-list-header"));

                        for (Location loc : spawnPoints) {
                            // todo click to teleport
                            player.sendMessage(plugin.getMessage("spawn-list-entry")
                                    .replace("{world}", loc.getWorld().getName())
                                    .replace("{x}", Double.toString(loc.getX()))
                                    .replace("{y}", Double.toString(loc.getY()))
                                    .replace("{z}", Double.toString(loc.getZ()))
                                    .replace("{pitch}", Float.toString(loc.getPitch()))
                                    .replace("{yaw}", Float.toString(loc.getYaw()))
                            );
                        }
                    }

                    return true;
                } else if (args[1].equalsIgnoreCase("add")) {
                    // try to add the location
                    boolean added = locationManager.addSpawnPoint(player.getLocation());

                    // send feedback message
                    String messageId = added ? "spawn-add-success" : "spawn-add-already";
                    player.sendMessage(plugin.getMessage(messageId));

                    return true;
                } else if (args[1].equalsIgnoreCase("remove")) {
                    // try to remove the location and send feedback message
                    boolean removed = locationManager.removeSpawnPoint(player.getLocation());
                    String messageId = removed ? "spawn-remove-success" : "spawn-remove-none";
                    player.sendMessage(plugin.getMessage(messageId));

                    return true;
                }
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("cinematic")) {
                // validate character type
                Character character;
                try {
                    character = Character.valueOf(args[1].toUpperCase());
                } catch (IllegalArgumentException e) {
                    player.sendMessage(plugin.getMessage("invalid-character-type")
                            .replace("{input}", args[1])
                            .replace("{valid}", Arrays.stream(Character.values())
                                    .map(Character::name)
                                    .map(String::toLowerCase)
                                    .collect(Collectors.joining(", ")))
                    );
                    return true;
                }

                if (args[2].equalsIgnoreCase("set")) {

                    // set the location and send feedback message
                    locationManager.setCinematicLoc(character, player.getLocation());
                    player.sendMessage(plugin.getMessage("cinematic-location-set-success")
                            .replace("{character}", character.name().toLowerCase()));

                    return true;
                } else if (args[2].equalsIgnoreCase("tp")) {

                    Optional<Location> cinematicLoc = locationManager.getCinematicLoc(character);
                    if (cinematicLoc.isPresent()) {
                        // teleport the player
                        player.sendMessage(plugin.getMessage("cinematic-location-tp")
                                .replace("{character}", character.name().toLowerCase()));
                        player.teleport(cinematicLoc.get());
                    } else {
                        // the location is not present
                        player.sendMessage(plugin.getMessage("cinematic-location-none")
                                .replace("{character}", character.name().toLowerCase()));
                    }
                    return true;
                }
            }
        }

        sendHelpMenu(sender);
        return true;
    }

    private void sendHelpMenu(CommandSender sender) {
        // todo send messages
        /*
         /ablockalypse reload
         /ablockalypse backstory
         /ablockalypse hospital set
         /ablockalypse hospital tp
         /ablockalypse cinematic <character> set
         /ablockalypse cinematic <character> tp
         /ablockalypse spawnpoint list
         /ablockalypse spawnpoint add
         /ablockalypse spawnpoint remove
         */
    }

}
