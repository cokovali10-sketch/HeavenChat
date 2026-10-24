package com.vitalis.chat;

/**
 * Welcome/quit line templating. Pure logic (no Bukkit) so it can be unit-tested.
 * Supported placeholders: %player% %online% %max%.
 */
public final class WelcomeText {

    private WelcomeText() {
    }

    public static String render(String template, String player, int online, int max) {
        if (template == null) {
            return "";
        }
        return template
                .replace("%player%", player == null ? "" : player)
                .replace("%online%", Integer.toString(Math.max(0, online)))
                .replace("%max%", Integer.toString(Math.max(0, max)));
    }
}
