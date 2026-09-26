package com.vitalis.chat.logic;

import com.vitalis.chat.NickClick;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Clickable nick: parses, has click+hover, spaces allowed (API channel), fits LP storage. */
class NickClickTest {

    @Test
    void parsesAndShowsNick() {
        String v = NickClick.build("Kaito21243", 90, "Охотник");
        Component c = assertDoesNotThrow(() -> MiniMessage.miniMessage().deserialize(v));
        assertEquals("Kaito21243", PlainTextComponentSerializer.plainText().serialize(c));
    }

    @Test
    void hasClickSuggestAndHover() {
        String v = NickClick.build("Arctusss", 45, "");
        assertTrue(v.contains("<click:suggest_command:'/w Arctusss '>"), v);
        assertTrue(v.contains("В игре: 45 мин"), v);
    }

    @Test
    void hoverReadableWithSpaces() {
        String v = NickClick.build("Kai", 90, "Охотник");
        assertTrue(v.contains("В игре: 1.5 ч · Охотник"), v);
    }

    @Test
    void shortEnoughForLp() {
        assertTrue(NickClick.build("VeryLongNickName123", 100000, "Путешественник Нижнего Мира").length() < 200);
    }

    @Test
    void quotesStripped() {
        assertFalse(NickClick.build("a\"b", 10, null).contains("\""));
    }
}
