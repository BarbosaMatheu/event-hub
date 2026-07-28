package com.eventhub.event_hub.exceptions;

import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import java.time.Instant;
import org.springframework.http.HttpStatus;

@RestControllerAdvice
public class GlobalExceptionHandler {
        @ExceptionHandler(ResourceNotFoundException.class)
        public ResponseEntity<StandardError> handleResourceNotFoundException(ResourceNotFoundException ex, HttpServletRequest request) {
            HttpStatus status = HttpStatus.NOT_FOUND;

            StandardError error = new StandardError(
                    Instant.now(),
                    status.value(),
                    "Resource Not Found",
                    ex.getMessage(),
                    request.getRequestURI()
            );
            
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
}
