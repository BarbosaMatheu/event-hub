package com.eventhub.event_hub.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EventSpecificationsTest {

    @Test
    void escapesLikeWildcardsSoUserInputIsLiteral() {
        assertEquals("50\\% off", EventSpecifications.escapeLike("50% off"));
        assertEquals("a\\_b", EventSpecifications.escapeLike("a_b"));
        assertEquals("a\\\\b", EventSpecifications.escapeLike("a\\b"));
    }

    @Test
    void leavesPlainTextUntouched() {
        assertEquals("jazz night", EventSpecifications.escapeLike("jazz night"));
    }
}
