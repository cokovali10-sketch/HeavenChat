package com.vitalis.chat.logic;

import com.vitalis.chat.WelcomeText;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Welcome line templating (pure logic). */
class WelcomeTextTest {

    @Test
    void replacesAllPlaceholders() {
        String out = WelcomeText.render("[+] %player% зашёл (%online%/%max%)", "Kai", 5, 20);
        assertEquals("[+] Kai зашёл (5/20)", out);
    }

    @Test
    void nullTemplateIsEmpty() {
        assertEquals("", WelcomeText.render(null, "Kai", 1, 20));
    }

    @Test
    void textWithoutPlaceholdersIsUnchanged() {
        assertEquals("просто текст", WelcomeText.render("просто текст", "Kai", 1, 20));
    }

    @Test
    void nullPlayerBecomesEmpty() {
        assertEquals("привет !", WelcomeText.render("привет %player%!", null, 0, 20));
    }
}
