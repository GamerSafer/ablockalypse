package com.gamersafer.minecraft.ablockalypse.menu;

import com.gamersafer.minecraft.ablockalypse.AblockalypsePlugin;
import com.gamersafer.minecraft.ablockalypse.Character;
import com.gamersafer.minecraft.ablockalypse.database.api.StoryStorage;
import com.gamersafer.minecraft.ablockalypse.listener.MenuListener;
import com.gamersafer.minecraft.ablockalypse.location.LocationManager;
import com.gamersafer.minecraft.ablockalypse.story.OnboardingSessionData;
import com.gamersafer.minecraft.ablockalypse.util.FormatUtil;
import io.papermc.lib.PaperLib;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.conversations.Conversation;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.ExactMatchConversationCanceller;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.ValidatingPrompt;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

public class CharacterSelectionMenu implements InventoryHolder {

    private static final Object SESSION_DATA_KEY = new Object();

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

    public static void pickCharacter(Player player, StoryStorage storyStorage, LocationManager locationManager, AblockalypsePlugin plugin, Character clickedCharacter) {
        player.closeInventory();

        // the player clicked a character. make sure they don't have an active story
        storyStorage.getActiveStory(player.getUniqueId())
                .thenAccept(story -> {
                    // make sure we're on the primary thread
                    plugin.sync(() -> {
                        if (story.isPresent()) {
                            // this should never happen to regular players
                            // only staff members are able to open the menu even if they have already started a story
                            player.sendMessage(plugin.getMessage("character-selector-already"));
                            return;
                        }

                        // make sure the player has the permission to select this character
                        if (!player.hasPermission("ablockalypse.canselect." + clickedCharacter.name().toLowerCase())) {
                            player.sendMessage(plugin.getMessage("character-selector-no-permission"));
                            return;
                        }

                        // teleport the player to the character-specific showroom
                        Optional<Location> cinematicLocOpt = locationManager.getCinematicLoc(clickedCharacter);
                        if (cinematicLocOpt.isPresent()) {
                            PaperLib.teleportAsync(player, cinematicLocOpt.get());
                            player.playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
                        } else {
                            plugin.getLogger().warning("Unable to teleport " + player.getName() + " to the " + clickedCharacter.name() + " cinematic location since it's not set.");
                        }

                        // show titles. we can assume the description contains at least 1 line
                        Component mainTitle = Component.text(clickedCharacter.getDisplayName());
                        Component subtitle = Component.text(clickedCharacter.getDescription().get(0));

                        Title title = Title.title(mainTitle, subtitle);
                        player.showTitle(title);

                        // send character description description in chat
                        String descriptionJoined = String.join(" ", clickedCharacter.getDescription());
                        player.sendMessage(clickedCharacter.getDisplayName() + ChatColor.GRAY + ": " + descriptionJoined);

                        // wait X seconds and start onboarding conversation. during the conversation, the chat will be disabled for the player
                        int delay = plugin.getConfig().getInt("onboarding.cinematic-delay-seconds");
                        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                            Conversation conversation = new Conversation(plugin, player, new ConfirmCharacterSelectionPrompt());
                            conversation.getContext().setSessionData(SESSION_DATA_KEY, new OnboardingSessionData(player.getUniqueId(), clickedCharacter));
                            conversation.setLocalEchoEnabled(true);

                            conversation.getCancellers().add(new ExactMatchConversationCanceller("no"));
                            conversation.addConversationAbandonedListener(conversationCancellerEvent -> {
                                if (conversationCancellerEvent.getCanceller() instanceof ExactMatchConversationCanceller) {
                                    Player conversationCancellerPlayer = ((Player) conversationCancellerEvent.getContext().getForWhom());

                                    // teleport the player back to the hospital
                                    //noinspection OptionalGetWithoutIsPresent at this point we can assume it's present
                                    PaperLib.teleportAsync(player, locationManager.getNextSpawnPoint().get())
                                            .thenAccept(ignore -> {
                                                plugin.sync(() -> {
                                                    player.playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);

                                                    // send feedback message
                                                    conversationCancellerPlayer.sendMessage(plugin.getMessage("onboarding-prompt-cancelled"));

                                                    // reopen characters menu
                                                    new CharacterSelectionMenu(player);
                                                });
                                            });
                                }
                            });

                            player.beginConversation(conversation);
                            player.playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);

                        }, delay * 20L);
                    });
                });
    }

    // todo listen to InventoryDragEvent

    private static class ConfirmCharacterSelectionPrompt extends ValidatingPrompt {

        private static final Pattern PATTERN = Pattern.compile("^(?:yes|no)$", Pattern.CASE_INSENSITIVE);

        @Override
        protected boolean isInputValid(@NotNull ConversationContext context, @NotNull String input) {
            return PATTERN.matcher(input).matches();
        }

        @Override
        protected @Nullable Prompt acceptValidatedInput(@NotNull ConversationContext context, @NotNull String input) {
            if (input.equalsIgnoreCase("yes")) {
                OnboardingSessionData data = getOnboardingSessionData(context);
                data.setCharacterConfirmed();
                return new NameInputPrompt();
            }
            return null;
        }

        @Override
        public @NotNull String getPromptText(@NotNull ConversationContext context) {
            OnboardingSessionData data = getOnboardingSessionData(context);
            return AblockalypsePlugin.getInstance().getMessage("onboarding-prompt-confirm")
                    .replace("{character}", data.getCharacter().getDisplayName());
        }
    }

    private static class NameInputPrompt extends ValidatingPrompt {

        private static final Pattern PATTERN = Pattern.compile("^(?=.{3,20}$)(?![_.])(?!.*[_.]{2})[a-zA-Z \\d._]+(?<![_.])$", Pattern.CASE_INSENSITIVE);

        @Override
        protected boolean isInputValid(@NotNull ConversationContext context, @NotNull String input) {
            return PATTERN.matcher(input).matches();
        }

        @Override
        protected @Nullable Prompt acceptValidatedInput(@NotNull ConversationContext context, @NotNull String input) {
            OnboardingSessionData data = getOnboardingSessionData(context);
            data.setName(input);

            // extra check to be super sure we're working with valid data
            Player player = (Player) context.getForWhom();
            if (!data.getPlayerUuid().equals(player.getUniqueId())) {
                throw new IllegalStateException("Illegal onboarding process state. The following session data is being used by " + player.getUniqueId() + " " + data);
            }

            // attempt to start new story
            AblockalypsePlugin.getInstance().startNewStory(data);

            return null;
        }

        @Override
        public @NotNull String getPromptText(@NotNull ConversationContext context) {
            // the condition should always be true
            if (context.getForWhom() instanceof Player player) {
                player.playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
            }
            return AblockalypsePlugin.getInstance().getMessage("onboarding-prompt-name");
        }
    }

    private static OnboardingSessionData getOnboardingSessionData(ConversationContext context) {
        return (OnboardingSessionData) context.getSessionData(SESSION_DATA_KEY);
    }
}
