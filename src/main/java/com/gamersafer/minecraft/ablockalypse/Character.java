package com.gamersafer.minecraft.ablockalypse;

import com.gamersafer.minecraft.ablockalypse.util.FormatUtil;
import com.gamersafer.minecraft.ablockalypse.util.ItemUtil;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;

public enum Character {

        /* TODO remove
TODO Nurse - Raises effectiveness of bandages/ first aid kits - hook into ItemsAdder
Farmer - Can repair armour
TODO Sprinter - Can sprint faster - flat increase while walking and running
Construction Worker - Reduces damage taken
Police Officer - Does more weapon damage
TODO Shop Owner - Items cost less - not needed
Mechanic - Can repair weapons
Chef - Raises effectiveness of all foods
TODO Dog Walker - Can tame/ starts with wolf (can re-tame wolves if they lose theirs) - give wolfs when spawn the first time. if the wolf dies, can tame another one. onlyl 1 at a time
Warehouse Worker (Spy?) - Has Nightvision
TODO Thief - Name (both character name and username) is hidden at all times (only visible within 5 block radius)
TODO Gambler (Youtuber?) - increase of luck on mob drops - loottable, when a gambler kills an entity there is a chance to get something
I.T Consultant - Quiet (zombies can only hear them if within 5 block radius)
Free Runner - No fall damage
Baller - Jump Boost
Survivalist - Hunger and TODO Thirst decreases slower - hook into ItemsAdder
 */


    NURSE,
    FARMER,
    SPRINTER,
    CONSTRUCTION_WORKER,
    POLICE_OFFICER,
    SHOP_OWNER,
    MECHANIC,
    CHEF,
    DOG_WALKER,
    WAREHOUSE_WORKER,
    THIEF,
    GAMBLER,
    IT_CONSULTANT,
    FREE_RUNNER,
    BALLER,
    SURVIVALIST,
    ;

    private int menuIndex = -1;
    private String displayName;
    private String description;

    private ItemStack menuItem;

    public static void reload() {
        Arrays.stream(values()).forEach(Character::resetConfigValues);
    }

    private void resetConfigValues() {
        menuIndex = -1;
        displayName = null;
        description = null;
        menuItem = null;
    }

    public int getMenuIndex() {
        if (menuIndex < 0) {
            menuIndex = AblockalypsePlugin.getInstance().getConfig().getInt(getConfigPath() + ".menu.index");
        }
        return menuIndex;
    }

    public String getDisplayName() {
        if (displayName == null) {
            displayName = getColoredConfigString(getConfigPath() + ".display-name");
        }
        return displayName;
    }

    public String getDescription() {
        if (description == null) {
            description = getColoredConfigString(getConfigPath() + ".description");
        }
        return description;
    }

    public ItemStack getMenuItem() {
        if (menuItem == null) {
            String skullTexture = AblockalypsePlugin.getInstance().getConfig().getString(getConfigPath() + ".menu.texture");
            menuItem = ItemUtil.createPlayerHead(skullTexture, 1, getDisplayName(), getDescription());
        }
        return menuItem;
    }

    private String getConfigPath() {
        return "character." + name().toLowerCase();
    }

    private String getColoredConfigString(String path) {
        String str = AblockalypsePlugin.getInstance().getConfig().getString(path);
        return FormatUtil.color(str);
    }

}
