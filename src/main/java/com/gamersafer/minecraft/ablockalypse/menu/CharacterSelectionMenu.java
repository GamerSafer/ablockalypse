package com.gamersafer.minecraft.ablockalypse.menu;

import com.gamersafer.minecraft.ablockalypse.AblockalypsePlugin;
import com.gamersafer.minecraft.ablockalypse.Character;
import com.gamersafer.minecraft.ablockalypse.util.FormatUtil;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class CharacterSelectionMenu {

    private static CharacterSelectionMenu instance;
    private final Inventory inventory;

    public CharacterSelectionMenu() {
        String title = FormatUtil.color(AblockalypsePlugin.getInstance().getConfig().getString("menu.character-selection.title"));
        int size = AblockalypsePlugin.getInstance().getConfig().getInt("menu.character-selection.size");

        this.inventory = Bukkit.createInventory(null, size, title);

        insertItems();
    }

    public static void reload() {
        instance = null;
    }

    public static void open(Player player) {
        if (instance == null) {
            instance = new CharacterSelectionMenu();
        }
        player.playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
        player.openInventory(instance.getInventory());
    }

    public static boolean isEquals(Inventory inventory) {
        return instance != null && inventory.equals(instance.getInventory());
    }

    private void insertItems() {
        for (Character character : Character.values()) {
            inventory.setItem(character.getMenuIndex(), character.getMenuItem());
        }
    }

    private Inventory getInventory() {
        return inventory;
    }

}
