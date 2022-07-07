package com.gamersafer.minecraft.ablockalypse;

import com.gamersafer.minecraft.ablockalypse.command.AblockalypseCommand;
import com.gamersafer.minecraft.ablockalypse.database.StoryDAO;
import com.gamersafer.minecraft.ablockalypse.database.api.StoryStorage;
import com.gamersafer.minecraft.ablockalypse.listener.EntityDamageByEntityListener;
import com.gamersafer.minecraft.ablockalypse.listener.EntityDamageListener;
import com.gamersafer.minecraft.ablockalypse.listener.EntityTameListener;
import com.gamersafer.minecraft.ablockalypse.listener.EntityTargetLivingEntityListener;
import com.gamersafer.minecraft.ablockalypse.listener.FoodLevelChangeListener;
import com.gamersafer.minecraft.ablockalypse.listener.MenuListener;
import com.gamersafer.minecraft.ablockalypse.listener.PlayerDeathListener;
import com.gamersafer.minecraft.ablockalypse.listener.PlayerJoinListener;
import com.gamersafer.minecraft.ablockalypse.listener.PrepareAnvilListener;
import com.gamersafer.minecraft.ablockalypse.location.LocationManager;
import com.gamersafer.minecraft.ablockalypse.papi.AblockalypsePAPIExpansion;
import com.gamersafer.minecraft.ablockalypse.story.OnboardingSessionData;
import com.gamersafer.minecraft.ablockalypse.story.StoryCache;
import com.gamersafer.minecraft.ablockalypse.util.FormatUtil;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.papermc.lib.PaperLib;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class AblockalypsePlugin extends JavaPlugin {

    private static AblockalypsePlugin instance;

    private HikariDataSource dataSource;
    private StoryStorage storyStorage;
    private LocationManager locationManager;
    private CharacterNametagManager nametagManager;

    public static AblockalypsePlugin getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;

        // FIXME uncomment once shaded
//        TagAPI.onEnable(this);

        saveDefaultConfig();
        FormatUtil.reload(getConfig());

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

        this.nametagManager = new CharacterNametagManager(this, storyStorage);

        // register commands
        //noinspection ConstantConditions
        getCommand(AblockalypseCommand.COMMAND).setExecutor(new AblockalypseCommand(this, storyStorage, locationManager));

        // register listeners
        getServer().getPluginManager().registerEvents(new EntityDamageByEntityListener(storyStorage), this);
        getServer().getPluginManager().registerEvents(new EntityDamageListener(storyStorage), this);
        getServer().getPluginManager().registerEvents(new EntityTameListener(this, storyStorage), this);
        getServer().getPluginManager().registerEvents(new EntityTargetLivingEntityListener(storyStorage), this);
        getServer().getPluginManager().registerEvents(new FoodLevelChangeListener(storyStorage), this);
        getServer().getPluginManager().registerEvents(new MenuListener(this, storyStorage, locationManager), this);
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(this, storyStorage, locationManager, nametagManager), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this, storyStorage, locationManager, nametagManager), this);
        getServer().getPluginManager().registerEvents(new PrepareAnvilListener(this, storyStorage), this);

        // register PAPI expansion
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new AblockalypsePAPIExpansion(storyStorage).register();
        }
    }

    @Override
    public void onDisable() {
        storyStorage.shutdown();
        locationManager.shutdown();

        // FIXME uncomment once shaded
//        TagAPI.onDisable();

        dataSource.close();
    }

    public void reload() {
        reloadConfig();
        FormatUtil.reload(getConfig());
        Character.reload();
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
        return FormatUtil.color(getConfig().getString("message." + messageId));
    }

    public List<String> getMessageList(String messageId) {
        return getConfig().getStringList("message." + messageId).stream()
                .map(FormatUtil::color)
                .collect(Collectors.toList());
    }

    public CompletableFuture<Void> sync(Runnable r) {
        if (!getServer().isPrimaryThread()) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            getServer().getScheduler().runTaskLater(this, () -> {
                try {
                    r.run();
                    future.complete(null);
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            }, 0);
            return future;
        } else {
            r.run();
            return CompletableFuture.completedFuture(null);
        }
    }

    public <T> CompletableFuture<T> sync(Supplier<T> r) {
        if (!getServer().isPrimaryThread()) {
            CompletableFuture<T> future = new CompletableFuture<>();
            getServer().getScheduler().runTaskLater(this, () -> {
                try {
                    future.complete(r.get());
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            }, 0);
            return future;
        } else {
            return CompletableFuture.completedFuture(r.get());
        }
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
            throw new IllegalStateException("Unable to start a new story for the player " + data.getPlayerUuid() + ". They are offline.");
        }

        // make sure there player doesn't already have an active story
        storyStorage.getActiveStory(player.getUniqueId()).thenAccept(activeStory -> {
            if (activeStory.isPresent()) {
                throw new IllegalStateException("Unable to start a new story for " + data.getPlayerUuid() + ". They already have an active story!");
            }

            // start new story
            storyStorage.startNewStory(data).thenAccept(story -> {
                // back to the main thread
                sync(() -> {
                    // try to teleport the player to the next spawn location
                    Optional<Location> spawnPoint = locationManager.getNextHospitalLoc();
                    if (spawnPoint.isPresent()) {
                        PaperLib.teleportAsync(player, spawnPoint.get());
                        player.playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
                    } else {
                        // there isn't any configured spawn
                        getLogger().severe("Unable to teleport the player " + data.getPlayerUuid() + " to the hospital. Please configure the l!");
                    }

                    // give permanent potion effects
                    PotionEffectType potionEffectType = switch (story.character()) {
                        case WAREHOUSE_WORKER -> PotionEffectType.NIGHT_VISION;
                        case BALLER -> PotionEffectType.JUMP;
                        default -> null;
                    };
                    if (potionEffectType != null) {
                        player.addPotionEffect(new PotionEffect(potionEffectType, Integer.MAX_VALUE, 1));
                    }

                    // change speed for sprinter
                    if (story.character() == Character.SPRINTER) {
                        player.setWalkSpeed(0.3f);
                    }

                    // dispatch story-start commands
                    story.character().getCommandsOnStoryStart().stream()
                            .map(cmd -> cmd.replace("{name}", player.getName())
                                    .replace("{uuid}", player.getUniqueId().toString()))
                            .forEach(cmd -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd));

                    // update nametag
                    nametagManager.updateTag(player, story);

                    // send feedback message
                    player.sendMessage(getMessage("onboarding-prompt-started")
                            .replace("{character_name}", data.getName()));
                });
            }).thenRun(() -> getLogger().info("The player " + data.getPlayerUuid() + " just started a new story as a " + data.getCharacter().name()));
        });
    }

}
