package com.taskmanagement.adapter.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmanagement.adapter.api.dto.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Optional API key authentication.
 *
 * <p>Active only when the {@code api.key} property ({@code API_KEY} environment
 * variable) is set. Health endpoints remain public so container and load-balancer
 * probes keep working.</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class ApiKeyFilter extends OncePerRequestFilter {
    public static final String API_KEY_HEADER = "X-API-Key";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final byte[] configuredKey;

    public ApiKeyFilter(@Value("${api.key:}") String apiKey) {
        this.configuredKey = apiKey == null ? new byte[0] : apiKey.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return configuredKey.length == 0
            || "OPTIONS".equalsIgnoreCase(request.getMethod())
            || request.getRequestURI().startsWith("/actuator/health");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String providedKey = request.getHeader(API_KEY_HEADER);
        if (providedKey == null
            || !MessageDigest.isEqual(configuredKey, providedKey.getBytes(StandardCharsets.UTF_8))) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            OBJECT_MAPPER.writeValue(response.getWriter(), ApiResponse.failure(
                "Unauthorized", "UNAUTHORIZED", "Missing or invalid API key"));
            return;
        }
        filterChain.doFilter(request, response);
    }
}
