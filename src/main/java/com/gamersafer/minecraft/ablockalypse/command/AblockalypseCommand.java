package com.gamersafer.minecraft.ablockalypse.command;

import com.gamersafer.minecraft.ablockalypse.AblockalypsePlugin;
import com.gamersafer.minecraft.ablockalypse.Character;
import com.gamersafer.minecraft.ablockalypse.location.LocationManager;
import com.gamersafer.minecraft.ablockalypse.menu.CharacterSelectionMenu;
import io.papermc.lib.PaperLib;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class AblockalypseCommand implements CommandExecutor, TabCompleter {

    public static final String COMMAND = "ablockalypse";
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.00");

    private final AblockalypsePlugin plugin;
    private final LocationManager locationManager;

    public AblockalypseCommand(AblockalypsePlugin plugin, LocationManager locationManager) {
        this.plugin = plugin;
        this.locationManager = locationManager;
    }

    // todo permissions

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String alias, @NotNull String[] args) {
        // reload command
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.reload();
            plugin.sendMessage(sender, "config-reloaded");
            return true;
        }

        // only players will be allowed to run commands after this point
        if (!(sender instanceof Player player)) {
            if (args.length == 0) {
                sendHelpMenu(sender);
            } else {
                plugin.sendMessage(sender, "player-only");
            }
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
                        PaperLib.teleportAsync(player, hospitalLoc.get());
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
                                    .replace("{x}", DECIMAL_FORMAT.format(loc.getX()))
                                    .replace("{y}", DECIMAL_FORMAT.format(loc.getY()))
                                    .replace("{z}", DECIMAL_FORMAT.format(loc.getZ()))
                                    .replace("{pitch}", DECIMAL_FORMAT.format(loc.getPitch()))
                                    .replace("{yaw}", DECIMAL_FORMAT.format(loc.getYaw()))
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
                        PaperLib.teleportAsync(player, cinematicLoc.get());
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
        plugin.getMessageList("help-menu").forEach(sender::sendMessage);
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!command.getName().equalsIgnoreCase(COMMAND)) {
            return Collections.emptyList();
        }

        return switch (args.length) {
            case 0, 1 -> List.of("reload", "backstory", "hospital", "cinematic", "spawnpoint");
            case 2 -> {
                if (args[0].equalsIgnoreCase("hospital")) {
                    yield List.of("set", "tp");
                } else if (args[0].equalsIgnoreCase("spawnpoint")) {
                    yield List.of("list", "add", "remove");
                } else if (args[0].equalsIgnoreCase("cinematic")) {
                    yield Arrays.stream(Character.values())
                            .map(Character::name)
                            .map(String::toLowerCase)
                            .collect(Collectors.toList());
                }
                yield Collections.emptyList();
            }
            case 3 -> {
                if (args[0].equalsIgnoreCase("cinematic")) {
                    yield List.of("set", "tp");
                }
                yield Collections.emptyList();
            }
            default -> Collections.emptyList();
        };
    }
}
