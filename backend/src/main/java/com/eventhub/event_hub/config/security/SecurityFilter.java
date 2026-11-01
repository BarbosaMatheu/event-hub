package com.eventhub.event_hub.config.security;

import com.eventhub.event_hub.repositories.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Authenticates a request from its "Authorization: Bearer ..." header. Deliberately NOT a Spring
 * bean: it is created inside SecurityConfig so it runs exactly once, inside the security chain
 * (a Filter bean would also be auto-registered as a plain servlet filter).
 */
public class SecurityFilter extends OncePerRequestFilter {

    private static final String PREFIX = "Bearer ";

    private final TokenService tokenService;
    private final UserRepository userRepository;

    public SecurityFilter(TokenService tokenService, UserRepository userRepository) {
        this.tokenService = tokenService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(PREFIX)) {
            String login = tokenService.validateToken(header.substring(PREFIX.length()).trim());
            if (login != null) {
                // The user is re-read on every request: deleted users lose access immediately.
                userRepository.findByLogin(login).ifPresent(user -> {
                    var authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                });
            }
        }
        chain.doFilter(request, response);
    }
}
