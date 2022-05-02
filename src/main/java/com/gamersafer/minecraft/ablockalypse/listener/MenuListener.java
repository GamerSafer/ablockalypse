package com.gamersafer.minecraft.ablockalypse.listener;

import com.gamersafer.minecraft.ablockalypse.AblockalypsePlugin;
import com.gamersafer.minecraft.ablockalypse.Character;
import com.gamersafer.minecraft.ablockalypse.database.api.StoryStorage;
import com.gamersafer.minecraft.ablockalypse.location.LocationManager;
import com.gamersafer.minecraft.ablockalypse.menu.CharacterSelectionMenu;
import com.gamersafer.minecraft.ablockalypse.story.OnboardingSessionData;
import org.bukkit.Location;
import org.bukkit.conversations.Conversation;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.ExactMatchConversationCanceller;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.ValidatingPrompt;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MenuListener implements Listener {

    private static final Object SESSION_DATA_KEY = new Object();

    private static Map<Integer, Character> slotToCharacter;
    private final AblockalypsePlugin plugin;
    private final StoryStorage storyStorage;
    private final LocationManager locationManager;

    public MenuListener(AblockalypsePlugin plugin, StoryStorage storyStorage, LocationManager locationManager) {
        this.plugin = plugin;
        this.storyStorage = storyStorage;
        this.locationManager = locationManager;

        reload();
    }

    public static void reload() {
        slotToCharacter = Arrays.stream(Character.values())
                .collect(Collectors.toMap(Character::getMenuIndex, Function.identity()));
    }

    private static OnboardingSessionData getOnboardingSessionData(ConversationContext context) {
        return (OnboardingSessionData) context.getSessionData(SESSION_DATA_KEY);
    }

    @SuppressWarnings("unused")
    @EventHandler
    private void onInventoryClick(InventoryClickEvent event) {

        // character selection menu
        if (CharacterSelectionMenu.isEquals(event.getInventory())) {
            event.setCancelled(true);
            Character clickedCharacter = slotToCharacter.get(event.getRawSlot());

            if (clickedCharacter != null) {
                Player player = (Player) event.getWhoClicked();
                player.closeInventory();

                // the player clicked a character. make sure he doesn't have an active story
                storyStorage.getActiveStory(player.getUniqueId())
                        .thenAccept(story -> {
                            if (story.isPresent()) {
                                // this should never happen to regular players
                                // only staff members are able to open the menu even if they have already started a story
                                player.sendMessage(plugin.getMessage("character-selector-already"));
                                return;
                            }

                            // teleport the player to the character-specific showroom
                            Optional<Location> cinematicLocOpt = locationManager.getCinematicLoc(clickedCharacter);
                            if (cinematicLocOpt.isPresent()) {
                                player.teleport(cinematicLocOpt.get());
                            } else {
                                plugin.getLogger().warning("Unable to teleport " + player.getName() + " to the " + clickedCharacter.name() + " cinematic location since it's not set.");
                            }

                            // create and start onboarding conversation
                            Conversation conversation = new Conversation(plugin, player, new ConfirmCharacterSelectionPrompt());
                            conversation.getContext().setSessionData(SESSION_DATA_KEY, new OnboardingSessionData(player.getUniqueId(), clickedCharacter));
                            conversation.setLocalEchoEnabled(true);

                            conversation.getCancellers().add(new ExactMatchConversationCanceller("CANCEL"));
                            conversation.addConversationAbandonedListener(conversationCancellerEvent -> {
                                if (conversationCancellerEvent.getCanceller() instanceof ExactMatchConversationCanceller) {
                                    ((Player) conversationCancellerEvent.getContext().getForWhom())
                                            .sendMessage(plugin.getMessage("onboarding-prompt-cancelled"));

                                    // todo teleport the player somewhere
                                }
                            });
                            player.sendMessage(plugin.getMessage("onboarding-prompt-cancel"));

                            player.beginConversation(conversation);
                        });
            }
        }
    }

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
            // TODO teleport to hospital
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

        private static final Pattern PATTERN = Pattern.compile("^(?=.{8,20}$)(?![_.])(?!.*[_.]{2})[a-zA-Z\\d._]+(?<![_.])$", Pattern.CASE_INSENSITIVE);

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
            // todo wait X seconds (specified in the config) before starting the story
            AblockalypsePlugin.getInstance().startNewStory(data);

            return null;
        }

        @Override
        public @NotNull String getPromptText(@NotNull ConversationContext context) {
            return AblockalypsePlugin.getInstance().getMessage("onboarding-prompt-name");
        }
    }

}
