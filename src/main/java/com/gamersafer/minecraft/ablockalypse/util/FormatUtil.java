package com.gamersafer.minecraft.ablockalypse.util;

import org.apache.commons.lang.time.DurationFormatUtils;
import org.bukkit.ChatColor;
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
        return DurationFormatUtils.formatDuration(duration.toMillis(), durationFormat, true);
    }

    public static String color(String str) {
        return ChatColor.translateAlternateColorCodes('&', str);
    }

    public static List<String> color(List<String> str) {
        return str.stream().map(FormatUtil::color).collect(Collectors.toList());
    }

}
