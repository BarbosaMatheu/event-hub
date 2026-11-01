package com.eventhub.event_hub.api;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tiny HTTP client for the integration tests. It talks to the real embedded server over a real
 * socket, so security filters, JSON mapping and error handling are all exercised end to end.
 * Uses only the JDK, so the tests do not depend on Spring's test-client APIs.
 */
public class ApiClient {

    /** Status, body and headers of one response, with a few helpers to read JSON fields. */
    public record Response(int status, String body, HttpHeaders headers) {

        /** First string value of {@code field} anywhere in the body, or null. */
        public String str(String field) {
            Matcher m = Pattern.compile("\"" + Pattern.quote(field) + "\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(body);
            return m.find() ? m.group(1) : null;
        }

        /** First integer value of {@code field} anywhere in the body, or null. */
        public Long num(String field) {
            Matcher m = Pattern.compile("\"" + Pattern.quote(field) + "\"\\s*:\\s*(-?\\d+)").matcher(body);
            return m.find() ? Long.valueOf(m.group(1)) : null;
        }

        /** First decimal value of {@code field} anywhere in the body, or null. */
        public BigDecimal dec(String field) {
            Matcher m = Pattern.compile("\"" + Pattern.quote(field) + "\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)").matcher(body);
            return m.find() ? new BigDecimal(m.group(1)) : null;
        }

        public boolean contains(String text) {
            return body.contains(text);
        }
    }

    private final HttpClient http = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
    private final String baseUrl;

    public ApiClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public Response get(String path, String token) {
        return send("GET", path, token, null);
    }

    public Response post(String path, String json, String token) {
        return send("POST", path, token, json);
    }

    public Response put(String path, String json, String token) {
        return send("PUT", path, token, json);
    }

    public Response delete(String path, String token) {
        return send("DELETE", path, token, null);
    }

    public Response send(String method, String path, String token, String json) {
        return send(method, path, token, json, null, null);
    }

    public Response send(String method, String path, String token, String json, String headerName, String headerValue) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .header("Accept", "application/json");
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        if (headerName != null) {
            builder.header(headerName, headerValue);
        }
        if (json != null) {
            builder.header("Content-Type", "application/json");
            builder.method(method, HttpRequest.BodyPublishers.ofString(json));
        } else {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        }
        try {
            HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            return new Response(response.statusCode(), response.body(), response.headers());
        } catch (IOException e) {
            throw new IllegalStateException("HTTP call failed: " + method + " " + path, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("HTTP call interrupted: " + method + " " + path, e);
        }
    }
}
