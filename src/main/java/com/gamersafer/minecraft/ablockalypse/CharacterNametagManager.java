package com.gamersafer.minecraft.ablockalypse;

import com.gamersafer.minecraft.ablockalypse.database.api.StoryStorage;
import com.gamersafer.minecraft.ablockalypse.story.Story;
import com.lkeehl.tagapi.TagAPI;
import com.lkeehl.tagapi.TagBuilder;
import com.lkeehl.tagapi.api.Tag;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public class CharacterNametagManager {

    private static final int CHARACTER_NAME_LINE_PRIORITY = 1;

    private static final Function<Entity, Tag> DEFAULT_TAG = target ->
            TagBuilder.create(target).withLine(HumanEntity::getName).build();

    private final AblockalypsePlugin plugin;
    private final StoryStorage storyStorage;

    public CharacterNametagManager(AblockalypsePlugin plugin, StoryStorage storyStorage) {
        this.plugin = plugin;
        this.storyStorage = storyStorage;


        TagAPI.setDefaultTag(EntityType.PLAYER, DEFAULT_TAG);
    }

    public void updateTag(Player player) {
        storyStorage.getActiveStory(player.getUniqueId()).thenAccept(story -> {
            updateTag(player, story.orElse(null));
        });
    }

    public void updateTag(Player player, @Nullable Story activeStory) {
        plugin.sync(() -> {
            Tag tag = TagAPI.getTag(player);

            if (activeStory != null) {
                // the player has an active story. display character name above head
                tag.addTagLine(CHARACTER_NAME_LINE_PRIORITY).setGetName(pl -> activeStory.characterName());
                tag.updateTag();
            } else if (tag.getTagLines().size() > 1) {
                // the player has no active story and his previous character name is still displayed.
                // remove the character name from above his head
                tag.removeTag();
                DEFAULT_TAG.apply(player).giveTag();
            }
        });
    }

}
