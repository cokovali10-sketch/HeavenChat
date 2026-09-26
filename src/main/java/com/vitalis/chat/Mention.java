package com.vitalis.chat;

import java.util.Collection;
import java.util.Locale;

/** @mention detection. Pure logic, unit-tested. */
public final class Mention {
    private Mention() {}

    /**
     * Online nick mentioned as @Nick (case-insensitive, nick boundary).
     * Longest match wins (Kai vs Kaito). Returns actual nick or null.
     */
    public static String find(String message, Collection<String> onlineNicks) {
        if (message == null || !message.contains("@")) return null;
        String lower = message.toLowerCase(Locale.ROOT);
        String best = null;
        for (String nick : onlineNicks) {
            if (nick == null || nick.isEmpty()) continue;
            String needle = "@" + nick.toLowerCase(Locale.ROOT);
            int i = lower.indexOf(needle);
            while (i >= 0) {
                int end = i + needle.length();
                if (end >= lower.length() || !isNickChar(lower.charAt(end))) {
                    if (best == null || nick.length() > best.length()) best = nick;
                    break;
                }
                i = lower.indexOf(needle, i + 1);
            }
        }
        return best;
    }

    private static boolean isNickChar(char c) {
        return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9'
                || c == '_' || c >= 'а' && c <= 'я' || c >= 'А' && c <= 'Я' || c == 'ё' || c == 'Ё';
    }
}
