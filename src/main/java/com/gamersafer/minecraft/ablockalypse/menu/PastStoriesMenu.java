package com.gamersafer.minecraft.ablockalypse.menu;

import com.gamersafer.minecraft.ablockalypse.AblockalypsePlugin;
import com.gamersafer.minecraft.ablockalypse.story.Story;
import com.gamersafer.minecraft.ablockalypse.util.FormatUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class PastStoriesMenu implements InventoryHolder {

    public static final int PREVIOUS_PAGE_SLOT = 47;
    public static final int NEXT_PAGE_SLOT = 51;
    public static final ItemStack GLASS_PANE = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);

    private final String playerName;

    private final List<Inventory> pages;

    private int openedPageIndex = -1;

    public PastStoriesMenu(List<Story> stories, String playerName, Duration playtime) {
        // sort stories from most recent to oldest
        stories.sort((s1, s2) -> s1.startTime().compareTo(s2.startTime()) * -1);

        this.playerName = playerName;
        this.pages = new ArrayList<>();

        //noinspection ConstantConditions
        String title = FormatUtil.color(AblockalypsePlugin.getInstance().getConfig().getString("menu.past-stories.title").replace("{player}", playerName));

        int pageTot = (int) Math.ceil(stories.size() * 1f / 21);

        // 21 stories per page
        int pageIndex = 0;
        int storiesInPage = 0;
        for (Story story : stories) {
            if (storiesInPage == 21) {
                pageIndex++;
                storiesInPage = 0;
            }

            Inventory page;
            if (pages.size() < pageIndex + 1) {
                String pageTitle = title.replace("{page}", Integer.toString(pageIndex + 1))
                        .replace("{pageTot}", Integer.toString(pageTot));
                page = Bukkit.createInventory(this, 54, pageTitle);
                pages.add(pageIndex, page);

                // insert panes
                for (int i = 0; i < 9; i++) {
                    page.setItem(i, GLASS_PANE);
                    page.setItem(i + 45, GLASS_PANE);

                    if (i == 0 || i == 8) {
                        for (int j = i + 9; j < 54; j += 9) {
                            page.setItem(j, GLASS_PANE);
                        }
                    }
                }
            } else {
                page = pages.get(pageIndex);
            }

            ItemStack storyItem = createStoryItem(story);
            page.addItem(storyItem);

            storiesInPage++;
        }

        //noinspection ConstantConditions
        ItemStack nextPage = parseItemStack(AblockalypsePlugin.getInstance().getConfig().getConfigurationSection("menu.past-stories.items.next-page"), Collections.emptyMap());
        //noinspection ConstantConditions
        ItemStack previousPage = parseItemStack(AblockalypsePlugin.getInstance().getConfig().getConfigurationSection("menu.past-stories.items.prev-page"), Collections.emptyMap());
        //noinspection ConstantConditions
        ItemStack playerInfoItem = parseItemStack(AblockalypsePlugin.getInstance().getConfig().getConfigurationSection("menu.past-stories.items.player-info"),
                Map.of("{player}", playerName,
                        "{playtime}", FormatUtil.format(playtime)));

        for (int i = 0; i < pages.size(); i++) {
            Inventory page = pages.get(i);

            // prev page item
            if (i > 0) {
                page.setItem(PREVIOUS_PAGE_SLOT, previousPage);
            }

            // player info item
            page.setItem(49, playerInfoItem);

            // next page item
            if (i + 1 < pages.size()) {
                page.setItem(NEXT_PAGE_SLOT, nextPage);
            }
        }
    }

    private ItemStack createStoryItem(Story story) {
        ItemStack item = story.character().getMenuItem();

        String survivalTime = FormatUtil.format(story.survivalTime());
        String startDate = FormatUtil.format(story.startTime());

        List<String> lore;
        if (story.isActive()) {
            lore = FormatUtil.color(AblockalypsePlugin.getInstance().getConfig().getStringList("menu.past-stories.items.story-entry-active.lore"))
                    .stream()
                    .map(s -> s.replace("{survivaltime}", survivalTime)
                            .replace("{startDate}", startDate)
                    ).toList();
        } else {
            lore = FormatUtil.color(AblockalypsePlugin.getInstance().getConfig().getStringList("menu.past-stories.items.story-entry-past.lore"))
                    .stream()
                    .map(s -> {
                                assert story.deathCause() != null && story.endTime() != null && story.deathLocation() != null;

                                return s.replace("{survivaltime}", survivalTime)
                                        .replace("{startDate}", startDate)
                                        .replace("{endDate}", FormatUtil.format(story.endTime()))
                                        .replace("{deathCause}", FormatUtil.capitalize(story.deathCause().name()))
                                        .replace("{deathLocation}", FormatUtil.format(story.deathLocation()));
                            }
                    ).toList();
        }

        ItemMeta itemMeta = item.getItemMeta();

        itemMeta.displayName(Component.text(story.characterName(), NamedTextColor.GREEN));
        itemMeta.setLore(lore);

        item.setItemMeta(itemMeta);
        return item;
    }

    private ItemStack parseItemStack(ConfigurationSection configurationSection, Map<String, String> placeholders) {
        Material material = Material.valueOf(configurationSection.getString("material"));
        String name = FormatUtil.color(configurationSection.getString("name"));
        List<String> lore = FormatUtil.color(configurationSection.getStringList("lore"));

        if (!placeholders.isEmpty()) {
            for (Map.Entry<String, String> placeholder : placeholders.entrySet()) {
                name = name.replace(placeholder.getKey(), placeholder.getValue());
                lore.replaceAll(s -> s.replace(placeholder.getKey(), placeholder.getValue()));
            }
        }

        ItemStack item = new ItemStack(material);
        ItemMeta itemMeta = item.getItemMeta();

        if (itemMeta instanceof SkullMeta skullMeta) {
            skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(playerName));
        }

        itemMeta.setDisplayName(name);
        itemMeta.setLore(lore);

        item.setItemMeta(itemMeta);
        return item;
    }

    public void open(Player player) {
        open(player, 0);
    }

    public void openPreviousPage(Player player) {
        open(player, openedPageIndex - 1);
    }

    public void openNextPage(Player player) {
        open(player, openedPageIndex + 1);
    }

    private void open(Player player, int pageIndex) {
        openedPageIndex = pageIndex;
        player.openInventory(pages.get(pageIndex));
    }

    @Override
    public @NotNull Inventory getInventory() {
        return pages.get(0);
    }

}
