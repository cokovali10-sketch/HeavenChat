package com.vitalis.chat;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

/**
 * HeavenChat v1.0.0 - clickable nicks via LuckPerms META.
 * meta nickbtn = clickable nick (click: suggest "/w Nick ", hover: playtime).
 * Chat format uses %luckperms_meta_nickbtn% instead of {player}.
 * Refreshed on join + every 10 minutes.
 */
public class HeavenChat extends JavaPlugin implements Listener {
    private final PlaytimeIndex index = new PlaytimeIndex();
    private int refreshMinutes = 10;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();
        refreshMinutes = Math.max(1, getConfig().getInt("general.refresh-minutes", 10));
        int known = index.reload();
        getLogger().info("Playtime index: " + known + " players.");

        getServer().getPluginManager().registerEvents(this, this);

        // late join sync + periodic refresh (main thread: console commands are queued by LP)
        getServer().getScheduler().runTaskTimer(this, () -> {
            index.reload();
            for (Player p : getServer().getOnlinePlayers()) {
                try {
                    sync(p);
                } catch (Exception ignored) {}
            }
        }, 400L, (long) refreshMinutes * 20L * 60L);

        getLogger().info("HeavenChat enabled: click nick for /w, hover shows playtime.");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
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
        UUID u = e.getPlayer().getUniqueId();
        // keep meta (returns on join); nothing to clean
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

    /** Manual refresh for console/admin use later. */
    public void refreshAll() {
        index.reload();
        for (Player p : Bukkit.getOnlinePlayers()) {
            try {
                sync(p);
            } catch (Exception ignored) {}
        }
    }
}
