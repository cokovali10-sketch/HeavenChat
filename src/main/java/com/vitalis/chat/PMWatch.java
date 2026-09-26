package com.vitalis.chat;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Incoming PM detector without any Essentials classes (Paper plugins cannot
 * see other plugins' classes here — same lesson as PAPI).
 * Polls Essentials reply-to via reflection; a change means "X wrote to me".
 * Pure change-detection, unit-tested; reflection isolated in one method.
 */
public class PMWatch {
    private final Map<UUID, UUID> lastReplyTo = new HashMap<>();
    private final Map<UUID, Long> lastPing = new HashMap<>();

    /** Returns sender uuid if this is a NEW incoming PM (cooldown respected), else null. */
    public synchronized UUID poll(UUID me, UUID replyTo, long now, long cooldownMs) {
        UUID prev = lastReplyTo.get(me);
        lastReplyTo.put(me, replyTo);
        if (replyTo == null || replyTo.equals(me) || replyTo.equals(prev)) return null;
        Long pinged = lastPing.get(me);
        if (pinged != null && now - pinged < cooldownMs) return null;
        lastPing.put(me, now);
        return replyTo;
    }

    public synchronized void forget(UUID me) {
        lastReplyTo.remove(me);
        lastPing.remove(me);
    }
}
