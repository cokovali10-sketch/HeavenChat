package com.vitalis.chat;

import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * LuckPerms writer via ITS OWN classloader (proven EconomyBridge pattern).
 * Console commands split values on whitespace, so anything with spaces
 * (click commands, readable hover text) must go through the API instead.
 * All reflection, all guarded: any mismatch -> ready()==false, silent off.
 */
public class LpApi {
    private final Plugin plugin;
    private ClassLoader lpl;
    private Object userManager;
    private Method mGetUser;
    private Method mSaveUser;
    private Method mMetaBuilder;
    private Method mData;
    private Method mAdd;
    private boolean ready;

    public LpApi(Plugin plugin) {
        this.plugin = plugin;
    }

    /** Resolve everything once. Safe to call repeatedly (cached). */
    public synchronized boolean init() {
        if (ready) return true;
        try {
            Plugin lpPlugin = plugin.getServer().getPluginManager().getPlugin("LuckPerms");
            if (lpPlugin == null || !lpPlugin.isEnabled()) return false;
            lpl = lpPlugin.getClass().getClassLoader();
            Class<?> provider = Class.forName("net.luckperms.api.LuckPermsProvider", true, lpl);
            Object lp = provider.getMethod("get").invoke(null);
            userManager = lp.getClass().getMethod("getUserManager").invoke(lp);
            mGetUser = userManager.getClass().getMethod("getUser", UUID.class);
            Class<?> userClass = Class.forName("net.luckperms.api.model.user.User", true, lpl);
            mSaveUser = findMethod(userManager.getClass(), "saveUser", userClass);
            Class<?> metaNode = Class.forName("net.luckperms.api.node.types.MetaNode", true, lpl);
            mMetaBuilder = metaNode.getMethod("builder", String.class, String.class);
            mData = userClass.getMethod("data");
            Class<?> nodeClass = Class.forName("net.luckperms.api.node.Node", true, lpl);
            mAdd = null;
            ready = true;
            // data().add(Node): resolve lazily per user (impl class varies)
            addHelper = nodeClass;
        } catch (Throwable t) {
            ready = false;
            plugin.getLogger().warning("LuckPerms API unavailable, nick meta off: " + t.getMessage());
        }
        return ready;
    }

    private Class<?> addHelper;

    private Method findMethod(Class<?> c, String name, Class<?> param) {
        try {
            return c.getMethod(name, param);
        } catch (Throwable ignored) {
            return null;
        }
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
            Object node = builder.getClass().getMethod("build").invoke(builder);
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

    /** Remove all nodes of one meta key (MetaNode.getKey() is "meta.<key>"). */
    private void removeKey(Object data, String key) {
        try {
            Method toCollection = null;
            for (Method m : data.getClass().getMethods()) {
                if (m.getName().equals("toCollection") && m.getParameterCount() == 0) {
                    toCollection = m;
                    break;
                }
            }
            if (toCollection == null) return;
            Object col = toCollection.invoke(data);
            if (!(col instanceof java.util.Collection)) return;
            java.util.List<Object> victims = new java.util.ArrayList<>();
            for (Object node : (java.util.Collection<?>) col) {
                try {
                    // MetaNode.getMetaKey() is exact; getKey() fallback ("meta.<key>")
                    Object mk = null;
                    try {
                        mk = node.getClass().getMethod("getMetaKey").invoke(node);
                    } catch (Throwable ignored) {}
                    if (key.equals(mk)) {
                        victims.add(node);
                        continue;
                    }
                    Object k = node.getClass().getMethod("getKey").invoke(node);
                    if (k != null && (k.equals("meta." + key) || k.equals(key))) victims.add(node);
                } catch (Throwable ignored) {}
            }
            if (victims.isEmpty()) return;
            Method remover = null;
            for (Method m : data.getClass().getMethods()) {
                if ((m.getName().equals("remove") || m.getName().equals("removeExact"))
                        && m.getParameterCount() == 1) {
                    remover = m;
                    break;
                }
            }
            if (remover == null) return;
            for (Object v : victims) {
                try {
                    remover.invoke(data, v);
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
    }

    private Method findAdd(Object data) {
        for (Method m : data.getClass().getMethods()) {
            if (!m.getName().equals("add") || m.getParameterCount() != 1) continue;
            if (addHelper != null && m.getParameterTypes()[0].isAssignableFrom(addHelper)) return m;
            // accept (Node) or supertypes implemented by MetaNode
            try {
                if (m.getParameterTypes()[0].isAssignableFrom(
                        Class.forName("net.luckperms.api.node.types.MetaNode", true, lpl))) return m;
            } catch (Throwable ignored) {}
        }
        return null;
    }
}
