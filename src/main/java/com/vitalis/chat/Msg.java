package com.vitalis.chat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/** Legacy (&) + MiniMessage (<gradient>) in one call. Nothing to misparse. */
public final class Msg {

    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.builder().character('&').hexColors().build();
    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private Msg() {
    }

    public static Component color(String text) {
        if (text == null) {
            return Component.empty();
        }
        if (text.indexOf('<') >= 0 && text.indexOf('>') > 0) {
            try {
                return MINI.deserialize(legacyToMini(text));
            } catch (Exception e) {
                return LEGACY.deserialize(text.replaceAll("<[^>]*>", ""));
            }
        }
        return LEGACY.deserialize(text);
    }

    private static final java.util.Map<Character, String> CODES = new java.util.HashMap<>();

    static {
        String[] names = {
                "black", "dark_blue", "dark_green", "dark_aqua", "dark_red", "dark_purple",
                "gold", "gray", "dark_gray", "blue", "green", "aqua", "red", "light_purple",
                "yellow", "white"
        };
        char[] keys = "0123456789abcdef".toCharArray();
        for (int i = 0; i < keys.length; i++) {
            CODES.put(keys[i], names[i]);
        }
        CODES.put('k', "obfuscated");
        CODES.put('l', "bold");
        CODES.put('m', "strikethrough");
        CODES.put('n', "underlined");
        CODES.put('o', "italic");
        CODES.put('r', "reset");
    }

    private static String legacyToMini(String text) {
        StringBuilder out = new StringBuilder(text.length() + 16);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '&' && i + 1 < text.length()) {
                String tag = CODES.get(Character.toLowerCase(text.charAt(i + 1)));
                if (tag != null) {
                    out.append('<').append(tag).append('>');
                    i++;
                    continue;
                }
            }
            out.append(c);
        }
        return out.toString();
    }
}
