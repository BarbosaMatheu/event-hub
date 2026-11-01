package com.eventhub.event_hub.services;

import com.eventhub.event_hub.config.security.LoginRateLimiter;
import com.eventhub.event_hub.config.security.TokenService;
import com.eventhub.event_hub.dtos.AuthResponse;
import com.eventhub.event_hub.dtos.LoginRequest;
import com.eventhub.event_hub.dtos.RegisterRequest;
import com.eventhub.event_hub.dtos.UserResponse;
import com.eventhub.event_hub.enums.UserRole;
import com.eventhub.event_hub.exceptions.ConflictException;
import com.eventhub.event_hub.models.User;
import com.eventhub.event_hub.repositories.UserRepository;
import com.eventhub.event_hub.utils.Logins;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final LoginRateLimiter rateLimiter;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       TokenService tokenService,
                       LoginRateLimiter rateLimiter) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
        this.rateLimiter = rateLimiter;
    }

    /** Public registration always creates a regular user; admins are bootstrapped, never self-registered. */
    @Transactional
    public UserResponse register(RegisterRequest request) {
        String login = Logins.normalize(request.login());
        if (userRepository.existsByLogin(login)) {
            throw new ConflictException("This login is already in use");
        }
        try {
            User saved = userRepository.saveAndFlush(
                    new User(login, passwordEncoder.encode(request.password()), UserRole.USER));
            log.info("New user registered: {}", saved.getLogin());
            return UserResponse.from(saved);
        } catch (DataIntegrityViolationException e) {
            // Two simultaneous registrations of the same login: the unique constraint decides.
            throw new ConflictException("This login is already in use");
        }
    }

    public AuthResponse login(LoginRequest request, String clientIp) {
        String login = Logins.normalize(request.login());
        String rateKey = login + "|" + clientIp;

        rateLimiter.assertNotBlocked(rateKey);

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(login, request.password()));
        } catch (AuthenticationException e) {
            rateLimiter.recordFailure(rateKey);
            log.warn("Failed login attempt for '{}' from {}", login, clientIp);
            throw e;
        }
        rateLimiter.reset(rateKey);

        User user = (User) authentication.getPrincipal();
        return new AuthResponse(tokenService.generateToken(user), "Bearer",
                tokenService.getExpirationSeconds(), UserResponse.from(user));
    }
}
