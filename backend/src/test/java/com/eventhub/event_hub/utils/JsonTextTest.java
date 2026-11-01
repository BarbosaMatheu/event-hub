package com.eventhub.event_hub.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonTextTest {

    @Test
    void escapesQuotesBackslashesAndControlCharacters() {
        assertEquals("a\\\"b\\\\c\\nd", JsonText.escape("a\"b\\c\nd"));
        assertEquals("\\u0001", JsonText.escape("\u0001"));
    }

    @Test
    void nullBecomesEmpty() {
        assertEquals("", JsonText.escape(null));
    }
}
