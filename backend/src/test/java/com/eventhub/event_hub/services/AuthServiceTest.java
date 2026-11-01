package com.eventhub.event_hub.services;

import com.eventhub.event_hub.config.security.LoginRateLimiter;
import com.eventhub.event_hub.config.security.TokenService;
import com.eventhub.event_hub.dtos.AuthResponse;
import com.eventhub.event_hub.dtos.LoginRequest;
import com.eventhub.event_hub.dtos.RegisterRequest;
import com.eventhub.event_hub.dtos.UserResponse;
import com.eventhub.event_hub.enums.UserRole;
import com.eventhub.event_hub.exceptions.ConflictException;
import com.eventhub.event_hub.exceptions.TooManyRequestsException;
import com.eventhub.event_hub.models.User;
import com.eventhub.event_hub.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private TokenService tokenService;
    @Mock private LoginRateLimiter rateLimiter;
    @InjectMocks private AuthService service;

    // ------------------------------------------------------------------ register

    @Test
    void registerAlwaysCreatesARegularUserWithAHashedPassword() {
        when(userRepository.existsByLogin("ann")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("HASHED");
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse response = service.register(new RegisterRequest("Ann", "password123"));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(saved.capture());
        assertEquals("ann", saved.getValue().getLogin());
        assertEquals("HASHED", saved.getValue().getPassword());
        assertEquals(UserRole.USER, saved.getValue().getRole());
        assertEquals(UserRole.USER, response.role());
    }

    @Test
    void registerRejectsAnExistingLogin() {
        when(userRepository.existsByLogin("ann")).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.register(new RegisterRequest("ann", "password123")));
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void registerTurnsARaceOnTheUniqueConstraintIntoAConflict() {
        when(userRepository.existsByLogin("ann")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("HASHED");
        when(userRepository.saveAndFlush(any(User.class))).thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThrows(ConflictException.class, () -> service.register(new RegisterRequest("ann", "password123")));
    }

    // ------------------------------------------------------------------ login

    @Test
    void loginReturnsATokenAndClearsTheFailureCounter() {
        User user = new User("ann", "HASHED", UserRole.USER);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(user);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(tokenService.generateToken(user)).thenReturn("jwt-token");
        when(tokenService.getExpirationSeconds()).thenReturn(3600L);

        AuthResponse response = service.login(new LoginRequest("ANN", "password123"), "10.0.0.1");

        assertEquals("jwt-token", response.token());
        assertEquals("Bearer", response.tokenType());
        assertEquals(3600L, response.expiresInSeconds());
        assertEquals("ann", response.user().login());
        verify(rateLimiter).reset("ann|10.0.0.1");
        verify(rateLimiter, never()).recordFailure(any());
    }

    @Test
    void failedLoginIsRecordedAndTheExceptionPropagates() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("bad"));

        assertThrows(BadCredentialsException.class,
                () -> service.login(new LoginRequest("ann", "wrong-password"), "10.0.0.1"));

        verify(rateLimiter).recordFailure("ann|10.0.0.1");
        verify(rateLimiter, never()).reset(any());
        verifyNoInteractions(tokenService);
    }

    @Test
    void blockedClientsNeverReachTheAuthenticationManager() {
        doThrow(new TooManyRequestsException("slow down", 60)).when(rateLimiter).assertNotBlocked("ann|10.0.0.1");

        assertThrows(TooManyRequestsException.class,
                () -> service.login(new LoginRequest("ann", "password123"), "10.0.0.1"));

        verifyNoInteractions(authenticationManager);
    }
}
