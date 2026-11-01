package com.eventhub.event_hub.controllers;

import com.eventhub.event_hub.dtos.AuthResponse;
import com.eventhub.event_hub.dtos.LoginRequest;
import com.eventhub.event_hub.dtos.RegisterRequest;
import com.eventhub.event_hub.dtos.UserResponse;
import com.eventhub.event_hub.models.User;
import com.eventhub.event_hub.services.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Registration, login and the current user")
public class AuthenticationController {

    private final AuthService authService;

    public AuthenticationController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Create a regular user account",
            description = "Always creates a USER. Returns 409 if the login is already taken.")
    public ResponseEntity<UserResponse> register(@RequestBody @Valid RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate and receive a JWT",
            description = "Returns 401 for bad credentials and 429 after too many failed attempts.")
    public AuthResponse login(@RequestBody @Valid LoginRequest request, HttpServletRequest http) {
        return authService.login(request, http.getRemoteAddr());
    }

    @GetMapping("/me")
    @Operation(summary = "The authenticated user")
    public UserResponse me(@AuthenticationPrincipal User currentUser) {
        return UserResponse.from(currentUser);
    }
}
