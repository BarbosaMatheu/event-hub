package com.eventhub.event_hub.exceptions;

import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import jakarta.servlet.http.HttpServletRequest;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {
        
        @ExceptionHandler(ResourceNotFoundException.class)
        public ResponseEntity<StandardError> handleResourceNotFoundException(ResourceNotFoundException e, HttpServletRequest request) {
            HttpStatus status = HttpStatus.NOT_FOUND;

            StandardError error = new StandardError(
                    Instant.now(),
                    status.value(),
                    "Resource Not Found",
                    e.getMessage(),
                    request.getRequestURI()
            );
            
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }

        @ExceptionHandler(BusinessException.class)
        public ResponseEntity<StandardError> handleBussinesException(BusinessException e, HttpServletRequest request) {
                HttpStatus status = HttpStatus.BAD_REQUEST;
                StandardError err = new StandardError(
                        Instant.now(),
                        status.value(),
                        "Business Rule Violation",
                        e.getMessage(),
                        request.getRequestURI()
                );
                return ResponseEntity.status(status).body(err);
        }
}
