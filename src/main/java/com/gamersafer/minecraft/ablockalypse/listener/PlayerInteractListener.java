package com.gamersafer.minecraft.ablockalypse.listener;

import com.gamersafer.minecraft.ablockalypse.AblockalypsePlugin;
import com.gamersafer.minecraft.ablockalypse.database.api.StoryStorage;
import com.gamersafer.minecraft.ablockalypse.safehouse.BoosterManager;
import com.gamersafer.minecraft.ablockalypse.safehouse.Safehouse;
import com.gamersafer.minecraft.ablockalypse.safehouse.SafehouseManager;
import me.NoChance.PvPManager.PvPlayer;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

public class PlayerInteractListener implements Listener {

    private final static int MILLIS_BETWEEN_INTERACTIONS = 250;

    private final AblockalypsePlugin plugin;
    private final SafehouseManager safehouseManager;
    private final StoryStorage storyStorage;
    private final BoosterManager boosterManager;

    private final Map<UUID, ClickDuration> breakingInClicks;
    private final Map<UUID, ClickDuration> claimingClicks;
    private final Map<UUID, Integer> warnedOwners;

    public PlayerInteractListener(AblockalypsePlugin plugin, SafehouseManager safehouseManager, BoosterManager boosterManager,
                                  StoryStorage storyStorage) {
        this.plugin = plugin;
        this.safehouseManager = safehouseManager;
        this.boosterManager = boosterManager;
        this.storyStorage = storyStorage;
        this.breakingInClicks = new HashMap<>();
        this.claimingClicks = new HashMap<>();
        this.warnedOwners = new HashMap<>();
    }

    @SuppressWarnings("unused")
    @EventHandler
    private void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if ((event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) && event.getItem() != null && event.getItem().getType() == Material.COMPASS) {
            // make compass point to the nearest unclaimed safehouse
            player.sendMessage(plugin.getMessage("compass-searching"));

            // todo the operation below could be resource intensive, add an x seconds cooldown ?
            Optional<Safehouse> nearestUnclaimedSafehouse = safehouseManager.getNearestUnclaimedSafehouse(player.getLocation());
            if (nearestUnclaimedSafehouse.isPresent()) {
                player.sendMessage(plugin.getMessage("compass-found"));
                player.setCompassTarget(nearestUnclaimedSafehouse.get().getDoorLocation());
            } else {
                player.sendMessage(plugin.getMessage("compass-none"));
            }
            event.setCancelled(true);
        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
            Block clickedBlock = event.getClickedBlock();
            // try to get the safehouse associated with the potentially clicked door
            Optional<Safehouse> clickedSafehouse = safehouseManager.getSafehouseFromDoor(clickedBlock);
            if (clickedSafehouse.isPresent()) {
                event.setCancelled(true);
                Safehouse safehouse = clickedSafehouse.get();

                // allow anyone to exit safehouses
                if (safehouseManager.isInsideSafehouse(safehouse, player.getLocation()) && safehouse.getOutsideLocation() != null) {
                    // remove boosters and teleport outside
                    boosterManager.removeBoosters(player);
                    player.teleport(safehouse.getOutsideLocation());
                    return;
                }

                if (safehouseManager.canAccess(safehouse, player.getUniqueId())) {
                    // the player is outside the safehouse, teleport them inside and remove boosters
                    if (safehouse.getSpawnLocation() != null) {
                        player.teleport(safehouse.getSpawnLocation());
                        boosterManager.tryGiveBoosters(player);
                    }
                } else {
                    ItemStack item = event.getItem();

                    if (safehouseManager.isKey(item)) {
                        // prevent pve players from claiming safehouses
                        PvPlayer pvPlayer = PvPlayer.get(player);
                        if (!pvPlayer.hasPvPEnabled()) {
                            player.sendMessage(plugin.getMessage("claim-pve-no"));
                            return;
                        }

                        // after a house is raided, only the previous owner and the player who raided it can claim it
                        if (safehouse.canClaim(player.getUniqueId())) {
                            int claimingDurationSeconds = safehouseManager.getClaimingDuration(player, safehouse);

                            // tell the player they need to sneak
                            if (!player.isSneaking()) {
                                player.sendMessage(plugin.getMessage("claim-not-sneaking"));
                                return;
                            }

                            // check if the player who is trying to claim this house already owns one house
                            if (warnedOwners.getOrDefault(player.getUniqueId(), -1) != safehouse.getId()
                                    && safehouseManager.getSafehouseFromOwnerUuid(player.getUniqueId()).isPresent()) {
                                // the player already owns a house. notify they will lose their house and its content if they claim this one
                                player.sendMessage(plugin.getMessage("claim-already-own"));
                                warnedOwners.put(player.getUniqueId(), safehouse.getId());
                                return;
                            }

                            if (claimingClicks.containsKey(player.getUniqueId())) {
                                if (claimingClicks.get(player.getUniqueId()).getMillisSinceFirstClick() >= claimingDurationSeconds * 1000L) {
                                    if (claimingClicks.get(player.getUniqueId()).getMillisSinceLastClick() <= MILLIS_BETWEEN_INTERACTIONS) {
                                        Optional<Safehouse> previousSafehouseOpt = safehouseManager.getSafehouseFromOwnerUuid(player.getUniqueId());
                                        previousSafehouseOpt.ifPresent(Safehouse::removeOwner);
                                        Optional<Player> previousOwnerOpt = safehouse.getPreviousOwnerPlayer();
                                        if (previousOwnerOpt.isPresent() && !previousOwnerOpt.get().getUniqueId().equals(player.getUniqueId())) {
                                            previousOwnerOpt.get().sendMessage(plugin.getMessage("claim-done-previous-owner"));
                                        }
                                        // clear the house if it was claimed by someone who was not the previous owner,
                                        if (previousOwnerOpt.isEmpty() || !previousOwnerOpt.get().getUniqueId().equals(player.getUniqueId())) {
                                            safehouseManager.clearSafehouseContent(safehouse);
                                        }
                                        safehouse.setOwner(player.getUniqueId());
                                        player.sendMessage(plugin.getMessage("claim-done"));
                                        if (previousSafehouseOpt.isPresent()) {
                                            player.sendMessage(plugin.getMessage("claim-done-lost"));
                                        }
                                        consumeItemInHand(player);
                                    } else {
                                        // start over
                                        startClaiming(player, safehouse, claimingDurationSeconds);
                                    }
                                } else {
                                    if (claimingClicks.get(player.getUniqueId()).getMillisSinceLastClick() <= MILLIS_BETWEEN_INTERACTIONS) {
                                        if (claimingClicks.get(player.getUniqueId()).recordNewClick()) {
                                            long secondsLeft = claimingDurationSeconds - claimingClicks.get(player.getUniqueId()).getMillisSinceFirstClick() / 1000;
                                            player.sendMessage(plugin.getMessage("claim-during")
                                                    .replace("{seconds}", Long.toString(secondsLeft)));
                                        }
                                    } else {
                                        // start over
                                        startClaiming(player, safehouse, claimingDurationSeconds);
                                    }
                                }
                            } else {
                                startClaiming(player, safehouse, claimingDurationSeconds);
                            }
                        } else {
                            if (safehouse.wasRecentlyRaided()) {
                                // the house was recently raided and this player is not allowed to claim it yet
                                player.sendMessage(plugin.getMessage("raid-recent-no"));
                            } else {
                                player.sendMessage(plugin.getMessage("claim-already-owned"));
                            }
                        }
                    } else if (safehouseManager.isLockpickOrCrowbar(item)) {
                        // prevent pve players from breaking into safehouses
                        PvPlayer pvPlayer = PvPlayer.get(player);
                        if (!pvPlayer.hasPvPEnabled()) {
                            player.sendMessage(plugin.getMessage("break-in-pve-no"));
                            return;
                        }

                        // return if the player is already allowed to claim the house. in that case they don't need to break in,
                        if (safehouse.canClaim(player.getUniqueId())) {
                            player.sendMessage(plugin.getMessage("break-in-not-needed"));
                            return;
                        }

                        // make sure the owner is online or at this time of the day break-ins are allowed
                        if (safehouse.getOwnerPlayer().isEmpty() && !safehouseManager.areRaidsEnabled()) {
                            player.sendMessage(plugin.getMessage("break-in-disallow"));
                            return;
                        }

                        // tell the player they need to sneak
                        if (!player.isSneaking()) {
                            player.sendMessage(plugin.getMessage("break-in-not-sneaking"));
                            return;
                        }

                        // duration based on door level
                        int breakingInDurationSeconds = safehouseManager.getBreakInDuration(player, safehouse);

                        if (breakingInClicks.containsKey(player.getUniqueId())) {
                            if (breakingInClicks.get(player.getUniqueId()).getMillisSinceFirstClick() >= breakingInDurationSeconds * 1000L) {
                                if (breakingInClicks.get(player.getUniqueId()).getMillisSinceLastClick() <= MILLIS_BETWEEN_INTERACTIONS) {
                                    Optional<Player> previousOwnerOpt = safehouse.getOwnerPlayer();
                                    safehouse.handleBreakIn(player.getUniqueId());
                                    previousOwnerOpt.ifPresent(previousOwner -> previousOwner.sendMessage(plugin.getMessage("break-in-done-owner")));
                                    player.sendMessage(plugin.getMessage("break-in-done"));
                                    consumeItemInHand(player);
                                } else {
                                    // start over
                                    startBreakIn(player, safehouse, breakingInDurationSeconds);
                                }
                            } else {
                                if (breakingInClicks.get(player.getUniqueId()).getMillisSinceLastClick() <= MILLIS_BETWEEN_INTERACTIONS) {
                                    if (breakingInClicks.get(player.getUniqueId()).recordNewClick()) {
                                        long secondsLeft = breakingInDurationSeconds - breakingInClicks.get(player.getUniqueId()).getMillisSinceFirstClick() / 1000;
                                        player.sendMessage(plugin.getMessage("break-in-during")
                                                .replace("{seconds}", Long.toString(secondsLeft)));
                                    }
                                } else {
                                    // start over
                                    startBreakIn(player, safehouse, breakingInDurationSeconds);
                                }
                            }
                        } else {
                            startBreakIn(player, safehouse, breakingInDurationSeconds);
                        }
                    } else if ((!claimingClicks.containsKey(player.getUniqueId()) || claimingClicks.get(player.getUniqueId()).getMillisSinceLastClick() > MILLIS_BETWEEN_INTERACTIONS + 1000)
                            && (!breakingInClicks.containsKey(player.getUniqueId()) || breakingInClicks.get(player.getUniqueId()).getMillisSinceLastClick() > MILLIS_BETWEEN_INTERACTIONS + 1000)) {
                        if (safehouse.isClaimed()) {
                            player.sendMessage(plugin.getMessage("safehouse-click-claimed"));
                        } else {
                            player.sendMessage(plugin.getMessage("safehouse-click-not-claimed"));
                        }
                    }
                }
            }
        }
    }

    private void startBreakIn(Player intruder, Safehouse safehouse, int breakingInDurationSecond) {
        Optional<Player> houseOwnerOpt = safehouse.getOwnerPlayer();
        // the isPresent check below is not needed. players can break into houses only when the owner is online
        houseOwnerOpt.ifPresent(houseOwner -> {
            // get the character name of the intruder and notify the homeowner
            storyStorage.getActiveStory(intruder.getUniqueId()).thenAccept(storyOpt -> {
                if (storyOpt.isEmpty()) {
                    plugin.getLogger().log(Level.SEVERE, "The player " + intruder.getUniqueId() + " doesn't have an active story and they were able to try breaking into the safehouse " + safehouse.getId());
                    return;
                }
                houseOwner.sendMessage(plugin.getMessage("break-in-started-owner")
                        .replace("{character-name}", storyOpt.get().characterName()));
            });
        });
        breakingInClicks.put(intruder.getUniqueId(), new ClickDuration());
        intruder.sendMessage(plugin.getMessage("break-in-started")
                .replace("{seconds}", Integer.toString(breakingInDurationSecond)));
    }

    private void startClaiming(Player intruder, Safehouse safehouse, int breakingInDurationSecond) {
        Optional<Player> previousOwnerOpt = safehouse.getPreviousOwnerPlayer();
        previousOwnerOpt.ifPresent(previousOwner -> {
            // notify the previous owner if they haven't moved on and claimed another safehouse
            if (safehouseManager.getSafehouseFromOwnerUuid(previousOwner.getUniqueId()).isEmpty()) {
                previousOwner.sendMessage(plugin.getMessage("claim-started-owner"));
            }
        });
        claimingClicks.put(intruder.getUniqueId(), new ClickDuration());
        intruder.sendMessage(plugin.getMessage("claim-started")
                .replace("{seconds}", Integer.toString(breakingInDurationSecond)));
    }

    private void consumeItemInHand(Player player) {
        int itemAmount = player.getInventory().getItemInMainHand().getAmount();
        if (itemAmount > 1) {
            player.getInventory().getItemInMainHand().setAmount(itemAmount - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }
    }

    private static class ClickDuration {
        private final long firstClick;
        private long lastClick;

        private int updateSecond = 1;

        public ClickDuration() {
            this.firstClick = System.currentTimeMillis();
            this.lastClick = this.firstClick;
        }

        /**
         * Records a new click.
         *
         * @return true once every second
         */
        public boolean recordNewClick() {
            this.lastClick = System.currentTimeMillis();

            if (getMillisSinceFirstClick() >= updateSecond * 1000L && getMillisSinceFirstClick() < (updateSecond + 1) * 1000L) {
                updateSecond++;
                return true;
            }
            return false;
        }

        public long getMillisSinceFirstClick() {
            return System.currentTimeMillis() - firstClick;
        }

        public long getMillisSinceLastClick() {
            return System.currentTimeMillis() - lastClick;
        }

    }

}
