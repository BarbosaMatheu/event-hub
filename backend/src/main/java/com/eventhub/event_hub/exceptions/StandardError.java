package com.eventhub.event_hub.exceptions;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/** The single error format returned by every endpoint. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record StandardError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldViolation> violations
) {
    public static StandardError of(int status, String error, String message, String path) {
        return new StandardError(Instant.now(), status, error, message, path, null);
    }
}
