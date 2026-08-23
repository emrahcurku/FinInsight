package com.fininsight.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Filter that establishes an end-to-end correlation ID for request tracing.
 * Sets the correlation ID in the SLF4J MDC context and HTTP response headers.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String MDC_CORRELATION_ID_KEY = "correlationId";
    private static final Pattern VALID_CORRELATION_ID = Pattern.compile("^[a-zA-Z0-9_-]{1,64}$");

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String correlationId = resolveCorrelationId(request.getHeader(CORRELATION_ID_HEADER));

        try {
            MDC.put(MDC_CORRELATION_ID_KEY, correlationId);
            response.setHeader(CORRELATION_ID_HEADER, correlationId);
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_CORRELATION_ID_KEY);
        }
    }

    public static String getCurrentCorrelationId() {
        String correlationId = MDC.get(MDC_CORRELATION_ID_KEY);
        if (StringUtils.hasText(correlationId) && VALID_CORRELATION_ID.matcher(correlationId.trim()).matches()) {
            return correlationId.trim();
        }
        return UUID.randomUUID().toString();
    }

    private String resolveCorrelationId(String headerValue) {
        if (StringUtils.hasText(headerValue) && VALID_CORRELATION_ID.matcher(headerValue.trim()).matches()) {
            return headerValue.trim();
        }
        return UUID.randomUUID().toString();
    }
}
