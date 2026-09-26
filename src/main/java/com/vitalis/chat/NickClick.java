package com.vitalis.chat;

/**
 * Public-chat nick via LuckPerms META, set through the LP API.
 * Proven by live matrix test: OriginChat parses COLOR tags in values,
 * but strips event tags (click/hover). So the nick carries the ACTIVE
 * TITLE GRADIENT — nothing else. Click/hover live in /w PMs instead.
 * <gradient:#FROM:#TO>Nick</gradient> (or plain Nick without a title).
 * Pure logic, unit-tested. Short, no whitespace issues either way.
 */
public final class NickClick {
    private NickClick() {}

    public static String build(String nick, String fromHex, String toHex) {
        String safeNick = nick.replace("\"", "");
        if (fromHex == null || toHex == null) return safeNick;
        return "<gradient:" + fromHex + ":" + toHex + ">" + safeNick + "</gradient>";
    }
}
