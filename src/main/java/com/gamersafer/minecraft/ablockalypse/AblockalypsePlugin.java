package com.gamersafer.minecraft.ablockalypse;

import com.gamersafer.minecraft.ablockalypse.command.AblockalypseCommand;
import com.gamersafer.minecraft.ablockalypse.command.AppendingCommandExecutor;
import com.gamersafer.minecraft.ablockalypse.database.SafehouseDAO;
import com.gamersafer.minecraft.ablockalypse.database.StoryDAO;
import com.gamersafer.minecraft.ablockalypse.database.api.SafehouseStorage;
import com.gamersafer.minecraft.ablockalypse.database.api.StoryStorage;
import com.gamersafer.minecraft.ablockalypse.leaderboard.SurvivalTimeLeaderboard;
import com.gamersafer.minecraft.ablockalypse.listener.ChunkUnloadListener;
import com.gamersafer.minecraft.ablockalypse.listener.EntityDamageListener;
import com.gamersafer.minecraft.ablockalypse.listener.EntityDamagedByEntityListener;
import com.gamersafer.minecraft.ablockalypse.listener.EntityInteractEntityListener;
import com.gamersafer.minecraft.ablockalypse.listener.EntityPotionEffectListener;
import com.gamersafer.minecraft.ablockalypse.listener.EntityTameListener;
import com.gamersafer.minecraft.ablockalypse.listener.FoodLevelChangeListener;
import com.gamersafer.minecraft.ablockalypse.listener.HDBLoadListener;
import com.gamersafer.minecraft.ablockalypse.listener.MenuListener;
import com.gamersafer.minecraft.ablockalypse.listener.PlayerBuildListener;
import com.gamersafer.minecraft.ablockalypse.listener.PlayerDeathListener;
import com.gamersafer.minecraft.ablockalypse.listener.PlayerInteractListener;
import com.gamersafer.minecraft.ablockalypse.listener.PlayerItemConsumeListener;
import com.gamersafer.minecraft.ablockalypse.listener.PlayerJoinListener;
import com.gamersafer.minecraft.ablockalypse.listener.PlayerQuitListener;
import com.gamersafer.minecraft.ablockalypse.listener.PlayerToggleSneakListener;
import com.gamersafer.minecraft.ablockalypse.listener.PlayerToggleSprintListener;
import com.gamersafer.minecraft.ablockalypse.listener.RepairItemListener;
import com.gamersafer.minecraft.ablockalypse.location.LocationManager;
import com.gamersafer.minecraft.ablockalypse.papi.AblockalypsePAPIExpansion;
import com.gamersafer.minecraft.ablockalypse.safehouse.BoosterManager;
import com.gamersafer.minecraft.ablockalypse.safehouse.SafehouseCache;
import com.gamersafer.minecraft.ablockalypse.safehouse.SafehouseManager;
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
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class AblockalypsePlugin extends JavaPlugin {

    private static AblockalypsePlugin instance;

    private HikariDataSource dataSource;
    private StoryStorage storyStorage;
    private SafehouseStorage safehouseStorage;
    private SafehouseManager safehouseManager;
    private BoosterManager boosterManager;
    private SurvivalTimeLeaderboard survivalTimeLeaderboard;
    private LocationManager locationManager;

    public static AblockalypsePlugin getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;

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
        this.safehouseStorage = new SafehouseCache(new SafehouseDAO(dataSource));
        this.safehouseManager = new SafehouseManager(this, safehouseStorage, Bukkit.getWorld(Objects.requireNonNull(getConfig().getString("safehouse-world"))));
        this.boosterManager = new BoosterManager(this, safehouseManager);
        this.locationManager = new LocationManager();
        this.survivalTimeLeaderboard = new SurvivalTimeLeaderboard(this, storyStorage);

        // register commands
        //noinspection ConstantConditions
        AblockalypseCommand executor = new AblockalypseCommand(this, storyStorage, locationManager, safehouseManager, boosterManager);
        PluginCommand parentCommand = getCommand(AblockalypseCommand.COMMAND);
        parentCommand.setExecutor(executor);

        AppendingCommandExecutor<AblockalypseCommand> appending = new AppendingCommandExecutor<>(executor, parentCommand);
        appending.register(getCommand("stories"));
        appending.register(getCommand("backstory"));
        appending.register(getCommand("safehouse"));
        appending.register(getCommand("bunker"));
        // register listeners
        getServer().getPluginManager().registerEvents(new ChunkUnloadListener(), this);
        getServer().getPluginManager().registerEvents(new EntityDamagedByEntityListener(boosterManager), this);
        getServer().getPluginManager().registerEvents(new EntityDamageListener(storyStorage), this);
        getServer().getPluginManager().registerEvents(new EntityInteractEntityListener(), this);
        getServer().getPluginManager().registerEvents(new EntityPotionEffectListener(this), this);
        getServer().getPluginManager().registerEvents(new EntityTameListener(this, storyStorage), this);
        getServer().getPluginManager().registerEvents(new FoodLevelChangeListener(storyStorage, boosterManager), this);
        getServer().getPluginManager().registerEvents(new HDBLoadListener(), this);
        getServer().getPluginManager().registerEvents(new MenuListener(this, storyStorage, locationManager, safehouseManager, boosterManager), this);
        getServer().getPluginManager().registerEvents(new PlayerBuildListener(this, safehouseManager), this);
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(this, storyStorage, locationManager, boosterManager, safehouseManager), this);
        getServer().getPluginManager().registerEvents(new PlayerInteractListener(this, safehouseManager, boosterManager, storyStorage), this);
        getServer().getPluginManager().registerEvents(new PlayerItemConsumeListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this, storyStorage, locationManager), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(storyStorage, boosterManager), this);
        getServer().getPluginManager().registerEvents(new PlayerToggleSneakListener(storyStorage), this);
        getServer().getPluginManager().registerEvents(new PlayerToggleSprintListener(storyStorage), this);
        getServer().getPluginManager().registerEvents(new RepairItemListener(this, storyStorage), this);

        // register PAPI expansion
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new AblockalypsePAPIExpansion(storyStorage, survivalTimeLeaderboard, safehouseManager).register();
        }
    }

    @Override
    public void onDisable() {
        // flush updated survival times to the database
        getServer().getOnlinePlayers().stream()
                .map(Player::getUniqueId)
                .forEach(storyStorage::updateSurvivalTime);

        storyStorage.shutdown();
        safehouseStorage.shutdown();
        locationManager.shutdown();
        safehouseManager.shutdown();

        dataSource.close();
    }

    public void reload() {
        reloadConfig();
        FormatUtil.reload(getConfig());
        Character.reload();
        MenuListener.reload();
        survivalTimeLeaderboard.reload();
        safehouseManager.reload();
    }

    public StoryStorage getStoryStorage() {
        return storyStorage;
    }

    public SafehouseStorage getSafehouseStorage() {
        return safehouseStorage;
    }

    public SafehouseManager getSafehouseManager() {
        return safehouseManager;
    }

    public BoosterManager getBoosterManager() {
        return boosterManager;
    }

    public void sendMessage(CommandSender user, String messageId) {
        user.sendMessage(getMessage(messageId));
    }

    public String getMessage(String messageId) {
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
                    giveCharacterPermanentPotionEffect(player, story.character());

                    // give dog to dog walker
                    if (story.character() == Character.VETERINARIAN) {
                        Wolf wolf = (Wolf) player.getWorld().spawnEntity(player.getLocation(), EntityType.WOLF, CreatureSpawnEvent.SpawnReason.CUSTOM);
                        wolf.setOwner(player);
                    }

                    // dispatch story-start commands
                    story.character().getCommandsOnStoryStart().stream()
                            .map(cmd -> cmd.replace("{name}", player.getName())
                                    .replace("{uuid}", player.getUniqueId().toString()))
                            .forEach(cmd -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd));


                    // send feedback message
                    player.sendMessage(getMessage("onboarding-prompt-started")
                            .replace("{character_name}", data.getName()));
                });
            }).thenRun(() -> getLogger().info("The player " + data.getPlayerUuid() + " just started a new story as a " + data.getCharacter().name()));
        });
    }

    public void giveCharacterPermanentPotionEffect(Player player) {
        //noinspection CodeBlock2Expr
        storyStorage.getActiveStory(player.getUniqueId()).thenAccept(story -> {
            story.ifPresent(value -> giveCharacterPermanentPotionEffect(player, value.character()));
        });
    }

    public void giveCharacterPermanentPotionEffect(Player player, Character character) {
        sync(() -> {
            PotionEffectType potionEffectType = switch (character) {
                case OFFICER -> PotionEffectType.DAMAGE_RESISTANCE;
                case PRIVATE -> PotionEffectType.INCREASE_DAMAGE;
                case BALLER -> PotionEffectType.JUMP;
                default -> null;
            };
            if (potionEffectType != null) {
                player.addPotionEffect(new PotionEffect(potionEffectType, Integer.MAX_VALUE, 0));
            }
        });
    }

}
