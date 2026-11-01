package com.eventhub.event_hub.exceptions;

/** The requested resource does not exist, or is not visible to the caller (HTTP 404). */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
