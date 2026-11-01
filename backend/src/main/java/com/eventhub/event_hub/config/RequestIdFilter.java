package com.eventhub.event_hub.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Gives every request a correlation id (X-Request-Id), exposes it in the log pattern and in the
 * response, and writes one access-log line per request. Lets a user report "request abc123
 * failed" and lets you find every log line of that request.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Request-Id";
    private static final Logger log = LoggerFactory.getLogger(RequestIdFilter.class);
    private static final Pattern SAFE_ID = Pattern.compile("^[A-Za-z0-9._-]{1,64}$");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String incoming = request.getHeader(HEADER);
        String requestId = (incoming != null && SAFE_ID.matcher(incoming).matches())
                ? incoming
                : UUID.randomUUID().toString();

        MDC.put("requestId", requestId);
        response.setHeader(HEADER, requestId);
        long start = System.nanoTime();
        try {
            chain.doFilter(request, response);
        } finally {
            String path = request.getRequestURI();
            if (!path.startsWith("/actuator")) {
                long ms = (System.nanoTime() - start) / 1_000_000;
                log.info("{} {} -> {} ({} ms)", request.getMethod(), path, response.getStatus(), ms);
            }
            MDC.remove("requestId");
        }
    }
}
