package com.gamersafer.minecraft.ablockalypse;

import com.gamersafer.minecraft.ablockalypse.command.AblockalypseCommand;
import com.gamersafer.minecraft.ablockalypse.database.StoryDAO;
import com.gamersafer.minecraft.ablockalypse.database.api.StoryStorage;
import com.gamersafer.minecraft.ablockalypse.listener.MenuListener;
import com.gamersafer.minecraft.ablockalypse.location.LocationManager;
import com.gamersafer.minecraft.ablockalypse.menu.CharacterSelectionMenu;
import com.gamersafer.minecraft.ablockalypse.story.StoryCache;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

public class AblockalypsePlugin extends JavaPlugin {

    private static AblockalypsePlugin instance;

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
        HikariDataSource dataSource = new HikariDataSource(hikariConfig);

        this.storyStorage = new StoryCache(new StoryDAO(dataSource));
        this.locationManager = new LocationManager();

        // register commands
        //noinspection ConstantConditions
        getCommand("ablockalypse").setExecutor(new AblockalypseCommand(this, locationManager));

        // register listeners
        getServer().getPluginManager().registerEvents(new MenuListener(this, storyStorage), this);
    }

    @Override
    public void onDisable() {
        storyStorage.shutdown();
        locationManager.shutdown();
    }

    public void reload() {
        reloadConfig();
        Character.reload();
        CharacterSelectionMenu.reload();
        MenuListener.reload();
    }

    public void sendMessage(CommandSender user, String messageId) {
        user.sendMessage(getMessage(messageId));
    }

    public String getMessage(String messageId) {
        //noinspection ConstantConditions
        return ChatColor.translateAlternateColorCodes('&', getConfig().getString("message." + messageId));
    }

}
