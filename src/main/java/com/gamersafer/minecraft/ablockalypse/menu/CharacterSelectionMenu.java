package com.gamersafer.minecraft.ablockalypse.menu;

import com.gamersafer.minecraft.ablockalypse.AblockalypsePlugin;
import com.gamersafer.minecraft.ablockalypse.Character;
import com.gamersafer.minecraft.ablockalypse.util.FormatUtil;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CharacterSelectionMenu implements InventoryHolder {

    private final Inventory inventory;
    private final Player player;

    public CharacterSelectionMenu(Player player) {
        this.player = player;

        String title = FormatUtil.color(AblockalypsePlugin.getInstance().getConfig().getString("menu.character-selection.title"));
        int size = AblockalypsePlugin.getInstance().getConfig().getInt("menu.character-selection.size");

        this.inventory = Bukkit.createInventory(this, size, title);

        insertItems();
    }

    public void open() {
        player.playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
        player.openInventory(getInventory());
    }

    private void insertItems() {
        for (Character character : Character.values()) {
            ItemStack icon = character.getMenuItem();

            if (!player.hasPermission("ablockalypse.canselect." + character.name().toLowerCase())) {
                ItemMeta iconMeta = icon.getItemMeta();

                List<String> lore = iconMeta.getLore();
                List<String> toAdd = FormatUtil.color(AblockalypsePlugin.getInstance().getConfig().getStringList("menu.character-selection.no-permission"));
                //noinspection ConstantConditions
                lore.addAll(toAdd);

                iconMeta.setLore(lore);
                icon.setItemMeta(iconMeta);
            }

            inventory.setItem(character.getMenuIndex(), icon);
        }
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
