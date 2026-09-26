package com.vitalis.chat.logic;

import com.vitalis.chat.PlaytimeIndex;
import com.vitalis.chat.PmFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** PM components render as components (no string parsing involved). */
class PmTest {

    private static String plain(Component c) {
        return PlainTextComponentSerializer.plainText().serialize(c);
    }

    @Test
    void incomingRenders() {
        Component c = PmFormat.incoming("Kaito", "Охотник", 90, "привет");
        assertDoesNotThrow(() -> MiniMessage.miniMessage().serialize(c));
        String p = plain(c);
        assertTrue(p.contains("Kaito"), p);
        assertTrue(p.contains("привет"), p);
        assertTrue(p.contains("[Ответить]"), p);
    }

    @Test
    void outgoingRenders() {
        Component c = PmFormat.outgoing("Kaito", "ку");
        assertTrue(plain(c).contains("Kaito"));
    }

    @Test
    void hoverText() {
        assertEquals("В игре: 45 мин", PmFormat.hoverText("", 45));
        assertEquals("Титул: Охотник\nВ игре: 1.5 ч", PmFormat.hoverText("Охотник", 90));
    }

    @Test
    void hoursText() {
        assertEquals("45 мин", PlaytimeIndex.hours(45));
        assertEquals("2 ч", PlaytimeIndex.hours(120));
        assertEquals("1.5 ч", PlaytimeIndex.hours(90));
    }

    @Test
    void parsesRealPlayersShape() throws Exception {
        File tmp = File.createTempFile("players", ".yml");
        String uuid = UUID.randomUUID().toString();
        java.nio.file.Files.writeString(tmp.toPath(),
                uuid + ":\n  playMinutes: 86\n  activeTitle: hunter\n  broken: 1\n"
                        + "not-a-uuid:\n  playMinutes: 5\n");
        YamlConfiguration y = YamlConfiguration.loadConfiguration(tmp);
        Map<UUID, Long> m = PlaytimeIndex.parse(y);
        assertEquals(1, m.size());
        assertEquals(86L, m.get(UUID.fromString(uuid)));
        tmp.delete();
    }
}
