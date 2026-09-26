package com.vitalis.chat;

/**
 * Clickable nick meta value. Pure logic, unit-tested.
 * <click:suggest_command:'/w Nick '><hover:show_text:'В игре: X'>Nick</hover></click>
 */
public final class NickValue {
    private NickValue() {}

    public static String build(String nick, String hoursText) {
        String safeNick = nick.replace("\"", "");
        String safeHours = hoursText.replace("\"", "").replace("'", "");
        return "<click:suggest_command:'/w " + safeNick + " '>"
                + "<hover:show_text:'<gray>В игре: <white>" + safeHours + "</gray>'>"
                + safeNick + "</hover></click>";
    }
}
