package com.eventhub.event_hub.utils;

import java.util.Locale;

/** Logins are case-insensitive: they are always stored and looked up in normalised form. */
public final class Logins {

    private Logins() {}

    public static String normalize(String login) {
        return login == null ? null : login.trim().toLowerCase(Locale.ROOT);
    }
}
