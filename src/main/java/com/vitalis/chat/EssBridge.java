package com.vitalis.chat;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Essentials bridge via ITS OWN classloader (proven EconomyBridge pattern).
 * Reads User.getReplyTo() reflectively (UUID or uuid-string).
 * Null-safe: any failure -> null (feature quietly stays off).
 */
public final class EssBridge {
    private EssBridge() {}

    public static UUID replyTo(Plugin ess, Player p) {
        try {
            Object user = getUser(ess, p);
            if (user == null) return null;
            Method m = user.getClass().getMethod("getReplyTo");
            Object v = m.invoke(user);
            if (v instanceof UUID u) return u;
            if (v instanceof String s && !s.isEmpty()) {
                try {
                    return UUID.fromString(s);
                } catch (IllegalArgumentException ignored) {}
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static Object getUser(Plugin ess, Player p) {
        Class<?>[] tries = {Player.class, UUID.class, Object.class, String.class};
        Object[] args = {p, p.getUniqueId(), p, p.getName()};
        for (int i = 0; i < tries.length; i++) {
            try {
                Method m = ess.getClass().getMethod("getUser", tries[i]);
                Object u = m.invoke(ess, args[i]);
                if (u != null) return u;
            } catch (Throwable ignored) {}
        }
        return null;
    }
}
