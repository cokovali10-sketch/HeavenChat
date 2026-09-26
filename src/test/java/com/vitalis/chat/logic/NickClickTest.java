package com.vitalis.chat.logic;

import com.vitalis.chat.NickClick;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Gradient nick: parses, shows nick, fits LP storage. Colors only (events stripped by chat). */
class NickClickTest {

    @Test
    void parsesAndShowsNick() {
        String v = NickClick.build("Kaito21243", "#FF0000", "#00FF00");
        Component c = assertDoesNotThrow(() -> MiniMessage.miniMessage().deserialize(v));
        assertEquals("Kaito21243", PlainTextComponentSerializer.plainText().serialize(c));
    }

    @Test
    void plainWithoutTitle() {
        assertEquals("Bob", NickClick.build("Bob", null, null));
    }

    @Test
    void shortEnoughForLp() {
        assertTrue(NickClick.build("VeryLongNickName123", "#BDBDBD", "#FFFFFF").length() < 200);
    }

    @Test
    void quotesStripped() {
        assertFalse(NickClick.build("a\"b", "#FFF", "#000").contains("\""));
    }

    @Test
    void noEventTags() {
        // chat strips events: value must carry zero click/hover tags
        String v = NickClick.build("Kai", "#111111", "#222222");
        assertFalse(v.contains("click"));
        assertFalse(v.contains("hover"));
    }
}
