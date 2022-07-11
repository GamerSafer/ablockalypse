package com.gamersafer.minecraft.ablockalypse.command;

import com.gamersafer.minecraft.ablockalypse.AblockalypsePlugin;
import com.gamersafer.minecraft.ablockalypse.Character;
import com.gamersafer.minecraft.ablockalypse.database.api.StoryStorage;
import com.gamersafer.minecraft.ablockalypse.location.LocationManager;
import com.gamersafer.minecraft.ablockalypse.menu.CharacterSelectionMenu;
import com.gamersafer.minecraft.ablockalypse.menu.PastStoriesMenu;
import dev.lone.itemsadder.api.FontImages.PlayerHudsHolderWrapper;
import dev.lone.itemsadder.api.FontImages.PlayerQuantityHudWrapper;
import io.papermc.lib.PaperLib;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class AblockalypseCommand implements CommandExecutor, TabCompleter {

    public static final String COMMAND = "ablockalypse";
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.00");

    private final AblockalypsePlugin plugin;
    private final StoryStorage storyStorage;
    private final LocationManager locationManager;

    public AblockalypseCommand(AblockalypsePlugin plugin, StoryStorage storyStorage, LocationManager locationManager) {
        this.plugin = plugin;
        this.storyStorage = storyStorage;
        this.locationManager = locationManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String alias, @NotNull String[] args) {
        // reload command
        if (args.length == 1 && args[0].equalsIgnoreCase("reload") && hasPermission(sender, Permission.CMD_RELOAD)) {
            plugin.reload();
            plugin.sendMessage(sender, "config-reloaded");
            return true;
        } else if (args.length == 4 && args[0].equalsIgnoreCase("itemsadder") && (sender instanceof ConsoleCommandSender)) {
            // /ablockalypse itemsadder <player> <action> <value>
            // command for internal use only. items adder doesn't call any event when a custom item is used. as a workaround
            // the item will dispatch this command adn we'll be able to listen to it here

            // make sure the player is online
            Player player = Bukkit.getPlayer(args[1]);
            if (player == null) {
                sender.sendMessage("Unable to get player " + args[1] + ". Ignoring the itemsadder action they were trying to do.");
                return false;
            }

            float value;
            try {
                value = Float.parseFloat(args[3]);
            } catch (NumberFormatException exception) {
                sender.sendMessage("Unable to parse float value " + args[3] + ". Ignoring the itemsadder action.");
                return false;
            }

            String action = args[2].toLowerCase();
            if (action.equals("heal")) {
                // Nurse - Raises effectiveness of bandages/ first aid kits  (action = heal)
                storyStorage.getActiveStory(player.getUniqueId()).thenAccept(story -> {
                    if (story.isPresent() && story.get().character() == Character.NURSE) {
                        plugin.sync(() -> {
                            //noinspection ConstantConditions
                            double maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
                            player.setHealth(Math.max(player.getHealth() + 2, maxHealth));
                        });
                    }
                });
            } else if (action.equals("thirst_increase")) {
                // Survivalist - (Hunger decreases slower and) Thirst increases faster (water items are more effective)
                storyStorage.getActiveStory(player.getUniqueId()).thenAccept(story -> {
                    if (story.isPresent() && story.get().character() == Character.SURVIVALIST) {
                        plugin.sync(() -> {
                            PlayerHudsHolderWrapper huds = new PlayerHudsHolderWrapper(player);
                            PlayerQuantityHudWrapper thirstHud = new PlayerQuantityHudWrapper(huds, "ablockalypse:thirst_bar");
                            if (thirstHud.exists()) {
                                thirstHud.setFloatValue(Math.max(thirstHud.getFloatValue() + 2, 10f));
                            } else {
                                // todo if the player is riding or is underwater the hud is not presnet.
                                //  this message may be sent too often. test
                                sender.sendMessage("Unable to increase the thirst bar of the survivalist " + player.getName() + ". They don't have a thirst bar.");
                            }
                        });
                    }
                });
            } else {
                throw new IllegalArgumentException("Unknown action " + action);
            }

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
            if (args[0].equalsIgnoreCase("backstory") && hasPermission(sender, Permission.CMD_BACKSTORY)) {
                // display the character selection menu. this subcommand is supposed to be used only by staff members for debug purposes
                new CharacterSelectionMenu(player).open();
                return true;
            } else if (args[0].equalsIgnoreCase("stories") && hasPermission(sender, Permission.CMD_STORIES_OWN)) {

                storyStorage.getAllStories(player.getUniqueId()).thenAccept(stories -> {
                    if (stories.isEmpty()) {
                        plugin.sendMessage(player, "stories-own-none");
                        return;

                    }

                    storyStorage.getPlaytime(player.getUniqueId()).thenAccept(playtime -> {
                        // create and open the menu
                        plugin.sync(() -> new PastStoriesMenu(stories, player.getName(), playtime).open(player));
                    });
                });

                return true;
            } else if (args[0].toLowerCase().startsWith("tploc")) {
                String messagePrefix = null;
                Optional<Location> location = Optional.empty();

                if (args[0].toLowerCase().startsWith("tploc-hospital:") && hasPermission(sender, Permission.CMD_HOSPITAL_TP)) {
                    messagePrefix = "hospital-tp";

                    // try to parse the location
                    try {
                        int locationHash = Integer.parseInt(args[0].split(":")[1]);
                        location = locationManager.getHospitalPoints().stream()
                                .filter(loc -> loc.hashCode() == locationHash)
                                .findFirst();
                    } catch (Exception ignore) {
                    }
                } else if (args[0].toLowerCase().startsWith("tploc-spawn:") && hasPermission(sender, Permission.CMD_SPAWNPOINT_TP)) {
                    messagePrefix = "spawn-tp";

                    // try to parse the location
                    try {
                        int locationHash = Integer.parseInt(args[0].split(":")[1]);
                        location = locationManager.getSpawnPoints().stream()
                                .filter(loc -> loc.hashCode() == locationHash)
                                .findFirst();
                    } catch (Exception ignore) {
                    }
                }

                if (messagePrefix != null) {
                    // try to teleport and send feedback message
                    if (location.isPresent()) {
                        PaperLib.teleportAsync(player, location.get());
                        player.playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
                        plugin.sendMessage(sender, messagePrefix + "-success");
                    } else {
                        plugin.sendMessage(sender, messagePrefix + "-invalid");
                    }
                    return true;
                }
            }
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("stories") && hasPermission(sender, Permission.CMD_STORIES_OTHERS)) {

                String targetName = args[1];
                UUID targetUuid = Bukkit.getPlayerUniqueId(targetName);

                if (targetUuid == null) {
                    player.sendMessage(plugin.getMessage("stories-target-invalid")
                            .replace("{name}", targetName));
                    return true;
                }

                storyStorage.getAllStories(targetUuid).thenAccept(stories -> {
                    if (stories.isEmpty()) {
                        player.sendMessage(plugin.getMessage("stories-other-none")
                                .replace("{name}", targetName));
                        return;
                    }

                    storyStorage.getPlaytime(targetUuid).thenAccept(playtime -> {
                        // create and open the menu
                        plugin.sync(() -> new PastStoriesMenu(stories, targetName, playtime).open(player));
                    });
                });
                return true;
            } else if (args[0].equalsIgnoreCase("spawnpoint")) {
                if (args[1].equalsIgnoreCase("list") && hasPermission(sender, Permission.CMD_SPAWNPOINT_LIST)) {
                    List<Location> spawnPoints = locationManager.getSpawnPoints();
                    if (spawnPoints.isEmpty()) {
                        player.sendMessage(plugin.getMessage("spawn-list-none"));
                    } else {
                        player.sendMessage(plugin.getMessage("spawn-list-header"));

                        for (Location loc : spawnPoints) {
                            String locMessage = plugin.getMessage("spawn-list-entry")
                                    .replace("{world}", loc.getWorld().getName())
                                    .replace("{x}", DECIMAL_FORMAT.format(loc.getX()))
                                    .replace("{y}", DECIMAL_FORMAT.format(loc.getY()))
                                    .replace("{z}", DECIMAL_FORMAT.format(loc.getZ()))
                                    .replace("{pitch}", DECIMAL_FORMAT.format(loc.getPitch()))
                                    .replace("{yaw}", DECIMAL_FORMAT.format(loc.getYaw()));

                            Component message = Component.text(locMessage).clickEvent(ClickEvent
                                    .runCommand("/ablockalypse tploc-spawn:" + loc.hashCode()));
                            player.sendMessage(message);
                        }
                    }

                    return true;
                } else if (args[1].equalsIgnoreCase("add") && hasPermission(sender, Permission.CMD_SPAWNPOINT_ADD)) {
                    // try to add the location
                    boolean added = locationManager.addSpawnPoint(player.getLocation());

                    // send feedback message
                    String messageId = added ? "spawn-add-success" : "spawn-add-already";
                    player.sendMessage(plugin.getMessage(messageId));

                    return true;
                } else if (args[1].equalsIgnoreCase("remove") && hasPermission(sender, Permission.CMD_SPAWNPOINT_REMOVE)) {
                    // try to remove the location and send feedback message
                    boolean removed = locationManager.removeSpawnPoint(player.getLocation());
                    String messageId = removed ? "spawn-remove-success" : "spawn-remove-none";
                    player.sendMessage(plugin.getMessage(messageId));

                    return true;
                }
            } else if (args[0].equalsIgnoreCase("hospital")) {
                if (args[1].equalsIgnoreCase("list") && hasPermission(sender, Permission.CMD_HOSPITAL_LIST)) {
                    List<Location> hospitalPoints = locationManager.getHospitalPoints();
                    if (hospitalPoints.isEmpty()) {
                        player.sendMessage(plugin.getMessage("hospital-list-none"));
                    } else {
                        player.sendMessage(plugin.getMessage("hospital-list-header"));

                        for (Location loc : hospitalPoints) {
                            String locMessage = plugin.getMessage("hospital-list-entry")
                                    .replace("{world}", loc.getWorld().getName())
                                    .replace("{x}", DECIMAL_FORMAT.format(loc.getX()))
                                    .replace("{y}", DECIMAL_FORMAT.format(loc.getY()))
                                    .replace("{z}", DECIMAL_FORMAT.format(loc.getZ()))
                                    .replace("{pitch}", DECIMAL_FORMAT.format(loc.getPitch()))
                                    .replace("{yaw}", DECIMAL_FORMAT.format(loc.getYaw()));

                            Component message = Component.text(locMessage).clickEvent(ClickEvent
                                    .runCommand("/ablockalypse tploc-hospital:" + loc.hashCode()));
                            player.sendMessage(message);
                        }
                    }

                    return true;
                } else if (args[1].equalsIgnoreCase("add") && hasPermission(sender, Permission.CMD_HOSPITAL_ADD)) {
                    // try to add the location
                    boolean added = locationManager.addHospitalLoc(player.getLocation());

                    // send feedback message
                    String messageId = added ? "hospital-add-success" : "hospital-add-already";
                    player.sendMessage(plugin.getMessage(messageId));

                    return true;
                } else if (args[1].equalsIgnoreCase("remove") && hasPermission(sender, Permission.CMD_HOSPITAL_REMOVE)) {
                    // try to remove the location and send feedback message
                    boolean removed = locationManager.removeHospitalLoc(player.getLocation());
                    String messageId = removed ? "hospital-remove-success" : "hospital-remove-none";
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

                if (args[2].equalsIgnoreCase("set") && hasPermission(sender, Permission.CMD_CINEMATIC_SET)) {

                    // set the location and send feedback message
                    locationManager.setCinematicLoc(character, player.getLocation());
                    player.sendMessage(plugin.getMessage("cinematic-location-set-success")
                            .replace("{character}", character.name().toLowerCase()));

                    return true;
                } else if (args[2].equalsIgnoreCase("tp") && hasPermission(sender, Permission.CMD_CINEMATIC_TP)) {

                    Optional<Location> cinematicLoc = locationManager.getCinematicLoc(character);
                    if (cinematicLoc.isPresent()) {
                        // teleport the player
                        player.sendMessage(plugin.getMessage("cinematic-location-tp")
                                .replace("{character}", character.name().toLowerCase()));
                        PaperLib.teleportAsync(player, cinematicLoc.get());
                        player.playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
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

    private boolean hasPermission(CommandSender sender, Permission... permission) {
        boolean hasPerm = Arrays.stream(permission).map(Permission::toString).allMatch(sender::hasPermission);
        if (!hasPerm) {
            plugin.sendMessage(sender, "no-permission");
        }
        return hasPerm;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!command.getName().equalsIgnoreCase(COMMAND)) {
            return Collections.emptyList();
        }

        return switch (args.length) {
            case 0, 1 -> List.of("reload", "backstory", "stories", "hospital", "cinematic", "spawnpoint");
            case 2 -> {
                if (args[0].equalsIgnoreCase("stories")) {
                    yield Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
                } else if (args[0].equalsIgnoreCase("spawnpoint") || args[0].equalsIgnoreCase("hospital")) {
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
