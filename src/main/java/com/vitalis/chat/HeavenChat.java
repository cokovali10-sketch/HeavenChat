package com.vitalis.chat;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * HeavenChat v1.1.0 - clickable nicks + @mentions + PM ping.
 * - meta nickbtn: click nick -> suggest "/w Nick ", hover -> playtime
 * - @Nick in chat: highlight + pling to the mentioned player
 * - incoming /msg: soft orb pickup sound (Essentials reply-to polling)
 */
public class HeavenChat extends JavaPlugin implements Listener {
    private final PlaytimeIndex index = new PlaytimeIndex();
    private final PMWatch pmWatch = new PMWatch();
    private final Map<String, Long> mentionCd = new HashMap<>();
    /** online nicks maintained on sync join/quit (never touch Bukkit player list async) */
    private final java.util.Set<String> nicks = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private int refreshMinutes = 10;
    private boolean mentionsOn = true;
    private boolean highlightOn = true;
    private Sound mentionSound;
    private float mentionVol = 0.7f;
    private float mentionPitch = 1.4f;
    private long mentionCdMs = 5000;
    private boolean pmOn = true;
    private Sound pmSound;
    private float pmVol = 0.6f;
    private float pmPitch = 1.0f;
    private long pmCdMs = 3000;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();
        refreshMinutes = Math.max(1, getConfig().getInt("general.refresh-minutes", 10));
        mentionsOn = getConfig().getBoolean("mentions.enabled", true);
        highlightOn = getConfig().getBoolean("mentions.highlight", true);
        mentionSound = sound(getConfig().getString("mentions.sound", "BLOCK_NOTE_BLOCK_PLING"),
                Sound.BLOCK_NOTE_BLOCK_PLING);
        mentionVol = (float) getConfig().getDouble("mentions.volume", 0.7);
        mentionPitch = (float) getConfig().getDouble("mentions.pitch", 1.4);
        mentionCdMs = getConfig().getLong("mentions.cooldown-seconds", 5) * 1000L;
        pmOn = getConfig().getBoolean("pm.enabled", true);
        pmSound = sound(getConfig().getString("pm.sound", "ENTITY_EXPERIENCE_ORB_PICKUP"),
                Sound.ENTITY_EXPERIENCE_ORB_PICKUP);
        pmVol = (float) getConfig().getDouble("pm.volume", 0.6);
        pmPitch = (float) getConfig().getDouble("pm.pitch", 1.0);
        pmCdMs = getConfig().getLong("pm.cooldown-seconds", 3) * 1000L;

        int known = index.reload();
        getLogger().info("Playtime index: " + known + " players.");

        for (Player p : getServer().getOnlinePlayers()) nicks.add(p.getName());

        getServer().getPluginManager().registerEvents(this, this);

        // nick refresh (main thread: console commands are queued by LP)
        getServer().getScheduler().runTaskTimer(this, () -> {
            index.reload();
            for (Player p : getServer().getOnlinePlayers()) {
                try {
                    sync(p);
                } catch (Exception ignored) {}
            }
        }, 400L, (long) refreshMinutes * 20L * 60L);

        // PM watcher (main thread, cheap map reads)
        if (pmOn) {
            getServer().getScheduler().runTaskTimer(this, () -> {
                try {
                    org.bukkit.plugin.Plugin ess =
                            getServer().getPluginManager().getPlugin("Essentials");
                    if (ess == null || !ess.isEnabled()) return;
                    long now = System.currentTimeMillis();
                    for (Player p : getServer().getOnlinePlayers()) {
                        UUID from = EssBridge.replyTo(ess, p);
                        UUID sender = pmWatch.poll(p.getUniqueId(), from, now, pmCdMs);
                        if (sender != null) {
                            p.playSound(p.getLocation(), pmSound, pmVol, pmPitch);
                        }
                    }
                } catch (Exception ignored) {}
            }, 100L, 40L);
        }

        getLogger().info("HeavenChat enabled: nicks, mentions, pm ping.");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        nicks.add(p.getName());
        getServer().getScheduler().runTaskLater(this, () -> {
            try {
                if (p.isOnline()) {
                    index.reload();
                    sync(p);
                }
            } catch (Exception ignored) {}
        }, 60L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e) {
        pmWatch.forget(e.getPlayer().getUniqueId());
        nicks.remove(e.getPlayer().getName());
    }

    /**
     * @Nick highlight + ping. LOWEST so chat formatters apply afterwards.
     * Only &lt;&gt; of player-typed text are escaped (anti tag-injection);
     * & codes pass through untouched (players may color their messages).
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(io.papermc.paper.event.player.AsyncChatEvent e) {
        if (!mentionsOn) return;
        Player author = e.getPlayer();
        String raw;
        try {
            raw = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                    .plainText().serialize(e.message());
        } catch (Exception ex) {
            return;
        }
        if (!raw.contains("@")) return;
        String nick = Mention.find(raw, nicks);
        if (nick == null || nick.equalsIgnoreCase(author.getName())) return;
        // highlight is optional (kill-switch if a chat plugin prints tags literally)
        if (highlightOn) {
            try {
                e.message(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                        .deserialize(highlight(raw, nick)));
            } catch (Exception ignored) {
                return;
            }
        }
        long now = System.currentTimeMillis();
        String key = nick.toLowerCase(java.util.Locale.ROOT);
        Long last = mentionCd.get(key);
        if (last != null && now - last < mentionCdMs) return;
        mentionCd.put(key, now);
        final String want = nick;
        Bukkit.getScheduler().runTask(this, () -> {
            try {
                Player t = Bukkit.getPlayerExact(want);
                if (t != null && t.isOnline()) {
                    t.playSound(t.getLocation(), mentionSound, mentionVol, mentionPitch);
                }
            } catch (Exception ignored) {}
        });
    }

    static String escapeTags(String s) {
        return s.replace("<", "&lt;").replace(">", "&gt;");
    }

    /**
     * Wrap the @mention in underline only — title tint colors stay untouched.
     * Player-typed &lt;&gt; are escaped (anti tag-injection), & codes pass through.
     */
    public static String highlight(String raw, String nick) {
        Matcher m = Pattern.compile("(?i)@" + Pattern.quote(nick)).matcher(raw);
        if (!m.find()) return escapeTags(raw);
        String before = escapeTags(raw.substring(0, m.start()));
        String after = escapeTags(raw.substring(m.end()));
        return before + "<underlined>@" + nick + "</underlined>" + after;
    }

    /** Push quoted meta value (spaces survive as one LP arg). */
    public void sync(Player p) {
        if (getServer().getPluginManager().getPlugin("LuckPerms") == null) return;
        String v = NickValue.build(p.getName(), PlaytimeIndex.hours(index.minutes(p.getUniqueId())));
        try {
            getServer().dispatchCommand(getServer().getConsoleSender(),
                    "lp user " + p.getName() + " meta set nickbtn \"" + v + "\"");
        } catch (Exception ex) {
            getLogger().warning("LuckPerms command failed: " + ex.getMessage());
        }
    }

    private static Sound sound(String name, Sound fallback) {
        try {
            return Sound.valueOf(name.toUpperCase(java.util.Locale.ROOT));
        } catch (Exception e) {
            return fallback;
        }
    }
}
