package com.vitalis.chat;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Reads HeavenTitles data directly (same file pattern as HeavenTops):
 * config.yml titles: id -> {name, from, to}; players.yml: uuid -> {activeTitle, playMinutes}.
 * For PM hover lines (title + playtime). No plugin dependency.
 */
public class TitlesIndex {
    public static class Title {
        public final String name;
        public final String from;
        public final String to;

        Title(String name, String from, String to) {
            this.name = name;
            this.from = from;
            this.to = to;
        }
    }

    private final Map<String, Title> titles = new HashMap<>();
    private final Map<UUID, String> active = new HashMap<>();
    private final Map<UUID, Long> minutes = new HashMap<>();

    public synchronized void reload() {
        titles.clear();
        active.clear();
        minutes.clear();
        try {
            File cfg = new File(new File("plugins"), "HeavenTitles/config.yml");
            if (cfg.exists()) {
                YamlConfiguration y = YamlConfiguration.loadConfiguration(cfg);
                ConfigurationSection sec = y.getConfigurationSection("titles");
                if (sec != null) {
                    for (String id : sec.getKeys(false)) {
                        titles.put(id, new Title(
                                sec.getString(id + ".name", id),
                                sec.getString(id + ".from", "#FFFFFF"),
                                sec.getString(id + ".to", "#FFFFFF")));
                    }
                }
            }
        } catch (Exception ignored) {}
        try {
            File pl = new File(new File("plugins"), "HeavenTitles/players.yml");
            if (pl.exists()) {
                YamlConfiguration y = YamlConfiguration.loadConfiguration(pl);
                for (String key : y.getKeys(false)) {
                    try {
                        UUID uuid = UUID.fromString(key);
                        ConfigurationSection s = y.getConfigurationSection(key);
                        if (s == null) continue;
                        String a = s.getString("activeTitle", "");
                        if (a != null && !a.isEmpty()) active.put(uuid, a);
                        long m = s.getLong("playMinutes", 0);
                        if (m > 0) minutes.put(uuid, m);
                    } catch (IllegalArgumentException ignored) {}
                }
            }
        } catch (Exception ignored) {}
    }

    public synchronized Title titleOf(UUID uuid) {
        String id = active.get(uuid);
        return id == null ? null : titles.get(id);
    }

    public synchronized long minutes(UUID uuid) {
        return minutes.getOrDefault(uuid, 0L);
    }

    public synchronized int titleCount() {
        return titles.size();
    }
}
