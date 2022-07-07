package com.gamersafer.minecraft.ablockalypse;

import com.gamersafer.minecraft.ablockalypse.database.api.StoryStorage;
import com.gamersafer.minecraft.ablockalypse.story.Story;
import com.lkeehl.tagapi.TagAPI;
import com.lkeehl.tagapi.api.Tag;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class CharacterNametagManager {

    private final AblockalypsePlugin plugin;
    private final StoryStorage storyStorage;

    public CharacterNametagManager(AblockalypsePlugin plugin, StoryStorage storyStorage) {
        this.plugin = plugin;
        this.storyStorage = storyStorage;
    }

    public void updateTag(Player player) {
        storyStorage.getActiveStory(player.getUniqueId()).thenAccept(story -> {
            updateTag(player, story.orElse(null));
        });
    }

    public void updateTag(Player player, @Nullable Story activeStory) {
        plugin.sync(() -> {
            Tag tag = TagAPI.getTag(player);
            if (tag != null) {
                tag.removeTag();
            }
            tag = Tag.create(player);
            tag.addTagLine(10).setGetName(pl -> player.getName());

            if (activeStory != null) {
                // the player has an active story. display character name above head
                tag.addTagLine(9).setGetName(pl -> activeStory.characterName());
            } else {
                // the player has no active story and their previous character name is still displayed.
                // remove the character name from above their head
            }
            tag.giveTag();
        });
    }

}
