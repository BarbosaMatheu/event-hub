package com.eventhub.event_hub.config.security;

import com.eventhub.event_hub.utils.JsonText;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

/** 403 in the same JSON shape as every other error. */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"timestamp\":\"" + Instant.now() + "\",\"status\":403,"
                + "\"error\":\"Forbidden\",\"message\":\"You do not have permission to perform this action\","
                + "\"path\":\"" + JsonText.escape(request.getRequestURI()) + "\"}");
    }
}
