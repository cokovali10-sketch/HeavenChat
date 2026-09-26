package com.vitalis.chat;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Reads HeavenTitles players.yml directly (same pattern as HeavenTops):
 * uuid -> {playMinutes}. Only what chat needs, nothing else.
 */
public class PlaytimeIndex {
    private final Map<UUID, Long> minutes = new HashMap<>();

    /** Reload from disk. Returns players known. */
    public synchronized int reload() {
        minutes.clear();
        File f = new File(new File("plugins"), "HeavenTitles/players.yml");
        if (!f.exists()) return 0;
        try {
            YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
            for (String key : y.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    ConfigurationSection s = y.getConfigurationSection(key);
                    if (s == null) continue;
                    long m = s.getLong("playMinutes", 0);
                    if (m > 0) minutes.put(uuid, m);
                } catch (IllegalArgumentException ignored) {}
            }
        } catch (Exception ignored) {}
        return minutes.size();
    }

    public synchronized long minutes(UUID uuid) {
        return minutes.getOrDefault(uuid, 0L);
    }

    /** "86 мин" / "1.5 ч" — same wording as titles GUI. Pure logic. */
    public static String hours(long playMinutes) {
        if (playMinutes < 60) return playMinutes + " мин";
        double h = playMinutes / 60.0;
        if (h == Math.floor(h)) return ((long) h) + " ч";
        return String.format(Locale.ROOT, "%.1f ч", h);
    }

    /** Test seam: pure file parsing without server. */
    public static Map<UUID, Long> parse(YamlConfiguration y) {
        Map<UUID, Long> out = new HashMap<>();
        for (String key : y.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                ConfigurationSection s = y.getConfigurationSection(key);
                if (s == null) continue;
                long m = s.getLong("playMinutes", 0);
                if (m > 0) out.put(uuid, m);
            } catch (IllegalArgumentException ignored) {}
        }
        return out;
    }
}
