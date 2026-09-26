package com.vitalis.chat.logic;

import com.vitalis.chat.NickValue;
import com.vitalis.chat.PlaytimeIndex;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Nick value parses as MiniMessage; playtime index reads real players.yml shape. */
class ChatTest {

    @Test
    void valueParsesAndShowsNick() {
        String v = NickValue.build("Kaito21243", "1.5 ч");
        Component c = assertDoesNotThrow(() -> MiniMessage.miniMessage().deserialize(v));
        assertEquals("Kaito21243", PlainTextComponentSerializer.plainText().serialize(c));
    }

    @Test
    void valueHasClickAndHover() {
        String v = NickValue.build("Arctusss", "90 мин");
        assertTrue(v.contains("<click:suggest_command:'/w Arctusss '>"), v);
        assertTrue(v.contains("<hover:show_text:'"), v);
        assertTrue(v.contains("90 мин"), v);
        assertTrue(v.length() < 200);
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
                uuid + ":\n  playMinutes: 86\n  broken: 1\n"
                        + "not-a-uuid:\n  playMinutes: 5\n");
        YamlConfiguration y = YamlConfiguration.loadConfiguration(tmp);
        Map<UUID, Long> m = PlaytimeIndex.parse(y);
        assertEquals(1, m.size());
        assertEquals(86L, m.get(UUID.fromString(uuid)));
        tmp.delete();
    }
}
