package com.vitalis.chat.logic;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Full chat line exactly as OriginChat assembles it:
 * format + LP prefix + nick meta + tint meta + message.
 * Plain text must equal the human-readable line — zero leaks.
 */
class ChatLineTest {
    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private static String plain(String mm) {
        Component c = MINI.deserialize(mm);
        return PlainTextComponentSerializer.plainText().serialize(c);
    }

    private static String line(String prefix, String nick, String tintHex, String msg) {
        String fmt = "<gray>[Global]</gray> <white>PREFIX NICK:</white> <#TINT%>{message}";
        return fmt.replace("PREFIX", prefix)
                .replace("NICK", nick)
                .replace("TINT%", tintHex)
                .replace("{message}", msg);
    }

    @Test
    void fullLineClean() {
        String prefix = "<gradient:#BDBDBD:#FFFFFF>[Новичок]</gradient>";
        String nick = "<click:suggest_command:'/w Kai '><hover:show_text:'В игре: 1.5 ч · Охотник'>Kai</hover></click>";
        assertEquals("[Global] [Новичок] Kai: привет",
                plain(line(prefix, nick, "A1B2C3", "привет")));
    }

    @Test
    void tintOffWhiteClean() {
        String prefix = "<gradient:#FFD700:#8C6D00>[Богач]</gradient>";
        String nick = "<click:suggest_command:'/w Bob '><hover:show_text:'Наиграно:45мин'>Bob</hover></click>";
        assertEquals("[Global] [Богач] Bob: ку",
                plain(line(prefix, nick, "FFFFFF", "ку")));
    }

    @Test
    void groupPrefixLegacyClean() {
        // group prefix with legacy & codes (OriginChat converts legacy first, like Msg does)
        String fmt = "<gray>[Global]</gray> <white>&c[Админ] NICK:</white> <#TINT%>{message}";
        String nick = "<click:suggest_command:'/w A '><hover:show_text:'Наиграно:2ч'>A</hover></click>";
        String mm = fmt.replace("&c", "<red>")
                .replace("NICK", nick).replace("TINT%", "FFFFFF").replace("{message}", "да");
        assertEquals("[Global] [Админ] A: да", plain(mm));
    }
}
