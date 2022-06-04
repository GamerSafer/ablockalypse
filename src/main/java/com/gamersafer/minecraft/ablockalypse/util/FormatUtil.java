package com.gamersafer.minecraft.ablockalypse.util;

import org.apache.commons.lang.time.DurationFormatUtils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public final class FormatUtil {

    private static DateTimeFormatter dateTimeFormatter;
    private static String durationFormat;

    private FormatUtil() {
        // prevent initialization
    }

    public static void reload(FileConfiguration reloadedConfig) {
        //noinspection ConstantConditions
        dateTimeFormatter = DateTimeFormatter.ofPattern(reloadedConfig.getString("format.date-time"));
        durationFormat = reloadedConfig.getString("format.duration");
    }

    public static String format(LocalDateTime date) {
        return date.format(dateTimeFormatter);
    }

    public static String format(Duration duration) {
        return DurationFormatUtils.formatDuration(duration.toMillis(), durationFormat, false);
    }

    public static String format(Location location) {
        return location.getWorld().getName() + " x=" + location.getBlockX() + " y=" + location.getBlockY() + " z=" + location.getBlockZ();
    }

    public static String color(String str) {
        return ChatColor.translateAlternateColorCodes('&', str);
    }

    public static List<String> color(List<String> str) {
        return str.stream().map(FormatUtil::color).collect(Collectors.toList());
    }

    public static String capitalize(String str) {
        str = str.toLowerCase();
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

}
