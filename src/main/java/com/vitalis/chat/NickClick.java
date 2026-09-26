package com.vitalis.chat;

/**
 * Public-chat nick via LuckPerms META, set through the LP API
 * (console splits whitespace — API has no such limit).
 * <click:suggest_command:'/w Nick '><hover:show_text:'В игре: 90 ч · Охотник'>Nick</hover></click>
 * Pure logic, unit-tested. Kept under LP H2 VARCHAR(200).
 */
public final class NickClick {
    private NickClick() {}

    public static String build(String nick, long playMinutes, String titleName) {
        String safeNick = nick.replace("\"", "");
        String hours = PlaytimeIndex.hours(playMinutes);
        String hov = "В игре: " + hours;
        if (titleName != null && !titleName.isEmpty()) hov += " · " + titleName;
        return "<click:suggest_command:'/w " + safeNick + " '>"
                + "<hover:show_text:'" + hov + "'>"
                + safeNick + "</hover></click>";
    }
}
