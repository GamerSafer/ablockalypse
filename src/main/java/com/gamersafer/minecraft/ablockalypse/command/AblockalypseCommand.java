package com.gamersafer.minecraft.ablockalypse.command;

import com.gamersafer.minecraft.ablockalypse.AblockalypsePlugin;
import com.gamersafer.minecraft.ablockalypse.Character;
import com.gamersafer.minecraft.ablockalypse.database.api.StoryStorage;
import com.gamersafer.minecraft.ablockalypse.location.LocationManager;
import com.gamersafer.minecraft.ablockalypse.menu.CharacterSelectionMenu;
import com.gamersafer.minecraft.ablockalypse.menu.PastStoriesMenu;
import com.gamersafer.minecraft.ablockalypse.menu.SafehouseBoostersMenu;
import com.gamersafer.minecraft.ablockalypse.menu.SafehouseDoorMenu;
import com.gamersafer.minecraft.ablockalypse.safehouse.Booster;
import com.gamersafer.minecraft.ablockalypse.safehouse.BoosterManager;
import com.gamersafer.minecraft.ablockalypse.safehouse.Safehouse;
import com.gamersafer.minecraft.ablockalypse.safehouse.SafehouseManager;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import dev.lone.itemsadder.api.FontImages.PlayerHudsHolderWrapper;
import dev.lone.itemsadder.api.FontImages.PlayerQuantityHudWrapper;
import io.papermc.lib.PaperLib;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.Door;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Wolf;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AblockalypseCommand implements CommandExecutor, TabCompleter {

    public static final String COMMAND = "ablockalypse";
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.00");

    private final AblockalypsePlugin plugin;
    private final StoryStorage storyStorage;
    private final LocationManager locationManager;
    private final SafehouseManager safehouseManager;
    private final BoosterManager boosterManager;

    public AblockalypseCommand(AblockalypsePlugin plugin, StoryStorage storyStorage, LocationManager locationManager,
                               SafehouseManager safehouseManager, BoosterManager boosterManager) {
        this.plugin = plugin;
        this.storyStorage = storyStorage;
        this.locationManager = locationManager;
        this.safehouseManager = safehouseManager;
        this.boosterManager = boosterManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String alias, @NotNull String[] args) {
        // reload command
        if (args.length == 1 && args[0].equalsIgnoreCase("reload") && hasPermission(sender, Permission.CMD_RELOAD)) {
            plugin.reload();
            plugin.sendMessage(sender, "config-reloaded");
            return true;
        } else if (args.length == 3 && args[0].equalsIgnoreCase("story") && hasPermission(sender, Permission.CMD_STORY)) {
            // make sure the target player is online
            Player player = Bukkit.getPlayer(args[1]);
            if (player == null) {
                sender.sendMessage("Unable to get player " + args[1] + ". Ignoring the itemsadder action they were trying to do.");
                return false;
            }

            if (args[2].equalsIgnoreCase("nextlevel")) {
                // try to increase the level of target's active story, if present
                storyStorage.getActiveStory(player.getUniqueId()).thenAccept(story -> {
                    if (story.isPresent()) {
                        // increase level and run commands
                        plugin.sync(() -> {
                            int newLevel = story.get().increaseLevel();
                            storyStorage.updateLevel(story.get()).thenRun(() -> plugin.sync(() -> {
                                for (String levelUpCmd : story.get().character().getCommandsOnLevelUp(newLevel)) {
                                    levelUpCmd = levelUpCmd.replace("{name}", player.getName())
                                            .replace("{uuid}", player.getUniqueId().toString());
                                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), levelUpCmd);
                                }
                                sender.sendMessage("Active backstory level of player " + player.getName() + " set to " + newLevel);
                            })).exceptionally(throwable -> {
                                throwable.printStackTrace();
                                return null;
                            });
                        });
                    } else {
                        sender.sendMessage("The player " + player.getName() + " doesn't have an active story");
                    }
                });
                return true;
            }
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

//            float value;
//            try {
//                value = Float.parseFloat(args[3]);
//            } catch (NumberFormatException exception) {
//                sender.sendMessage("Unable to parse float value " + args[3] + ". Ignoring the itemsadder action.");
//                return false;
//            }

            String action = args[2].toLowerCase();
            if (action.equals("heal")) {
                // Nurse - Raises effectiveness of bandages/ first aid kits  (action = heal)
                storyStorage.getActiveStory(player.getUniqueId()).thenAccept(story -> {
                    if (story.isPresent() && story.get().character() == Character.NURSE) {
                        plugin.sync(() -> {
                            //noinspection ConstantConditions
                            double maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
                            player.setHealth(Math.min(player.getHealth() + 2, maxHealth));
                        });
                    }
                });
            } else if (action.equals("thirst_increase")) {
                // increase the thirst if the player as the active thirst booster
                if (boosterManager.hasBooster(player, Booster.LESS_THIRST)) {
                    PlayerHudsHolderWrapper huds = new PlayerHudsHolderWrapper(player);
                    PlayerQuantityHudWrapper thirstHud = new PlayerQuantityHudWrapper(huds, "ablockalypse:thirst_bar");
                    if (thirstHud.exists()) {
                        thirstHud.setFloatValue(Math.min(thirstHud.getFloatValue() + 2, 10f));
                    } else {
                        sender.sendMessage("Unable to increase the thirst bar of the player " + player.getName() + " who has a less_thirst booster. They don't have a thirst bar.");
                    }
                }

                // Survivalist - (Hunger decreases slower and) Thirst increases faster (water items are more effective)
                storyStorage.getActiveStory(player.getUniqueId()).thenAccept(story -> {
                    if (story.isPresent() && story.get().character() == Character.SURVIVALIST) {
                        plugin.sync(() -> {
                            PlayerHudsHolderWrapper huds = new PlayerHudsHolderWrapper(player);
                            PlayerQuantityHudWrapper thirstHud = new PlayerQuantityHudWrapper(huds, "ablockalypse:thirst_bar");
                            if (thirstHud.exists()) {
                                thirstHud.setFloatValue(Math.min(thirstHud.getFloatValue() + 2, 10f));
                            } else {
                                sender.sendMessage("Unable to increase the thirst bar of the survivalist " + player.getName() + ". They don't have a thirst bar.");
                            }
                        });
                    }
                });
            } else {
                throw new IllegalArgumentException("Unknown action " + action);
            }

            return true;
        } else if (args.length == 3 && args[0].equalsIgnoreCase("reset") && hasPermission(sender, Permission.CMD_RESET)) {
            boolean targetAll = args[1].equalsIgnoreCase("all");
            boolean endAllStories = false;
            OfflinePlayer targetPlayer = null;
            if (!targetAll) {
                targetPlayer = Bukkit.getOfflinePlayer(args[1]);
                if (!targetPlayer.hasPlayedBefore()) {
                    sender.sendMessage("Unable to find the player " + args[1]);
                    return false;
                }
            }

            CompletableFuture<Void> resetFuture;
            if (args[2].equalsIgnoreCase("current")) {
                if (targetAll) {
                    resetFuture = storyStorage.resetActiveStory();
                } else {
                    resetFuture = storyStorage.resetActiveStory(targetPlayer.getUniqueId());
                }
            } else if (args[2].equalsIgnoreCase("history")) {
                endAllStories = true;
                if (targetAll) {
                    resetFuture = storyStorage.resetAllStories();
                } else {
                    resetFuture = storyStorage.resetAllStories(targetPlayer.getUniqueId());
                }
            } else {
                sender.sendMessage("Invalid subcommand");
                return false;
            }

            OfflinePlayer finalTargetPlayer = targetPlayer;
            boolean finalEndAllStories = endAllStories;
            resetFuture.thenRun(() -> {
                if (finalEndAllStories) {
                    plugin.sync(() -> {
                        Collection<? extends OfflinePlayer> targetPlayers;
                        if (targetAll) {
                            targetPlayers = Bukkit.getOnlinePlayers();
                        } else {
                            targetPlayers = List.of(finalTargetPlayer);
                        }
                        // reset walking speed and potion effects
                        targetPlayers.forEach(offlinePlayer -> {
                            Player onlinePlayer = offlinePlayer.getPlayer();
                            if (onlinePlayer != null) {
                                onlinePlayer.setWalkSpeed(0.2f); // set default walking speed. it's changed for sprinters
                                Arrays.stream(PotionEffectType.values()).forEach(onlinePlayer::removePotionEffect);
                            }
                        });
                        // remove all tamed wolves
                        List<UUID> targetPlayersUuids = targetPlayers.stream().map(OfflinePlayer::getUniqueId).toList();
                        for (World world : Bukkit.getWorlds()) {
                            for (Entity wolfEntity : world.getEntitiesByClasses(Wolf.class)) {
                                //noinspection ConstantConditions
                                if (((Tameable) wolfEntity).isTamed() && targetPlayersUuids.contains(((Tameable) wolfEntity).getOwner().getUniqueId())) {
                                    wolfEntity.remove();
                                }
                            }
                        }
                        sender.sendMessage("Reset completed!");
                    });
                } else {
                    sender.sendMessage("Reset completed!");
                }
            });
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
                if (args.length == 1) {
                    new CharacterSelectionMenu(player).open();
                } else if (args.length == 2) {
                    Character character = Character.valueOf(args[1].toLowerCase());
                    Player targetPlayer = player;
                    if (args.length == 3) {
                        targetPlayer = Bukkit.getPlayer(args[2]);
                    }

                    CharacterSelectionMenu.pickCharacter(targetPlayer, storyStorage, locationManager, plugin, character);
                }
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
            } else if (args[0].equalsIgnoreCase("safehouse")) {
                Optional<Safehouse> safehouseOptional = safehouseManager.getSafehouseFromOwnerUuid(player.getUniqueId());
                if (args[1].equalsIgnoreCase("teleport")) {
                    if (safehouseOptional.isEmpty()) {
                        player.sendMessage(plugin.getMessage("safehouse-not-owner"));
                        return false;
                    }
                    PaperLib.teleportAsync(player, safehouseOptional.get().getSpawnLocation());
                    return true;
                } else if (args[1].equalsIgnoreCase("boosters")) {
                    if (safehouseOptional.isEmpty()) {
                        player.sendMessage(plugin.getMessage("safehouse-not-owner"));
                        return false;
                    }

                    new SafehouseBoostersMenu(player, safehouseOptional.get()).open();
                    return true;
                } else if (args[1].equalsIgnoreCase("door")) {
                    if (safehouseOptional.isEmpty()) {
                        player.sendMessage(plugin.getMessage("safehouse-not-owner"));
                        return false;
                    }
                    new SafehouseDoorMenu(safehouseOptional.get()).open(player);
                    return true;
                } else if (args[1].equalsIgnoreCase("claimedlist") && hasPermission(sender, Permission.CMD_SAFEHOUSE)) {
                    List<Safehouse> claimedSafehouses = safehouseManager.getClaimedSafehouses();
                    if (claimedSafehouses.isEmpty()) {
                        player.sendMessage(plugin.getMessage("safehouse-not-owner"));
                    } else {
                        int idx = 1;
                        for (Safehouse claimedSafehouse : claimedSafehouses) {
                            String ownerName = Optional.of(Bukkit.getOfflinePlayer(claimedSafehouse.getOwner()))
                                    .map(OfflinePlayer::getName)
                                    .orElse("Unknown");
                            player.sendMessage(plugin.getMessage("safehouse-claimedlist-entry")
                                    .replace("{idx}", String.valueOf(idx++))
                                    .replace("{region}", claimedSafehouse.getRegionName())
                                    .replace("{owner_name}", ownerName)
                            );
                        }
                    }
                    return true;
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("cinematic")) {
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
        } else if (args.length > 2 && args[0].equalsIgnoreCase("safehouse") && hasPermission(sender, Permission.CMD_SAFEHOUSE)) {
            if (args.length == 3 && args[2].equalsIgnoreCase("create")) {
                String regionName = args[1];

                // make sure there isn't another safehouse in that region
                if (safehouseManager.getSafehouseFromRegion(regionName).isPresent()) {
                    player.sendMessage(plugin.getMessage("safehouse-create-invalid-exists")
                            .replace("{name}", regionName));
                    return false;
                }

                // try to get the region
                RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(player.getWorld()));
                ProtectedRegion region = Objects.requireNonNull(regionManager).getRegion(regionName);
                if (region == null) {
                    player.sendMessage(plugin.getMessage("safehouse-create-invalid-region")
                            .replace("{name}", regionName));
                    return false;
                }

                safehouseManager.createSafehouse(regionName).thenAccept(safehouse -> {
                    player.sendMessage(plugin.getMessage("safehouse-create-success")
                            .replace("{id}", Integer.toString(safehouse.getId())));

                });
                return true;
            }

            Optional<Safehouse> safehouseOpt;
            try {
                int safehouseID = Integer.parseInt(args[1]);
                safehouseOpt = safehouseManager.getSafehouseFromID(safehouseID);
            } catch (NumberFormatException ignore) {
                safehouseOpt = safehouseManager.getSafehouseFromRegion(args[1]);
            }

            // make sure that safehouse exists
            if (safehouseOpt.isEmpty()) {
                player.sendMessage(plugin.getMessage("safehouse-cmd-invalid-id")
                        .replace("{id-region}", args[1]));
                return false;
            }
            Safehouse safehouse = safehouseOpt.get();

            if (args.length == 3) {
                if (args[2].equalsIgnoreCase("delete")) {
                    // try to delete the safehouse
                    safehouseManager.deleteSafehouse(safehouse).thenRun(() -> {
                        player.sendMessage(plugin.getMessage("safehouse-deleted"));

                        safehouse.getOwnerPlayer().ifPresent(owner -> {
                            // notify owner
                            owner.sendMessage(plugin.getMessage("safehouse-deleted-owner"));
                        });
                    });
                    return true;
                } else if (args[2].equalsIgnoreCase("teleport")) {
                    if (safehouse.getSpawnLocation() == null) {
                        player.sendMessage(plugin.getMessage("safehouse-spawn-not-set"));
                        return false;
                    }

                    player.sendMessage(plugin.getMessage("safehouse-spawn-teleporting"));
                    player.teleport(safehouse.getSpawnLocation());
                    return true;
                } else if (args[2].equalsIgnoreCase("setdoor")) {
                    Block targetBlock = player.getTargetBlock(4);

                    if (targetBlock == null || !targetBlock.getType().name().endsWith("_DOOR")) {
                        player.sendMessage(plugin.getMessage("safehouse-door-set-invalid"));
                        return false;
                    }

                    Door door = (Door) targetBlock.getState().getBlockData();
                    if (door.getHalf() != Bisected.Half.TOP) {
                        player.sendMessage(plugin.getMessage("safehouse-door-set-invalid-half"));
                        return false;
                    }
                    door.setOpen(false);
                    safehouse.setDoorLocation(targetBlock.getLocation());
                    player.sendMessage(plugin.getMessage("safehouse-door-set"));
                    return true;
                } else if (args[2].equalsIgnoreCase("setspawn")) {
                    Location spawnLocation = player.getLocation();

                    if (!safehouseManager.isInsideSafehouse(safehouse, spawnLocation)) {
                        player.sendMessage(plugin.getMessage("safehouse-spawn-set-invalid"));
                        return false;
                    }

                    safehouse.setSpawnLocation(spawnLocation);
                    player.sendMessage(plugin.getMessage("safehouse-spawn-set"));
                    return true;
                } else if (args[2].equalsIgnoreCase("setoutside")) {
                    Location outsideLocation = player.getLocation();

                    if (safehouseManager.isInsideSafehouse(safehouse, outsideLocation)) {
                        player.sendMessage(plugin.getMessage("safehouse-outside-set-invalid"));
                        return false;
                    }

                    safehouse.setOutsideLocation(outsideLocation);
                    player.sendMessage(plugin.getMessage("safehouse-outside-set"));
                    return true;
                } else if (args[2].equalsIgnoreCase("nextdoorlevel")) {
                    int updatedLevel = safehouse.increaseDoorLevel();
                    player.sendMessage(plugin.getMessage("safehouse-door-level-increased")
                            .replace("{level}", Integer.toString(updatedLevel)));
                    return true;
                }
            } else if (args.length == 4) {
                if (args[2].equalsIgnoreCase("setowner")) {
                    if (safehouse.getOwner() != null) {
                        player.sendMessage(plugin.getMessage("safehouse-setowner-already"));
                        return false;
                    }

                    String targetName = args[3];
                    Player targetPlayer = Bukkit.getPlayer(targetName);
                    if (targetPlayer == null) {
                        player.sendMessage(plugin.getMessage("safehouse-setowner-not-found"));
                        return false;
                    }

                    safehouse.setOwner(targetPlayer.getUniqueId());
                    safehouseManager.dispatchSafehouseClaimCommands(targetPlayer.getName());
                    player.sendMessage(plugin.getMessage("safehouse-setowner-set"));

                    return true;
                } else if (args[2].equalsIgnoreCase("settype")) {
                    Optional<Safehouse.Type> safehouseTypeOpt = Safehouse.Type.fromString(args[3]);
                    if (safehouseTypeOpt.isEmpty()) {
                        player.sendMessage(plugin.getMessage("safehouse-settype-invalid"));
                        return false;
                    }

                    safehouse.setType(safehouseTypeOpt.get());
                    player.sendMessage(plugin.getMessage("safehouse-settype-set"));
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
            case 0, 1 ->
                    List.of("reload", "backstory", "stories", "hospital", "cinematic", "spawnpoint", "safehouse", "story", "reset");
            case 2 -> {
                if (args[0].equalsIgnoreCase("stories") || args[0].equalsIgnoreCase("story")) {
                    yield Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
                } else if (args[0].equalsIgnoreCase("spawnpoint") || args[0].equalsIgnoreCase("hospital")) {
                    yield List.of("list", "add", "remove");
                } else if (args[0].equalsIgnoreCase("cinematic")) {
                    yield Arrays.stream(Character.values())
                            .map(Character::name)
                            .map(String::toLowerCase)
                            .collect(Collectors.toList());
                } else if (args[0].equalsIgnoreCase("safehouse")) {
                    Stream<String> result = Stream.of("teleport", "boosters", "door");
                    if (sender.hasPermission(Permission.CMD_SAFEHOUSE.toString())) {
                        result = Stream.concat(safehouseManager.getSafehouseRegionNamesIds().stream(), Stream.concat(result, Stream.of("claimedlist")));
                    }
                    yield result.toList();
                } else if (args[0].equalsIgnoreCase("reset")) {
                    yield Stream.concat(Bukkit.getOnlinePlayers().stream().map(Player::getName), Stream.of("all")).toList();
                } else if (args[0].equalsIgnoreCase("backstory")) {
                    yield Arrays.asList(Character.values()).stream().map((character) -> character.name().toLowerCase()).toList();
                }
                yield Collections.emptyList();
            }
            case 3 -> {
                if (args[0].equalsIgnoreCase("cinematic")) {
                    yield List.of("set", "tp");
                } else if (args[0].equalsIgnoreCase("story")) {
                    yield List.of("nextlevel");
                } else if (args[0].equalsIgnoreCase("reset")) {
                    yield List.of("current", "history");
                } else if (args[0].equalsIgnoreCase("safehouse") && sender.hasPermission(Permission.CMD_SAFEHOUSE.toString())) {
                    yield List.of("delete", "create", "teleport", "setdoor", "setspawn", "setoutside", "nextdoorlevel", "setowner", "settype");
                }
                yield Collections.emptyList();
            }
            case 4 -> {
                if (args[0].equalsIgnoreCase("safehouse") && args[2].equalsIgnoreCase("settype") && sender.hasPermission(Permission.CMD_SAFEHOUSE.toString())) {
                    yield Arrays.stream(Safehouse.Type.values()).map(Safehouse.Type::name).toList();
                }
                yield Collections.emptyList();
            }
            default -> Collections.emptyList();
        };
    }
}
