package com.eventhub.event_hub.exceptions;

/** One invalid field of a request body. */
public record FieldViolation(String field, String message) {}
