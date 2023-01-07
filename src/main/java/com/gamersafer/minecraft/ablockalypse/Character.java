package com.gamersafer.minecraft.ablockalypse;

import com.gamersafer.minecraft.ablockalypse.util.FormatUtil;
import com.gamersafer.minecraft.ablockalypse.util.ItemUtil;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public enum Character {

    NURSE,
    TAILOR,
    SPRINTER,
    OFFICER,
    PRIVATE,
    REPAIR_TECH,
    COOK,
    VETERINARIAN,
    LOCKSMITH,
    GAMBLER,
    LIBRARIAN,
    FREE_RUNNER,
    BALLER,
    SURVIVALIST,
    ;

    private int menuIndex = -1;
    private String displayName;
    private List<String> description, commandsOnStoryStart, commandsOnStoryEnd;
    private Multimap<Integer, String> commandsOnLevelUp;

    private ItemStack menuItem;

    public static void reload() {
        Arrays.stream(values()).forEach(Character::resetConfigValues);
    }

    private void resetConfigValues() {
        menuIndex = -1;
        displayName = null;
        description = null;
        commandsOnStoryStart = null;
        commandsOnStoryEnd = null;
        commandsOnLevelUp = null;
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

    public List<String> getDescription() {
        if (description == null) {
            description = getColoredConfigStringList(getConfigPath() + ".description");
        }
        return description;
    }

    public List<String> getCommandsOnStoryStart() {
        if (commandsOnStoryStart == null) {
            commandsOnStoryStart = AblockalypsePlugin.getInstance().getConfig().getStringList(getConfigPath() + ".run-commands.story-start");
        }
        return commandsOnStoryStart;
    }

    public List<String> getCommandsOnStoryEnd() {
        if (commandsOnStoryEnd == null) {
            commandsOnStoryEnd = AblockalypsePlugin.getInstance().getConfig().getStringList(getConfigPath() + ".run-commands.story-end");
        }
        return commandsOnStoryEnd;
    }

    public Collection<String> getCommandsOnLevelUp(int newLevel) {
        if (commandsOnLevelUp == null) {
            commandsOnLevelUp = LinkedListMultimap.create();
            ConfigurationSection levelIncreaseConfigSection = AblockalypsePlugin.getInstance().getConfig().getConfigurationSection(getConfigPath() + ".run-commands.level-increase");
            if (levelIncreaseConfigSection != null) {
                levelIncreaseConfigSection.getKeys(false)
                        .stream()
                        .map(Integer::valueOf)
                        .forEach(level -> {
                            commandsOnLevelUp.putAll(level, levelIncreaseConfigSection.getStringList(String.valueOf(level)));
                        });
            }
        }
        return commandsOnLevelUp.get(newLevel);
    }

    public ItemStack getMenuItem() {
        if (menuItem == null) {
            String skullTexture = AblockalypsePlugin.getInstance().getConfig().getString(getConfigPath() + ".menu.texture");
            menuItem = ItemUtil.createPlayerHead(skullTexture, 1, getDisplayName(), getDescription());
        }
        return menuItem.clone();
    }

    private String getConfigPath() {
        return "character." + name().toLowerCase();
    }

    private String getColoredConfigString(String path) {
        String str = AblockalypsePlugin.getInstance().getConfig().getString(path);
        return FormatUtil.color(str);
    }

    private List<String> getColoredConfigStringList(String path) {
        List<String> str = AblockalypsePlugin.getInstance().getConfig().getStringList(path);
        return FormatUtil.color(str);
    }

}
