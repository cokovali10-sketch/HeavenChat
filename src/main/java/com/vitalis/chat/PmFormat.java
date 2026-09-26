package com.vitalis.chat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;

/**
 * PM message components. Pure logic, unit-tested.
 * Everything is built in code (Adventure) — no placeholders, no console
 * commands, no whitespace restrictions. Renders natively, nothing to break.
 */
public final class PmFormat {
    private PmFormat() {}

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    /** "В игре: 86 мин" / "В игре: 1.5 ч" (+ титул строкой ниже, если есть). */
    public static String hoverText(String titleName, long playMinutes) {
        String h = PlaytimeIndex.hours(playMinutes);
        if (titleName != null && !titleName.isEmpty()) {
            return "Титул: " + titleName + "\nВ игре: " + h;
        }
        return "В игре: " + h;
    }

    /** Sender nick part: hover (title + playtime), click suggests "/w Nick ". */
    public static Component nickPart(String nick, String titleName, long playMinutes) {
        return Component.text(nick)
                .color(NamedTextColor.WHITE)
                .hoverEvent(HoverEvent.showText(Component.text(hoverText(titleName, playMinutes))))
                .clickEvent(ClickEvent.suggestCommand("/w " + nick + " "));
    }

    /** Full incoming line: [ЛС] Nick → тебе: text [Ответить] (click answers). */
    public static Component incoming(String fromNick, String titleName, long playMinutes, String text) {
        return Component.text("[ЛС] ", NamedTextColor.GRAY)
                .append(nickPart(fromNick, titleName, playMinutes))
                .append(Component.text(" → тебе: ", NamedTextColor.GRAY))
                .append(Component.text(text, NamedTextColor.WHITE))
                .append(Component.text(" "))
                .append(Component.text("[Ответить]", NamedTextColor.GREEN)
                        .hoverEvent(HoverEvent.showText(Component.text("Клик — ответить")))
                        .clickEvent(ClickEvent.suggestCommand("/w " + fromNick + " ")));
    }

    /** Outgoing line: [ЛС] ты → Nick: text. */
    public static Component outgoing(String toNick, String text) {
        return Component.text("[ЛС] ", NamedTextColor.GRAY)
                .append(Component.text("ты", NamedTextColor.GRAY))
                .append(Component.text(" → ", NamedTextColor.GRAY))
                .append(Component.text(toNick, NamedTextColor.WHITE))
                .append(Component.text(": ", NamedTextColor.GRAY))
                .append(Component.text(text, NamedTextColor.WHITE));
    }

    /** Safe MiniMessage just for the gradient prefix (proven tags only). */
    public static Component prefix(String mini) {
        try {
            return MINI.deserialize(mini);
        } catch (Exception e) {
            return Component.text("[Чат] ", NamedTextColor.GRAY);
        }
    }
}
