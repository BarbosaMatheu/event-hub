package com.eventhub.event_hub.exceptions;

/** The request conflicts with the current state, e.g. duplicate login or sold-out event (HTTP 409). */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
