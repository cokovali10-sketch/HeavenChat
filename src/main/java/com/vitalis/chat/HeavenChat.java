package com.vitalis.chat;

import net.kyori.adventure.title.Title;
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
 * HeavenChat v1.2.0.
 * - /w + /r whispers: Adventure components built in code (no placeholders,
 *   no console commands, nothing to misparse). Click [Ответить], hover stats.
 * - @Nick in public chat: underline + ping (highlight kill-switch in config).
 * - NO LuckPerms meta, NO PlaceholderAPI classes: unbreakable by design.
 */
public class HeavenChat extends JavaPlugin implements Listener {
    private final TitlesIndex titles = new TitlesIndex();
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
    private Sound pmSound;
    private float pmVol = 0.6f;
    private float pmPitch = 1.0f;
    private long pmCooldownMs = 2000;
    private String chatPrefix = "<gradient:#7AD9FF:#FFD76A>[Чат]</gradient> <gray>";
    private boolean welcomeEnabled = true;
    private String firstJoinMsg;
    private String joinMsg;
    private String quitMsg;
    private boolean titleEnabled = true;
    private String titleText;
    private String subtitleText;
    private String firstSubtitleText;
    private int titleFadeIn = 10;
    private int titleStay = 70;
    private int titleFadeOut = 20;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();
        refreshMinutes = Math.max(1, getConfig().getInt("general.refresh-minutes", 10));
        chatPrefix = getConfig().getString("general.prefix", chatPrefix);
        mentionsOn = getConfig().getBoolean("mentions.enabled", true);
        highlightOn = getConfig().getBoolean("mentions.highlight", true);
        mentionSound = sound(getConfig().getString("mentions.sound", "BLOCK_NOTE_BLOCK_PLING"),
                Sound.BLOCK_NOTE_BLOCK_PLING);
        mentionVol = (float) getConfig().getDouble("mentions.volume", 0.7);
        mentionPitch = (float) getConfig().getDouble("mentions.pitch", 1.4);
        mentionCdMs = getConfig().getLong("mentions.cooldown-seconds", 5) * 1000L;
        pmSound = sound(getConfig().getString("pm.sound", "ENTITY_EXPERIENCE_ORB_PICKUP"),
                Sound.ENTITY_EXPERIENCE_ORB_PICKUP);
        pmVol = (float) getConfig().getDouble("pm.volume", 0.6);
        pmPitch = (float) getConfig().getDouble("pm.pitch", 1.0);
        pmCooldownMs = getConfig().getLong("pm.cooldown-seconds", 2) * 1000L;
        welcomeEnabled = getConfig().getBoolean("welcome.enabled", true);
        firstJoinMsg = getConfig().getString("welcome.first-join",
                "&8[&a+&8] &6✦ &f%player% &eвпервые на сервере! Поприветствуем!");
        joinMsg = getConfig().getString("welcome.join",
                "&8[&a+&8] &f%player% &7зашёл на сервер &8(&f%online%&7/&f%max%&8)");
        quitMsg = getConfig().getString("welcome.quit",
                "&8[&c-&8] &f%player% &7вышел с сервера");
        titleEnabled = getConfig().getBoolean("welcome.title.enabled", true);
        titleText = getConfig().getString("welcome.title.title",
                "<gradient:#FFD76A:#7AD9FF><bold>✦ ʜᴇᴀᴠᴇɴ ✦</bold></gradient>");
        subtitleText = getConfig().getString("welcome.title.subtitle",
                "<gray>Привет, <#7AD9FF><bold>%player%</bold></gray>");
        firstSubtitleText = getConfig().getString("welcome.title.first-subtitle",
                "<#FFD76A>Добро пожаловать в Heaven!");
        titleFadeIn = getConfig().getInt("welcome.title.fade-in", 10);
        titleStay = getConfig().getInt("welcome.title.stay", 70);
        titleFadeOut = getConfig().getInt("welcome.title.fade-out", 20);

        titles.reload();
        getLogger().info("Titles index: " + titles.titleCount() + " titles.");

        for (Player p : getServer().getOnlinePlayers()) nicks.add(p.getName());

        try {
            registerCommand("w", "Личка: /w <ник> <текст>",
                    java.util.List.of(), new WCommand(this, false));
            registerCommand("r", "Ответить на личку: /r <текст>",
                    java.util.List.of(), new WCommand(this, true));
        } catch (NoSuchMethodError e) {
            getLogger().severe("Brigadier register unavailable, /w disabled.");
        }
        getServer().getPluginManager().registerEvents(this, this);

        // refresh titles/playtime cache (file reads only, feeds PM hover)
        getServer().getScheduler().runTaskTimerAsynchronously(this, titles::reload,
                400L, (long) refreshMinutes * 20L * 60L);

        getLogger().info("HeavenChat enabled: /w /r whispers, mentions.");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        nicks.add(e.getPlayer().getName());
        if (!welcomeEnabled) {
            return;
        }
        Player p = e.getPlayer();
        boolean first = !p.hasPlayedBefore();
        int online = getServer().getOnlinePlayers().size();
        String template = first ? firstJoinMsg : joinMsg;
        e.joinMessage(Msg.color(WelcomeText.render(template, p.getName(),
                online, getServer().getMaxPlayers())));
        if (titleEnabled) {
            final Player pl = p;
            final String ttl = WelcomeText.render(titleText, p.getName(), online, getServer().getMaxPlayers());
            final String sub = WelcomeText.render(first ? firstSubtitleText : subtitleText,
                    p.getName(), online, getServer().getMaxPlayers());
            getServer().getScheduler().runTaskLater(this, () -> {
                if (!pl.isOnline()) return;
                try {
                    Title.Times times = Title.Times.times(
                            java.time.Duration.ofMillis(Math.max(1, titleFadeIn) * 50L),
                            java.time.Duration.ofMillis(Math.max(1, titleStay) * 50L),
                            java.time.Duration.ofMillis(Math.max(1, titleFadeOut) * 50L));
                    pl.showTitle(Title.title(Msg.color(ttl), Msg.color(sub), times));
                } catch (Throwable ignored) {
                }
            }, 10L);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e) {
        nicks.remove(e.getPlayer().getName());
        if (!welcomeEnabled) {
            return;
        }
        Player p = e.getPlayer();
        // the quitting player is still counted in the list -> -1
        int online = Math.max(0, getServer().getOnlinePlayers().size() - 1);
        e.quitMessage(Msg.color(WelcomeText.render(quitMsg, p.getName(),
                online, getServer().getMaxPlayers())));
    }

    /**
     * @Nick highlight + ping. LOWEST so chat formatters apply afterwards.
     * highlight:false = sound only (kill-switch if tags ever print literally).
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
        org.bukkit.Bukkit.getScheduler().runTask(this, () -> {
            try {
                Player t = org.bukkit.Bukkit.getPlayerExact(want);
                if (t != null && t.isOnline()) {
                    t.playSound(t.getLocation(), mentionSound, mentionVol, mentionPitch);
                }
            } catch (Exception ignored) {}
        });
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

    static String escapeTags(String s) {
        return s.replace("<", "&lt;").replace(">", "&gt;");
    }

    public TitlesIndex titles() { return titles; }
    public String chatPrefix() { return chatPrefix; }
    public Sound pmSound() { return pmSound; }
    public float pmVol() { return pmVol; }
    public float pmPitch() { return pmPitch; }
    public long pmCooldownMs() { return pmCooldownMs; }

    private static Sound sound(String name, Sound fallback) {
        try {
            return Sound.valueOf(name.toUpperCase(java.util.Locale.ROOT));
        } catch (Exception e) {
            return fallback;
        }
    }
}
