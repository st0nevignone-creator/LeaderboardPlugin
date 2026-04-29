package it.leaderboardPlugin.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class Utils {

    private static final LegacyComponentSerializer SERIALIZER =
            LegacyComponentSerializer.legacyAmpersand();

    public static Component color(String text) {
        return SERIALIZER.deserialize(text);
    }

    public static String colorStr(String text) {
        return text.replace("&", "§");
    }

    public static String formatKD(double kd) {
        return String.format("%.2f", kd);
    }

    public static String formatTime(long minutes) {
        if (minutes < 60) return minutes + "m";
        long h = minutes / 60;
        long m = minutes % 60;
        return h + "h " + m + "m";
    }
}
