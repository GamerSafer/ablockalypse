package com.gamersafer.minecraft.ablockalypse.menu;

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
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SafehouseDoorMenu implements InventoryHolder {

    public static final int SLOT_LEVEL_1 = 20;
    public static final int SLOT_LEVEL_2 = 22;
    public static final int SLOT_LEVEL_3 = 24;

    private static final ItemStack BLACK_PANE = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
    private static final ItemStack RED_PANE = new ItemStack(Material.RED_STAINED_GLASS_PANE);
    private final Inventory inventory;
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

    public SafehouseDoorMenu(Safehouse safehouse) {
        this.safehouse = safehouse;
        this.inventory = Bukkit.createInventory(this, 9 * 6, FormatUtil.color("&4&lDoor Upgrades"));

        insertItems();
    }

    public Safehouse getSafehouse() {
        return safehouse;
    }

    public void open(Player player) {
        player.playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
        player.openInventory(getInventory());
    }

    private void insertItems() {
        int doorLevel = safehouse.getDoorLevel();

        // insert doors
        ItemStack door1 = ItemUtil.createItem(Material.WARPED_DOOR, "&3Level 1", List.of("&7Break in duration: &a15 seconds"));
        ItemStack door2 = ItemUtil.createItem(Material.SPRUCE_DOOR, "&3Level 2", List.of("&7Break in duration: &a30 seconds", "&7Cost: &a" + SafehouseManager.getDoorLevelCost(2) + " planks and " + SafehouseManager.getDoorLevelCost(2) + " sheet metal"));
        ItemStack door3 = ItemUtil.createItem(Material.IRON_DOOR, "&3Level 3", List.of("&7Break in duration: &a45 seconds", "&7Cost: &a" + SafehouseManager.getDoorLevelCost(3) + " planks and " + SafehouseManager.getDoorLevelCost(2) + " sheet metal"));

        ItemUtil.addItemGlow(door1);
        if (doorLevel >= 2) {
            ItemUtil.addItemGlow(door2);
        }
        if (doorLevel >= 3) {
            ItemUtil.addItemGlow(door3);
        }

        inventory.setItem(SLOT_LEVEL_1, door1);
        inventory.setItem(SLOT_LEVEL_2, door2);
        inventory.setItem(SLOT_LEVEL_3, door3);

        // insert panes
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
        inventory.setItem(45, BACK);
        inventory.setItem(49, CLOSE);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

}
