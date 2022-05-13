package com.gamersafer.minecraft.ablockalypse;

import com.gamersafer.minecraft.ablockalypse.command.AblockalypseCommand;
import com.gamersafer.minecraft.ablockalypse.database.StoryDAO;
import com.gamersafer.minecraft.ablockalypse.database.api.StoryStorage;
import com.gamersafer.minecraft.ablockalypse.listener.EntityDamageByEntityListener;
import com.gamersafer.minecraft.ablockalypse.listener.EntityDamageListener;
import com.gamersafer.minecraft.ablockalypse.listener.EntityTargetLivingEntityListener;
import com.gamersafer.minecraft.ablockalypse.listener.FoodLevelChangeListener;
import com.gamersafer.minecraft.ablockalypse.listener.MenuListener;
import com.gamersafer.minecraft.ablockalypse.listener.PlayerDeathListener;
import com.gamersafer.minecraft.ablockalypse.listener.PrepareAnvilListener;
import com.gamersafer.minecraft.ablockalypse.location.LocationManager;
import com.gamersafer.minecraft.ablockalypse.menu.CharacterSelectionMenu;
import com.gamersafer.minecraft.ablockalypse.story.OnboardingSessionData;
import com.gamersafer.minecraft.ablockalypse.story.StoryCache;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class AblockalypsePlugin extends JavaPlugin {

    private static AblockalypsePlugin instance;

    private HikariDataSource dataSource;
    private StoryStorage storyStorage;
    private LocationManager locationManager;

    public static AblockalypsePlugin getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        // initialize connection pool
        ConfigurationSection dbConfig = getConfig().getConfigurationSection("mysql");
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc:mysql://" + dbConfig.getString("host") + "/" + dbConfig.getString("database-name"));
        hikariConfig.setUsername(dbConfig.getString("username"));
        hikariConfig.setPassword(dbConfig.getString("password"));
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        dataSource = new HikariDataSource(hikariConfig);

        this.storyStorage = new StoryCache(new StoryDAO(dataSource));
        this.locationManager = new LocationManager();

        // register commands
        //noinspection ConstantConditions
        getCommand(AblockalypseCommand.COMMAND).setExecutor(new AblockalypseCommand(this, locationManager));

        // register listeners
        getServer().getPluginManager().registerEvents(new EntityDamageByEntityListener(storyStorage), this);
        getServer().getPluginManager().registerEvents(new EntityDamageListener(storyStorage), this);
        getServer().getPluginManager().registerEvents(new EntityTargetLivingEntityListener(storyStorage), this);
        getServer().getPluginManager().registerEvents(new FoodLevelChangeListener(storyStorage), this);
        getServer().getPluginManager().registerEvents(new MenuListener(this, storyStorage, locationManager), this);
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(this, storyStorage, locationManager), this);
        getServer().getPluginManager().registerEvents(new PrepareAnvilListener(this, storyStorage), this);
    }

    @Override
    public void onDisable() {
        storyStorage.shutdown();
        locationManager.shutdown();

        dataSource.close();
    }

    public void reload() {
        reloadConfig();
        Character.reload();
        CharacterSelectionMenu.reload();
        MenuListener.reload();
    }

    public StoryStorage getStoryStorage() {
        return storyStorage;
    }

    public void sendMessage(CommandSender user, String messageId) {
        user.sendMessage(getMessage(messageId));
    }

    public String getMessage(String messageId) {
        //noinspection ConstantConditions
        return ChatColor.translateAlternateColorCodes('&', getConfig().getString("message." + messageId));
    }

    public List<String> getMessageList(String messageId) {
        return getConfig().getStringList("message." + messageId).stream()
                .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                .collect(Collectors.toList());
    }

    // todo move to StoryManager
    public void startNewStory(OnboardingSessionData data) {
        // make sure we have all the data to start a new story
        if (!data.isComplete()) {
            throw new IllegalStateException("The following session data incomplete. Unable to start a story. data=" + data);
        }

        // make sure the player is online
        Player player = Bukkit.getPlayer(data.getPlayerUuid());
        if (player == null) {
            throw new IllegalStateException("Unable to start a new story for the player " + data.getPlayerUuid() + ". He is offline.");
        }

        // make sure there player doesn't already have an active story
        storyStorage.getActiveStory(player.getUniqueId()).thenAccept(activeStory -> {
            if (activeStory.isPresent()) {
                throw new IllegalStateException("Unable to start a new story for " + data.getPlayerUuid() + ". He already has an active story!");
            }

            // start new story
            storyStorage.startNewStory(data).thenAccept(story -> {
                // back to the main thread
                getServer().getScheduler().runTask(this, () -> {
                    // try to teleport the player to the next spawn location
                    Optional<Location> spawnPoint = locationManager.getNextSpawnPoint();
                    if (spawnPoint.isPresent()) {
                        player.teleport(spawnPoint.get());
                    } else {
                        // there isn't any configured spawn
                        getLogger().severe("Unable to teleport the player " + data.getPlayerUuid() + " to a spawn point. Please configure them!");
                    }

                    // give permanent potion effects
                    PotionEffectType potionEffectType = switch (story.character()) {
                        case WAREHOUSE_WORKER -> PotionEffectType.NIGHT_VISION;
                        case BALLER -> PotionEffectType.JUMP;
                        default -> null;
                    };
                    if (potionEffectType != null) {
                        player.addPotionEffect(new PotionEffect(potionEffectType, 1, Integer.MAX_VALUE));
                    }

                    // send feedback message
                    player.sendMessage(getMessage("onboarding-prompt-started")
                            .replace("{character_name}", data.getName()));
                });
            }).thenRun(() -> getLogger().info("The player " + data.getPlayerUuid() + " just started a new story as a " + data.getCharacter().name()));
        });
    }

}
