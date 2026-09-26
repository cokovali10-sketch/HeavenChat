package com.vitalis.chat;

import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * LuckPerms writer via ITS OWN classloader (proven EconomyBridge pattern).
 * Console commands split values on whitespace, so anything with spaces
 * (click commands, readable hover text) must go through the API instead.
 *
 * Two hard lessons baked in:
 *  - LP impl classes are package-private: EVERY reflected method needs
 *    setAccessible(true) or foreign classloaders get IllegalAccessException.
 *  - every call is guarded: any mismatch -> feature quietly stays off.
 */
public class LpApi {
    private final Plugin plugin;
    private ClassLoader lpl;
    private Object userManager;
    private Method mGetUser;
    private Method mSaveUser;
    private Method mMetaBuilder;
    private Method mData;
    private boolean ready;
    private boolean tried;

    public LpApi(Plugin plugin) {
        this.plugin = plugin;
    }

    /** Resolve everything once. Safe to call repeatedly (cached). */
    public synchronized boolean init() {
        if (ready || tried) return ready;
        tried = true;
        try {
            Plugin lpPlugin = plugin.getServer().getPluginManager().getPlugin("LuckPerms");
            if (lpPlugin == null || !lpPlugin.isEnabled()) return false;
            lpl = lpPlugin.getClass().getClassLoader();
            Class<?> provider = Class.forName("net.luckperms.api.LuckPermsProvider", true, lpl);
            Object lp = acc(provider.getMethod("get")).invoke(null);
            userManager = acc(lp.getClass().getMethod("getUserManager")).invoke(lp);
            mGetUser = acc(userManager.getClass().getMethod("getUser", UUID.class));
            Class<?> userClass = Class.forName("net.luckperms.api.model.user.User", true, lpl);
            mSaveUser = acc(find(userManager.getClass(), "saveUser", userClass));
            Class<?> metaNode = Class.forName("net.luckperms.api.node.types.MetaNode", true, lpl);
            mMetaBuilder = acc(metaNode.getMethod("builder", String.class, String.class));
            mData = acc(userClass.getMethod("data"));
            ready = mGetUser != null && mMetaBuilder != null && mData != null;
            if (ready) {
                plugin.getLogger().info("LuckPerms API hooked (nick meta via API).");
            }
        } catch (Throwable t) {
            ready = false;
            plugin.getLogger().warning("LuckPerms API unavailable, nick meta off: " + t.getMessage());
        }
        return ready;
    }

    /**
     * Set a meta key (removes same-key nodes first, then adds — true replace).
     * Spaces allowed — no console involved. Fresh keys avoid legacy junk.
     */
    public boolean setMeta(UUID uuid, String key, String value) {
        if (!init()) return false;
        try {
            Object user = mGetUser.invoke(userManager, uuid);
            if (user == null) return false;
            Object data = mData.invoke(user);
            removeKey(data, key);
            Object builder = mMetaBuilder.invoke(null, key, value);
            Method build = acc(builder.getClass().getMethod("build"));
            Object node = build.invoke(builder);
            Method add = findAdd(data);
            if (add == null) return false;
            add.invoke(data, node);
            if (mSaveUser != null) {
                try {
                    mSaveUser.invoke(userManager, user);
                } catch (Throwable ignored) {}
            }
            return true;
        } catch (Throwable t) {
            plugin.getLogger().warning("LP meta set failed for " + key + ": " + t.getMessage());
            return false;
        }
    }

    /** Remove all nodes of one meta key (exact getMetaKey match, getKey fallback). */
    private void removeKey(Object data, String key) {
        try {
            Method toCollection = acc(find(data.getClass(), "toCollection"));
            if (toCollection == null) return;
            Object col = toCollection.invoke(data);
            if (!(col instanceof Collection)) return;
            List<Object> victims = new ArrayList<>();
            for (Object node : (Collection<?>) col) {
                try {
                    Object mk = null;
                    try {
                        Method gmk = acc(find(node.getClass(), "getMetaKey"));
                        if (gmk != null) mk = gmk.invoke(node);
                    } catch (Throwable ignored) {}
                    if (key.equals(mk)) {
                        victims.add(node);
                        continue;
                    }
                    Method gk = acc(find(node.getClass(), "getKey"));
                    if (gk == null) continue;
                    Object k = gk.invoke(node);
                    if (k != null && (k.equals("meta." + key) || k.equals(key))) victims.add(node);
                } catch (Throwable ignored) {}
            }
            if (victims.isEmpty()) return;
            Method remover = pickSingleParam(data.getClass(), "remove", "removeExact");
            if (remover == null) return;
            for (Object v : victims) {
                try {
                    remover.invoke(data, v);
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
    }

    private Method findAdd(Object data) {
        try {
            Class<?> metaNode = Class.forName("net.luckperms.api.node.types.MetaNode", true, lpl);
            for (Method m : data.getClass().getMethods()) {
                if (!m.getName().equals("add") || m.getParameterCount() != 1) continue;
                try {
                    if (m.getParameterTypes()[0].isAssignableFrom(metaNode)) return acc(m);
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static Method find(Class<?> c, String name, Class<?>... params) {
        try {
            if (params.length == 0) {
                for (Method m : c.getMethods()) {
                    if (m.getName().equals(name) && m.getParameterCount() == 0) return m;
                }
                return null;
            }
            return c.getMethod(name, params);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Method pickSingleParam(Class<?> c, String... names) {
        for (String n : names) {
            for (Method m : c.getMethods()) {
                if (m.getName().equals(n) && m.getParameterCount() == 1) return acc(m);
            }
        }
        return null;
    }

    private static Method acc(Method m) {
        if (m != null) {
            try {
                m.setAccessible(true);
            } catch (Throwable ignored) {}
        }
        return m;
    }
}
