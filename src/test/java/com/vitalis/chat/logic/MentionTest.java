package com.vitalis.chat.logic;

import com.vitalis.chat.Mention;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Mentions, PM change detection, nick value. No server needed. */
class MentionTest {

    @Test
    void findsMentionCaseInsensitive() {
        assertEquals("Kaito21243",
                Mention.find("привет @kaito21243 как дела", List.of("Kaito21243", "Arctusss")));
    }

    @Test
    void longestNickWins() {
        assertEquals("Kaito21243",
                Mention.find("@Kaito21243 привет", List.of("Kaito", "Kaito21243")));
    }

    @Test
    void noFalsePositives() {
        assertNull(Mention.find("просто текст без собаки", List.of("Kai")));
        assertNull(Mention.find("мой@email.com", List.of("mail")));
        assertNull(Mention.find(null, List.of("Kai")));
    }

    @Test
    void nickBoundary() {
        // @Kai followed by more nick chars is not a mention of Kai
        assertNull(Mention.find("@Kaito привет", List.of("Kai")));
    }

    @Test
    void highlightUnderlinesOnly() {
        String out = com.vitalis.chat.HeavenChat.highlight("привет @Kai как дела", "Kai");
        net.kyori.adventure.text.Component c = assertDoesNotThrow(
                () -> net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(out));
        assertEquals("привет @Kai как дела",
                net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                        .plainText().serialize(c));
        assertTrue(out.contains("<underlined>@Kai</underlined>"), out);
        // no color tags: title tint owns the colors
        assertFalse(out.contains("<#"), out);
    }
}
