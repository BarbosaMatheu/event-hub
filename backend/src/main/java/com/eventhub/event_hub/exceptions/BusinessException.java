package com.eventhub.event_hub.exceptions;

/** A business rule was violated by the request (HTTP 400). */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
