package com.fininsight.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class CorrelationIdFilterTest {

    private CorrelationIdFilter filter;

    @BeforeEach
    public void setUp() {
        filter = new CorrelationIdFilter();
        MDC.clear();
    }

    @Test
    @DisplayName("CorrelationIdFilter preserves valid incoming correlation ID in response header and cleans up MDC")
    void testPreservesIncomingCorrelationId() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, "req-12345-abc");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER)).isEqualTo("req-12345-abc");
        assertThat(MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID_KEY)).isNull(); // Cleaned up in finally
    }

    @Test
    @DisplayName("CorrelationIdFilter generates UUID if header is missing")
    void testGeneratesNewCorrelationIdWhenMissing() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        String generatedHeader = response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);
        assertThat(generatedHeader).isNotBlank();
        assertThat(generatedHeader).hasSize(36); // Standard UUID length
        assertThat(MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID_KEY)).isNull();
    }

    @Test
    @DisplayName("CorrelationIdFilter generates new UUID if incoming header contains invalid characters")
    void testReplacesInvalidCorrelationId() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, "<script>alert(1)</script>");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        String generatedHeader = response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);
        assertThat(generatedHeader).doesNotContain("<script>");
        assertThat(generatedHeader).hasSize(36);
    }
}
