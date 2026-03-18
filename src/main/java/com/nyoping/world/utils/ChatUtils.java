package com.nyoping.world.utils;

import org.bukkit.ChatColor;

public class ChatUtils {

    public static String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public static String prefix() {
        return colorize("&6[NyopingWorld] &r");
    }

    public static String success(String message) {
        return prefix() + colorize("&a" + message);
    }

    public static String error(String message) {
        return prefix() + colorize("&c" + message);
    }

    public static String info(String message) {
        return prefix() + colorize("&e" + message);
    }
}
