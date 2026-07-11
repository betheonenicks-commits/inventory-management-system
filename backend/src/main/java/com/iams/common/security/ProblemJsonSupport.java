package com.iams.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iams.common.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.MDC;

/**
 * Writes the same RFC 7807 problem+json shape as ApiExceptionHandler, for the
 * two cases (401/403) that occur in Spring Security's filter chain, before
 * DispatcherServlet - and therefore before @RestControllerAdvice - is reachable.
 */
final class ProblemJsonSupport {

    private static final String PROBLEM_BASE = "https://iams.internal/problems/";

    private ProblemJsonSupport() {
    }

    static void write(HttpServletRequest request, HttpServletResponse response, ObjectMapper mapper,
                       int status, String slug, String title, String detail, String errorCode) throws IOException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", PROBLEM_BASE + slug);
        body.put("title", title);
        body.put("status", status);
        body.put("detail", detail);
        body.put("instance", request.getRequestURI());
        body.put("errorCode", errorCode);
        body.put("traceId", MDC.get(CorrelationIdFilter.MDC_KEY));
        body.put("timestamp", Instant.now().toString());

        response.setStatus(status);
        response.setContentType("application/problem+json");
        response.getWriter().write(mapper.writeValueAsString(body));
    }
}
