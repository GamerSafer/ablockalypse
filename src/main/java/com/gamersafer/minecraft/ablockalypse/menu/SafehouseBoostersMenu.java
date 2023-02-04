package com.gamersafer.minecraft.ablockalypse.menu;

import com.gamersafer.minecraft.ablockalypse.AblockalypsePlugin;
import com.gamersafer.minecraft.ablockalypse.safehouse.Booster;
import com.gamersafer.minecraft.ablockalypse.safehouse.BoosterManager;
import com.gamersafer.minecraft.ablockalypse.safehouse.Safehouse;
import com.gamersafer.minecraft.ablockalypse.safehouse.SafehouseManager;
import com.gamersafer.minecraft.ablockalypse.util.FormatUtil;
import com.gamersafer.minecraft.ablockalypse.util.ItemUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SafehouseBoostersMenu implements InventoryHolder {

    public static final Map<Integer, Booster> BOOSTER_SLOTS = Map.of(
            20, Booster.SPEED,
            21, Booster.STRENGTH,
            22, Booster.RESISTANCE,
            23, Booster.REGENERATION,
            24, Booster.JUMP_BOOST,
            29, Booster.LESS_HUNGER,
            30, Booster.LESS_THIRST,
            31, Booster.NIGHT_VISION,
            32, Booster.LUCK,
            33, Booster.KNOCKBACK
    );

    private static final ItemStack BLACK_PANE = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
    private static final ItemStack RED_PANE = new ItemStack(Material.RED_STAINED_GLASS_PANE);
    private final Inventory inventory;
    private final Player player;
    private final Safehouse safehouse;

    private static final ItemStack BACK = new ItemStack(Material.ARROW);
    static {
        BACK.editMeta((meta) -> {
            meta.displayName(Component.text("Back", Style.style(NamedTextColor.DARK_RED,
                    TextDecoration.ITALIC.withState(TextDecoration.State.FALSE),
                    TextDecoration.BOLD.withState(TextDecoration.State.TRUE)
            )));
        });
    }
    private static final ItemStack CLOSE = new ItemStack(Material.BARRIER);
    static {
        CLOSE.editMeta((meta) -> {
            meta.displayName(Component.text("Close", Style.style(NamedTextColor.DARK_RED,
                    TextDecoration.ITALIC.withState(TextDecoration.State.FALSE),
                    TextDecoration.BOLD.withState(TextDecoration.State.TRUE)
            )));
        });
    }

    public SafehouseBoostersMenu(Player player, Safehouse safehouse) {
        this.player = player;
        this.safehouse = safehouse;
        if (!safehouse.isOwner(player)) {
            throw new IllegalArgumentException("Player is not the owner of the safehouse");
        }

        this.inventory = Bukkit.createInventory(this, 9 * 6, FormatUtil.color("&4&lSafehouse Boosters"));

        insertPanes();
        insertBoosterItems();
    }

    public Safehouse getSafehouse() {
        return safehouse;
    }

    public void open() {
        player.playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
        player.openInventory(getInventory());
    }

    private void insertBoosterItems() {
        for (int slot : BOOSTER_SLOTS.keySet()) {
            Booster booster = BOOSTER_SLOTS.get(slot);
            ItemStack head = booster.getHead();
            ItemMeta headMeta = head.getItemMeta();
            headMeta.displayName(Component.text(booster.getDisplayName()).color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD));

            List<Component> lore = new ArrayList<>();
            if (player.hasPermission(booster.getPermission())) {
                if (safehouse.getActiveBoosters().contains(booster)) {
                    lore.add(Component.text("Active").color(NamedTextColor.GREEN));
                } else {
                    lore.add(Component.text("Inactive").color(NamedTextColor.RED));
                }
            } else {
                // we can assume that if the owner doesn't have the permission to activate the booster
                // it can't be active. boosters permissions can't be revoked.
                lore.add(Component.text("You can't activate this booster!").color(NamedTextColor.RED));
            }
            headMeta.lore(lore);

            head.setItemMeta(headMeta);
            if (safehouse.getActiveBoosters().contains(booster)) {
                ItemUtil.addItemGlow(head);
            }
            inventory.setItem(slot, head);
        }
    }

    @SuppressWarnings("DuplicatedCode")
    private void insertPanes() {
        for (int i = 0; i <= 9; i++) {
            inventory.setItem(i, BLACK_PANE);
        }
        for (int i = 10; i <= 16; i++) {
            inventory.setItem(i, RED_PANE);
        }
        for (int i = 17; i <= 18; i++) {
            inventory.setItem(i, BLACK_PANE);
        }
        inventory.setItem(19, RED_PANE);
        inventory.setItem(25, RED_PANE);
        for (int i = 26; i <= 27; i++) {
            inventory.setItem(i, BLACK_PANE);
        }
        inventory.setItem(28, RED_PANE);
        inventory.setItem(34, RED_PANE);
        for (int i = 35; i <= 36; i++) {
            inventory.setItem(i, BLACK_PANE);
        }
        for (int i = 37; i <= 43; i++) {
            inventory.setItem(i, RED_PANE);
        }
        for (int i = 44; i <= 53; i++) {
            inventory.setItem(i, BLACK_PANE);
        }

        for (Booster booster : Booster.values()) {
            inventory.addItem(booster.getHead());
        }

        inventory.setItem(45, BACK);
        inventory.setItem(49, CLOSE);
    }

    public void handleClick(int slot, SafehouseManager safehouseManager, BoosterManager boosterManager) {
        if (slot == 45) {
            player.performCommand("dm open safehousemain");
            return;
        }
        if (slot == 49) {
            player.closeInventory();
            return;
        }

        if (BOOSTER_SLOTS.containsKey(slot)) {
            Booster booster = BOOSTER_SLOTS.get(slot);
            if (player.hasPermission(booster.getPermission())) {
                if (safehouse.getActiveBoosters().contains(booster)) {
                    safehouse.deactivateBooster(booster);
                    player.sendMessage(AblockalypsePlugin.getInstance().getMessage("safehouse-booster-deactivated")
                            .replace("{booster}", booster.getDisplayName()));
                } else {
                    int maxBoosters = safehouse.getType().getMaxBoostersAmount();
                    if (safehouse.activateBooster(booster)) {
                        player.sendMessage(AblockalypsePlugin.getInstance().getMessage("safehouse-booster-activated")
                                .replace("{booster}", booster.getDisplayName()));
                    } else {
                        player.sendMessage(AblockalypsePlugin.getInstance().getMessage("safehouse-booster-max")
                                .replace("{max}", Integer.toString(maxBoosters)));
                        return;
                    }
                }
                //refresh menu
                insertBoosterItems();

                // refresh boosters for players inside the safehouse
                safehouseManager.getEntitiesInSafehouse(safehouse).stream()
                        .filter(entity -> entity instanceof Player)
                        .map(Player.class::cast)
                        .forEach(boosterManager::tryGiveBoosters);
            }
            // we can assume that if the owner doesn't have the permission to activate the booster it is not active
        }
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
