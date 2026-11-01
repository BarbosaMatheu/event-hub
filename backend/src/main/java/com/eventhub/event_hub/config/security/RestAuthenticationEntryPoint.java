package com.eventhub.event_hub.config.security;

import com.eventhub.event_hub.utils.JsonText;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

/** 401 in the same JSON shape as every other error (instead of an empty body). */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        write(response, request.getRequestURI());
    }

    static void write(HttpServletResponse response, String path) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"timestamp\":\"" + Instant.now() + "\",\"status\":401,"
                + "\"error\":\"Unauthorized\",\"message\":\"Authentication is required to access this resource\","
                + "\"path\":\"" + JsonText.escape(path) + "\"}");
    }
}
